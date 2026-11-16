/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.widget

import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.SymbolPairMatch.SymbolPair

/**
 * Strategy interface to handle auto-deletion of matching symbol pairs on backspace.
 *
 * Allows customizable and pluggable behavior for unpairing brackets, quotes, and symbols.
 *
 * @author Rosemoe
 */
fun interface SymbolPairDeleteHandler {

    /**
     * Determines whether an auto-closing symbol pair should be deleted at the current cursor position.
     *
     * @param editor The current [CodeEditor] instance
     * @param content The text content buffer
     * @param line Current cursor line index
     * @param column Current cursor column index
     * @param symbolPairs Active [SymbolPairMatch] for the editor/language
     * @return [DeleteResult] specifying the exact text range to delete, or null if standard backspace should proceed
     */
    fun shouldDeletePair(
        editor: CodeEditor,
        content: Content,
        line: Int,
        column: Int,
        symbolPairs: SymbolPairMatch
    ): DeleteResult?

    data class DeleteResult(
        val startLine: Int,
        val startColumn: Int,
        val endLine: Int,
        val endColumn: Int
    )
}

/**
 * Default framework implementation of [SymbolPairDeleteHandler].
 *
 * Supports:
 * - Directly adjacent matching pairs: `{|}` -> deletes both open and close symbols.
 * - Whitespace-enclosed pairs: `{|   }` -> deletes open symbol, enclosed whitespace, and close symbol.
 * - Symmetric inner spaces: `{ | }` -> deletes surrounding spaces, reducing to `{|}`.
 * - Multi-character pairs: `"""|"""`, `'''|'''`, etc.
 */
open class DefaultSymbolPairDeleteHandler(
    var allowEnclosedWhitespace: Boolean = true,
    var deleteSymmetricInnerSpaces: Boolean = true
) : SymbolPairDeleteHandler {

    override fun shouldDeletePair(
        editor: CodeEditor,
        content: Content,
        line: Int,
        column: Int,
        symbolPairs: SymbolPairMatch
    ): SymbolPairDeleteHandler.DeleteResult? {
        val lineText = content.getLineString(line)

        // Gather all pairs from override props and language symbolPairs, falling back to defaults
        val allPairs = mutableListOf<SymbolPair>()
        val seen = mutableSetOf<Pair<String, String>>()
        for (p in editor.props.overrideSymbolPairs.getAllPairs()) {
            if (seen.add(Pair(p.open, p.close))) allPairs.add(p)
        }
        for (p in symbolPairs.getAllPairs()) {
            if (seen.add(Pair(p.open, p.close))) allPairs.add(p)
        }
        for (p in SymbolPairMatch.DefaultSymbolPairs().getAllPairs()) {
            if (seen.add(Pair(p.open, p.close))) allPairs.add(p)
        }
        if (allPairs.isEmpty()) return null

        // Sort pairs by open length descending so longer pairs (e.g. """ before ") take priority
        val sortedPairs = allPairs.sortedWith(
            compareByDescending<SymbolPair> { it.open.length }
                .thenByDescending { it.close.length }
        )

        for (pair in sortedPairs) {
            val open = pair.open
            val close = pair.close

            // 1. Direct match or whitespace after open: cursor right after open
            if (column >= open.length && lineText.regionMatches(column - open.length, open, 0, open.length)) {
                var k = 0
                while (column + k < lineText.length && isWhitespaceChar(lineText[column + k])) {
                    k++
                }

                val hasClosingMatch = (column + k + close.length <= lineText.length) &&
                        lineText.regionMatches(column + k, close, 0, close.length)

                if (hasClosingMatch) {
                    if (k == 0) {
                        // Direct adjacency: {|} -> delete both
                        return SymbolPairDeleteHandler.DeleteResult(
                            line,
                            column - open.length,
                            line,
                            column + close.length
                        )
                    } else if (allowEnclosedWhitespace) {
                        // Cursor right after open with only whitespace before close: {|   } -> delete all
                        return SymbolPairDeleteHandler.DeleteResult(
                            line,
                            column - open.length,
                            line,
                            column + k + close.length
                        )
                    }
                }
            }

            // 2. Symmetric inner whitespace: { | } -> delete 1 space on left and 1 space on right
            if (deleteSymmetricInnerSpaces && column > 0 && column < lineText.length) {
                var spacesBefore = 0
                while (column - spacesBefore - 1 >= 0 && lineText[column - spacesBefore - 1] == ' ') {
                    spacesBefore++
                }

                var spacesAfter = 0
                while (column + spacesAfter < lineText.length && lineText[column + spacesAfter] == ' ') {
                    spacesAfter++
                }

                if (spacesBefore > 0 && spacesAfter > 0 && spacesBefore == spacesAfter) {
                    val openStart = column - spacesBefore - open.length
                    val closeEnd = column + spacesAfter + close.length
                    if (openStart >= 0 && closeEnd <= lineText.length) {
                        val matchesOpen = lineText.regionMatches(openStart, open, 0, open.length)
                        val matchesClose = lineText.regionMatches(column + spacesAfter, close, 0, close.length)
                        if (matchesOpen && matchesClose) {
                            return SymbolPairDeleteHandler.DeleteResult(
                                line,
                                column - 1,
                                line,
                                column + 1
                            )
                        }
                    }
                }
            }
        }

        return null
    }

    private fun isWhitespaceChar(c: Char): Boolean = c == ' ' || c == '\t'
}
