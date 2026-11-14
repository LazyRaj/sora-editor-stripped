/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *    Modified by Raj Dave (aka LazyRaj)
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

package io.github.rosemoe.sora.text

/**
 * Line separator types
 *
 * @author Rosemoe
 */
enum class LineSeparator(val content: String) {

    /**
     * No separator. Used internally
     */
    NONE(""),
    LF("\n"),
    CR("\r"),
    CRLF("\r\n");

    val length: Int = content.length

    val chars: CharArray = content.toCharArray()

    companion object {

        /**
         * Get target line separator from a line separator string.
         *
         * @param str line separator string
         * @throws IllegalArgumentException if the given str is not a line separator
         */
        fun fromSeparatorString(str: String): LineSeparator {
            return when (str) {
                "\r" -> CR
                "\n" -> LF
                "\r\n" -> CRLF
                "" -> NONE
                else -> throw IllegalArgumentException("unknown line separator type")
            }
        }

        /**
         * Get target line separator from a line separator string.
         *
         * @param text the whole text
         * @param start start index of the line separator
         * @param end end index of the line separator
         * @throws IllegalArgumentException if the given str is not a line separator
         */
        fun fromSeparatorString(text: CharSequence, start: Int, end: Int): LineSeparator {
            if (end == start) {
                return NONE
            }
            if (end - start == 1) {
                val ch = text[start]
                if (ch == '\r') return CR
                if (ch == '\n') return LF
            }
            if (end - start == 2 && text[start] == '\r' && text[start + 1] == '\n') {
                return CRLF
            }
            throw IllegalArgumentException("unknown line separator type")
        }
    }
}

/**
 * Extension property to resolve [LineSeparator] from a [CharSequence].
 */
inline val CharSequence.lineSeparator: LineSeparator
    get() = LineSeparator.fromSeparatorString(this.toString())

