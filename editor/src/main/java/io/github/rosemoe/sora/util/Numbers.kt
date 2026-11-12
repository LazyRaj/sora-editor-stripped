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
package io.github.rosemoe.sora.util

import kotlin.math.max
import kotlin.math.min

/**
 * Ported from Java java.lang.Integer
 */
object Numbers {

    @JvmField
    val DigitTens = charArrayOf(
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'
    )

    @JvmField
    val DigitOnes = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )

    @JvmStatic
    fun stringSize(x: Int): Int {
        var num = x
        var d = 1
        if (num >= 0) {
            d = 0
            num = -num
        }
        var p = -10
        for (i in 1 until 10) {
            if (num > p) return i + d
            p *= 10
        }
        return 10 + d
    }

    @JvmStatic
    fun getChars(i: Int, index: Int, buf: CharArray) {
        var num = i
        var q: Int
        var r: Int
        var charPos = index

        val negative = num < 0
        if (!negative) {
            num = -num
        }

        while (num <= -100) {
            q = num / 100
            r = (q * 100) - num
            num = q
            buf[--charPos] = DigitOnes[r]
            buf[--charPos] = DigitTens[r]
        }

        buf[--charPos] = DigitOnes[-num]
        if (num < -9) {
            buf[--charPos] = DigitTens[-num]
        }

        if (negative) {
            buf[--charPos] = '-'
        }
    }

    @JvmStatic
    fun clearBit(flags: Int, flag: Int): Int {
        return if ((flags and flag) != 0) flags xor flag else flags
    }

    @JvmStatic
    fun clearBits(flags: Int, bitsToClear: Int): Int {
        val mask = bitsToClear.inv()
        return flags and mask
    }

    @JvmStatic
    fun coerceIn(value: Int, minVal: Int, maxVal: Int): Int {
        return max(minVal, min(maxVal, value))
    }
}
