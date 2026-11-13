/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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

package io.github.rosemoe.sora.graphics

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.text.GetChars
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.util.Log
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.SpanMerger
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.lang.styling.inlayHint.CharacterSide
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint
import io.github.rosemoe.sora.lang.styling.span.SpanConstColorResolver
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs
import io.github.rosemoe.sora.lang.styling.span.SpanExternalRenderer
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.text.FunctionCharacters
import io.github.rosemoe.sora.text.bidi.Directions
import io.github.rosemoe.sora.text.bidi.IDirections
import io.github.rosemoe.sora.text.bidi.VisualDirections
import io.github.rosemoe.sora.text.breaker.WordBreaker
import io.github.rosemoe.sora.text.breaker.WordBreakerEmpty
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.util.RendererUtils
import io.github.rosemoe.sora.util.ReversedListView
import io.github.rosemoe.sora.util.TemporaryFloatBuffer
import io.github.rosemoe.sora.widget.layout.RowElement
import io.github.rosemoe.sora.widget.layout.RowElementTypes
import io.github.rosemoe.sora.widget.rendering.RenderingConstants
import io.github.rosemoe.sora.widget.rendering.TextAdvancesCache
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * [TextRow] is a helper class for a single text row to shape, measure and draw.
 *
 * Each row is firstly reordered according to the logical line directions analysis. Then
 * each run is processed from visually left to right. Elements in a run maybe a segment of text,
 * or an inlay hint (inline element).
 *
 * A special case is text breaking, which happens in wordwrap mode. When breaking text, the runs are
 * processed logically because a single row must represent a logically continuous text segment.
 *
 * The indices are mostly unchecked in this class, so caller have to duty to offer valid indices.
 *
 * @author Rosemoe
 */
open class TextRow {

    companion object {
        private const val LOG_TAG = "TextRow"
        private val SPAN_COMPARATOR = Comparator<Span?> { a, b ->
            when {
                a == null && b == null -> 0
                a == null -> -1
                b == null -> 1
                else -> a.column.compareTo(b.column)
            }
        }

        /**
         * The minimum character count of a span to be auto-truncated.
         */
        private const val MIN_AUTO_TRUNCATE_LENGTH = 64

        /**
         * Max context length when text run is truncated
         */
        private const val MAX_CONTEXT_LENGTH = 256

        private fun mergeComposingBackgrounds(
            lower: List<Span>?,
            start: Int,
            end: Int,
            composingText: CharSequence?,
            offset: Int
        ): List<Span>? {
            if (composingText !is Spanned) {
                return lower
            }
            val composing = composingText
            val composingStart = max(0, start - offset)
            val composingEnd = min(composing.length, end - offset)
            if (composingStart >= composingEnd) {
                return lower
            }
            val backgrounds =
                composing.getSpans(composingStart, composingEnd, BackgroundColorSpan::class.java)
            if (backgrounds.isEmpty()) {
                return lower
            }
            val boundaries = ArrayList<Int>(backgrounds.size * 2)
            for (background in backgrounds) {
                val from = max(composingStart, composing.getSpanStart(background))
                val to = min(composingEnd, composing.getSpanEnd(background))
                if (from < to && background.backgroundColor != 0) {
                    boundaries.add(from)
                    boundaries.add(to)
                }
            }
            if (boundaries.isEmpty()) {
                return lower
            }
            boundaries.sort()
            val upper = ArrayList<Span>(boundaries.size)
            var previousBoundary = -1
            var previousColor = 0
            for (boundary in boundaries) {
                if (boundary == previousBoundary) {
                    continue
                }
                previousBoundary = boundary
                var color = 0
                for (background in backgrounds) {
                    if (boundary < composingEnd &&
                        composing.getSpanStart(background) <= boundary &&
                        boundary < composing.getSpanEnd(background) &&
                        background.backgroundColor != 0
                    ) {
                        color = background.backgroundColor
                    }
                }
                if (color != previousColor) {
                    val span = if (color == 0) {
                        SpanFactory.obtainNoExt(offset + boundary, 0L)
                    } else {
                        SpanFactory.obtain(offset + boundary, 0L).apply {
                            setSpanExt(
                                SpanExtAttrs.EXT_COLOR_RESOLVER,
                                SpanConstColorResolver(0, color)
                            )
                        }
                    }
                    upper.add(span)
                    previousColor = color
                }
            }
            if (upper.isEmpty()) {
                return lower
            }
            return SpanMerger.merge(
                if (lower.isNullOrEmpty()) listOf(
                    SpanFactory.obtainNoExt(
                        0,
                        EditorColorScheme.TEXT_NORMAL.toLong()
                    )
                ) else lower,
                upper
            )
        }
    }

    private val tmpRect = RectF()
    private val tmpIndices = IntArray(4)
    private val tmpSpan = SpanFactory.obtainNoExt(0, 0L)
    private var text: ContentLine? = null
    private var directions: Directions? = null
    var textStart: Int = 0
        private set
    var textEnd: Int = 0
        private set
    private var spans: List<Span> = emptyList()
    private var inlineElements: List<InlayHint> = emptyList()
    private var params: TextRowParams? = null
    private var inlayHintRenderParams: InlayHintRenderParams? = null
    private var paint: Paint? = null
    private var measureCache: TextAdvancesCache? = null
    private var selectedStart: Int = -1
    private var selectedEnd: Int = -1

    fun set(
        text: ContentLine,
        start: Int,
        end: Int,
        spans: List<Span>?,
        inlineElements: List<InlayHint>?,
        directions: Directions,
        paint: Paint,
        measureCache: TextAdvancesCache?,
        params: TextRowParams
    ) {
        set(
            text,
            start,
            end,
            spans,
            inlineElements,
            directions,
            paint,
            measureCache,
            params,
            null,
            0
        )
    }

