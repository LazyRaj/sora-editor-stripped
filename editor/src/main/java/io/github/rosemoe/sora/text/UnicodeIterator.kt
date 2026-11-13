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

import io.github.rosemoe.sora.util.ObjectPool

open class UnicodeIterator private constructor() {

    private var text: CharSequence? = null
    var codePoint: Int = 0
        private set
    var startIndex: Int = 0
        private set
    var endIndex: Int = 0
        private set
    private var limit: Int = 0

    open fun recycle() {
        sPool.recycle(this)
    }

    open fun set(text: CharSequence, start: Int, end: Int) {
        if ((start or end or (end - start) or (text.length - end)) < 0) {
            throw IndexOutOfBoundsException()
        }
        this.text = text
        this.startIndex = start
        this.endIndex = start
        this.limit = end
    }

    open fun hasNext(): Boolean {
        return endIndex < limit
    }

    open fun nextCodePoint(): Int {
        val nonNullText = text ?: return 0
        startIndex = endIndex
        if (startIndex >= limit) {
            codePoint = 0
        } else {
            endIndex++
            val ch = nonNullText[startIndex]
            if (Character.isHighSurrogate(ch) && endIndex < limit) {
                codePoint = Character.toCodePoint(ch, nonNullText[endIndex])
                endIndex++
            } else {
                codePoint = ch.code
            }
        }
        return codePoint
    }

    companion object {
        private val sPool = object : ObjectPool<UnicodeIterator>() {
            override fun allocateNew(): UnicodeIterator {
                return UnicodeIterator()
            }

            override fun onRecycleObject(recycledObj: UnicodeIterator) {
                recycledObj.text = null
            }
        }

        @JvmStatic
        fun obtain(text: CharSequence, start: Int, end: Int): UnicodeIterator {
            val r = sPool.obtain()
            r.set(text, start, end)
            return r
        }
    }
}
