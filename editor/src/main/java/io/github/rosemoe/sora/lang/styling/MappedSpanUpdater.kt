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

import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Update spans on text change event
 *
 * @author Rosemoe
 */
object MappedSpanUpdater {

    fun shiftSpansOnMultiLineDelete(
        map: MutableList<MutableList<Span>>,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ) {
        var lineCount = endLine - startLine - 1
        // Remove unrelated lines
        while (lineCount > 0) {
            SpanFactory.recycleAll(map.removeAt(startLine + 1))
            lineCount--
        }
        // Clean up start line
        val startLineSpans = map[startLine]
        var index = startLineSpans.size - 1
        while (index > 0) {
            if (startLineSpans[index].column >= startColumn) {
                startLineSpans.removeAt(index).recycle()
                index--
            } else {
                break
            }
        }
        // Shift end line
        val endLineSpans = map.removeAt(startLine + 1)
        for (i in endLineSpans.indices) {
            endLineSpans[i].shiftColumnBy(startColumn - endColumn)
        }
        while (endLineSpans.size > 1) {
            if (endLineSpans[0].column <= startColumn && endLineSpans[1].column <= startColumn) {
                endLineSpans.removeAt(0).recycle()
            } else {
                break
            }
        }
        if (endLineSpans[0].column <= startColumn) {
            endLineSpans[0].column = startColumn
        }
        startLineSpans.addAll(endLineSpans)
    }

    fun shiftSpansOnSingleLineDelete(
        map: MutableList<MutableList<Span>>?,
        line: Int,
        startCol: Int,
        endCol: Int
    ) {
        if (map == null || map.isEmpty()) {
            return
        }
        val spanList = map[line]
        val startIndex = findSpanIndexFor(spanList, 0, startCol)
        if (startIndex == -1) {
            // No span is to be updated
            return
        }
        var endIndex = findSpanIndexFor(spanList, startIndex, endCol)
        if (endIndex == -1) {
            endIndex = spanList.size
        }
        // Remove spans inside delete text
        val removeCount = endIndex - startIndex
        for (i in 0 until removeCount) {
            spanList.removeAt(startIndex).recycle()
        }
        // Shift spans
        val delta = endCol - startCol
        var shiftIdx = startIndex
        while (shiftIdx < spanList.size) {
            spanList[shiftIdx].shiftColumnBy(-delta)
            shiftIdx++
        }
        // Ensure there is span
        if (spanList.isEmpty() || spanList[0].column != 0) {
            spanList.add(0, SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
        }
        // Remove spans with length 0
        var i = 0
        while (i + 1 < spanList.size) {
            if (spanList[i].column >= spanList[i + 1].column) {
                spanList.removeAt(i).recycle()
                i--
            }
            i++
        }
    }

    fun shiftSpansOnSingleLineInsert(
        map: MutableList<MutableList<Span>>?,
        line: Int,
        startCol: Int,
        endCol: Int
    ) {
        if (map == null || map.isEmpty()) {
            return
        }
        val spanList = map[line]
        var index = findSpanIndexFor(spanList, 0, startCol)
        if (index == -1) {
            return
        }
        val originIndex = index
        // Shift spans after insert position
        val delta = endCol - startCol
        while (index < spanList.size) {
            spanList[index++].shiftColumnBy(delta)
        }
        // Add extra span for line start
        if (originIndex == 0) {
            val first = spanList[0]
            if (first.column == EditorColorScheme.TEXT_NORMAL && first.hasSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR)) {
                first.column = 0
            } else {
                spanList.add(0, SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
            }
        }
    }

    fun shiftSpansOnMultiLineInsert(
        map: MutableList<MutableList<Span>>,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ) {
        // Find extended span
        val startLineSpans = map[startLine]
        var extendedSpanIndex = findSpanIndexFor(startLineSpans, 0, startColumn)
        if (extendedSpanIndex == -1) {
            extendedSpanIndex = startLineSpans.size - 1
        }
        if (startLineSpans[extendedSpanIndex].column > startColumn) {
            extendedSpanIndex--
        }
        val extendedSpan: Span =
            if (extendedSpanIndex < 0 || extendedSpanIndex >= startLineSpans.size) {
                SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
            } else {
                startLineSpans[extendedSpanIndex]
            }
        // Create map lines for new lines
        for (i in 0 until endLine - startLine) {
            val list = mutableListOf<Span>()
            val newSpan = extendedSpan.copy()
            newSpan.column = 0
            list.add(newSpan)
            map.add(startLine + 1, list)
        }
        // Add original spans to new line
        val endLineSpans = map[endLine]
        var idx = extendedSpanIndex
        while (idx < startLineSpans.size) {
            val span = startLineSpans[idx++]
            val newSpan = span.copy()
            newSpan.column = (span.column - startColumn + endColumn).coerceAtLeast(0)
            endLineSpans.add(newSpan)
        }
        while (extendedSpanIndex + 1 < startLineSpans.size) {
            startLineSpans.removeAt(startLineSpans.size - 1).recycle()
        }
        if (endLineSpans.size > 1 && endLineSpans[0].column == 0 && endLineSpans[1].column == 0) {
            endLineSpans.removeAt(0).recycle()
        }
    }

    private fun findSpanIndexFor(spans: List<Span>, initialPosition: Int, targetCol: Int): Int {
        for (i in initialPosition until spans.size) {
            if (spans[i].column >= targetCol) {
                return i
            }
        }
        return -1
    }
}
