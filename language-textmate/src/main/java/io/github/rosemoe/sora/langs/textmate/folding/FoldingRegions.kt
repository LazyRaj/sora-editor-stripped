/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
package io.github.rosemoe.sora.langs.textmate.folding

import android.util.SparseIntArray
import java.util.ArrayDeque

class FoldingRegions(
    private val startIndexes: SparseIntArray,
    private val endIndexes: SparseIntArray
) {
    private var parentsComputed: Boolean = false

    init {
        if (startIndexes.size() != endIndexes.size() || startIndexes.size() > IndentRange.MAX_FOLDING_REGIONS) {
            throw Exception("invalid startIndexes or endIndexes size")
        }
    }

    fun length(): Int = startIndexes.size()

    fun getStartLineNumber(index: Int): Int {
        return startIndexes.get(index) and IndentRange.MAX_LINE_NUMBER
    }

    fun getEndLineNumber(index: Int): Int {
        return endIndexes.get(index) and IndentRange.MAX_LINE_NUMBER
    }

    fun toRegion(index: Int): FoldingRegion {
        return FoldingRegion(this, index)
    }

    private fun isInsideLast(
        parentIndexes: ArrayDeque<Int>,
        startLineNumber: Int,
        endLineNumber: Int
    ): Boolean {
        val index = parentIndexes.peek() ?: return false
        return getStartLineNumber(index) <= startLineNumber && getEndLineNumber(index) >= endLineNumber
    }

    @Throws(Exception::class)
    private fun ensureParentIndices() {
        if (!parentsComputed) {
            parentsComputed = true
            val parentIndexes = ArrayDeque<Int>()
            val len = startIndexes.size()
            for (i in 0 until len) {
                val startLineNumber = startIndexes.get(i)
                val endLineNumber = endIndexes.get(i)
                if (startLineNumber > IndentRange.MAX_LINE_NUMBER || endLineNumber > IndentRange.MAX_LINE_NUMBER) {
                    throw Exception("startLineNumber or endLineNumber must not exceed ${IndentRange.MAX_LINE_NUMBER}")
                }
                while (parentIndexes.isNotEmpty() && !isInsideLast(
                        parentIndexes,
                        startLineNumber,
                        endLineNumber
                    )
                ) {
                    parentIndexes.pop()
                }
                val parentIndex = if (parentIndexes.isNotEmpty()) parentIndexes.peek() else -1
                parentIndexes.push(i)
                startIndexes.put(i, startLineNumber + ((parentIndex and 0xFF) shl 24))
                endIndexes.put(i, endLineNumber + ((parentIndex and 0xFF00) shl 16))
            }
        }
    }

    @Throws(Exception::class)
    fun getParentIndex(index: Int): Int {
        ensureParentIndices()
        val parent = ((startIndexes.get(index) and IndentRange.MASK_INDENT) ushr 24) +
                ((endIndexes.get(index) and IndentRange.MASK_INDENT) ushr 16)
        return if (parent == IndentRange.MAX_FOLDING_REGIONS) {
            -1
        } else {
            parent
        }
    }
}
