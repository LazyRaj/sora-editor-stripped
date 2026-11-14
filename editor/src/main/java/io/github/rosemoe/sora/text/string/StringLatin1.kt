/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

package io.github.rosemoe.sora.text.string

import java.nio.charset.StandardCharsets

object StringLatin1 {

    fun canEncode(c: Char): Boolean = c.code <= 0x00FF

    fun getChar(value: ByteArray, index: Int): Char = (value[index].toInt() and 0xFF).toChar()

    fun putChar(value: ByteArray, index: Int, c: Char) {
        value[index] = c.code.toByte()
    }

    fun copyChars(src: ByteArray, srcBegin: Int, dst: ByteArray, dstBegin: Int, len: Int) {
        System.arraycopy(src, srcBegin, dst, dstBegin, len)
    }

    fun getChars(value: ByteArray, srcBegin: Int, srcEnd: Int, dst: CharArray, dstBegin: Int) {
        var d = dstBegin
        for (i in srcBegin until srcEnd) {
            dst[d++] = getChar(value, i)
        }
    }

    fun appendTo(value: ByteArray, length: Int, sb: StringBuilder) {
        for (i in 0 until length) {
            sb.append(getChar(value, i))
        }
    }

    fun newString(value: ByteArray, length: Int): String {
        return String(value, 0, length, StandardCharsets.ISO_8859_1)
    }

    fun inflateToUTF16(value: ByteArray, length: Int, newCapacity: Int): ByteArray {
        val utf16 = ByteArray(StringUTF16.bytesForChars(newCapacity))
        for (i in 0 until length) {
            StringUTF16.putChar(utf16, i, getChar(value, i))
        }
        return utf16
    }
}

/**
 * Extension property to check if a character can be encoded in Latin1 (ISO-8859-1).
 */
inline val Char.canEncodeLatin1: Boolean
    get() = StringLatin1.canEncode(this)

