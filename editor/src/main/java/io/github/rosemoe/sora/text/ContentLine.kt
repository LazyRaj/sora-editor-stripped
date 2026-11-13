/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
/**
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.text

import android.text.GetChars
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage
import io.github.rosemoe.sora.text.bidi.BidiRequirementChecker
import io.github.rosemoe.sora.text.bidi.TextBidi
import io.github.rosemoe.sora.text.string.StringLatin1
import io.github.rosemoe.sora.text.string.StringUTF16
import io.github.rosemoe.sora.util.ShareableData
import java.util.concurrent.atomic.AtomicInteger

class ContentLine : CharSequence, GetChars, BidiRequirementChecker, ShareableData<ContentLine> {

    companion object {
        private const val LATIN1: Byte = 0
        private const val UTF16: Byte = 1

        private fun requiresUTF16(s: CharSequence, start: Int, end: Int): Boolean {
            for (i in start until end) {
                if (!StringLatin1.canEncode(s[i])) {
                    return true
                }
            }
            return false
        }
    }

    private var value: ByteArray
    private var coder: Byte
    private var _length: Int

    private var rtlAffectingCount: Int
    private var _lineSeparator: LineSeparator? = null

    @Transient
    private var refCount: AtomicInteger? = null

    constructor() : this(true)

    constructor(text: CharSequence?) : this(true) {
        insert(0, text)
    }

    constructor(src: ContentLine) {
        _length = src._length
        coder = src.coder
        value = if (coder == LATIN1) {
            ByteArray(src._length + 16)
        } else {
            ByteArray(StringUTF16.bytesForChars(src._length + 16))
        }
        rtlAffectingCount = src.rtlAffectingCount
        _lineSeparator = src._lineSeparator
        if (coder == LATIN1) {
            StringLatin1.copyChars(src.value, 0, value, 0, _length)
        } else {
            StringUTF16.copyChars(src.value, 0, value, 0, _length)
        }
    }

    constructor(size: Int) {
        _length = 0
        coder = LATIN1
        value = ByteArray(size)
        rtlAffectingCount = 0
    }

    private constructor(initialize: Boolean) {
        if (initialize) {
            _length = 0
            coder = LATIN1
            value = ByteArray(32)
        } else {
            _length = 0
            coder = LATIN1
            value = ByteArray(0)
        }
        rtlAffectingCount = 0
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index > _length) {
            throw StringIndexOutOfBoundsException("index = $index, length = $_length")
        }
    }

    private fun ensureCapacity(capacity: Int) {
        if (charCapacity() < capacity) {
            val oldCapacity = charCapacity()
            val newLength = if (oldCapacity * 2 < capacity) capacity + 2 else oldCapacity * 2
            val newValue = if (coder == LATIN1) {
                ByteArray(newLength)
            } else {
                ByteArray(StringUTF16.bytesForChars(newLength))
            }
            if (coder == LATIN1) {
                StringLatin1.copyChars(value, 0, newValue, 0, _length)
            } else {
                StringUTF16.copyChars(value, 0, newValue, 0, _length)
            }
            value = newValue
        }
    }

    private fun charCapacity(): Int = if (coder == LATIN1) value.size else value.size shr 1

    private fun charAtInternal(index: Int): Char =
        if (coder == LATIN1) StringLatin1.getChar(value, index) else StringUTF16.getChar(
            value,
            index
        )

    private fun putCharInternal(index: Int, c: Char) {
        if (coder == LATIN1) {
            StringLatin1.putChar(value, index, c)
        } else {
            StringUTF16.putChar(value, index, c)
        }
    }

    private fun copyCharsInternal(
        src: ByteArray,
        srcBegin: Int,
        dst: ByteArray,
        dstBegin: Int,
        len: Int
    ) {
        if (coder == LATIN1) {
            StringLatin1.copyChars(src, srcBegin, dst, dstBegin, len)
        } else {
            StringUTF16.copyChars(src, srcBegin, dst, dstBegin, len)
        }
    }

    private fun toUTF16IfNeeded(capacity: Int) {
        if (coder == LATIN1) {
            val oldCapacity = charCapacity()
            val newCapacity = if (oldCapacity * 2 < capacity) capacity + 2 else oldCapacity * 2
            value = StringLatin1.inflateToUTF16(value, _length, newCapacity)
            coder = UTF16
        }
    }

    fun insert(dstOffset: Int, s: CharSequence?): ContentLine {
        val seq = s ?: "null"
        return insert(dstOffset, seq, 0, seq.length)
    }

    fun insert(dstOffset: Int, s: CharSequence?, start: Int, end: Int): ContentLine {
        val seq = s ?: "null"
        if (dstOffset < 0 || dstOffset > length) {
            throw IndexOutOfBoundsException("dstOffset $dstOffset")
        }
        if (start < 0 || end < 0 || start > end || end > seq.length) {
            throw IndexOutOfBoundsException("start $start, end $end, s.length() ${seq.length}")
        }
        val len = end - start
        if (coder == LATIN1 && requiresUTF16(seq, start, end)) {
            toUTF16IfNeeded(_length + len)
        }
        ensureCapacity(_length + len)
        copyCharsInternal(value, dstOffset, value, dstOffset + len, _length - dstOffset)
        var offset = dstOffset
        for (i in start until end) {
            val ch = seq[i]
            putCharInternal(offset++, ch)
            if (TextBidi.couldAffectRtl(ch)) {
                rtlAffectingCount++
            }
        }
        _length += len
        return this
    }

    fun insert(offset: Int, c: Char): ContentLine {
        if (coder == LATIN1 && !StringLatin1.canEncode(c)) {
            toUTF16IfNeeded(_length + 1)
        }
        ensureCapacity(_length + 1)
        if (offset < _length) {
            copyCharsInternal(value, offset, value, offset + 1, _length - offset)
        }
        if (TextBidi.couldAffectRtl(c)) {
            rtlAffectingCount++
        }
        putCharInternal(offset, c)
        _length += 1
        return this
    }

    fun delete(start: Int, end: Int): ContentLine {
        if (start < 0) {
            throw StringIndexOutOfBoundsException(start)
        }
        val actualEnd = if (end > _length) _length else end
        if (start > actualEnd) {
            throw StringIndexOutOfBoundsException()
        }
        val len = actualEnd - start
        if (len > 0) {
            for (i in start until actualEnd) {
                if (TextBidi.couldAffectRtl(charAtInternal(i))) {
                    rtlAffectingCount--
                }
            }
            copyCharsInternal(value, start + len, value, start, _length - actualEnd)
            _length -= len
        }
        return this
    }

    override fun mayNeedBidi(): Boolean = rtlAffectingCount > 0

    fun append(text: CharSequence?): ContentLine = insert(_length, text)

    override val length: Int
        get() = _length

    @UnsupportedUserUsage
    override fun get(index: Int): Char {
        if (index >= _length) {
            val separator = lineSeparator
            return if (separator.length > 0) separator.content[index - _length] else '\n'
        }
        return charAtInternal(index)
    }

    override fun subSequence(startIndex: Int, endIndex: Int): ContentLine {
        checkIndex(startIndex)
        checkIndex(endIndex)
        if (endIndex < startIndex) {
            throw StringIndexOutOfBoundsException("start is greater than end")
        }
        val res = ContentLine(false)
        res.coder = coder
        if (res.coder == LATIN1) {
            res.value = ByteArray(endIndex - startIndex + 16)
            StringLatin1.copyChars(value, startIndex, res.value, 0, endIndex - startIndex)
        } else {
            res.value = ByteArray(StringUTF16.bytesForChars(endIndex - startIndex + 16))
            StringUTF16.copyChars(value, startIndex, res.value, 0, endIndex - startIndex)
        }
        res._length = endIndex - startIndex

        if (rtlAffectingCount > 0) {
            for (i in 0 until res._length) {
                if (TextBidi.couldAffectRtl(res[i])) {
                    res.rtlAffectingCount++
                }
            }
        }
        return res
    }

    fun appendTo(sb: StringBuilder) {
        if (coder == LATIN1) {
            StringLatin1.appendTo(value, _length, sb)
        } else {
            StringUTF16.appendTo(value, _length, sb)
        }
    }

    override fun toString(): String =
        if (coder == LATIN1) StringLatin1.newString(
            value,
            _length
        ) else StringUTF16.newString(value, _length)

    fun toStringWithNewline(): String {
        if (charCapacity() == _length) {
            ensureCapacity(_length + 1)
        }
        putCharInternal(_length, '\n')
        return if (coder == LATIN1) {
            StringLatin1.newString(value, _length + 1)
        } else {
            StringUTF16.newString(value, _length + 1)
        }
    }

    override fun getChars(srcBegin: Int, srcEnd: Int, dst: CharArray, dstBegin: Int) {
        if (srcBegin < 0) throw StringIndexOutOfBoundsException(srcBegin)
        if (srcEnd < 0 || srcEnd > _length) throw StringIndexOutOfBoundsException(srcEnd)
        if (srcBegin > srcEnd) throw StringIndexOutOfBoundsException("srcBegin > srcEnd")
        if (coder == LATIN1) {
            StringLatin1.getChars(value, srcBegin, srcEnd, dst, dstBegin)
        } else {
            StringUTF16.getChars(value, srcBegin, srcEnd, dst, dstBegin)
        }
    }

    var lineSeparator: LineSeparator
        get() = _lineSeparator ?: LineSeparator.NONE
        set(value) {
            _lineSeparator = value
        }

    fun copy(): ContentLine {
        val clone = ContentLine(false)
        clone._length = _length
        clone.coder = coder
        clone.value = ByteArray(value.size)
        System.arraycopy(value, 0, clone.value, 0, value.size)
        clone.rtlAffectingCount = rtlAffectingCount
        clone._lineSeparator = _lineSeparator
        return clone
    }

    @Synchronized
    override fun retain() {
        val rc = refCount
        if (rc == null) {
            refCount = AtomicInteger(2)
        } else {
            rc.incrementAndGet()
        }
    }

    @Synchronized
    override fun release() {
        val rc = refCount ?: return
        val count = rc.decrementAndGet()
        check(count >= 0) { "illegal operation. There is no active owner" }
    }

    override fun isMutable(): Boolean {
        val rc = refCount
        return rc == null || rc.get() <= 1
    }

    @Synchronized
    override fun toMutable(): ContentLine {
        if (isMutable()) {
            return this
        }
        return copy()
    }
}
