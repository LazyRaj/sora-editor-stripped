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

package io.github.rosemoe.sora.text.bidi

import android.text.TextUtils
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.util.TemporaryCharBuffer
import java.text.Bidi

/**
 * Text bidirectional utils. Some codes are from AOSP
 *
 * @author Rosemoe
 */
object TextBidi {

    /**
     * Compute text directions for the given text
     */
    @JvmStatic
    fun getDirections(text: CharSequence): Directions {
        val len = text.length
        if (doesNotNeedBidi(text)) {
            return Directions(longArrayOf(IntPair.pack(0, 0)), len)
        }
        val chars = TemporaryCharBuffer.obtain(len)
        TextUtils.getChars(text, 0, len, chars, 0)
        val bidi = Bidi(chars, 0, null, 0, len, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)
        val runs = LongArray(bidi.runCount)
        for (i in runs.indices) {
            runs[i] = IntPair.pack(bidi.getRunStart(i), bidi.getRunLevel(i))
        }
        TemporaryCharBuffer.recycle(chars)
        return Directions(runs, len)
    }

    @JvmStatic
    fun couldAffectRtl(c: Char): Boolean {
        val code = c.code
        return (0x0590 <= code && code <= 0x08FF) || // RTL scripts
                code == 0x200E || // Bidi format character
                code == 0x200F || // Bidi format character
                (0x202A <= code && code <= 0x202E) || // Bidi format characters
                (0x2066 <= code && code <= 0x2069) || // Bidi format characters
                (0xD800 <= code && code <= 0xDFFF) || // Surrogate pairs
                (0xFB1D <= code && code <= 0xFDFF) || // Hebrew and Arabic presentation forms
                (0xFE70 <= code && code <= 0xFEFE) // Arabic presentation forms
    }

    /**
     * Returns true if there is no character present that may potentially affect RTL layout.
     * Since this calls couldAffectRtl() above, it's also quite conservative, in the way that
     * it may return 'false' (needs bidi) although careful consideration may tell us it should
     * return 'true' (does not need bidi).
     */
    @JvmStatic
    fun doesNotNeedBidi(text: CharSequence): Boolean {
        if (text is BidiRequirementChecker) {
            return !text.mayNeedBidi()
        }
        val len = text.length
        for (i in 0 until len) {
            if (couldAffectRtl(text[i])) {
                return false
            }
        }
        return true
    }
}
