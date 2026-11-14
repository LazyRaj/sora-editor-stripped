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

/**
 * Get whether Identifier part/start quickly
 *
 * @author Rosemoe
 */
object MyCharacter {

    private val bitsIsStart = IntArray(2048)
    private val bitsIsPart = IntArray(2048)

    init {
        for (i in 0..65535) {
            val c = i.toChar()
            if (Character.isJavaIdentifierPart(c)) {
                set(bitsIsPart, i)
            }
            if (Character.isJavaIdentifierStart(c)) {
                set(bitsIsStart, i)
            }
        }
    }

    private fun get(values: IntArray, bitIndex: Int): Boolean {
        return (values[bitIndex / 32] and (1 shl (bitIndex % 32))) != 0
    }

    private fun set(values: IntArray, bitIndex: Int) {
        values[bitIndex / 32] = values[bitIndex / 32] or (1 shl (bitIndex % 32))
    }

    fun isJavaIdentifierPart(key: Char): Boolean {
        return get(bitsIsPart, key.code)
    }

    fun isJavaIdentifierStart(key: Char): Boolean {
        return get(bitsIsStart, key.code)
    }

    fun couldBeEmoji(cp: Int): Boolean {
        return cp in 0x1F000..0x1FAFF
    }

    fun isFitzpatrick(cp: Int): Boolean {
        return cp in 0x1F3FB..0x1F3FF
    }

    fun isZWJ(cp: Int): Boolean {
        return cp == 0x200D
    }

    fun isZWNJ(cp: Int): Boolean {
        return cp == 0x200C
    }

    fun isVariationSelector(cp: Int): Boolean {
        return cp == 0xFE0E || cp == 0xFE0F
    }

    fun isAlpha(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z')
    }
}

inline val Char.isJavaIdentifierPart: Boolean get() = MyCharacter.isJavaIdentifierPart(this)

inline val Char.isJavaIdentifierStart: Boolean get() = MyCharacter.isJavaIdentifierStart(this)

inline val Char.isAlpha: Boolean get() = MyCharacter.isAlpha(this)

inline val Int.couldBeEmoji: Boolean get() = MyCharacter.couldBeEmoji(this)

inline val Int.isFitzpatrick: Boolean get() = MyCharacter.isFitzpatrick(this)

inline val Int.isZWJ: Boolean get() = MyCharacter.isZWJ(this)

inline val Int.isZWNJ: Boolean get() = MyCharacter.isZWNJ(this)

inline val Int.isVariationSelector: Boolean get() = MyCharacter.isVariationSelector(this)

