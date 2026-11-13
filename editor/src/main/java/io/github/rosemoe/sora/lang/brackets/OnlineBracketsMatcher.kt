/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.lang.brackets

import io.github.rosemoe.sora.text.Content

/**
 * Compute paired bracket when queried.
 *
 * @param pairs Pairs. For example: `charArrayOf('(', ')', '{', '}')`
 * @param limit Max length to search
 * @author Rosemoe
 */
open class OnlineBracketsMatcher(
    private val pairs: CharArray,
    private val limit: Int
) : BracketsProvider {

    init {
        require((pairs.size and 1) == 0) { "pairs must have even length" }
    }

    private fun findIndex(ch: Char): Int {
        for (i in pairs.indices) {
            if (ch == pairs[i]) {
                return i
            }
        }
        return -1
    }

    private fun tryComputePaired(text: Content, index: Int): PairedBracket? {
        val a = text[index]
        val symbolIndex = findIndex(a)
        if (symbolIndex != -1) {
            val b = pairs[symbolIndex xor 1]
            var stack = 0
            if ((symbolIndex and 1) == 0) {
                // Find forward
                var i = index + 1
                val len = text.length
                while (i < len && i - index < limit) {
                    val ch = text[i]
                    if (ch == b) {
                        if (stack <= 0) {
                            return PairedBracket(index, i)
                        } else {
                            stack--
                        }
                    } else if (ch == a) {
                        stack++
                    }
                    i++
                }
            } else {
                // Find backward
                var i = index - 1
                while (i >= 0 && index - i < limit) {
                    val ch = text[i]
                    if (ch == b) {
                        if (stack <= 0) {
                            return PairedBracket(i, index)
                        } else {
                            stack--
                        }
                    } else if (ch == a) {
                        stack++
                    }
                    i--
                }
            }
        }
        return null
    }

    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? {
        var pairedBracket: PairedBracket? = null
        if (index > 0) {
            pairedBracket = tryComputePaired(text, index - 1)
        }
        if (pairedBracket == null && index < text.length) {
            pairedBracket = tryComputePaired(text, index)
        }
        return pairedBracket
    }
}
