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

import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer
import java.nio.charset.Charset

/**
 * Helper class for creating or saving [Content] objects, with minimal extra memory usage when
 * processing.
 *
 * @author Rosemoe
 */
object ContentIO {

    private const val BUFFER_SIZE = 16384
    internal const val CHAR_BUFFER_SIZE = 1024

    /**
     * Create a [Content] from stream.
     * The stream will get closed if the operation is successfully done.
     *
     * @param stream Source stream
     */
    @JvmStatic
    @Throws(IOException::class)
    fun createFrom(stream: InputStream): Content = createFrom(stream, Charset.defaultCharset())

    /**
     * Create a [Content] from stream.
     * The stream will get closed if the operation is successfully done.
     *
     * @param stream  Source stream
     * @param charset Charset for decoding the content
     */
    @JvmStatic
    @Throws(IOException::class)
    fun createFrom(stream: InputStream, charset: Charset): Content =
        createFrom(InputStreamReader(stream, charset))

    /**
     * Create a [Content] from reader.
     *
     * The reader will get closed if the operation is successfully done.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun createFrom(reader: Reader): Content {
        val content = Content()
        content.setUndoEnabled(false)
        val buffer = CharArray(BUFFER_SIZE)
        val wrapper = CharArrayWrapper(buffer, 0)
        var count: Int
        while (reader.read(buffer).also { count = it } != -1) {
            if (count > 0) {
                if (buffer[count - 1] == '\r') {
                    val peek = reader.read()
                    if (peek == '\n'.code) {
                        wrapper.setDataCount(count - 1)
                        var line = content.lineCount - 1
                        content.insert(line, content.getColumnCount(line), wrapper)
                        line = content.lineCount - 1
                        content.insert(line, content.getColumnCount(line), "\r\n")
                        continue
                    } else if (peek != -1) {
                        wrapper.setDataCount(count)
                        var line = content.lineCount - 1
                        content.insert(line, content.getColumnCount(line), wrapper)
                        line = content.lineCount - 1
                        content.insert(line, content.getColumnCount(line), (peek.toChar()).toString())
                        continue
                    }
                }
                wrapper.setDataCount(count)
                val line = content.lineCount - 1
                content.insert(line, content.getColumnCount(line), wrapper)
            }
        }
        reader.close()
        content.setUndoEnabled(true)
        return content
    }

    /**
     * Write the text to the given stream with default charset. Close the stream if `closeOnSucceed` is true.
     *
     * @param text           Text to be written
     * @param stream         Output stream
     * @param closeOnSucceed If true, the stream will be closed when operation is successfully
     */
    @JvmStatic
    @Throws(IOException::class)
    fun writeTo(text: Content, stream: OutputStream, closeOnSucceed: Boolean) {
        writeTo(text, stream, Charset.defaultCharset(), closeOnSucceed)
    }

    /**
     * Write the text to the given stream with given charset. Close the stream if `closeOnSucceed` is true.
     *
     * @param text           Text to be written
     * @param stream         Output stream
     * @param charset        Charset of output bytes
     * @param closeOnSucceed If true, the stream will be closed when operation is successfully
     */
    @JvmStatic
    @Throws(IOException::class)
    fun writeTo(text: Content, stream: OutputStream, charset: Charset, closeOnSucceed: Boolean) {
        writeTo(text, OutputStreamWriter(stream, charset), closeOnSucceed)
    }

    /**
     * Write the text to the given writer. Close the writer if `closeOnSucceed` is true.
     *
     * If you use [BufferedWriter], make sure you set an appropriate buffer size. We recommend using the default size (8192) or larger.
     *
     * @param text           Text to be written
     * @param writer         Output writer
     * @param closeOnSucceed If true, the stream will be closed when operation is successful
     */
    @JvmStatic
    @Throws(IOException::class)
    @Suppress("DEPRECATION")
    fun writeTo(text: Content, writer: Writer, closeOnSucceed: Boolean) {
        // Use buffered writer to avoid frequently IO when there are a lot of short lines
        val buffered = if (writer is BufferedWriter) writer else BufferedWriter(writer, BUFFER_SIZE)
        val buf = CharArray(CHAR_BUFFER_SIZE)
        try {
            text.runReadActionsOnLines(0, text.lineCount - 1, Content.ContentLineConsumer2 { _, line, _ ->
                try {
                    // Write line content
                    var i = 0
                    while (i < line.length) {
                        val end = minOf(i + buf.size, line.length)
                        line.getChars(i, end, buf, 0)
                        buffered.write(buf, 0, end - i)
                        i = end
                    }
                    // Write line feed (the last line has empty line feed)
                    buffered.write(line.lineSeparator.chars)
                } catch (e: IOException) {
                    // To be handled by outer code
                    throw RuntimeException(e)
                }
            })
        } catch (e: RuntimeException) {
            val cause = e.cause
            if (cause is IOException) {
                throw cause
            } else {
                throw e
            }
        }
        buffered.flush()
        if (closeOnSucceed) {
            buffered.close()
        }
    }
}
