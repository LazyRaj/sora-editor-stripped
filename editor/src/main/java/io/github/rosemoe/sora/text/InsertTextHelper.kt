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

open class InsertTextHelper private constructor() {

    var index: Int = 0
        private set
    var indexNext: Int = 0
        private set

    private var text: CharSequence? = null
    private var length: Int = 0

    open fun recycle() {
        synchronized(sCached) {
            for (i in sCached.indices) {
                if (sCached[i] == null) {
                    sCached[i] = this
                    reset()
                    break
                }
            }
        }
    }

    private fun init(text: CharSequence) {
        this.text = text
        index = -1
        indexNext = 0
        length = text.length
    }

    open fun forward(): Int {
        val nonNullText = text ?: return TYPE_EOF
        index = indexNext
        if (index == length) {
            return TYPE_EOF
        }
        var ch = nonNullText[index]
        return when (ch) {
            '\n' -> {
                indexNext = index + 1
                TYPE_NEWLINE
            }

            '\r' -> {
                indexNext = if (index + 1 < length && nonNullText[index + 1] == '\n') {
                    index + 2
                } else {
                    index + 1
                }
                TYPE_NEWLINE
            }

            else -> {
                indexNext = index + 1
                while (indexNext < length) {
                    ch = nonNullText[indexNext]
                    if (ch == '\n' || ch == '\r') {
                        break
                    }
                    indexNext++
                }
                TYPE_LINE_CONTENT
            }
        }
    }

    open fun reset() {
        text = null
        length = 0
        index = 0
        indexNext = 0
    }

    companion object {
        const val TYPE_LINE_CONTENT: Int = 0
        const val TYPE_NEWLINE: Int = 1
        const val TYPE_EOF: Int = 2

        private val sCached = arrayOfNulls<InsertTextHelper>(8)

        private fun obtain(): InsertTextHelper {
            synchronized(sCached) {
                for (i in sCached.indices) {
                    val cache = sCached[i]
                    if (cache != null) {
                        sCached[i] = null
                        return cache
                    }
                }
            }
            return InsertTextHelper()
        }

        fun forInsertion(text: CharSequence): InsertTextHelper {
            val o = obtain()
            o.init(text)
            return o
        }
    }
}
