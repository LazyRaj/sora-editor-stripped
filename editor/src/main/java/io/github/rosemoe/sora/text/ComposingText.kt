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

package io.github.rosemoe.sora.text

import io.github.rosemoe.sora.annotations.UnsupportedUserUsage

@UnsupportedUserUsage
open class ComposingText {

    @JvmField
    var startIndex: Int = -1

    @JvmField
    var endIndex: Int = -1

    @JvmField
    var preSetComposing: Boolean = false

    /**
     * The original composing text, including any IME decorations.
     */
    @JvmField
    var text: CharSequence? = null

    open fun set(start: Int, end: Int) {
        if (start != startIndex || end != endIndex) {
            text = null
        }
        startIndex = start
        endIndex = end
    }

    open fun adjustLength(length: Int) {
        if (endIndex - startIndex != length) {
            text = null
        }
        endIndex = startIndex + length
    }

    open fun reset() {
        startIndex = -1
        endIndex = -1
        preSetComposing = false
        text = null
    }

    open val isComposing: Boolean
        get() = preSetComposing || (startIndex >= 0 && endIndex >= 0)

    open fun shiftOnInsert(insertStart: Int, insertEnd: Int) {
        val length = insertEnd - insertStart
        if (startIndex <= insertStart && endIndex >= insertStart) {
            text = null
            endIndex += length
        }
        if (startIndex > insertStart) {
            startIndex += length
            endIndex += length
        }
    }

    open fun shiftOnDelete(deleteStart: Int, deleteEnd: Int) {
        val length = deleteEnd - deleteStart
        val sharedStart = maxOf(deleteStart, startIndex)
        val sharedEnd = minOf(deleteEnd, endIndex)
        if (sharedEnd <= sharedStart) {
            if (startIndex >= deleteEnd) {
                startIndex -= length
                endIndex -= length
            }
        } else {
            text = null
            val sharedLength = sharedEnd - sharedStart
            endIndex -= sharedLength
            if (startIndex > deleteStart) {
                val shiftLeftCount = startIndex - deleteStart
                startIndex -= shiftLeftCount
                endIndex -= shiftLeftCount
            }
        }
    }
}
