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
// Modified by Raj Dave (aka LazyRaj)
package io.github.rosemoe.sora.lang.styling

import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.util.Collections

/**
 * Store spans by mapping.
 *
 * @see Builder
 */
class MappedSpans private constructor(
    private val spanMap: MutableList<MutableList<Span>>
) : Spans {

    override fun getLineCount(): Int = spanMap.size

    override fun adjustOnInsert(start: CharPosition, end: CharPosition) {
        val startLine = start.line
        val endLine = end.line
        val startColumn = start.column
        val endColumn = end.column
        if (startLine == endLine) {
            MappedSpanUpdater.shiftSpansOnSingleLineInsert(
                spanMap,
                startLine,
                startColumn,
                endColumn
            )
        } else {
            MappedSpanUpdater.shiftSpansOnMultiLineInsert(
                spanMap,
                startLine,
                startColumn,
                endLine,
                endColumn
            )
        }
    }

    override fun adjustOnDelete(start: CharPosition, end: CharPosition) {
        val startLine = start.line
        val endLine = end.line
        val startColumn = start.column
        val endColumn = end.column
        if (startLine == endLine) {
            MappedSpanUpdater.shiftSpansOnSingleLineDelete(
                spanMap,
                startLine,
                startColumn,
                endColumn
            )
        } else {
            MappedSpanUpdater.shiftSpansOnMultiLineDelete(
                spanMap,
                startLine,
                startColumn,
                endLine,
                endColumn
            )
        }
    }

    override fun read(): Spans.Reader = MappedSpansAccessor()

    override fun supportsModify(): Boolean = true

    override fun modify(): Spans.Modifier = MappedSpansAccessor()

    /**
     * Allow you to build a span map linearly.
     */
    class Builder(lineCapacity: Int = 128) {

        private val spans: MutableList<MutableList<Span>> = ArrayList(lineCapacity)
        private var last: Span? = null

        /**
         * Add a new span if required.
         *
         * If no special style is specified, you can use colorId as style long integer
         *
         * @param spanLine Line
         * @param column   Column
         * @param style    Style of text
         */
        fun addIfNeeded(spanLine: Int, column: Int, style: Long) {
            if (last?.style == style) {
                return
            }
            add(spanLine, SpanFactory.obtainNoExt(column, style))
        }

        /**
         * Add a span directly
         *
         * Note: the line should always >= the line of span last committed
         *
         * If two spans are on the same line, you must add them in order by their column
         *
         * @param spanLine The line position of span
         * @param span     The span
         */
        fun add(spanLine: Int, span: Span) {
            var mapLine = spans.size - 1
            if (spanLine == mapLine) {
                spans[spanLine].add(span)
            } else if (spanLine > mapLine) {
                val extendedSpan =
                    last ?: SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
                while (mapLine < spanLine) {
                    val lineSpans = mutableListOf<Span>()
                    lineSpans.add(copyAndSetColumn(extendedSpan, 0))
                    spans.add(lineSpans)
                    mapLine++
                }
                val lineSpans = spans[spanLine]
                if (span.column == 0) {
                    lineSpans.clear()
                }
                lineSpans.add(span)
            } else {
                throw IllegalStateException("Invalid position")
            }
            last = span
        }

        /**
         * This method must be called when whole text is analyzed.
         * **Note that it is not the line count but line index!**
         *
         * @param line The line is the line last of text
         */
        fun determine(line: Int) {
            var mapLine = spans.size - 1
            val extendedSpan =
                last ?: SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
            while (mapLine < line) {
                val lineSpans = mutableListOf<Span>()
                lineSpans.add(copyAndSetColumn(extendedSpan, 0))
                spans.add(lineSpans)
                mapLine++
            }
        }

        /**
         * Ensure the list not empty
         */
        fun addNormalIfNull() {
            if (spans.isEmpty()) {
                val spanList = mutableListOf<Span>()
                spanList.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
                spans.add(spanList)
            }
        }

        fun build(): MappedSpans = MappedSpans(spans)
    }

    private inner class MappedSpansAccessor : Spans.Reader, Spans.Modifier {

        private var span: List<Span>? = null

        private fun checkLine(): List<Span> {
            return span ?: throw IllegalStateException("line must be set first")
        }

        override fun moveToLine(line: Int) {
            if (line == -1) {
                span = null
                return
            }
            span = spanMap[line]
        }

        override fun getSpanCount(): Int = checkLine().size

        override fun getSpanAt(index: Int): Span {
            return checkLine()[index]
        }

        override fun getSpansOnLine(line: Int): List<Span> {
            return Collections.unmodifiableList(spanMap[line])
        }

        override fun setSpansOnLine(line: Int, spans: List<Span>) {
            val last = spanMap[spanMap.size - 1]
            val extend = last[last.size - 1]
            while (spanMap.size <= line) {
                val list = mutableListOf<Span>()
                list.add(copyAndSetColumn(extend, 0))
                spanMap.add(list)
            }
            spanMap[line] = spans.toMutableList()
        }

        override fun addLineAt(line: Int, spans: List<Span>) {
            spanMap.add(line, spans.toMutableList())
        }

        override fun deleteLineAt(line: Int) {
            spanMap.removeAt(line)
        }
    }

    companion object {
        private fun copyAndSetColumn(s: Span, column: Int): Span {
            val span = s.copy()
            span.column = column
            return span
        }
    }
}
