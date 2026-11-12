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

package io.github.rosemoe.sora.graphics

import android.graphics.Paint
import android.util.SparseArray
import io.github.rosemoe.sora.text.CharArrayWrapper
import io.github.rosemoe.sora.text.FunctionCharacters
import java.util.Arrays
import kotlin.math.ceil
import kotlin.math.min

class SingleCharacterWidths(private val tabWidth: Int) {

    companion object {
        /**
         * Floating-point precision steps.
         *
         * Introduced to avoid accumulated floating-point errors.
         */
        private const val PRECISION = 1000L

        @JvmStatic
        fun isEmoji(ch: Char): Boolean {
            val code = ch.code
            return code == 0xd83c || code == 0xd83d || code == 0xd83e
        }
    }

    @JvmField
    val widths = FloatArray(10)

    @JvmField
    val codePointWidths = SparseArray<Float>()

    @JvmField
    val buffer = CharArray(10)

    private val cache = FloatArray(65536)

    var isHandleFunctionCharacters: Boolean = false

    /**
     * Clear caches of font
     */
    fun clearCache() {
        Arrays.fill(cache, 0f)
        codePointWidths.clear()
    }

    /**
     * Measure a single character
     */
    fun measureChar(ch: Char, p: Paint): Float {
        var c = ch
        var rate = 1
        if (c == '\t') {
            c = ' '
            rate = tabWidth
        }
        var width = cache[c.code]
        if (width == 0f) {
            buffer[0] = c
            width = p.measureText(buffer, 0, 1)
            cache[c.code] = width
        }
        return width * rate
    }

    /**
     * Measure a single character
     * @param cp Code Point
     */
    fun measureCodePoint(cp: Int, p: Paint): Float {
        if (cp <= 65535) {
            return measureChar(cp.toChar(), p)
        }
        var width = codePointWidths.get(cp)
        if (width == null) {
            val count = Character.toChars(cp, buffer, 0)
            width = p.measureText(buffer, 0, count)
            codePointWidths.put(cp, width)
        }
        return width
    }

    /**
     * Measure text
     */
    fun measureText(chars: CharArray, start: Int, end: Int, p: Paint): Float {
        return measureText(CharArrayWrapper(chars, chars.size), start, end, p)
    }

    fun measureText(str: CharSequence, p: Paint): Float {
        return measureText(str, 0, str.length, p)
    }

    /**
     * Measure text
     */
    fun measureText(str: CharSequence, start: Int, end: Int, p: Paint): Float {
        var width = 0L
        var i = start
        while (i < end) {
            val ch = str[i]
            if (isEmoji(ch)) {
                if (i + 4 <= end) {
                    p.getTextWidths(str, i, i + 4, widths)
                    if (widths[0] > 0 && widths[1] == 0f && widths[2] == 0f && widths[3] == 0f) {
                        width += ceil(widths[0] * PRECISION).toLong()
                        i += 4
                        continue
                    }
                }
                val commitEnd = min(end, i + 2)
                val len = commitEnd - i
                for (j in 0 until len) {
                    buffer[j] = str[i + j]
                }
                width += ceil(p.measureText(buffer, 0, len) * PRECISION).toLong()
                i += len
            } else if (isHandleFunctionCharacters && FunctionCharacters.isEditorFunctionChar(ch)) {
                val name = FunctionCharacters.getNameForFunctionCharacter(ch)
                for (j in 0 until name.length) {
                    width += ceil(measureChar(name[j], p) * PRECISION).toLong()
                }
                i++
            } else {
                width += ceil(measureChar(ch, p) * PRECISION).toLong()
                i++
            }
        }
        return width.toFloat() / PRECISION
    }
}
