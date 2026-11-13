/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
/*
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.widget.rendering

import androidx.annotation.IntRange
import kotlin.math.max
import kotlin.math.min

/**
 * This class is introduced in order to avoid accumulated floating-point error for extreme long lines
 *
 * @author Rosemoe
 */
class TextAdvancesCache(@IntRange(from = 0) val size: Int) {

    companion object {
        /**
         * Divide the prefix sum by this block size
         */
        private const val BLOCK_SIZE = 1 shl 18
    }

    private val cache: Array<FloatArray>

    init {
        require(size >= 0) { "invalid size: $size" }
        val count = (size + BLOCK_SIZE - 1) / BLOCK_SIZE
        cache = Array(count) { i ->
            val elementCount = if (i == count - 1) size - BLOCK_SIZE * (count - 1) else BLOCK_SIZE
            FloatArray(elementCount + 1)
        }
    }

    /**
     * Set advance at the given index
     */
    fun setAdvanceAt(index: Int, advance: Float) {
        val i = index / BLOCK_SIZE
        val j = index % BLOCK_SIZE
        cache[i][j] = advance
    }

    /**
     * Compute the prefix sum cache
     */
    fun finishBuilding() {
        for (arr in cache) {
            var pending = arr[0]
            arr[0] = 0f
            for (i in 1 until arr.size) {
                val tmp = arr[i]
                arr[i] = arr[i - 1] + pending
                pending = tmp
            }
        }
    }

    /**
     * Get advance for character at the given index
     */
    fun getAdvanceAt(index: Int): Float {
        val i = index / BLOCK_SIZE
        val j = index % BLOCK_SIZE
        return cache[i][j + 1] - cache[i][j]
    }

    /**
     * Get the sum of character advances of the given text region
     *
     * @param start inclusive start
     * @param end   exclusive end
     */
    fun getAdvancesSum(start: Int, end: Int): Float {
        if (cache.size == 1) {
            // Normal case
            return cache[0][end] - cache[0][start]
        }
        val low = start / BLOCK_SIZE
        val high = end / BLOCK_SIZE
        var result = 0f
        for (i in low..high) {
            val segStart = i * BLOCK_SIZE
            val segEnd = min((i + 1) * BLOCK_SIZE, size)
            val sharedStart = max(start, segStart)
            val sharedEnd = min(end, segEnd)
            if (sharedStart < sharedEnd) {
                result += cache[i][sharedEnd - segStart] - cache[i][sharedStart - segStart]
            }
        }
        return result
    }
}
