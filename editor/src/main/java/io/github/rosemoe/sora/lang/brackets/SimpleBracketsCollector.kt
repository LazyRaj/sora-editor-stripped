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
package io.github.rosemoe.sora.lang.brackets

import android.util.SparseIntArray
import io.github.rosemoe.sora.text.Content

/**
 * Collect brackets for simple languages. Not very effective. Not thread-safe.
 *
 * @author Rosemoe
 */
open class SimpleBracketsCollector : BracketsProvider {

    private val mapping: SparseIntArray = SparseIntArray()

    /**
     * Add new pair
     */
    open fun add(start: Int, end: Int) {
        // add 1 to avoid zeros
        mapping.put(start + 1, end + 1)
        mapping.put(end + 1, start + 1)
    }

    /**
     * Remove all pairs
     */
    open fun clear() {
        mapping.clear()
    }

    private fun getForIndex(index: Int): PairedBracket? {
        val another = mapping.get(index + 1) - 1
        if (another != -1) {
            val left = minOf(index, another)
            val right = maxOf(index, another)
            return PairedBracket(left, right)
        }
        return null
    }

    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? {
        var res = if (index - 1 >= 0) getForIndex(index - 1) else null
        if (res == null) {
            res = getForIndex(index)
        }
        return res
    }
}
