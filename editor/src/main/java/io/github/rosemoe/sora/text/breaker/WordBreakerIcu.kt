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

import io.github.rosemoe.sora.text.CharSequenceIterator
import io.github.rosemoe.sora.text.ContentLine
import java.text.BreakIterator
import java.text.CharacterIterator

open class WordBreakerIcu(protected val text: ContentLine) : WordBreaker {

    protected val wrappingIterator: BreakIterator

    init {
        val textIterator: CharacterIterator = CharSequenceIterator(text)
        wrappingIterator = BreakIterator.getLineInstance()
        wrappingIterator.setText(textIterator)
    }

    override fun getOptimizedBreakPoint(start: Int, end: Int): Int {
        var resultEnd = end
        // Merging trailing whitespaces is not supported by editor, so force to break here
        if (resultEnd > 0 && !Character.isWhitespace(text[resultEnd - 1]) && !wrappingIterator.isBoundary(resultEnd)) {
            // Break text at last boundary
            val lastBoundary = wrappingIterator.preceding(resultEnd)
            if (lastBoundary != BreakIterator.DONE) {
                val suggestedNext = maxOf(start, minOf(resultEnd, lastBoundary))
                if (suggestedNext > start) {
                    resultEnd = suggestedNext
                }
            }
        }
        return resultEnd
    }
}
