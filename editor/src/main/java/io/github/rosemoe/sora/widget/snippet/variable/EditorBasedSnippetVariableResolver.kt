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
package io.github.rosemoe.sora.widget.snippet.variable

import io.github.rosemoe.sora.text.ICUUtils
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.CodeEditor

open class EditorBasedSnippetVariableResolver(
    private val editor: CodeEditor
) : ISnippetVariableResolver {

    override fun getResolvableNames(): Array<String> {
        return arrayOf(
            "TM_CURRENT_LINE", "TM_LINE_INDEX", "TM_LINE_NUMBER", "CURSOR_INDEX", "CURSOR_NUMBER",
            "TM_CURRENT_WORD", "SELECTION", "TM_SELECTED_TEXT"
        )
    }

    override fun resolve(name: String): String {
        val cursor = editor.cursor
        return when (name) {
            "TM_CURRENT_LINE", "TM_LINE_NUMBER" -> (cursor.leftLine + 1).toString()
            "TM_LINE_INDEX" -> cursor.leftLine.toString()
            "CURSOR_INDEX" -> cursor.left.toString()
            "CURSOR_NUMBER" -> (cursor.left + 1).toString()
            "TM_CURRENT_WORD" -> {
                val text = editor.text
                val line = text.getLine(cursor.leftLine)
                val res = ICUUtils.getWordRange(line, cursor.leftColumn, true)
                line.subSequence(IntPair.getFirst(res), IntPair.getSecond(res)).toString()
            }

            "SELECTION", "TM_SELECTED_TEXT" -> {
                editor.text.subSequence(cursor.left, cursor.right).toString()
            }

            else -> throw IllegalArgumentException("Unsupported variable name:$name")
        }
    }
}
