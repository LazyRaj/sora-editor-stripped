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
/*
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.widget.layout

import android.util.SparseArray
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.graphics.TextRow
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.CodeEditor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.min

/**
 * Wordwrap layout for editor
 *
 * This layout will not let character displayed outside the editor's width
 *
 * However, using this can be power-costing because we will have to recreate this layout in various
 * conditions, such as when the line number increases and its width grows or when the text size has changed
 *
 * @author Rose
 */
open class WordwrapLayout(
    editor: CodeEditor,
    text: Content?,
    private val antiWordBreaking: Boolean,
    private val supportRtlRow: Boolean,
    oldLayout: WordwrapLayout?,
    clearCache: Boolean
) : AbstractLayout(editor, text) {

    companion object {
        /**
         * When measuring text in wordwrap mode, we must use the max possible width of the character sequence
         * so that no character will be invisible after its styles are applied on actual drawing.
         */
        private val sSpansForWordwrap: MutableList<Span> = ArrayList<Span>().apply {
            add(SpanFactory.obtainNoExt(0, TextStyle.makeStyle(0, 0, true, true, false)))
        }
    }

    private val width: Int
    private val miniGraphWidth: Float
    private var rowTable: MutableList<RowRegion>?

    /**
     * Bumped on every text edit, so results of an async break that started before the edit are discarded
     */
    private val editCount = AtomicInteger(0)

    init {
        rowTable = if (oldLayout != null && oldLayout.rowTable != null) {
            oldLayout.rowTable
        } else {
            ArrayList()
        }
        if (clearCache) {
            rowTable?.clear()
        }
        miniGraphWidth =
            if ((editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
                editor.renderer.miniGraphWidth
            } else {
                0f
            }
        width =
            editor.width - (editor.measureTextRegionOffset() + editor.textPaint.measureText("a")).toInt() - (miniGraphWidth * 2).toInt()
        breakAllLines()
    }

    private fun breakAllLines() {
        val content = text ?: return
        val ed = editor ?: return
        val editCountLocal = editCount.get()
        val taskCount = min(
            SUBTASK_COUNT,
            ceil(content.lineCount.toFloat() / MIN_LINE_COUNT_FOR_SUBTASK).toInt()
        ).coerceAtLeast(1)
        val sizeEachTask = content.lineCount / taskCount
        val monitor = TaskMonitor(taskCount) { results, _ ->
            val curEditor = this.editor
            if (curEditor != null) {
                val r2 = ArrayList<WordwrapResult>()
                for (result in results) {
                    if (result is WordwrapResult) {
                        r2.add(result)
                    }
                }
                r2.sort()
                curEditor.postInLifecycle {
                    if (this.editor !== curEditor) {
                        return@postInLifecycle
                    }
                    if (editCountLocal != editCount.get()) {
                        breakAllLines()
                        return@postInLifecycle
                    }
                    val table = rowTable ?: ArrayList<RowRegion>().also { rowTable = it }
                    table.clear()
                    for (wordwrapResult in r2) {
                        table.addAll(wordwrapResult.regions)
                    }
                    curEditor.setLayoutBusy(false)
                    curEditor.eventHandler.scrollBy(0f, 0f)
                }
            }
        }
        ed.setLayoutBusy(true)
        for (i in 0 until taskCount) {
            val start = sizeEachTask * i
            val end = if (i + 1 == taskCount) content.lineCount - 1 else sizeEachTask * (i + 1) - 1
            submitTask(WordwrapAnalyzeTask(monitor, i, start, end, editCountLocal))
        }
    }

    private fun findRow(line: Int): Int {
        val table = rowTable ?: return 0
        if (table.isEmpty()) return 0
        var left = 0
        var right = table.size
        while (left <= right) {
            val mid = (left + right) / 2
            if (mid < 0 || mid >= table.size) {
                left = mid.coerceIn(0, table.size - 1)
                break
            }
            val value = table[mid].line
            if (value < line) {
                left = mid + 1
            } else if (value > line) {
                right = mid - 1
            } else {
                left = mid
                break
            }
        }
        var index = left.coerceIn(0, table.size - 1)
        while (index > 0 && table[index].startColumn > 0) {
            index--
        }
        return index
    }

    fun findRow(line: Int, column: Int): Int {
        val table = rowTable ?: return 0
        if (table.isEmpty()) return 0
        var row = findRow(line)
        while (table[row].endColumn <= column && row + 1 < table.size && table[row + 1].line == line) {
            row++
        }
        return row
    }

    private fun breakLines(startLine: Int, endLine: Int) {
        val table = rowTable ?: return
        var insertPosition = 0
        while (insertPosition < table.size) {
            if (table[insertPosition].line < startLine) {
                insertPosition++
            } else {
                break
            }
        }
        while (insertPosition < table.size) {
            val line = table[insertPosition].line
            if (line in startLine..endLine) {
                table.removeAt(insertPosition)
            } else {
                break
            }
        }
        val content = text ?: return
        val newRegions = ArrayList<RowRegion>()
        for (i in startLine..endLine) {
            newRegions.addAll(breakLine(i, content.getLine(i), null))
        }
        table.addAll(insertPosition, newRegions)
    }

    /**
     * Break a single line
     */
    private fun breakLine(line: Int, sequence: ContentLine, paint: Paint?): List<RowRegion> {
        val ed = editor ?: return emptyList()
        val content = text ?: return emptyList()
        val p = paint ?: Paint(ed.isRenderFunctionCharacters).apply {
            set(ed.textPaint)
        }
        val tr = TextRow()
        val directions = content.getLineDirections(line)
        tr.set(
            sequence,
            0,
            sequence.length,
            sSpansForWordwrap,
            getInlayHints(line),
            directions,
            p,
            null,
            ed.renderer.createTextRowParams()
        )

        var isRtlBased = false
        if (supportRtlRow && sequence.mayNeedBidi()) {
            var minRunLevel = Int.MAX_VALUE
            for (i in 0 until directions.runCount) {
                minRunLevel = minOf(minRunLevel, directions.getRunLevel(i))
            }
            if ((minRunLevel and 1) != 0) {
                isRtlBased = true
            }
        }

        val rows = tr.breakText(width, antiWordBreaking)
        val results = ArrayList<RowRegion>()
        for (row in rows) {
            results.add(
                RowRegion(
                    line,
                    row.startColumn,
                    row.endColumn,
                    row.inlayHints,
                    row.rowWidth,
                    isRtlBased
                )
            )
        }
        return results
    }

    override fun beforeReplace(content: Content) {
        // Intentionally empty
    }

    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        super.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        editCount.incrementAndGet()
        val table = rowTable ?: return
        val delta = endLine - startLine
        if (delta != 0) {
            for (row in findRow(startLine + 1) until table.size) {
                table[row].line += delta
            }
        }
        breakLines(startLine, endLine)
    }

    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        super.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        editCount.incrementAndGet()
        val table = rowTable ?: return
        val delta = endLine - startLine
        if (delta != 0) {
            var startRow = findRow(startLine)
            while (startRow < table.size) {
                val line = table[startRow].line
                if (line in startLine..endLine) {
                    table.removeAt(startRow)
                } else {
                    break
                }
            }
            for (row in findRow(endLine + 1) until table.size) {
                val region = table[row]
                if (region.line >= endLine) {
                    region.line -= delta
                }
            }
        }
        breakLines(startLine, startLine)
    }

    override fun destroyLayout() {
        super.destroyLayout()
        rowTable = null
    }

    override fun getRowAt(rowIndex: Int): Row {
        val table = rowTable
        val content = text
        if (table == null || table.isEmpty() || content == null) {
            val r = Row()
            r.startColumn = 0
            r.endColumn = content?.getColumnCount(rowIndex) ?: 0
            r.isLeadingRow = true
            r.isTrailingRow = true
            r.lineIndex = rowIndex
            r.inlayHints = getInlayHints(rowIndex)
            return r
        }
        val region = table[rowIndex]
        val isLeadingRow = rowIndex <= 0 || table[rowIndex - 1].line != region.line
        val isTrailingRow = rowIndex + 1 >= table.size || table[rowIndex + 1].line != region.line
        return region.toRow(isLeadingRow, isTrailingRow, width.toFloat())
    }

    override fun getLineNumberForRow(row: Int): Int {
        val table = rowTable
        val content = text
        if (table == null || table.isEmpty() || content == null) {
            return (content?.lineCount?.minus(1) ?: 0).let { maxLine ->
                row.coerceIn(0, maxOf(0, maxLine))
            }
        }
        return if (row >= table.size) table[table.size - 1].line else table[row].line
    }

    override fun obtainRowIterator(
        initialRow: Int,
        preloadedLines: SparseArray<ContentLine>?
    ): RowIterator {
        val table = rowTable
        val content = text
        return if (table == null || table.isEmpty() || content == null) {
            LineBreakLayout.LineBreakLayoutRowItr(
                this,
                content ?: throw IllegalStateException("No text"),
                initialRow,
                preloadedLines
            )
        } else {
            WordwrapLayoutRowItr(initialRow)
        }
    }

    override fun getUpPosition(line: Int, column: Int): Long {
        val table = rowTable
        val content = text
        if (table == null || table.isEmpty() || content == null) {
            if (line - 1 < 0) {
                return IntPair.pack(0, 0)
            }
            var col = column
            val cColumn = content?.getColumnCount(line - 1) ?: 0
            if (col > cColumn) {
                col = cColumn
            }
            return IntPair.pack(line - 1, col)
        }
        val row = findRow(line, column)
        if (row > 0) {
            var offset = column - table[row].startColumn
            val lastRow = table[row - 1]
            val max = lastRow.endColumn - lastRow.startColumn
            offset = minOf(offset, max)
            return IntPair.pack(lastRow.line, lastRow.startColumn + offset)
        }
        return IntPair.pack(0, 0)
    }

    override fun getDownPosition(line: Int, column: Int): Long {
        val table = rowTable
        val content = text
        if (table == null || table.isEmpty() || content == null) {
            val cLine = content?.lineCount ?: 0
            return if (line + 1 >= cLine) {
                IntPair.pack(line, content?.getColumnCount(line) ?: 0)
            } else {
                var col = column
                val cColumn = content?.getColumnCount(line + 1) ?: 0
                if (col > cColumn) {
                    col = cColumn
                }
                IntPair.pack(line + 1, col)
            }
        }
        val row = findRow(line, column)
        return if (row + 1 < table.size) {
            var offset = column - table[row].startColumn
            val nextRow = table[row + 1]
            val max = nextRow.endColumn - nextRow.startColumn
            offset = minOf(offset, max)
            IntPair.pack(nextRow.line, nextRow.startColumn + offset)
        } else {
            IntPair.pack(line, content.getColumnCount(line))
        }
    }

    override val layoutWidth: Int
        get() = 0

    override val layoutHeight: Int
        get() {
            val table = rowTable
            val content = text
            val ed = editor ?: return 0
            return if (table == null || table.isEmpty()) {
                ed.rowHeight * (content?.lineCount ?: 0)
            } else {
                table.size * ed.rowHeight
            }
        }

    override fun getRowIndexForPosition(index: Int): Int {
        val ed = editor ?: return 0
        val pos = ed.text.indexer.getCharPosition(index)
        val line = pos.line
        val table = rowTable ?: return line
        if (table.isEmpty()) {
            return line
        }
        val column = pos.column
        var row = findRow(line)
        if (row < table.size) {
            var region = table[row]
            if (region.line != line) {
                return 0
            }
            while (region.startColumn < column && row + 1 < table.size) {
                row++
                region = table[row]
                if (region.line != line || region.startColumn > column) {
                    row--
                    break
                }
            }
            return row
        }
        return 0
    }

    override fun invalidateLines(range: StyleUpdateRange) {
        val content = text ?: return
        val itr = range.lineIndexIterator(content.lineCount - 1)
        while (itr.hasNext()) {
            val line = itr.nextInt()
            breakLines(line, line)
        }
    }

    override fun getVisualPositionForLayoutOffset(
        offsetX: Float,
        offsetY: Float
    ): Layout.VisualLocation {
        val table = rowTable
        val content = text
        val ed = editor ?: return Layout.VisualLocation(0, 0)
        if (table == null || table.isEmpty() || content == null) {
            val lineCount = content?.lineCount ?: 0
            val line = (offsetY / ed.rowHeight).toInt().coerceIn(0, maxOf(0, lineCount - 1))
            val tr = ed.renderer.createTextRow(line)
            val pos = tr.getElementPositionForCursorOffset(offsetX)
            return Layout.VisualLocation(line, pos.textOffset, pos.element, pos.isInElementBounds)
        }
        var row = (offsetY / ed.rowHeight).toInt()
        row = row.coerceIn(0, table.size - 1)
        val region = table[row]
        var offX = offsetX
        if (region.startColumn != 0) {
            offX -= miniGraphWidth
        }
        offX -= region.getRenderTranslateX(width.toFloat())
        val tr = ed.renderer.createTextRow(row)
        val pos = tr.getElementPositionForCursorOffset(offX)
        return Layout.VisualLocation(
            region.line,
            pos.textOffset,
            pos.element,
            pos.isInElementBounds
        )
    }

    override fun getCharLayoutOffset(line: Int, column: Int, array: FloatArray?): FloatArray {
        val dest = if (array == null || array.size < 2) FloatArray(2) else array
        val table = rowTable
        val ed = editor ?: return dest
        if (table == null || table.isEmpty()) {
            dest[0] = ed.getRowBottom(line).toFloat()
            val tr = ed.renderer.createTextRow(line)
            dest[1] = tr.getCursorOffsetForIndex(column)
            return dest
        }
        var row = findRow(line)
        if (row < table.size) {
            var region = table[row]
            if (region.line != line) {
                dest[1] = 0f
                dest[0] = 0f
                return dest
            }
            while (region.startColumn < column && row + 1 < table.size) {
                row++
                region = table[row]
                if (region.line != line || region.startColumn > column) {
                    row--
                    region = table[row]
                    break
                }
            }
            dest[0] = ed.getRowBottom(row).toFloat()
            val tr = ed.renderer.createTextRow(row)
            dest[1] = tr.getCursorOffsetForIndex(column)
            if (region.startColumn != 0) {
                dest[1] += miniGraphWidth
            }
            dest[1] += region.getRenderTranslateX(width.toFloat())
        } else {
            dest[1] = 0f
            dest[0] = 0f
        }
        return dest
    }

    override fun getRowCountForLine(line: Int): Int {
        val table = rowTable
        if (table == null || table.isEmpty()) {
            return 1
        }
        var row = findRow(line)
        var count = 0
        while (row < table.size && table[row].line == line) {
            count++
            row++
        }
        return count
    }

    /**
     * Get soft breaks on the given line
     */
    open fun getSoftBreaksForLine(line: Int): List<Int> {
        val table = rowTable
        if (table == null || table.isEmpty()) {
            return emptyList()
        }
        var row = findRow(line)
        val list = ArrayList<Int>()
        while (row < table.size && table[row].line == line) {
            val column = table[row].startColumn
            if (column != 0) {
                list.add(column)
            }
            row++
        }
        return list
    }

    override val rowCount: Int
        get() {
            val table = rowTable
            return if (table == null || table.isEmpty()) {
                text?.lineCount ?: 0
            } else {
                table.size
            }
        }

    internal class RowRegion(
        var line: Int,
        val startColumn: Int,
        val endColumn: Int,
        var inlayHints: List<InlayHint>?,
        var rowWidth: Float,
        var displayFromRight: Boolean
    ) {
        fun toRow(isLeadingRow: Boolean, isTrailingRow: Boolean, layoutWidth: Float): Row =
            Row(
                lineIndex = line,
                isLeadingRow = isLeadingRow,
                isTrailingRow = isTrailingRow,
                startColumn = startColumn,
                endColumn = endColumn,
                inlayHints = inlayHints ?: emptyList(),
                renderTranslateX = getRenderTranslateX(layoutWidth)
            )

        fun getRenderTranslateX(layoutWidth: Float): Float {
            return if (displayFromRight && layoutWidth > rowWidth) layoutWidth - rowWidth else 0f
        }

        override fun toString(): String {
            return "RowRegion(startColumn=$startColumn, endColumn=$endColumn, line=$line)"
        }
    }

    private class WordwrapResult(
        val index: Int,
        val regions: List<RowRegion>
    ) : Comparable<WordwrapResult> {
        override fun compareTo(other: WordwrapResult): Int {
            return index.compareTo(other.index)
        }
    }

    inner class WordwrapLayoutRowItr(
        private val initialRow: Int
    ) : RowIterator {

        private val result = Row()
        private var currentRow = initialRow

        override fun next(): Row {
            val table = rowTable ?: throw NoSuchElementException()
            if (!hasNext()) {
                throw NoSuchElementException()
            }
            val region = table[currentRow]
            result.lineIndex = region.line
            result.startColumn = region.startColumn
            result.endColumn = region.endColumn
            result.inlayHints = region.inlayHints ?: emptyList()
            result.isLeadingRow = currentRow <= 0 || table[currentRow - 1].line != region.line
            result.isTrailingRow =
                currentRow + 1 >= table.size || table[currentRow + 1].line != region.line
            result.renderTranslateX = region.getRenderTranslateX(width.toFloat())
            currentRow++
            return result
        }

        override fun hasNext(): Boolean {
            val table = rowTable ?: return false
            return currentRow in 0 until table.size
        }

        override fun reset() {
            currentRow = initialRow
        }
    }

    private inner class WordwrapAnalyzeTask(
        monitor: TaskMonitor,
        private val id: Int,
        private val start: Int,
        private val end: Int,
        private val editCountLocal: Int
    ) : LayoutTask<WordwrapResult>(monitor) {

        private val paint: Paint

        init {
            val ed = editor ?: throw IllegalStateException("Editor destroyed")
            paint = Paint(ed.isRenderFunctionCharacters).apply {
                set(ed.textPaint)
                onAttributeUpdate()
            }
        }

        override fun compute(): WordwrapResult {
            val list = ArrayList<RowRegion>()
            val content = text
            content?.runReadActionsOnLines(
                start,
                end,
                Content.ContentLineConsumer2 { index, line, abortFlag ->
                    list.addAll(breakLine(index, line, paint))
                    if (!shouldRun()) {
                        abortFlag.set = true
                    }
                })
            return WordwrapResult(id, list)
        }

        override fun shouldRun(): Boolean {
            return super.shouldRun() && editCount.get() == editCountLocal
        }
    }
}