    /**
     * Set the row with optional composing decorations for its logical line.
     *
     * @param composingText the composing text on this logical line, or `null`
     * @param composingTextStartColumn the composing text's start column in this logical line
     */
    fun set(
        text: ContentLine,
        start: Int,
        end: Int,
        spans: List<Span>?,
        inlineElements: List<InlayHint>?,
        directions: Directions,
        paint: Paint,
        measureCache: TextAdvancesCache?,
        params: TextRowParams,
        composingText: CharSequence?,
        composingTextStartColumn: Int
    ) {
        this.text = text
        textStart = start
        textEnd = end
        val resolvedSpans =
            mergeComposingBackgrounds(spans, start, end, composingText, composingTextStartColumn)
        this.spans = if (resolvedSpans.isNullOrEmpty()) {
            listOf(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
        } else {
            resolvedSpans
        }
        this.inlineElements = inlineElements ?: emptyList()
        this.directions = directions
        this.paint = paint
        this.params = params
        this.measureCache = measureCache
        this.inlayHintRenderParams = params.toInlayHintRenderParams()
    }

    /**
     * Update the range of text
     */
    fun setRange(start: Int, end: Int) {
        textStart = start
        textEnd = end
    }

    fun setSelectedRange(start: Int, end: Int) {
        selectedStart = start
        selectedEnd = end
    }

    /**
     * Get character advances for text breaking, in a single run
     */
    private fun getSingleRunAdvancesForBreaking(
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        advances: FloatArray?
    ): Float {
        val nonNullText = text ?: return 0f
        val nonNullPaint = paint ?: return 0f
        val nonNullParams = params ?: return 0f
        var lastEnd = start
        val tabWidth = nonNullParams.tabWidth * nonNullPaint.spaceWidth
        var width = 0f
        for (i in start..end) {
            if (i == end || nonNullText[i] == '\t') {
                if (i > lastEnd) {
                    width += nonNullPaint.myGetTextRunAdvances(
                        nonNullText,
                        lastEnd,
                        i - lastEnd,
                        contextStart,
                        contextEnd - contextStart,
                        isRtl,
                        advances,
                        lastEnd - start
                    )
                }
                if (i < end) {
                    width += tabWidth
                    advances?.set(i - start, tabWidth)
                }
                lastEnd = i + 1
            }
        }
        return width
    }

    /**
     * Get the character advances and horizontal advance in a single text run
     */
    private fun getTextRunAdvancesCacheable(
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        isRtl: Boolean,
        advances: FloatArray?,
        advancesIndex: Int
    ): Float {
        val cache = measureCache
        if (cache != null) {
            if (advances != null) {
                for (i in 0 until count) {
                    advances[advancesIndex + i] = cache.getAdvanceAt(index + i)
                }
            }
            return cache.getAdvancesSum(index, index + count)
        }
        val nonNullText = text ?: return 0f
        val nonNullPaint = paint ?: return 0f
        return nonNullPaint.myGetTextRunAdvances(
            nonNullText,
            index,
            count,
            contextIndex,
            contextCount,
            isRtl,
            advances,
            advancesIndex
        )
    }

    /**
     * Get the cursor horizontal advance in a single text run
     */
    private fun getRunAdvanceCacheable(
        offset: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean
    ): Float {
        val cache = measureCache
        if (cache != null) {
            return cache.getAdvancesSum(start, offset)
        }
        val nonNullText = text ?: return 0f
        val nonNullPaint = paint ?: return 0f
        return nonNullPaint.myGetTextRunAdvances(
            nonNullText,
            start,
            offset - start,
            contextStart,
            contextEnd - start,
            isRtl,
            null,
            0
        )
    }

    /**
     * Find the character index from cursor horizontal offset
     */
    private fun findOffsetByAdvanceCacheable(
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        advance: Float
    ): Int {
        val cache = measureCache
        if (cache != null) {
            var left = start
            var right = end
            val base = cache.getAdvancesSum(0, start)
            while (left <= right) {
                val mid = (left + right) / 2
                if (mid < start || mid >= end) {
                    left = mid
                    break
                }
                val value = cache.getAdvancesSum(0, mid) - base
                when {
                    value > advance -> right = mid - 1
                    value < advance -> left = mid + 1
                    else -> {
                        left = mid
                        break
                    }
                }
            }
            if (cache.getAdvancesSum(0, left) - base > advance) {
                left--
            }
            return max(start, min(end, left))
        }
        val nonNullText = text ?: return start
        val nonNullPaint = paint ?: return start
        return nonNullPaint.findOffsetByRunAdvance(
            nonNullText,
            start,
            end,
            contextStart,
            contextEnd,
            isRtl,
            advance
        )
    }

    /**
     * Iterate the runs in the row
     *
     * @param reorderVisually `true` to reorder the elements visually. Otherwise, runs are consumed logically.
     */
    private fun iterateRuns(consumer: RunElementsConsumer, reorderVisually: Boolean) {
        val nonNullText = text ?: return
        val nonNullDirs = directions ?: return
        var pointers: ListPointers? = null
        val dirs: IDirections = if (reorderVisually && nonNullText.mayNeedBidi()) {
            VisualDirections(nonNullDirs)
        } else {
            nonNullDirs
        }
        for (i in 0 until dirs.runCount) {
            val runEnd = dirs.getRunEnd(i)
            val runStart = dirs.getRunStart(i)
            val segmentStart = max(runStart, textStart)
            val segmentEnd = min(runEnd, textEnd)
            if (segmentStart >= segmentEnd) {
                continue
            }
            pointers = seekStartIndices(segmentStart)
            if (!generateAndConsumeSingleRun(
                    segmentStart,
                    segmentEnd,
                    dirs.isRunRtl(i),
                    pointers,
                    consumer
                )
            ) {
                break
            }
        }
        var currInlineIndex = pointers?.inlineElementIndex ?: 0
        val trailingInlineRun = ArrayList<RowElement>()
        while (currInlineIndex < inlineElements.size &&
            getExpectedInlayHintColumn(inlineElements[currInlineIndex]) == textEnd
        ) {
            val e = RowElement().apply {
                type = RowElementTypes.INLAY_HINT
                displayColumnPosition = textEnd
                inlayHint = inlineElements[currInlineIndex++]
            }
            trailingInlineRun.add(e)
        }
        if (trailingInlineRun.isNotEmpty()) {
            val finalPointers = pointers ?: seekStartIndices(textEnd).also { pointers = it }
            finalPointers.inlineElementIndex = currInlineIndex
            consumer.accept(trailingInlineRun, false, finalPointers)
        }
    }

    /**
     * Get the expected column position to render after.
     */
    private fun getExpectedInlayHintColumn(inlayHint: InlayHint): Int {
        var position = inlayHint.column
        if (inlayHint.displaySide == CharacterSide.RIGHT) {
            position++
        }
        return min(position, textEnd)
    }

    /**
     * Seek the start indices for [spans] and [inlineElements]
     */
    private fun seekStartIndices(segmentStart: Int): ListPointers {
        tmpSpan.column = segmentStart
        var spanIndex = Collections.binarySearch(spans, tmpSpan, SPAN_COMPARATOR)
        if (spanIndex < 0) {
            spanIndex = -(spanIndex + 1)
        }
        if (spanIndex == spans.size) {
            spanIndex--
        }
        while (spanIndex > 0 && spans[spanIndex].column >= segmentStart) {
            spanIndex--
        }
        var inlineIndex = 0
        while (inlineIndex < inlineElements.size && inlineElements[inlineIndex].column < segmentStart) {
            inlineIndex++
        }
        return ListPointers(spanIndex, inlineIndex)
    }

    /**
     * Generate elements in a unidirectional run, and consume them.
     */
    private fun generateAndConsumeSingleRun(
        segmentStart: Int,
        segmentEnd: Int,
        isRtl: Boolean,
        pointers: ListPointers,
        consumer: RunElementsConsumer
    ): Boolean {
        val runElements = ArrayList<RowElement>()
        var lastEndIndex = segmentStart
        while (true) {
            if (pointers.inlineElementIndex < inlineElements.size &&
                inlineElements[pointers.inlineElementIndex].column < segmentEnd
            ) {
                val inlay = inlineElements[pointers.inlineElementIndex]
                val position = getExpectedInlayHintColumn(inlay)
                val element = RowElement()
                if (lastEndIndex == position) {
                    pointers.inlineElementIndex++
                    element.type = RowElementTypes.INLAY_HINT
                    element.inlayHint = inlay
                    element.displayColumnPosition = position
                } else {
                    element.type = RowElementTypes.TEXT
                    element.startColumn = lastEndIndex
                    element.endColumn = position
                    element.isRtlText = isRtl
                    lastEndIndex = position
                }
                runElements.add(element)
            } else if (lastEndIndex < segmentEnd) {
                val element = RowElement().apply {
                    type = RowElementTypes.TEXT
                    startColumn = lastEndIndex
                    endColumn = segmentEnd
                    isRtlText = isRtl
                }
                lastEndIndex = segmentEnd
                runElements.add(element)
            } else {
                break
            }
        }
        val result = consumer.accept(runElements, isRtl, pointers)
        val spansSize = spans.size
        while (pointers.spanIndex + 1 < spansSize && spans[pointers.spanIndex + 1].column <= segmentEnd) {
            pointers.spanIndex++
        }
        return result
    }

    /**
     * Break the logical line into rows
     * @param width Max width of a row
     * @return At least one row is returned, even when the line is empty.
     */
    fun breakText(width: Int, antiWordBreaking: Boolean): List<WordwrapRow> {
        val nonNullText = text ?: return emptyList()
        val nonNullPaint = paint ?: return emptyList()
        val nonNullParams = params ?: return emptyList()
        val rows = ArrayList<WordwrapRow>()
        val optimizer =
            if (antiWordBreaking) WordBreaker.Factory.newInstance(nonNullText) else WordBreakerEmpty.INSTANCE

        class TextBreaker : RunElementsConsumer {
            var currentRow = WordwrapRow()
            var currentWidth = 0f

            override fun accept(
                e: List<RowElement>,
                isRtl: Boolean,
                pointers: ListPointers
            ): Boolean {
                for (element in e) {
                    if (element.type == RowElementTypes.TEXT) {
                        handleText(element)
                    } else if (element.type == RowElementTypes.INLAY_HINT) {
                        handleInlineElement(element)
                    }
                }
                return true
            }

            fun commitRow() {
                currentRow.rowWidth = currentWidth
                rows.add(currentRow)
                currentWidth = 0f
                currentRow = WordwrapRow()
            }

            fun handleText(e: RowElement) {
                val advances = TemporaryFloatBuffer.obtain(e.endColumn - e.startColumn)
                val runWidth = getSingleRunAdvancesForBreaking(
                    e.startColumn,
                    e.endColumn,
                    e.startColumn,
                    e.endColumn,
                    e.isRtlText,
                    advances
                )

                if (currentWidth + runWidth < width) {
                    if (currentRow.isEmpty) {
                        currentRow.setInitialRange(e.startColumn, e.endColumn)
                    } else {
                        currentRow.setEndColumn(e.endColumn)
                    }
                    currentWidth += runWidth
                    TemporaryFloatBuffer.recycle(advances)
                    return
                }

                val limit = e.endColumn - e.startColumn
                var offset = 0
                while (offset < limit) {
                    var next = GraphemeBoundsBreaker.findGraphemeBreakPoint(
                        advances,
                        limit,
                        (width - currentWidth).toInt(),
                        offset
                    )
                    if (next == offset) {
                        if (currentRow.isEmpty) {
                            next++
                        } else {
                            commitRow()
                            continue
                        }
                    }
                    val beforeOptimization = next
                    next = optimizer.getOptimizedBreakPoint(
                        e.startColumn + offset,
                        e.startColumn + next
                    ) - e.startColumn
                    var advance = 0f
                    for (j in offset until next) {
                        advance += advances[j]
                    }
                    if (currentRow.isEmpty) {
                        currentRow.setInitialRange(e.startColumn + offset, e.startColumn + next)
                    } else {
                        currentRow.setEndColumn(e.startColumn + next)
                    }
                    currentWidth += advance
                    if (beforeOptimization != next) {
                        commitRow()
                    }
                    offset = next
                }
                TemporaryFloatBuffer.recycle(advances)
            }

            fun handleInlineElement(e: RowElement) {
                val inlay = e.inlayHint ?: return
                val renderer =
                    nonNullParams.inlayHintRendererProvider.getInlayHintRendererForType(inlay.type)
                var w = 0f
                if (renderer != null && inlayHintRenderParams != null) {
                    w = max(0f, renderer.measure(inlay, nonNullPaint, inlayHintRenderParams!!))
                }
                if (currentRow.isEmpty || currentWidth + w > width) {
                    if (!currentRow.isEmpty) {
                        commitRow()
                    }
                    currentRow.setInitialRange(e.displayColumnPosition, e.displayColumnPosition)
                    currentRow.addInlayHint(inlay)
                    currentWidth = w
                } else {
                    currentRow.addInlayHint(inlay)
                    currentWidth += w
                }
            }

            fun appendTailIfNeeded() {
                if (!currentRow.isEmpty) {
                    commitRow()
                }
            }
        }

        val breaker = TextBreaker()
        iterateRuns(breaker, false)
        if (rows.isEmpty() && breaker.currentRow.isEmpty) {
            breaker.currentRow.isEmpty = false
            breaker.currentRow.startColumn = textStart
            breaker.currentRow.endColumn = textEnd
        }
        breaker.appendTailIfNeeded()
        return rows
    }

    /**
     * The context for iterating in run elements.
     */
    private class IteratingContext {
        var lastStyle: Long = -1L
        var minOffset: Float = 0f
        var maxOffset: Float = Float.MAX_VALUE
        var targetCharOffset: Int = -1
        var resultOffset: Float = 0f
        var targetHorizontalOffset: Float = -1f
        var resultCharOffset: Int = -1
        var resultElement: RowElement? = null
        var isInElementBounds: Boolean = false
        var startCharOffset: Int = 0
        var endCharOffset: Int = 0
        var regionBuffer: RegionBuffer? = null
        var autoClip: Boolean = false
        var drawTextConsumer: DrawTextConsumer? = null
        var currentSpan: Span? = null
        var advances: TextAdvancesCache? = null
    }

    /**
     * Check if cursor at given offset should be displayed in the text segment
     */
    private fun checkCursorOffsetInSegment(offset: Int, start: Int, end: Int): Boolean {
        return offset in start until end || (offset == end && end == textEnd)
    }

    /**
     * Clip text region to patch certain regions
     */
    private fun clipRegionForPatchDrawing(
        textOffset: Float,
        width: Float,
        italics: Boolean,
        canvas: Canvas
    ) {
        val nonNullParams = params ?: return
        if (!italics) {
            canvas.clipRect(textOffset, 0f, textOffset + width, nonNullParams.rowHeight.toFloat())
            return
        }
        val path = Path()
        val y = nonNullParams.textBottom.toFloat()
        path.moveTo(textOffset, y)
        path.lineTo(textOffset - RenderingConstants.TEXT_SKEW_X * y, 0f)
        path.lineTo(textOffset + width - RenderingConstants.TEXT_SKEW_X * y, 0f)
        path.lineTo(textOffset + width, y)
        path.close()
        canvas.clipPath(path)
    }

    /**
     * Draw a single function character
     */
    protected open fun drawFunctionCharacter(
        canvas: Canvas,
        offsetX: Float,
        width: Float,
        ch: Char
    ) {
        val nonNullParams = params ?: return
        val nonNullPaint = paint ?: return
        val paintGraph = nonNullParams.graphPaint
        val metricsGraph = nonNullParams.graphMetrics
        paintGraph.textAlign = android.graphics.Paint.Align.CENTER
        val heightScaled = (metricsGraph.descent - metricsGraph.ascent).toFloat()
        val centerY = nonNullParams.rowHeight / 2f
        val baseline = centerY - heightScaled / 2f - metricsGraph.ascent
        paintGraph.color = nonNullPaint.color
        val name = FunctionCharacters.getNameForFunctionCharacter(ch)
        canvas.drawText(name, offsetX + width / 2f, baseline, paintGraph)
        paintGraph.textAlign = android.graphics.Paint.Align.LEFT

        val actualWidth = paintGraph.measureText(name)
        tmpRect.top = centerY - heightScaled / 2f
        tmpRect.bottom = centerY + heightScaled / 2f
        tmpRect.left = offsetX + width / 2f - actualWidth / 2f
        tmpRect.right = offsetX + width / 2f + actualWidth / 2f
        val color = nonNullPaint.color
        nonNullPaint.color =
            nonNullParams.colorScheme.getColor(EditorColorScheme.FUNCTION_CHAR_BACKGROUND_STROKE)
        nonNullPaint.style = android.graphics.Paint.Style.STROKE
        nonNullPaint.strokeWidth = nonNullParams.rowHeight * 0.05f
        val radius = nonNullParams.rowHeight * nonNullParams.roundTextBackgroundFactor
        canvas.drawRoundRect(tmpRect, radius, radius, nonNullPaint)
        nonNullPaint.style = android.graphics.Paint.Style.FILL
        nonNullPaint.color = color
    }

    /**
     * Terminal function for a single-styled text run. Commit the given text region to Canvas.
     * This function also handles function characters rendering.
     */
    private fun commitTextRunToCanvas(
        paintStart: Int,
        paintEnd: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        canvas: Canvas,
        offset: Float,
        width: Float
    ) {
        val nonNullText = text ?: return
        val nonNullPaint = paint ?: return
        val nonNullParams = params ?: return
        if (nonNullPaint.isRenderFunctionCharacters) {
            var lastEnd = paintStart
            val initOffset = offset + (if (isRtl) width else 0f)
            var drawOffset = initOffset
            for (i in paintStart..paintEnd) {
                var ch = '\u0000'
                if (i == paintEnd || FunctionCharacters.isEditorFunctionChar(nonNullText[i].also {
                        ch = it
                    })) {
                    if (i - lastEnd > 0) {
                        if (isRtl) {
                            nonNullPaint.textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        GraphicsCompat.drawTextRun(
                            canvas,
                            nonNullText,
                            lastEnd,
                            i - lastEnd,
                            contextStart,
                            contextEnd - contextStart,
                            drawOffset,
                            nonNullParams.textBaseline.toFloat(),
                            isRtl,
                            nonNullPaint
                        )
                        if (isRtl) {
                            nonNullPaint.textAlign = android.graphics.Paint.Align.LEFT
                        }
                    }
                    if (i == paintEnd) {
                        break
                    }
                    val chAdvance =
                        nonNullPaint.measureText(FunctionCharacters.getNameForFunctionCharacter(ch))
                    var advance =
                        getRunAdvanceCacheable(i, paintStart, paintEnd, paintStart, paintEnd, isRtl)
                    drawFunctionCharacter(
                        canvas,
                        if (isRtl) initOffset - advance - chAdvance else initOffset + advance,
                        chAdvance,
                        ch
                    )
                    advance += chAdvance
                    drawOffset = initOffset + (if (isRtl) -advance else advance)
                    lastEnd = i + 1
                }
            }
        } else {
            GraphicsCompat.drawTextRun(
                canvas,
                nonNullText,
                paintStart,
                paintEnd - paintStart,
                contextStart,
                contextEnd - contextStart,
                offset,
                nonNullParams.textBaseline.toFloat(),
                isRtl,
                nonNullPaint
            )
        }
    }

    private fun commitTextRunToConsumer(
        paintStart: Int,
        paintEnd: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        canvas: Canvas?,
        offset: Float,
        width: Float,
        ctx: IteratingContext
    ) {
        val nonNullText = text ?: return
        val nonNullParams = params ?: return
        ctx.drawTextConsumer?.drawText(
            canvas,
            nonNullText,
            paintStart,
            paintEnd - paintStart,
            contextStart,
            contextEnd - contextStart,
            isRtl,
            offset,
            width,
            nonNullParams,
            ctx.currentSpan
        )
    }

    /**
     * Truncate the text run to be committed, try to limit the committed part to be in the visible region
     */
    private fun commitTextRunAutoTruncated(
        paintStart: Int,
        paintEnd: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        canvas: Canvas?,
        offset: Float,
        width: Float,
        ctx: IteratingContext
    ) {
        val nonNullText = text ?: return
        val nonNullPaint = paint ?: return
        if (paintEnd - paintStart < MIN_AUTO_TRUNCATE_LENGTH || measureCache == null) {
            if (ctx.drawTextConsumer != null) {
                commitTextRunToConsumer(
                    paintStart,
                    paintEnd,
                    contextStart,
                    contextEnd,
                    isRtl,
                    canvas,
                    offset,
                    width,
                    ctx
                )
            } else if (canvas != null) {
                commitTextRunToCanvas(
                    paintStart,
                    paintEnd,
                    contextStart,
                    contextEnd,
                    isRtl,
                    canvas,
                    offset,
                    width
                )
            }
        } else {
            val runAdvanceLeft = max(0f, ctx.minOffset - offset) - nonNullPaint.spaceWidth
            val runAdvanceRight = min(width, ctx.maxOffset - offset) + nonNullPaint.spaceWidth
            val boundForLeft = findOffsetByAdvanceCacheable(
                paintStart,
                paintEnd,
                contextStart,
                contextEnd,
                isRtl,
                runAdvanceLeft
            )
            val boundForRight = findOffsetByAdvanceCacheable(
                paintStart,
                paintEnd,
                contextStart,
                contextEnd,
                isRtl,
                runAdvanceRight
            )
            val commitStart = min(boundForLeft, boundForRight)
            val commitEnd = max(boundForLeft, boundForRight)

            if (commitStart < commitEnd) {
                var commitContextStart = commitStart
                var commitContextEnd = commitEnd
                while (commitContextStart - 1 >= contextStart &&
                    nonNullText[commitContextStart - 1] != ' ' &&
                    (commitContextEnd - commitContextStart) < MAX_CONTEXT_LENGTH
                ) {
                    commitContextStart--
                }
                while (commitContextEnd + 1 < contextEnd &&
                    nonNullText[commitContextEnd] != ' ' &&
                    (commitContextEnd - commitContextStart) < MAX_CONTEXT_LENGTH
                ) {
                    commitContextEnd++
                }

                val advanceStart = measureAdvanceInRun(
                    commitStart,
                    paintStart,
                    paintEnd,
                    contextStart,
                    contextEnd,
                    isRtl
                )
                val advanceEnd = measureAdvanceInRun(
                    commitEnd,
                    paintStart,
                    paintEnd,
                    contextStart,
                    contextEnd,
                    isRtl
                )
                val newWidth = abs(advanceStart - advanceEnd)
                val commitOffset = if (isRtl) offset + width - advanceEnd else offset + advanceStart
                if (ctx.drawTextConsumer != null) {
                    commitTextRunToConsumer(
                        commitStart,
                        commitEnd,
                        contextStart,
                        contextEnd,
                        isRtl,
                        canvas,
                        commitOffset,
                        newWidth,
                        ctx
                    )
                } else if (canvas != null) {
                    commitTextRunToCanvas(
                        commitStart,
                        commitEnd,
                        contextStart,
                        contextEnd,
                        isRtl,
                        canvas,
                        commitOffset,
                        newWidth
                    )
                }
            }
        }
    }

    /**
     * Split the region with breakpoints from selection, and commit them separately to achieve
     * different text color in selected region
     */
    private fun splitRegionsAndCommit(
        paintStart: Int,
        paintEnd: Int,
        isRtl: Boolean,
        canvas: Canvas?,
        offset: Float,
        width: Float,
        color: Int,
        ctx: IteratingContext
    ) {
        val nonNullPaint = paint ?: return
        val nonNullParams = params ?: return
        val selectionStart = max(paintStart, min(paintEnd, selectedStart))
        val selectionEnd = max(paintStart, min(paintEnd, selectedEnd))
        tmpIndices[0] = paintStart
        tmpIndices[1] = paintEnd
        tmpIndices[2] = selectionStart
        tmpIndices[3] = selectionEnd
        tmpIndices.sort()
        var advance = 0f
        for (i in 0 until tmpIndices.size - 1) {
            val commitStart = tmpIndices[i]
            val commitEnd = tmpIndices[i + 1]
            if (commitStart == commitEnd) {
                continue
            }
            if (commitStart >= selectionStart && commitEnd <= selectionEnd) {
                nonNullPaint.color =
                    nonNullParams.colorScheme.getColor(EditorColorScheme.TEXT_SELECTED)
            } else {
                nonNullPaint.color = color
            }
            val segmentWidth = getRunAdvanceCacheable(
                commitEnd,
                commitStart,
                commitEnd,
                paintStart,
                paintEnd,
                isRtl
            )
            if (isRtl) {
                commitTextRunAutoTruncated(
                    commitStart,
                    commitEnd,
                    paintStart,
                    paintEnd,
                    true,
                    canvas,
                    offset + width - advance - segmentWidth,
                    segmentWidth,
                    ctx
                )
            } else {
                commitTextRunAutoTruncated(
                    commitStart,
                    commitEnd,
                    paintStart,
                    paintEnd,
                    false,
                    canvas,
                    offset + advance,
                    segmentWidth,
                    ctx
                )
            }
            advance += segmentWidth
        }
    }

    /**
     * Terminal handler for text element.
     */
    private fun handleSingleStyledText(
        paintStart: Int,
        paintEnd: Int,
        isRtl: Boolean,
        span: Span,
        canvas: Canvas?,
        offset: Float,
        ctx: IteratingContext
    ): Float {
        val paintGeneral = paint ?: return 0f
        val nonNullParams = params ?: return 0f

        // Apply font style if required
        if ((canvas != null && ctx.drawTextConsumer == null) || measureCache == null) {
            val styleBits = span.styleBits
            if (styleBits != ctx.lastStyle) {
                paintGeneral.isFakeBoldText = TextStyle.isBold(styleBits)
                paintGeneral.textSkewX =
                    if (TextStyle.isItalics(styleBits)) RenderingConstants.TEXT_SKEW_X else 0f
                ctx.lastStyle = styleBits
            }
        }

        var advances: FloatArray? = null
        if (ctx.advances != null) {
            advances = TemporaryFloatBuffer.obtain(paintEnd - paintStart)
        }
        val width = getTextRunAdvancesCacheable(
            paintStart,
            paintEnd - paintStart,
            paintStart,
            paintEnd - paintStart,
            isRtl,
            advances,
            0
        )
        if (ctx.advances != null && advances != null) {
            for (i in paintStart until paintEnd) {
                ctx.advances!!.setAdvanceAt(i, advances[i - paintStart])
            }
            TemporaryFloatBuffer.recycle(advances)
        }
        if (checkCursorOffsetInSegment(ctx.targetCharOffset, paintStart, paintEnd)) {
            ctx.maxOffset = 0f
            val advance = getRunAdvanceCacheable(
                ctx.targetCharOffset,
                paintStart,
                paintEnd,
                paintStart,
                paintEnd,
                isRtl
            )
            ctx.resultOffset = if (isRtl) offset + width - advance else offset + advance
            return width
        }

        if (ctx.targetHorizontalOffset != -1f) {
            var runOffset = ctx.targetHorizontalOffset - offset
            if (isRtl) {
                runOffset = width - runOffset
            }
            if (runOffset > width) {
                ctx.resultCharOffset = paintEnd
            } else if (runOffset <= 0f) {
                ctx.resultCharOffset = paintStart
            } else {
                ctx.resultCharOffset = findOffsetByAdvanceCacheable(
                    paintStart,
                    paintEnd,
                    paintStart,
                    paintEnd,
                    isRtl,
                    runOffset
                )
            }
        }

        var regionLeft = -1f
        var regionRight = -1f
        if (ctx.regionBuffer != null || ctx.drawTextConsumer != null) {
            val sharedTextStart = max(paintStart, ctx.startCharOffset)
            val sharedTextEnd = min(paintEnd, ctx.endCharOffset)
            if (sharedTextStart < sharedTextEnd) {
                if (sharedTextStart == paintStart && sharedTextEnd == paintEnd) {
                    regionLeft = offset
                    regionRight = offset + width
                } else {
                    var startAdvance = getRunAdvanceCacheable(
                        sharedTextStart,
                        paintStart,
                        paintEnd,
                        paintStart,
                        paintEnd,
                        isRtl
                    )
                    var endAdvance = getRunAdvanceCacheable(
                        sharedTextEnd,
                        paintStart,
                        paintEnd,
                        paintStart,
                        paintEnd,
                        isRtl
                    )
                    startAdvance = if (isRtl) width - startAdvance else startAdvance
                    endAdvance = if (isRtl) width - endAdvance else endAdvance
                    regionLeft = offset + min(startAdvance, endAdvance)
                    regionRight = offset + max(startAdvance, endAdvance)
                }
                ctx.regionBuffer?.commitRegion(regionLeft, regionRight)
            }
        }

        val sharedStart = max(offset, ctx.minOffset)
        val sharedEnd = min(offset + width, ctx.maxOffset)
        if (sharedStart >= sharedEnd) {
            return width
        }

        if (canvas == null) {
            return width
        }

        if (ctx.drawTextConsumer != null) {
            val sharedTextStart = max(paintStart, ctx.startCharOffset)
            val sharedTextEnd = min(paintEnd, ctx.endCharOffset)
            if (sharedTextStart >= sharedTextEnd) {
                return width
            }
            if (ctx.autoClip) {
                canvas.save()
                clipRegionForPatchDrawing(
                    regionLeft,
                    regionRight - regionLeft,
                    TextStyle.isItalics(span.styleBits),
                    canvas
                )
            }
            ctx.currentSpan = span
            commitTextRunAutoTruncated(
                paintStart,
                paintEnd,
                paintStart,
                paintEnd,
                isRtl,
                canvas,
                offset,
                width,
                ctx
            )
            ctx.currentSpan = null
            ctx.lastStyle = -1
            if (ctx.autoClip) {
                canvas.restore()
            }
            return width
        }

        val paintOther = nonNullParams.miscPaint
        val renderer: SpanExternalRenderer? = span.getSpanExt(SpanExtAttrs.EXT_EXTERNAL_RENDERER)

        if (renderer != null && renderer.requirePreDraw()) {
            val saveCount = canvas.save()
            canvas.translate(offset, 0f)
            canvas.clipRect(
                0f,
                nonNullParams.rowTop.toFloat(),
                width,
                nonNullParams.rowHeight.toFloat()
            )
            try {
                renderer.draw(canvas, paintGeneral, nonNullParams.colorScheme, true)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error while invoking external renderer", e)
            }
            canvas.restoreToCount(saveCount)
        }

        val backgroundColor = RendererUtils.getBackgroundColor(span, nonNullParams.colorScheme)
        if (backgroundColor != 0 && paintStart != paintEnd) {
            tmpRect.set(
                offset,
                nonNullParams.rowTop.toFloat(),
                offset + width,
                nonNullParams.rowBottom.toFloat()
            )
            paintGeneral.color = backgroundColor
            val radius = nonNullParams.rowHeight * nonNullParams.roundTextBackgroundFactor
            canvas.drawRoundRect(tmpRect, radius, radius, paintGeneral)
        }

        val foregroundColor = RendererUtils.getForegroundColor(span, nonNullParams.colorScheme)
        if (selectedStart >= selectedEnd || selectedStart >= textEnd || selectedEnd <= textStart ||
            nonNullParams.colorScheme.getColor(EditorColorScheme.TEXT_SELECTED) == 0
        ) {
            paintGeneral.color = foregroundColor
            commitTextRunAutoTruncated(
                paintStart,
                paintEnd,
                paintStart,
                paintEnd,
                isRtl,
                canvas,
                offset,
                width,
                ctx
            )
        } else {
            splitRegionsAndCommit(
                paintStart,
                paintEnd,
                isRtl,
                canvas,
                offset,
                width,
                foregroundColor,
                ctx
            )
        }

        if (TextStyle.isStrikeThrough(span.style)) {
            val strikethroughColor =
                nonNullParams.colorScheme.getColor(EditorColorScheme.STRIKETHROUGH)
            paintOther.color =
                if (strikethroughColor == 0) paintGeneral.color else strikethroughColor
            val y = nonNullParams.rowTop + nonNullParams.rowHeight / 2f
            canvas.drawLine(offset, y, offset + width, y, paintOther)
        }

        val underlineColor = span.underlineColor
        if (underlineColor != null) {
            val underlineColorInt = underlineColor.resolve(nonNullParams.colorScheme)
            if (underlineColorInt != 0) {
                tmpRect.bottom = nonNullParams.textBottom.toFloat()
                tmpRect.top =
                    tmpRect.bottom - nonNullParams.textHeight * RenderingConstants.TEXT_UNDERLINE_WIDTH_FACTOR
                tmpRect.left = offset
                tmpRect.right = offset + width
                paintGeneral.color = underlineColorInt
                canvas.drawRect(tmpRect, paintGeneral)
            }
        }

        if (renderer != null && renderer.requirePostDraw()) {
            val saveCount = canvas.save()
            canvas.translate(offset, nonNullParams.rowTop.toFloat())
            canvas.clipRect(0f, 0f, width, nonNullParams.rowHeight.toFloat())
            try {
                renderer.draw(canvas, paintGeneral, nonNullParams.colorScheme, false)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error while invoking external renderer", e)
            }
            canvas.restoreToCount(saveCount)
        }
        return width
    }

    /**
     * Split text in a unidirectional run with span boundaries
     */
    private fun handleMultiStyledText(
        start: Int,
        end: Int,
        isRtl: Boolean,
        pointers: ListPointers,
        canvas: Canvas?,
        offset: Float,
        ctx: IteratingContext
    ): Float {
        var spanIndex = pointers.spanIndex
        val targetCharIndex = if (isRtl) end - 1 else start
        val spansSize = spans.size
        while (spanIndex + 1 < spansSize && spans[spanIndex + 1].column <= targetCharIndex) {
            spanIndex++
        }
        var localOffset = 0f
        if (isRtl) {
            var nextEnd = end
            while (nextEnd > start) {
                var moveSpanIndex = true
                val span = spans[spanIndex]
                val segmentStart = max(
                    start, if (spanIndex == 0) {
                        moveSpanIndex = false
                        0
                    } else {
                        span.column
                    }
                )
                val segmentEnd = nextEnd

                localOffset += handleSingleStyledText(
                    segmentStart,
                    segmentEnd,
                    isRtl,
                    span,
                    canvas,
                    offset + localOffset,
                    ctx
                )

                if (moveSpanIndex) {
                    spanIndex--
                }
                nextEnd = segmentStart

                if (offset + localOffset > ctx.maxOffset) {
                    break
                }
            }
        } else {
            var lastEnd = start
            while (lastEnd < end) {
                var moveSpanIndex = true
                val segmentEnd = min(
                    end,
                    if (spanIndex + 1 >= spansSize) {
                        moveSpanIndex = false
                        textEnd
                    } else {
                        spans[spanIndex + 1].column
                    }
                )
                val segmentStart = lastEnd
                val span = spans[spanIndex]

                localOffset += handleSingleStyledText(
                    segmentStart,
                    segmentEnd,
                    isRtl,
                    span,
                    canvas,
                    offset + localOffset,
                    ctx
                )

                lastEnd = segmentEnd
                if (moveSpanIndex) {
                    spanIndex++
                }

                if (offset + localOffset > ctx.maxOffset) {
                    break
                }
            }
        }
        return localOffset
    }

    /**
     * Split text in an unidirectional run with tab.
     * Tab character is consumed as terminal text segment here.
     */
    private fun handleSingleTextElement(
        e: RowElement,
        pointers: ListPointers,
        canvas: Canvas?,
        offset: Float,
        ctx: IteratingContext
    ): Float {
        val nonNullText = text ?: return 0f
        val nonNullPaint = paint ?: return 0f
        val nonNullParams = params ?: return 0f
        val isRtl = e.isRtlText
        var localOffset = 0f
        var lastEnd = if (isRtl) e.endColumn else e.startColumn
        val terminalIndex = if (isRtl) e.startColumn - 1 else e.endColumn
        val tabWidth = nonNullParams.tabWidth * nonNullPaint.spaceWidth

        var index = if (isRtl) e.endColumn - 1 else e.startColumn
        while (if (isRtl) index >= terminalIndex else index <= terminalIndex) {
            if (index == terminalIndex || nonNullText[index] == '\t') {
                val regionStart = if (isRtl) index + 1 else lastEnd
                val regionEnd = if (isRtl) lastEnd else index
                localOffset += handleMultiStyledText(
                    regionStart,
                    regionEnd,
                    isRtl,
                    pointers,
                    canvas,
                    offset + localOffset,
                    ctx
                )
                if (offset + localOffset > ctx.maxOffset) {
                    break
                }
                if (index != terminalIndex) {
                    if (index == ctx.targetCharOffset || (index + 1 == ctx.targetCharOffset && index + 1 == textEnd)) {
                        val advance = if (index == ctx.targetCharOffset) 0f else tabWidth
                        ctx.resultOffset = if (isRtl) {
                            localOffset + tabWidth - advance
                        } else {
                            localOffset + advance
                        }
                        ctx.maxOffset = 0f
                    }
                    if (ctx.targetHorizontalOffset != -1f) {
                        var runOffset = ctx.targetHorizontalOffset - offset - localOffset
                        if (isRtl) {
                            runOffset = tabWidth - runOffset
                        }
                        ctx.resultCharOffset = if (runOffset > tabWidth / 2f) {
                            index + 1
                        } else {
                            index
                        }
                    }
                    if (ctx.regionBuffer != null && index >= ctx.startCharOffset && index < ctx.endCharOffset) {
                        ctx.regionBuffer?.commitRegion(
                            offset + localOffset,
                            offset + localOffset + tabWidth
                        )
                    }
                    ctx.advances?.setAdvanceAt(index, tabWidth)
                    if (ctx.drawTextConsumer != null && index >= ctx.startCharOffset && index < ctx.endCharOffset) {
                        ctx.drawTextConsumer?.drawText(
                            canvas,
                            nonNullText,
                            index,
                            1,
                            index,
                            1,
                            isRtl,
                            offset + localOffset,
                            tabWidth,
                            nonNullParams,
                            null
                        )
                    }
                    localOffset += tabWidth
                }
                lastEnd = if (isRtl) index else index + 1
                if (offset + localOffset > ctx.maxOffset) {
                    break
                }
            }
            if (isRtl) index-- else index++
        }
        return localOffset
    }

    /**
     * Handle a single inline element in a unidirectional run
     */
    private fun handleSingleInlineElement(
        e: RowElement,
        canvas: Canvas?,
        offset: Float,
        ctx: IteratingContext
    ): Float {
        val inlay = e.inlayHint ?: return 0f
        val nonNullParams = params ?: return 0f
        val nonNullPaint = paint ?: return 0f
        val renderer =
            nonNullParams.inlayHintRendererProvider.getInlayHintRendererForType(inlay.type)
        var w = 0f
        if (renderer != null && inlayHintRenderParams != null) {
            w = max(0f, renderer.measure(inlay, nonNullPaint, inlayHintRenderParams!!))
        }
        ctx.regionBuffer?.commitPossibleInterval(offset, offset + w)
        if (canvas == null || ctx.drawTextConsumer != null) {
            return w
        }
        val regionLeft = offset
        val regionRight = offset + w
        val sharedStart = max(regionLeft, ctx.minOffset)
        val sharedEnd = min(regionRight, ctx.maxOffset)
        if (renderer != null && sharedStart < sharedEnd && inlayHintRenderParams != null) {
            val saveCount = canvas.save()
            canvas.translate(offset, nonNullParams.rowTop.toFloat())
            renderer.render(
                inlay,
                canvas,
                nonNullPaint,
                inlayHintRenderParams!!,
                nonNullParams.colorScheme,
                w
            )
            canvas.restoreToCount(saveCount)
            ctx.lastStyle = -1
        }
        return w
    }

    /**
     * Handle elements in a unidirectional run
     */
    private fun handleMultiElementRun(
        e: List<RowElement>,
        isRtl: Boolean,
        pointers: ListPointers,
        canvas: Canvas?,
        offset: Float,
        ctx: IteratingContext
    ): Float {
        val visualElements = if (isRtl) ReversedListView(e) else e
        var localOffset = 0f
        for (element in visualElements) {
            var offsetAdvance = 0f
            if (element.type == RowElementTypes.TEXT) {
                offsetAdvance =
                    handleSingleTextElement(element, pointers, canvas, offset + localOffset, ctx)
            } else if (element.type == RowElementTypes.INLAY_HINT) {
                offsetAdvance =
                    handleSingleInlineElement(element, canvas, offset + localOffset, ctx)
            }
            if (ctx.targetHorizontalOffset != -1f) {
                ctx.resultElement = element
                val leftOffset = offset + localOffset
                ctx.isInElementBounds =
                    leftOffset <= ctx.targetHorizontalOffset && ctx.targetHorizontalOffset <= leftOffset + offsetAdvance
            }
            localOffset += offsetAdvance
            if (offset + localOffset > ctx.maxOffset) {
                break
            }
        }
        return localOffset
    }

    /**
     * Draw text into the given canvas. Text metrics information is provided by previously set params.
     * Text is rendered from horizontal offset 0 from left to right.
     * @param canvas A pre-translated canvas to draw text
     * @param minHorizontalOffset Min visible horizontal offset in text
     * @param maxHorizontalOffset Max visible horizontal offset in text
     * @return packed integer and float. The first value represents if the row end is drawn; the second represents the final horizontal offset.
     */
    fun draw(canvas: Canvas, minHorizontalOffset: Float, maxHorizontalOffset: Float): Long {
        val ctx = IteratingContext().apply {
            minOffset = minHorizontalOffset
            maxOffset = maxHorizontalOffset
        }

        class DrawHandler : RunElementsConsumer {
            var horizontalOffset = 0f
            var isExhausted = true

            override fun accept(
                e: List<RowElement>,
                isRtl: Boolean,
                pointers: ListPointers
            ): Boolean {
                val runWidth =
                    handleMultiElementRun(e, isRtl, pointers, canvas, horizontalOffset, ctx)
                horizontalOffset += runWidth
                isExhausted = horizontalOffset < maxHorizontalOffset
                return isExhausted
            }
        }

        val handler = DrawHandler()
        iterateRuns(handler, true)
        return IntPair.packIntFloat(if (handler.isExhausted) 1 else 0, handler.horizontalOffset)
    }

    /**
     * Get the horizontal offset of cursor at the given index
     */
    fun getCursorOffsetForIndex(index: Int): Float {
        val ctx = IteratingContext().apply {
            targetCharOffset = index
        }

        class CursorOffsetHandler : RunElementsConsumer {
            var horizontalOffset = 0f

            override fun accept(
                e: List<RowElement>,
                isRtl: Boolean,
                pointers: ListPointers
            ): Boolean {
                val runWidth =
                    handleMultiElementRun(e, isRtl, pointers, null, horizontalOffset, ctx)
                horizontalOffset += runWidth
                return ctx.maxOffset != 0f
            }
        }

        val handler = CursorOffsetHandler()
        iterateRuns(handler, true)
        return ctx.resultOffset
    }

    /**
     * Get text index and element (inline elements considered) from the given cursor horizontal offset
     */
    fun getElementPositionForCursorOffset(offset: Float): ElementPosition {
        val ctx = IteratingContext().apply {
            targetHorizontalOffset = offset
            maxOffset = offset
        }
        iterateRuns(MaxOffsetIterationConsumer(ctx), true)
        val charOffset = if (ctx.resultCharOffset == -1) textStart else ctx.resultCharOffset
        return ElementPosition(ctx.resultElement, ctx.isInElementBounds, charOffset)
    }

    /**
     * Iterate over background regions. Visually consequent regions are merged into a single region.
     *
     * @param start Start of target background segment
     * @param end End of target background segment
     * @param allowLeadingBackground Allow leading inline elements to be included
     * @param allowTrailingBackground Allow trailing inline elements to be included
     */
    fun iterateBackgroundRegions(
        start: Int,
        end: Int,
        allowLeadingBackground: Boolean,
        allowTrailingBackground: Boolean,
        handler: BackgroundRegionConsumer
    ) {
        val ctx = IteratingContext().apply {
            startCharOffset = start
            endCharOffset = end
            regionBuffer =
                RegionBuffer(this, handler, allowLeadingBackground, allowTrailingBackground)
        }
        iterateRuns(MaxOffsetIterationConsumer(ctx), true)
        ctx.regionBuffer?.commitCurrentIfPresent()
    }

    /**
     * Iterate over terminal drawTextRun calls
     * @param start Start of target text segment
     * @param end End of target text segment
     * @param canvas A pre-translated canvas to clip
     * @param minHorizontalOffset Min visible horizontal offset in text
     * @param maxHorizontalOffset Max visible horizontal offset in text
     * @param autoClip Clip region outside the desired text segment
     */
    fun iterateDrawTextRegions(
        start: Int,
        end: Int,
        canvas: Canvas?,
        minHorizontalOffset: Float,
        maxHorizontalOffset: Float,
        autoClip: Boolean,
        consumer: DrawTextConsumer
    ) {
        val ctx = IteratingContext().apply {
            startCharOffset = start
            endCharOffset = end
            minOffset = minHorizontalOffset
            maxOffset = maxHorizontalOffset
            this.autoClip = autoClip
            drawTextConsumer = consumer
        }
        iterateRuns(MaxOffsetIterationConsumer(ctx, canvas), true)
    }

    /**
     * Compute row width
     */
    fun computeRowWidth(): Float {
        val ctx = IteratingContext()
        val handler = MaxOffsetIterationConsumer(ctx)
        iterateRuns(handler, true)
        return handler.horizontalOffset
    }

    /**
     * Measure text advance in unidirectional run
     */
    fun measureAdvanceInRun(
        offset: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean
    ): Float {
        return getRunAdvanceCacheable(offset, start, end, contextStart, contextEnd, isRtl)
    }

    /**
     * Build measure of logical line for currently-set text range. A logical line maybe split into
     * multiple rows, so each time only one row is measured.
     * [buildMeasureCacheTailor] should always be called for each line.
     * @see buildMeasureCacheTailor
     * @param cache size must be bigger than text end
     */
    fun buildMeasureCacheStep(cache: TextAdvancesCache) {
        val ctx = IteratingContext().apply {
            advances = cache
        }
        iterateRuns(MaxOffsetIterationConsumer(ctx), true)
    }

    /**
     * Finish the logical line measure cache building.
     * @see buildMeasureCacheStep
     * @param cache size must be bigger than text end
     */
    fun buildMeasureCacheTailor(cache: TextAdvancesCache) {
        cache.finishBuilding()
    }

    /**
     * Iteration consumer that stops when the horizontal offset exceeds the desired max offset
     */
    private inner class MaxOffsetIterationConsumer(
        private val ctx: IteratingContext,
        private val canvas: Canvas? = null
    ) : RunElementsConsumer {

        var horizontalOffset: Float = 0f

        override fun accept(e: List<RowElement>, isRtl: Boolean, pointers: ListPointers): Boolean {
            horizontalOffset += handleMultiElementRun(
                e,
                isRtl,
                pointers,
                canvas,
                horizontalOffset,
                ctx
            )
            return horizontalOffset < ctx.maxOffset
        }
    }

    /**
     * Result of [breakText]
     */
    open class WordwrapRow {
        var isEmpty: Boolean = true

        /**
         * Start column (inclusive)
         */
        @JvmField
        var startColumn: Int = 0

        /**
         * End column (exclusive)
         */
        @JvmField
        var endColumn: Int = 0

        /**
         * Inlay hints on the row, maybe null or empty
         */
        @JvmField
        var inlayHints: MutableList<InlayHint>? = null

        @JvmField
        var rowWidth: Float = 0f

        fun setInitialRange(start: Int, end: Int) {
            isEmpty = false
            startColumn = start
            endColumn = end
        }

        fun setEndColumn(column: Int) {
            check(!isEmpty) { "Row is empty" }
            this.endColumn = column
        }

        fun addInlayHint(inlayHint: InlayHint) {
            check(!isEmpty) { "Row is empty" }
            if (inlayHints == null) {
                inlayHints = ArrayList()
            }
            inlayHints!!.add(inlayHint)
        }
    }

    class ListPointers(
        @JvmField var spanIndex: Int,
        @JvmField var inlineElementIndex: Int
    ) {
        fun copy(): ListPointers = ListPointers(spanIndex, inlineElementIndex)
    }

    private class RegionBuffer(
        private val ctx: IteratingContext,
        private val consumer: BackgroundRegionConsumer,
        private val allowLeadingBackground: Boolean,
        private val allowTrailingBackground: Boolean
    ) {
        companion object {
            private const val EPS = 1e-6f
        }

        private var isEmpty = true
        private var currentLeft = 0f
        private var currentRight = 0f
        private var hasPossibleInterval = false
        private var intervalLeft = 0f
        private var intervalRight = 0f

        fun commitRegion(regionLeft: Float, regionRight: Float) {
            if (isEmpty) {
                if (hasPossibleInterval && abs(regionLeft - intervalRight) <= EPS) {
                    currentLeft = intervalLeft
                } else {
                    currentLeft = regionLeft
                }
                currentRight = regionRight
                isEmpty = false
                hasPossibleInterval = false
                return
            }
            if (!hasPossibleInterval && abs(regionLeft - currentRight) <= EPS) {
                currentRight = regionRight
                return
            } else if (hasPossibleInterval && abs(regionLeft - intervalRight) <= EPS) {
                currentRight = regionRight
                hasPossibleInterval = false
                return
            }
            commitCurrentIfPresent()
            isEmpty = false
            currentLeft = regionLeft
            currentRight = regionRight
        }

        fun commitPossibleInterval(regionLeft: Float, regionRight: Float) {
            if (isEmpty && !allowLeadingBackground) {
                return
            }
            if (hasPossibleInterval) {
                if (abs(regionLeft - intervalRight) <= EPS) {
                    intervalRight = regionRight
                } else {
                    hasPossibleInterval = false
                }
            } else if (abs(regionLeft - currentRight) <= EPS) {
                intervalLeft = regionLeft
                intervalRight = regionRight
                hasPossibleInterval = true
            }
        }

        fun commitCurrentIfPresent() {
            if (isEmpty) {
                return
            }
            if (hasPossibleInterval && allowTrailingBackground) {
                currentRight = intervalRight
            }
            if (!consumer.handleRegion(currentLeft, currentRight)) {
                ctx.maxOffset = 0f
            }
            isEmpty = true
            hasPossibleInterval = false
        }
    }

    fun interface RunElementsConsumer {
        fun accept(e: List<RowElement>, isRtl: Boolean, pointers: ListPointers): Boolean
    }

    fun interface BackgroundRegionConsumer {
        /**
         * Handle a background region
         *
         * @return true to continue the iteration; false otherwise.
         */
        fun handleRegion(left: Float, right: Float): Boolean
    }

    fun interface DrawTextConsumer {
        /**
         * @param span may be null, when tab encountered.
         */
        fun drawText(
            canvas: Canvas?,
            text: GetChars,
            index: Int,
            count: Int,
            contextIndex: Int,
            contextCount: Int,
            isRtl: Boolean,
            horizontalOffset: Float,
            width: Float,
            params: TextRowParams,
            span: Span?
        )
    }

    class ElementPosition(
        @JvmField val element: RowElement?,
        @JvmField val isInElementBounds: Boolean,
        @JvmField val textOffset: Int
    )
}
