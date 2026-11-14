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
package io.github.rosemoe.sora.lang.folding

import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.util.first
import io.github.rosemoe.sora.util.second

/**
 * Indicates a folding region
 */
open class FoldingRegion {

    private var start: Long
    private var end: Long
    var isCollapsed: Boolean = false
    private val childrenList: MutableList<FoldingRegion> = ArrayList()

    val children: List<FoldingRegion>
        get() = childrenList

    constructor(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) : this(
        IntPair.pack(startLine, startColumn),
        IntPair.pack(endLine, endColumn)
    ) {
        if (startLine > endLine || (startLine == endLine && startColumn > endColumn)) {
            throw IllegalArgumentException("start > end")
        }
    }

    internal constructor(start: Long, end: Long) {
        this.start = start
        this.end = end
    }

    val startLine: Int
        get() = start.first

    val startColumn: Int
        get() = start.second

    val endLine: Int
        get() = end.first

    val endColumn: Int
        get() = end.second

    open fun createChild(
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ): FoldingRegion {
        if (startLine < this.startLine || (startLine == this.startLine && startColumn < this.startColumn)) {
            throw IllegalArgumentException("child start is before parent start")
        }
        if (endLine > this.endLine || (endLine == this.endLine && endColumn > this.endColumn)) {
            throw IllegalArgumentException("child end is beyond parent end")
        }
        val child = FoldingRegion(startLine, startColumn, endLine, endColumn)
        childrenList.add(child)
        return child
    }
}
