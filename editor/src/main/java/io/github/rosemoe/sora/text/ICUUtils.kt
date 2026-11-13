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

import android.icu.text.BreakIterator
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.util.MyCharacter

/**
 * Utility class for invoking ICU library on Android API 30+.
 *
 * @author Rosemoe
 */
object ICUUtils {

    /**
     * Get word range for the given offset
     *
     * @param text Text to analyze
     * @param offset Required char offset of word
     * @param useIcu Whether to use ICU BreakIterator
     * @return Packed integer pair (start, end). Always contains the position [offset]
     */
    @JvmStatic
    fun getWordRange(text: CharSequence, offset: Int, useIcu: Boolean): Long {
        if (useIcu) {
            val itr = BreakIterator.getWordInstance()
            itr.text = CharSequenceIterator(text)
            val end = itr.following(offset)
            val start = itr.previous()
            return if (offset in start..end) {
                IntPair.pack(start, end)
            } else {
                getWordRangeFallback(text, offset)
            }
        }
        return getWordRangeFallback(text, offset)
    }

    /**
     * Primitive implementation of [getWordRange] when ICU is not enabled
     */
    @JvmStatic
    fun getWordRangeFallback(text: CharSequence, offset: Int): Long {
        var start = offset
        var end = offset
        val len = text.length
        while (end < len && MyCharacter.isJavaIdentifierPart(text[end])) {
            end++
        }
        if (end > offset) {
            while (start > 0 && MyCharacter.isJavaIdentifierPart(text[start - 1])) {
                start--
            }
        }
        return IntPair.pack(start, end)
    }
}
