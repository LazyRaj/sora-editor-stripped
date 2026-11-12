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

import kotlin.math.min

/**
 * This class provides region division and iteration with several [RegionProvider]s.
 *
 * @author Rosemoe
 */
open class RegionIterator(
    val max: Int,
    vararg providers: RegionProvider
) {

    private val providers: Array<out RegionProvider> = providers
    private val pointers = IntArray(providers.size)
    private val pointerStates = BooleanArray(providers.size)

    @JvmField
    protected var startIndex: Int = 0

    @JvmField
    protected var endIndex: Int = 0

    /**
     * Move to next region
     */
    fun nextRegion() {
        startIndex = endIndex
        var minNext = max
        for (i in providers.indices) {
            val next = if (pointers[i] < providers[i].pointCount) {
                val value = providers[i].getPointAt(pointers[i])
                if (value <= max) value else max
            } else {
                max
            }
            minNext = min(next, minNext)
        }
        endIndex = minNext
        for (i in providers.indices) {
            if (pointers[i] < providers[i].pointCount && providers[i].getPointAt(pointers[i]) == minNext) {
                pointers[i]++
                pointerStates[i] = true
            } else {
                pointerStates[i] = false
            }
        }
    }

    /**
     * Check if we can move to next region
     */
    fun hasNextRegion(): Boolean {
        return endIndex < max
    }

    /**
     * Get current index of dividing points in provider with given index [i]
     * @param i Index of provider
     * @return Current index of regions in that provider
     */
    fun getPointer(i: Int): Int {
        return pointers[i]
    }

    /**
     * Get the source index of dividing points in provider with given index [i].
     * Source index is the index of dividing point that leads to current region.
     * @param i Index of provider
     */
    fun getRegionSourcePointer(i: Int): Int {
        val pointerValue = if (pointers[i] < providers[i].pointCount) providers[i].getPointAt(i) else max
        return if ((endIndex <= pointerValue && pointerValue < max) || pointerStates[i]) pointers[i] - 1 else pointers[i]
    }

    fun getPointerValue(i: Int, j: Int): Int {
        val provider = providers[i]
        if (j < 0) {
            return 0
        }
        if (j >= provider.pointCount) {
            return max
        }
        val value = provider.getPointAt(j)
        return min(value, max)
    }

    /**
     * Get start index of the region
     * Note that this is inclusive.
     */
    fun getStartIndex(): Int {
        return startIndex
    }

    /**
     * Get end index of the region.
     * Note that this is exclusive.
     */
    fun getEndIndex(): Int {
        return min(endIndex, max)
    }

    /**
     * RegionProvider provides dividing points for [RegionIterator]. Note that the returned
     * sequence must follow an ascending order.
     */
    interface RegionProvider {
        /**
         * Get count of dividing points
         */
        val pointCount: Int

        /**
         * Get dividing point at given index
         * @param index Index of point
         * @return Dividing index in region
         */
        fun getPointAt(index: Int): Int
    }
}
