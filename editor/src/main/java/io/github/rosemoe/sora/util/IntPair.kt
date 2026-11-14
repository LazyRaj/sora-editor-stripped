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

/**
 * Pack two 32-bit integers (or an int and float) into a 64-bit long, and unpack it.
 *
 * This is effective for passing two primitive 32-bit numbers without creating a new object.
 *
 * @author Rosemoe
 * Modified by Raj Dave (aka LazyRaj)
 */
object IntPair {

    /**
     * Pack two int numbers into a long number
     *
     * @param first  First of pair
     * @param second Second of pair
     * @return Packed value
     */
    fun pack(first: Int, second: Int): Long {
        return (first.toLong() and 0xFFFFFFFFL shl 32) or (second.toLong() and 0xFFFFFFFFL)
    }

    /**
     * Get second of pair
     *
     * @param packedValue Packed value
     * @return Second of pair
     */
    fun getSecond(packedValue: Long): Int {
        return packedValue.toInt()
    }

    /**
     * Get first of pair
     *
     * @param packedValue Packed value
     * @return First of pair
     */
    fun getFirst(packedValue: Long): Int {
        return (packedValue ushr 32).toInt()
    }

    /**
     * Pack an int number and a floating-number into a long number
     *
     * @param first  First of pair
     * @param second Second of pair (float)
     * @return Packed value
     */
    fun packIntFloat(first: Int, second: Float): Long {
        return pack(first, second.toRawBits())
    }

    /**
     * Get second of pair, but as a floating number
     *
     * @param packedValue Packed value
     * @return Second of pair
     * @see packIntFloat
     */
    fun getSecondAsFloat(packedValue: Long): Float {
        return Float.fromBits(getSecond(packedValue))
    }
}

inline val Long.first: Int get() = (this ushr 32).toInt()

inline val Long.second: Int get() = this.toInt()

inline val Long.secondFloat: Float get() = Float.fromBits(toInt())

inline operator fun Long.component1(): Int = first

inline operator fun Long.component2(): Int = second

