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
package io.github.rosemoe.sora.langs.textmate

import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolPairMatch
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import org.eclipse.tm4e.languageconfiguration.internal.model.AutoClosingPairConditional
import java.util.Arrays

class TextMateSymbolPairMatch(private val language: TextMateLanguage) :
    SymbolPairMatch(DefaultSymbolPairs()) {

    private var enabled = true

    init {
        updatePair()
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            removeAllPairs()
        } else {
            updatePair()
        }
    }

    fun isEnabled(): Boolean = enabled

    fun updatePair() {
        if (!enabled) {
            return
        }

        val languageConfiguration = language.languageConfiguration ?: return

        removeAllPairs()

        val surroundingPairs = languageConfiguration.surroundingPairs
        val autoClosingPairs = languageConfiguration.autoClosingPairs

        val mergePairs = ArrayList<AutoClosingPairConditional>()

        if (autoClosingPairs != null) {
            mergePairs.addAll(autoClosingPairs)
        }

        if (surroundingPairs != null) {
            for (surroundingPair in surroundingPairs) {
                val newPair = AutoClosingPairConditional(
                    surroundingPair.open,
                    surroundingPair.close,
                    surroundingPairFlagWithList
                )
                mergePairs.add(newPair)
            }
        }

        for (pair in mergePairs) {
            putPair(pair.open, SymbolPair(pair.open, pair.close, SymbolPairEx(pair)))
        }
    }

    internal class SymbolPairEx(pair: AutoClosingPairConditional) : SymbolPair.SymbolPairEx {

        var notInTokenTypeArray: IntArray? = null
        var isSurroundingPair = false

        init {
            val notInList = pair.notIn
            if (notInList.isNullOrEmpty()) {
                notInTokenTypeArray = null
            } else {
                if (notInList.contains(surroundingPairFlag)) {
                    isSurroundingPair = true
                    if (notInList === surroundingPairFlagWithList) {
                        // done
                    } else {
                        notInList.remove(surroundingPairFlag)
                    }
                }

                if (notInList !== surroundingPairFlagWithList) {
                    val array = IntArray(notInList.size)
                    for (i in notInList.indices) {
                        val notInValue = notInList[i].lowercase()
                        var notInTokenType = StandardTokenType.String
                        when (notInValue) {
                            "string" -> {}
                            "comment" -> notInTokenType = StandardTokenType.Comment
                            "regex" -> notInTokenType = StandardTokenType.RegEx
                        }
                        array[i] = notInTokenType
                    }
                    Arrays.sort(array)
                    notInTokenTypeArray = array
                }
            }
        }

        override fun shouldReplace(editor: CodeEditor, contentLine: ContentLine, leftColumn: Int): Boolean {
            if (editor.cursor.isSelected) {
                return isSurroundingPair
            }
            if (isSurroundingPair) {
                return false
            }

            val array = notInTokenTypeArray ?: return true

            val cursor = editor.cursor
            val currentLine = cursor.leftLine
            val currentColumn = cursor.leftColumn

            val spansOnCurrentLine = editor.getSpansForLine(currentLine)
            val currentSpan = binarySearchSpan(spansOnCurrentLine, currentColumn)
            val extra = currentSpan?.extra

            if (extra is Int) {
                val index = Arrays.binarySearch(array, extra)
                return index < 0
            }

            return true
        }

        private fun checkIndex(index: Int, max: Int): Int {
            return index.coerceIn(0, max)
        }

        private fun binarySearchSpan(spanList: List<Span>?, column: Int): Span? {
            if (spanList.isNullOrEmpty()) return null
            var start = 0
            var end = spanList.size - 1
            val size = spanList.size - 1
            var currentSpan: Span? = null

            while (start <= end) {
                val middle = (start + end) / 2
                currentSpan = spanList[middle]
                if (currentSpan.column == column) {
                    break
                }
                if (currentSpan.column < column) {
                    val nextSpan = spanList[checkIndex(middle + 1, size)]
                    if (nextSpan.column > column) {
                        return currentSpan
                    }
                    start++
                    continue
                }
                val previousSpan = spanList[checkIndex(middle - 1, size)]
                if (previousSpan.column < column) {
                    return currentSpan
                }
                end--
            }

            return currentSpan
        }

        override fun shouldDoAutoSurround(content: Content): Boolean {
            return isSurroundingPair && content.cursor.isSelected
        }
    }

    companion object {
        private const val surroundingPairFlag = "surroundingPair"
        private val surroundingPairFlagWithList: List<String> = listOf(surroundingPairFlag)
    }
}
