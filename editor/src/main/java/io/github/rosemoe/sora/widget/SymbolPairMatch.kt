/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
/*
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.widget

import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine

/**
 * Define symbol pairs to complete them automatically when the user
 * enters the first character of pair.
 *
 * @author Rosemoe
 */
open class SymbolPairMatch(parent: SymbolPairMatch? = null) {

    private val singleCharPairMaps: MutableMap<Char, SymbolPair?> = HashMap()
    private val multipleCharByEndPairMaps: MutableMap<Char, MutableList<SymbolPair>> = HashMap()
    private var parent: SymbolPairMatch? = null

    init {
        setParent(parent)
    }

    protected open fun setParent(parent: SymbolPairMatch?) {
        this.parent = parent
    }

    /**
     * Put a pair of symbol completion.
     * When the user types the singleCharacter, it will be replaced by symbolPair.
     * symbolPair may be null to disable completion for this character.
     *
     * @see SymbolPair
     */
    open fun putPair(singleCharacter: Char, symbolPair: SymbolPair?) {
        singleCharPairMaps[singleCharacter] = symbolPair
    }

    /**
     * Put a pair of symbol completion.
     * When the user types the charArray, it will be replaced by symbolPair.
     *
     * @see SymbolPair
     */
    open fun putPair(charArray: CharArray, symbolPair: SymbolPair) {
        val endChar = charArray[charArray.size - 1]
        val list = multipleCharByEndPairMaps.getOrPut(endChar) { ArrayList() }
        list.add(symbolPair)
    }

    /**
     * Put a pair of symbol completion.
     * When the user types the openString, it will be replaced by symbolPair.
     *
     * @see putPair
     */
    open fun putPair(openString: String, symbolPair: SymbolPair) {
        putPair(openString.toCharArray(), symbolPair)
    }

    fun matchBestPairBySingleChar(editChar: Char): SymbolPair? {
        val pair = singleCharPairMaps[editChar]
        if (pair == null && parent != null) {
            return parent?.matchBestPairBySingleChar(editChar)
        }
        return pair
    }

    fun matchBestPairList(editChar: Char): List<SymbolPair> {
        var result = multipleCharByEndPairMaps[editChar]
        if (result == null && parent != null) {
            val parentResult = parent?.matchBestPairList(editChar)
            if (parentResult != null) {
                result = ArrayList(parentResult)
            }
        }
        return result ?: emptyList()
    }

    fun matchBestPair(
        editor: CodeEditor,
        cursorPosition: CharPosition,
        inputCharArray: CharArray?,
        endChar: Char
    ): SymbolPair? {
        val content = editor.text
        // do not apply single character pairs for text with length > 1
        val singleCharPair = if (inputCharArray == null) matchBestPairBySingleChar(endChar) else null

        // matches single character symbol pair first
        if (singleCharPair != null) {
            singleCharPair.measureCursorPosition(cursorPosition.index)
            return singleCharPair
        }

        // find all possible lists, with a single character for fast search
        val matchList = matchBestPairList(endChar)

        var matchPair: SymbolPair? = null
        for (pair in matchList) {
            if (!pair.shouldReplace(editor)) {
                continue
            }
            val openCharArray = pair.open.toCharArray()

            // if flag is not 1, no match
            var matchFlag = 1
            var insertIndex = cursorPosition.index

            // the size = 1, we need compare characters before cursor, ensure it match the whole open char array
            if (inputCharArray == null) {
                var arrayIndex = openCharArray.size - 2
                while (arrayIndex >= 0) {
                    if (insertIndex > 0) {
                        insertIndex--
                    } else {
                        matchFlag = 0
                        break
                    }
                    val contentChar = content[insertIndex]
                    matchFlag = matchFlag and (if (contentChar == openCharArray[arrayIndex]) 1 else 0)
                    arrayIndex--
                }
            } else {
                // Not fully tested.

                // Not all the time the user will enter a string that matches the symbol pair,
                // such as pasting text,
                // so if the length of the entered string is greater than the length of the symbol pair,
                // the two are considered to be mismatched
                if (inputCharArray.size > openCharArray.size) {
                    continue
                }

                var pairIndex = openCharArray.size - 1

                for (charIndex in inputCharArray.size - 1 downTo 1) {
                    matchFlag = matchFlag and (if (inputCharArray[charIndex] == openCharArray[pairIndex]) 1 else 0)
                    pairIndex--
                }

                // input text and symbol pair text not equal fully, continue compare characters before cursor
                if (matchFlag == 1 && pairIndex > 0) {
                    // When the loop is stopped the character position
                    // is still in the first position of the matched characters,
                    // we need to replace this character,
                    // so we need to subtract a character position
                    insertIndex--

                    while (pairIndex >= 0) {
                        matchFlag = matchFlag and (if (content[insertIndex] == openCharArray[pairIndex]) 1 else 0)
                        insertIndex--
                        pairIndex--
                    }
                }
            }

            if (matchFlag == 1) {
                matchPair = pair
                pair.measureCursorPosition(insertIndex)
                break
            }
        }
        return matchPair
    }

    open fun removeAllPairs() {
        singleCharPairMaps.clear()
        multipleCharByEndPairMaps.clear()
    }

    /**
     * Defines a replacement of input
     */
    open class SymbolPair @JvmOverloads constructor(
        @JvmField val open: String,
        @JvmField val close: String,
        var symbolPairEx: SymbolPairEx? = null
    ) {
        companion object {
            /**
             * Defines that this character does not have to be replaced
             */
            @JvmField
            val EMPTY_SYMBOL_PAIR: SymbolPair = SymbolPair("", "")
        }

        var cursorOffset: Int = 0
            protected set

        var insertOffset: Int = 0
            protected set

        open fun shouldReplace(editor: CodeEditor): Boolean {
            val ex = symbolPairEx ?: return false
            val content = editor.text
            val currentLine = content.getLine(content.cursor.leftLine)
            return ex.shouldReplace(editor, currentLine, content.cursor.leftColumn)
        }

        open fun shouldDoAutoSurround(content: Content): Boolean {
            return symbolPairEx?.shouldDoAutoSurround(content) ?: false
        }

        open fun measureCursorPosition(offsetIndex: Int) {
            cursorOffset = offsetIndex + open.length
            insertOffset = offsetIndex
        }

        interface SymbolPairEx {
            /**
             * The method will be called to decide whether to perform the replacement or not.
             *
             * @param editor      The current edit content
             * @param currentLine The current line edit in the editor, quick analysis it to decide whether to replaced
             * @param leftColumn  return current cursor column
             */
            fun shouldReplace(editor: CodeEditor, contentLine: ContentLine, leftColumn: Int): Boolean = true

            /**
             * when before the replaced and select a range, surrounds the selected content with return true.
             */
            fun shouldDoAutoSurround(content: Content): Boolean = false
        }
    }

    class DefaultSymbolPairs : SymbolPairMatch() {
        init {
            putPair('{', SymbolPair("{", "}"))
            putPair('(', SymbolPair("(", ")"))
            putPair('[', SymbolPair("[", "]"))
            putPair('"', SymbolPair("\"", "\"", object : SymbolPair.SymbolPairEx {
                override fun shouldDoAutoSurround(content: Content): Boolean {
                    return content.cursor.isSelected
                }
            }))
            putPair('\'', SymbolPair("'", "'", object : SymbolPair.SymbolPairEx {
                override fun shouldDoAutoSurround(content: Content): Boolean {
                    return content.cursor.isSelected
                }
            }))
        }
    }
}
