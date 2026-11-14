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

import java.io.IOException
import java.io.Reader

/**
 * Reference of a content due to be accessed in read-only mode.
 * Access can be validated during accesses.
 * [io.github.rosemoe.sora.text.TextReference.ValidateFailedException] may be thrown if the check is failed.
 * The result of methods may be dirty when the content is modified.
 *
 * @author Rosemoe
 */
open class ContentReference(ref: Content) : TextReference(ref) {

    val content: Content = ref

    override val reference: Content
        get() = content

    override fun get(index: Int): Char {
        validateAccess()
        return content[index]
    }

    open fun charAt(line: Int, column: Int): Char {
        validateAccess()
        return content.charAt(line, column)
    }

    open fun getCharIndex(line: Int, column: Int): Int {
        validateAccess()
        return content.getCharIndex(line, column)
    }

    open fun getCharPosition(line: Int, column: Int): CharPosition {
        validateAccess()
        return content.indexer.getCharPosition(line, column)
    }

    open fun getCharPosition(index: Int): CharPosition {
        validateAccess()
        return content.indexer.getCharPosition(index)
    }

    /**
     * @see Content.getLineCount
     */
    open val lineCount: Int
        get() {
            validateAccess()
            return content.lineCount
        }

    /**
     * @see Content.getColumnCount
     */
    open fun getColumnCount(line: Int): Int {
        validateAccess()
        return content.getColumnCount(line)
    }

    /**
     * @see Content.getLineSeparatorUnsafe
     */
    open fun getLineSeparator(line: Int): String {
        validateAccess()
        return content.getLineSeparatorUnsafe(line).content
    }

    /**
     * @see Content.getLineString
     */
    open fun getLine(line: Int): String {
        validateAccess()
        return content.getLineString(line)
    }

    /**
     * @see Content.getLineChars
     */
    open fun getLineChars(line: Int, dest: CharArray) {
        validateAccess()
        content.getLineChars(line, dest)
    }

    /**
     * @see Content.getLine
     */
    open fun appendLineTo(sb: StringBuilder, line: Int) {
        validateAccess()
        content.getLine(line).appendTo(sb)
    }

    /**
     * @see Content.getDocumentVersion
     */
    open val documentVersion: Long
        get() {
            validateAccess()
            return content.documentVersion
        }

    /**
     * Create a reader to read the text
     */
    open fun createReader(): Reader {
        return RefReader()
    }

    override fun setValidator(validator: Validator?): ContentReference {
        super.setValidator(validator)
        return this
    }

    private inner class RefReader : Reader() {

        private var markedLine: Int = 0
        private var markedColumn: Int = 0
        private var line: Int = 0
        private var column: Int = 0

        override fun read(chars: CharArray, offset: Int, length: Int): Int {
            if (chars.size < offset + length) {
                throw IllegalArgumentException("size not enough")
            }
            var read = 0
            while (read < length && line < lineCount) {
                val targetLine = content.getLine(line)
                val separatorLength = targetLine.lineSeparator.length
                val columnCount = targetLine.length
                var toRead = minOf(columnCount - column, length - read)
                toRead = maxOf(0, toRead)
                if (toRead > 0) {
                    content.getRegionOnLine(line, column, column + toRead, chars, offset + read)
                }
                column += toRead
                read += toRead
                while (read < length && columnCount <= column && column < columnCount + separatorLength) {
                    chars[offset + read] = targetLine.lineSeparator.content[column - columnCount]
                    read++
                    column++
                }
                if (column >= columnCount + separatorLength) {
                    line++
                    column = 0
                }
            }
            if (read == 0) {
                return -1
            }
            return read
        }

        override fun close() {
            line = Int.MAX_VALUE
            column = Int.MAX_VALUE
        }

        override fun markSupported(): Boolean = true

        override fun mark(readAheadLimit: Int) {
            markedLine = line
            markedColumn = column
        }

        override fun reset() {
            line = markedLine
            column = markedColumn
        }
    }
}
