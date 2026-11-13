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
import io.github.rosemoe.sora.graphics.SingleCharacterWidths
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.util.BlockIntList
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.CodeEditor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Layout implementation of editor
 * This layout is never broke unless there is actually a newline character
 *
 * @author Rose
 */
open class LineBreakLayout(
    editor: CodeEditor,
    text: Content?
) : AbstractLayout(editor, text) {

    private val reuseCount = AtomicInteger(0)
    private var widthMaintainer: BlockIntList? = null
    private var inlineElementsWidths: BlockIntList? = null
    private var measurer: SingleCharacterWidths? = null

    init {
        val m = SingleCharacterWidths(editor.tabWidth).apply {
            isHandleFunctionCharacters = editor.isRenderFunctionCharacters
        }
        measurer = m
        val wm = BlockIntList()
        val iew = BlockIntList()
        widthMaintainer = wm
        inlineElementsWidths = iew
        measureAllLines(wm, iew)
    }

    private fun measureAllLines(wm: BlockIntList, iew: BlockIntList) {
        val content = text ?: return
        val ed = editor ?: return
        val shadowPaint = Paint(ed.isRenderFunctionCharacters).apply {
            set(ed.textPaint)
            onAttributeUpdate()
        }
        val reuseCountLocal = reuseCount.get()
        val measurerLocal = measurer ?: return
        val monitor = TaskMonitor(1) { _, cancelledCount ->
            val curEditor = this.editor
            if (curEditor == null || cancelledCount > 0) {
                return@TaskMonitor
            }
            curEditor.postInLifecycle {
                if (this.editor !== curEditor || reuseCountLocal != reuseCount.get()) {
                    // This layout could have been abandoned when waiting for Runnable execution
                    return@postInLifecycle
                }
                curEditor.setLayoutBusy(false)
                curEditor.eventHandler.scrollBy(0f, 0f)
            }
        }
        val task = object : LayoutTask<Void?>(monitor) {
            override fun compute(): Void? {
                wm.lock.lock()
                try {
                    content.runReadActionsOnLines(
                        0,
                        content.lineCount - 1,
                        Content.ContentLineConsumer2 { index, line, abortFlag ->
                            val width =
                                measurerLocal.measureText(line, 0, line.length, shadowPaint).toInt()
                            val inlineElementsWidth =
                                measureInlayHints(getInlayHints(index), shadowPaint)
                            if (shouldRun()) {
                                wm.add(width + inlineElementsWidth)
                                iew.add(inlineElementsWidth)
                            } else {
                                abortFlag.set = true
                            }
                        })
                } finally {
                    wm.lock.unlock()
                }
                return null
            }

            override fun shouldRun(): Boolean {
                return super.shouldRun() && reuseCount.get() == reuseCountLocal
            }
        }
        ed.setLayoutBusy(true)
        submitTask(task)
    }

    private fun measureInlayHints(inlayHints: List<InlayHint>, paint: Paint): Int {
        val ed = editor ?: return 0
        var width = 0f
        val params = ed.renderer.createTextRowParams().toInlayHintRenderParams()
        for (inlayHint in inlayHints) {
            val renderer = ed.getInlayHintRendererForType(inlayHint.type) ?: continue
            val w = renderer.measure(inlayHint, paint, params)
            width += w
        }
        return width.toInt()
    }

    private fun measureLineAndUpdateInlineWidths(lineIndex: Int): Int {
        return measureLineAndUpdateInlineWidths(lineIndex, false)
    }

    private fun measureLineAndUpdateInlineWidths(lineIndex: Int, useAdd: Boolean): Int {
        val content = text ?: return 0
        val ed = editor ?: return 0
        val line = content.getLine(lineIndex)
        val inlayHintsWidth = measureInlayHints(getInlayHints(lineIndex), ed.textPaint)
        val iew = inlineElementsWidths
        if (iew != null) {
            if (useAdd) {
                iew.add(lineIndex, inlayHintsWidth)
            } else {
                iew.set(lineIndex, inlayHintsWidth)
            }
        }
        val m = measurer ?: return inlayHintsWidth
        return m.measureText(line, 0, line.length, ed.textPaint).toInt() + inlayHintsWidth
    }

    private fun measureTextRegion(lineIndex: Int, start: Int, end: Int): Int {
        val content = text ?: return 0
        val ed = editor ?: return 0
        val line = content.getLine(lineIndex)
        val m = measurer ?: return 0
        return m.measureText(line, start, end, ed.textPaint).toInt()
    }

    override fun obtainRowIterator(
        initialRow: Int,
        preloadedLines: SparseArray<ContentLine>?
    ): RowIterator {
        val content = text ?: throw IllegalStateException("Layout destroyed")
        return LineBreakLayoutRowItr(this, content, initialRow, preloadedLines)
    }

    override fun invalidateLines(range: StyleUpdateRange) {
        val content = text ?: return
        val wm = widthMaintainer ?: return
        val itr = range.lineIndexIterator(content.lineCount - 1)
        while (itr.hasNext()) {
            val line = itr.nextInt()
            wm.set(line, measureLineAndUpdateInlineWidths(line))
        }
    }

    override val rowCount: Int
        get() = text?.lineCount ?: 0

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
        val wm = widthMaintainer ?: return
        val iew = inlineElementsWidths ?: return
        val ed = editor ?: return
        for (i in startLine..endLine) {
            if (i == startLine) {
                if (endLine == startLine) {
                    val oldInlayWidths = iew.get(i)
                    val newInlayWidths = measureInlayHints(getInlayHints(i), ed.textPaint)
                    iew.set(i, newInlayWidths)
                    wm.set(
                        i,
                        wm.get(i) + measureTextRegion(
                            i,
                            startColumn,
                            endColumn
                        ) + (newInlayWidths - oldInlayWidths)
                    )
                } else {
                    wm.set(i, measureLineAndUpdateInlineWidths(i))
                }
            } else {
                wm.add(i, measureLineAndUpdateInlineWidths(i, true))
            }
        }
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
        val wm = widthMaintainer ?: return
        val iew = inlineElementsWidths ?: return
        val ed = editor ?: return
        if (startLine < endLine) {
            wm.removeRange(startLine + 1, endLine + 1)
            iew.removeRange(startLine + 1, endLine + 1)
        }
        if (startLine == endLine) {
            val oldInlayWidths = iew.get(startLine)
            val newInlayWidths = measureInlayHints(getInlayHints(startLine), ed.textPaint)
            iew.set(startLine, newInlayWidths)
            val m = measurer
            val deletedTextWidth =
                m?.measureText(deletedContent, 0, endColumn - startColumn, ed.textPaint)?.toInt()
                    ?: 0
            wm.set(
                startLine,
                wm.get(startLine) - deletedTextWidth + (newInlayWidths - oldInlayWidths)
            )
        } else {
            wm.set(startLine, measureLineAndUpdateInlineWidths(startLine))
        }
    }

    override fun getRowAt(rowIndex: Int): Row {
        val content = text ?: throw IllegalStateException("Layout destroyed")
        val row = Row()
        row.lineIndex = rowIndex
        row.startColumn = 0
        row.isLeadingRow = true
        row.isTrailingRow = true
        row.endColumn = content.getColumnCount(rowIndex)
        row.inlayHints = getInlayHints(rowIndex)
        return row
    }

    override fun getRowIndexForPosition(index: Int): Int {
        val ed = editor ?: return 0
        return ed.text.indexer.getCharPosition(index).line
    }

    override fun destroyLayout() {
        super.destroyLayout()
        widthMaintainer = null
        inlineElementsWidths = null
        measurer = null
    }

    override fun getLineNumberForRow(row: Int): Int {
        val content = text ?: return 0
        return row.coerceIn(0, content.lineCount - 1)
    }

    override val layoutWidth: Int
        get() {
            val wm = widthMaintainer ?: return 0
            return if (wm.size() == 0) Int.MAX_VALUE / 10 else wm.getMax()
        }

    override val layoutHeight: Int
        get() {
            val content = text ?: return 0
            val ed = editor ?: return 0
            return content.lineCount * ed.rowHeight
        }

    override fun getVisualPositionForLayoutOffset(
        offsetX: Float,
        offsetY: Float
    ): Layout.VisualLocation {
        val content = text ?: return Layout.VisualLocation(0, 0)
        val ed = editor ?: return Layout.VisualLocation(0, 0)
        val lineCount = content.lineCount
        val line = (offsetY / ed.rowHeight).toInt().coerceIn(0, lineCount - 1)
        val tr = ed.renderer.createTextRow(line)
        val pos = tr.getElementPositionForCursorOffset(offsetX)
        return Layout.VisualLocation(line, pos.textOffset, pos.element, pos.isInElementBounds)
    }

    override fun getCharLayoutOffset(line: Int, column: Int, array: FloatArray?): FloatArray {
        val dest = if (array == null || array.size < 2) FloatArray(2) else array
        val ed = editor ?: return dest
        dest[0] = ed.getRowBottom(line).toFloat()
        val tr = ed.renderer.createTextRow(line)
        dest[1] = tr.getCursorOffsetForIndex(column)
        return dest
    }

    override fun getRowCountForLine(line: Int): Int {
        return 1
    }

    override fun getDownPosition(line: Int, column: Int): Long {
        val content = text ?: return IntPair.pack(line, column)
        val cLine = content.lineCount
        return if (line + 1 >= cLine) {
            IntPair.pack(line, content.getColumnCount(line))
        } else {
            var col = column
            val cColumn = content.getColumnCount(line + 1)
            if (col > cColumn) {
                col = cColumn
            }
            IntPair.pack(line + 1, col)
        }
    }

    override fun getUpPosition(line: Int, column: Int): Long {
        if (line - 1 < 0) {
            return IntPair.pack(0, 0)
        }
        val content = text ?: return IntPair.pack(0, 0)
        var col = column
        val cColumn = content.getColumnCount(line - 1)
        if (col > cColumn) {
            col = cColumn
        }
        return IntPair.pack(line - 1, col)
    }

    open fun reuse(text: Content?) {
        val ed = editor ?: return
        this.text = text
        if (text == null) return
        reuseCount.getAndIncrement()
        val m = SingleCharacterWidths(ed.tabWidth).apply {
            isHandleFunctionCharacters = ed.isRenderFunctionCharacters
        }
        measurer = m
        val wm = widthMaintainer
        val iew = inlineElementsWidths
        try {
            if (wm != null && iew != null && wm.lock.tryLock(5, TimeUnit.MILLISECONDS)) {
                wm.lock.unlock()
                wm.clear()
                iew.clear()
                measureAllLines(wm, iew)
            } else {
                val newWm = BlockIntList()
                val newIew = BlockIntList()
                widthMaintainer = newWm
                inlineElementsWidths = newIew
                measureAllLines(newWm, newIew)
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Unable to wait for lock", e)
        }
    }

    open class LineBreakLayoutRowItr(
        private val layout: AbstractLayout,
        private val text: Content,
        private val initialRow: Int,
        private val preloadedLines: SparseArray<ContentLine>?
    ) : RowIterator {

        private val result: Row = Row().apply {
            isLeadingRow = true
            isTrailingRow = true
            startColumn = 0
        }
        private var currentRow: Int = initialRow

        override fun next(): Row {
            if (!hasNext()) {
                throw NoSuchElementException()
            }
            result.lineIndex = currentRow
            var line = preloadedLines?.get(currentRow)
            if (line == null) {
                line = text.getLine(currentRow)
            }
            result.endColumn = line.length
            result.inlayHints = layout.getInlayHints(result.lineIndex)
            currentRow++
            return result
        }

        override fun hasNext(): Boolean {
            return currentRow in 0 until text.lineCount
        }

        override fun reset() {
            currentRow = initialRow
        }
    }
}
