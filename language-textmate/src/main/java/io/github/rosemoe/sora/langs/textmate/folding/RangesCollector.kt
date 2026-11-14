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
/* Modified by Raj Dave (aka LazyRaj) */
package io.github.rosemoe.sora.langs.textmate.folding

import android.util.SparseIntArray
import io.github.rosemoe.sora.text.Content

class RangesCollector {
    private val startIndexes = SparseIntArray()
    private val endIndexes = SparseIntArray()
    private var length = 0

    fun insertFirst(startLineNumber: Int, endLineNumber: Int, indent: Int) {
        if (startLineNumber > IndentRange.MAX_LINE_NUMBER || endLineNumber > IndentRange.MAX_LINE_NUMBER) {
            return
        }
        val index = length
        startIndexes.put(index, startLineNumber)
        endIndexes.put(index, endLineNumber)
        length++
    }

    fun toIndentRanges(model: Content): FoldingRegions {
        return FoldingRegions(startIndexes, endIndexes)
    }
}
