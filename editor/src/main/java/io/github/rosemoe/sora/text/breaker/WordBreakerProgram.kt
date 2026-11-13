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

package io.github.rosemoe.sora.text.breaker

import io.github.rosemoe.sora.text.ContentLine

open class WordBreakerProgram(text: ContentLine) : WordBreakerIcu(text) {

    protected val length: Int = text.length

    override fun getOptimizedBreakPoint(start: Int, end: Int): Int {
        val icuResult = super.getOptimizedBreakPoint(start, end)
        if (icuResult != end || end <= start || Character.isWhitespace(text[end - 1])) {
            return icuResult
        }
        // The content can be placed on a single row
        if (end == length) {
            return end
        }
        // Add extra opportunities for dots
        var index = end - 1
        while (index > start) {
            if (text[index] == '.' && index - 1 >= start && !Character.isDigit(text[index - 1])) {
                // Break after this dot
                return index + 1
            }
            index--
        }
        return end
    }
}
