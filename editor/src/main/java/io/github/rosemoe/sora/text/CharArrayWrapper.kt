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

import android.text.GetChars
import java.nio.CharBuffer

/**
 * Wrapper for char array. Make char array work as a char sequence.
 *
 * @author Rosemoe
 */
open class CharArrayWrapper(
    private val data: CharArray,
    private val offset: Int,
    private var count: Int
) : CharSequence, GetChars {

    constructor(array: CharArray, dataCount: Int) : this(array, 0, dataCount)

    open fun setDataCount(count: Int) {
        this.count = count
    }

    override val length: Int
        get() = count

    override fun get(index: Int): Char {
        return data[offset + index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return CharBuffer.wrap(data, offset + startIndex, endIndex - startIndex)
    }

    override fun getChars(start: Int, end: Int, dest: CharArray, destOffset: Int) {
        if (end > count) {
            throw StringIndexOutOfBoundsException()
        }
        System.arraycopy(data, start + offset, dest, destOffset, end - start)
    }
}
