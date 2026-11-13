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

import java.text.CharacterIterator

/**
 * CharacterIterator implementation
 *
 * @author Rosemoe
 */
open class CharSequenceIterator(private val src: CharSequence) : CharacterIterator, Cloneable {

    private var index: Int = 0

    override fun first(): Char {
        index = 0
        return current()
    }

    override fun last(): Char {
        index = maxOf(0, src.length - 1)
        return current()
    }

    override fun current(): Char {
        return if (index in 0 until endIndex) src[index] else CharacterIterator.DONE
    }

    override fun next(): Char {
        if (index < endIndex - 1) {
            index++
            return src[index]
        }
        index = endIndex
        return CharacterIterator.DONE
    }

    override fun previous(): Char {
        if (index <= beginIndex) {
            return CharacterIterator.DONE
        }
        index--
        return src[index]
    }

    override fun setIndex(i: Int): Char {
        if (i < beginIndex || i > endIndex) {
            throw IllegalArgumentException("Invalid index $i")
        }
        index = i
        return current()
    }

    override fun getBeginIndex(): Int = 0

    override fun getEndIndex(): Int = src.length

    override fun getIndex(): Int = index

    public override fun clone(): Any {
        val another = CharSequenceIterator(src)
        another.index = index
        return another
    }
}
