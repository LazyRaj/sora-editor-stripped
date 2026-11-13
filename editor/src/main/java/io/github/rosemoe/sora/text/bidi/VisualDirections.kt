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

package io.github.rosemoe.sora.text.bidi

import io.github.rosemoe.sora.util.IntPair
import java.text.Bidi

/**
 * Helper class for reordering logical text runs to visual runs.
 *
 * @author Rosemoe
 */
open class VisualDirections(dirs: Directions) : IDirections {

    private class RunInfo(val range: Long, val level: Int)

    private val runs: Array<RunInfo>

    init {
        val count = dirs.runCount
        val infoArray = Array(count) { i ->
            RunInfo(IntPair.pack(dirs.getRunStart(i), dirs.getRunEnd(i)), dirs.getRunLevel(i))
        }
        val paramLevels = ByteArray(count) { i ->
            dirs.getRunLevel(i).toByte()
        }
        @Suppress("UNCHECKED_CAST")
        Bidi.reorderVisually(paramLevels, 0, infoArray as Array<Any>, 0, count)
        runs = infoArray
    }

    override val runCount: Int
        get() = runs.size

    override fun getRunStart(i: Int): Int {
        return IntPair.getFirst(runs[i].range)
    }

    override fun getRunEnd(i: Int): Int {
        return IntPair.getSecond(runs[i].range)
    }

    override fun getRunLevel(i: Int): Int {
        return runs[i].level
    }

    override fun isRunRtl(i: Int): Boolean {
        return (getRunLevel(i) and 1) != 0
    }
}
