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
 * ArrayList for primitive type long
 *
 * @author Rosemoe
 */
class LongArrayList(initialCapacity: Int = 64) {

    private var data: LongArray = LongArray(initialCapacity.coerceAtLeast(1))
    private var length = 0

    val size: Int get() = length

    val isEmpty: Boolean get() = length == 0

    val isNotEmpty: Boolean get() = length > 0

    /**
     * Add a value at end
     */
    fun add(value: Long) {
        data[length++] = value
        if (data.size == length) {
            val newData = LongArray(length shl 1)
            System.arraycopy(data, 0, newData, 0, length)
            data = newData
        }
    }

    /**
     * Get length of the list
     */
    fun size(): Int = length

    operator fun contains(value: Long): Boolean {
        for (i in 0 until length) {
            if (data[i] == value) return true
        }
        return false
    }

    /**
     * Set element at given index to [value]
     * @throws ArrayIndexOutOfBoundsException if index is invalid
     */
    operator fun set(index: Int, value: Long) {
        if (index >= length || index < 0) {
            throw ArrayIndexOutOfBoundsException(index)
        }
        data[index] = value
    }

    /**
     * Refers to C++ algorithm lower_bound().
     * Compare by [IntPair.getFirst] on each element.
     *
     * Note that, you guarantee the sequence in list is in ascendant order.
     *
     * @param key Target value
     * @return Index of target value, or index of the insertion point (that's the index of first element
     * bigger than [key] or array length)
     */
    fun lowerBoundByFirst(key: Int): Int {
        var low = 0
        var high = length - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = IntPair.getFirst(data[mid])

            if (midVal < key) {
                low = mid + 1
            } else if (midVal > key) {
                high = mid - 1
            } else {
                return mid // key found
            }
        }
        return low // key not found.
    }

    fun lowerBound(key: Long): Int {
        var low = 0
        var high = length - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = data[mid]

            if (midVal < key) {
                low = mid + 1
            } else if (midVal > key) {
                high = mid - 1
            } else {
                return mid // key found
            }
        }
        return low // key not found.
    }

    /**
     * Get element at given index
     * @throws ArrayIndexOutOfBoundsException if index is invalid
     */
    operator fun get(index: Int): Long {
        if (index >= length || index < 0) {
            throw ArrayIndexOutOfBoundsException(index)
        }
        return data[index]
    }

    fun clear() {
        length = 0
    }
}
