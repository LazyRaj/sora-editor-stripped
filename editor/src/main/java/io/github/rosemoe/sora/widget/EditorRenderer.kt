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
/* Modified by Raj Dave (aka LazyRaj) */

package io.github.rosemoe.sora.widget

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RenderNode
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.text.GetChars
import android.util.Log
import android.util.SparseArray
import androidx.collection.MutableIntList
import androidx.collection.MutableLongLongMap
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage
import io.github.rosemoe.sora.graphics.BubbleHelper
import io.github.rosemoe.sora.graphics.BufferedDrawPoints
import io.github.rosemoe.sora.graphics.GraphicsCompat
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.graphics.TextRow
import io.github.rosemoe.sora.graphics.TextRowParams
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.EmptyReader
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint
import io.github.rosemoe.sora.lang.styling.line.LineAnchorStyle
import io.github.rosemoe.sora.lang.styling.line.LineBackground
import io.github.rosemoe.sora.lang.styling.line.LineGutterBackground
import io.github.rosemoe.sora.lang.styling.line.LineSideIcon
import io.github.rosemoe.sora.lang.styling.line.LineStyles
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.text.Cursor
import io.github.rosemoe.sora.text.bidi.Directions
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.util.LongArrayList
import io.github.rosemoe.sora.util.MutableInt
import io.github.rosemoe.sora.util.Numbers
import io.github.rosemoe.sora.util.TemporaryCharBuffer
import io.github.rosemoe.sora.widget.DirectAccessProps.Companion.CURSOR_LINE_BG_OVERLAP_CURSOR
import io.github.rosemoe.sora.widget.DirectAccessProps.Companion.CURSOR_LINE_BG_OVERLAP_CUSTOM
import io.github.rosemoe.sora.widget.DirectAccessProps.Companion.CURSOR_LINE_BG_OVERLAP_MIXED
import io.github.rosemoe.sora.widget.internal.util.SpansUtils
import io.github.rosemoe.sora.widget.minimap.MinimapRenderer
import io.github.rosemoe.sora.widget.rendering.RenderingConstants
import io.github.rosemoe.sora.widget.rendering.TextAdvancesCache
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.style.DiagnosticIndicatorStyle
import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle
import java.util.Objects
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import android.graphics.Paint as AndroidPaint

open class EditorRenderer(protected val editor: CodeEditor) {

    companion object {
        private val PRESSED_DRAWABLE_STATE =
            intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
        private val DEFAULT_DRAWABLE_STATE = intArrayOf(android.R.attr.state_enabled)

        private const val LOG_TAG = "EditorRenderer"
        private val sDiagnosticsColorMapping = intArrayOf(
            0,
            EditorColorScheme.PROBLEM_TYPO,
            EditorColorScheme.PROBLEM_WARNING,
            EditorColorScheme.PROBLEM_ERROR
        )
        private val TMP_DESC = SelectionHandleStyle.HandleDescriptor()
    }

    @JvmField
    protected val bufferedDrawPoints: BufferedDrawPoints = BufferedDrawPoints()

    @JvmField
    val paintGeneral: Paint = Paint(editor.isRenderFunctionCharacters).apply {
        isAntiAlias = true
    }

    @JvmField
    val paintOther: Paint = Paint(false).apply {
        strokeWidth = editor.dpUnit * 1.8f
        strokeCap = AndroidPaint.Cap.ROUND
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }

    @JvmField
    val paintGraph: Paint = Paint(false).apply {
        isAntiAlias = true
    }

    @JvmField
    val viewRect: Rect = Rect()

    private val tmpRect: RectF = RectF()
    private val tmpPath: Path = Path()

    @JvmField
    val verticalScrollBarRect: RectF = RectF()

    @JvmField
    val horizontalScrollBarRect: RectF = RectF()

    private val postDrawLineNumbers = LongArrayList()
    private val postDrawCurrentLines = MutableIntList()
    private val matchedPositions = LongArrayList()
    private val highlightPositions = MutableLongLongMap()
    private val preloadedLines = SparseArray<ContentLine>()
    private val preloadedDirections = SparseArray<Directions>()
    private val collectedDiagnostics = ArrayList<DiagnosticRegion>()

    @JvmField
    var lastStuckLines: List<CodeBlock>? = null

    @JvmField
    var metricsText: AndroidPaint.FontMetricsInt

    var horizontalScrollbarThumbDrawable: Drawable? = null
    var horizontalScrollbarTrackDrawable: Drawable? = null
    var verticalScrollbarThumbDrawable: Drawable? = null
    var verticalScrollbarTrackDrawable: Drawable? = null

    private val lineBreakGraph: Drawable?
    private val softwrapLeftGraph: Drawable?
    private val softwrapRightGraph: Drawable?

    @Volatile
    private var displayTimestamp: Long = 0L

    private var metricsLineNumber: AndroidPaint.FontMetricsInt
    private var metricsGraph: AndroidPaint.FontMetricsInt
    private var cachedGutterWidth: Int = 0

    private var _cursor: Cursor? = null
    protected val cursor: Cursor
        get() = _cursor ?: editor.cursor

    @JvmField
    var lineBuf: ContentLine? = null

    private var _content: Content? = null
    var content: Content
        get() = _content ?: editor.text
        set(value) {
            _content = value
        }

    @Volatile
    private var renderingFlag: Boolean = false

    @JvmField
    var forcedRecreateLayout: Boolean = false

    private val minimapRenderer: MinimapRenderer

    init {
        metricsText = paintGeneral.fontMetricsInt
        metricsLineNumber = paintOther.fontMetricsInt
        metricsGraph = paintGraph.fontMetricsInt

        lineBreakGraph = editor.context.getDrawable(R.drawable.line_break)
        softwrapLeftGraph = editor.context.getDrawable(R.drawable.softwrap_left)
        softwrapRightGraph = editor.context.getDrawable(R.drawable.softwrap_right)
        minimapRenderer = MinimapRenderer(editor)

        onEditorFullTextUpdate()
    }

    /**
     * Called when the editor text is changed by [CodeEditor.setText]
     */
    open fun onEditorFullTextUpdate() {
        _cursor = editor.cursor
        _content = editor.text
    }

    open fun draw(canvas: Canvas) {
        val saveCount = canvas.save()
        canvas.translate(editor.offsetX.toFloat(), editor.offsetY.toFloat())
        renderingFlag = true
        try {
            drawView(canvas)
        } finally {
            renderingFlag = false
        }
        canvas.restoreToCount(saveCount)
    }

    open fun onSizeChanged(width: Int, height: Int) {
        viewRect.right = width
        viewRect.bottom = height
    }

    open val paint: Paint get() = paintGeneral
    open val otherPaint: Paint get() = paintOther
    open val graphPaint: Paint get() = paintGraph
    fun getPaintOther(): Paint = paintOther
    fun getPaintGraph(): Paint = paintGraph

    fun setCachedLineNumberWidth(width: Int) {
        cachedGutterWidth = width
    }

    fun setTextSizePxDirect(size: Float) {
        paintGeneral.setTextSizeWrapped(size)
        paintOther.textSize = size
        paintGraph.textSize = size * editor.props.functionCharacterSizeFactor
        metricsText = paintGeneral.fontMetricsInt
        metricsLineNumber = paintOther.fontMetricsInt
        metricsGraph = paintGraph.fontMetricsInt
        editor.renderContext.invalidateRenderNodes()
        updateTimestamp()
    }

    /**
     * Set text's typeface
     *
     * @param typefaceText New typeface
     */
    open fun setTypefaceText(typefaceText: Typeface?) {
        val resolvedTypeface = typefaceText ?: Typeface.DEFAULT
        paintGeneral.setTypefaceWrapped(resolvedTypeface)
        metricsText = paintGeneral.fontMetricsInt
        editor.renderContext.invalidateRenderNodes()
        updateTimestamp()
        editor.createLayout()
        editor.invalidate()
    }

    open fun setTypefaceLineNumber(typefaceLineNumber: Typeface?) {
        val resolvedTypeface = typefaceLineNumber ?: Typeface.MONOSPACE
        paintOther.typeface = resolvedTypeface
        metricsLineNumber = paintOther.fontMetricsInt
        editor.invalidate()
    }

    open fun setTextScaleX(textScaleX: Float) {
        paintGeneral.textScaleX = textScaleX
        paintOther.textScaleX = textScaleX
        onTextStyleUpdate()
    }

    open fun setLetterSpacing(letterSpacing: Float) {
        paintGeneral.letterSpacing = letterSpacing
        paintOther.letterSpacing = letterSpacing
        onTextStyleUpdate()
    }

    open fun onTextStyleUpdate() {
        paintGeneral.isRenderFunctionCharacters = editor.isRenderFunctionCharacters
        metricsGraph = paintGraph.fontMetricsInt
        metricsLineNumber = paintOther.fontMetricsInt
        metricsText = paintGeneral.fontMetricsInt
        editor.renderContext.invalidateRenderNodes()
        updateTimestamp()
        editor.createLayout()
        editor.invalidate()
    }

    /**
     * Update timestamp required for measuring cache
     */
    open fun updateTimestamp() {
        displayTimestamp = SystemClock.elapsedRealtimeNanos()
    }

    protected open fun prepareLine(line: Int) {
        lineBuf = getLine(line)
    }

    protected open fun getLine(line: Int): ContentLine {
        if (!renderingFlag) {
            return getLineDirect(line)
        }
        var line2 = preloadedLines.get(line)
        if (line2 == null) {
            line2 = content.getLine(line)
            preloadedLines.put(line, line2)
        }
        return line2
    }

    protected open fun getLineDirections(line: Int): Directions {
        if (!renderingFlag) {
            return content.getLineDirections(line)
        }
        var line2 = preloadedDirections.get(line)
        if (line2 == null) {
            line2 = content.getLineDirections(line)
            preloadedDirections.put(line, line2)
        }
        return line2
    }

    fun getLineDirect(line: Int): ContentLine = content.getLine(line)

    fun getColumnCount(line: Int): Int = getLine(line).length

    open fun updateLineDisplayList(renderNode: RenderNode, line: Int, spans: Spans.Reader) {
        val widthLine = drawSingleTextLine(null, line, 0f, 0f, spans, false)
        renderNode.setPosition(0, 0, (widthLine + 0.5f).toInt(), editor.rowHeight)
        val canvas = renderNode.beginRecording()
        try {
            drawSingleTextLine(canvas, line, 0f, 0f, spans, false)
        } finally {
            renderNode.endRecording()
        }
    }

    @UnsupportedUserUsage
    open fun createTextRow(rowIndex: Int): TextRow {
        val styles = editor.styles
        val spanMap = styles?.spans
        var spanReader = spanMap?.read()
        if (spanReader == null) {
            spanReader = EmptyReader.getInstance()
        }
        val row = editor.layout.getRowAt(rowIndex)
        val line = content.getLine(row.lineIndex)
        val tr = TextRow()
        val cache = editor.renderContext.cache.queryMeasureCache(row.lineIndex)
        var widths =
            if (cache != null && cache.updateTimestamp >= displayTimestamp) cache.widths else null
        widths = if (widths != null && widths.size > line.length) widths else null
        setTextRow(
            tr,
            row.lineIndex,
            line,
            row.startColumn,
            row.endColumn,
            SpansUtils.getSpansOnLine(spanReader, row.lineIndex),
            row.inlayHints ?: emptyList(),
            content.getLineDirections(row.lineIndex),
            widths
        )
        applySelectedTextRange(tr, row.lineIndex, line)
        return tr
    }

    private fun applySelectedTextRange(tr: TextRow, lineIndex: Int, line: ContentLine) {
        if (cursor.isSelected && lineIndex >= cursor.leftLine && lineIndex <= cursor.rightLine) {
            var startColInLine = if (lineIndex == cursor.leftLine) cursor.leftColumn else 0
            var endColInLine =
                if (lineIndex == cursor.rightLine) cursor.rightColumn else line.length
            startColInLine = max(tr.textStart, startColInLine)
            endColInLine = min(tr.textEnd, endColInLine)
            if (startColInLine < endColInLine) {
                tr.setSelectedRange(startColInLine, endColInLine)
            }
        }
    }

    protected open fun drawSingleTextLine(
        canvas: Canvas?,
        line: Int,
        offsetX: Float,
        offsetY: Float,
        spans: Spans.Reader?,
        visibleOnly: Boolean
    ): Float {
        prepareLine(line)
        val columnCount = getColumnCount(line)
        val activeSpans =
            if (spans == null || spans.getSpanCount() <= 0) EmptyReader.getInstance() else spans
        val tr = TextRow()
        val inlayHints = editor.inlayHints
        val lineInlays = inlayHints?.getForLine(line) ?: emptyList()
        val cache = editor.renderContext.cache.queryMeasureCache(line)
        var widths =
            if (cache != null && cache.updateTimestamp >= displayTimestamp) cache.widths else null
        val currentLineBuf = lineBuf!!
        widths = if (widths != null && widths.size > currentLineBuf.length) widths else null
        setTextRow(
            tr,
            line,
            currentLineBuf,
            0,
            columnCount,
            SpansUtils.getSpansOnLine(activeSpans, line),
            lineInlays,
            getLineDirections(line),
            widths
        )
        applySelectedTextRange(tr, line, currentLineBuf)
        if (canvas != null) {
            canvas.save()
            canvas.translate(offsetX, editor.getRowTop(0) + offsetY)
            if (visibleOnly) {
                val visibleStart = max(0f, -offsetX)
                val visibleEnd = max(visibleStart, -offsetX + editor.width)
                tr.draw(canvas, visibleStart, visibleEnd)
            } else {
                tr.draw(canvas, 0f, Float.MAX_VALUE)
            }
            canvas.restore()
        }
        return if (canvas == null) tr.computeRowWidth() else 0f
    }

    open fun hasSideHintIcons(): Boolean {
        val styles = editor.styles
        if (styles != null) {
            val countMap = styles.styleTypeCount
            if (countMap != null) {
                val count = countMap[LineSideIcon::class.java]
                if (count != null && count.value > 0) {
                    return true
                }
            }
        }
        val providers = editor.extraStylesProviders
        if (providers.isNotEmpty()) {
            val first = editor.firstVisibleLine
            val last = editor.lastVisibleLine
            val extra = ArrayList<LineAnchorStyle>()
            for (i in first..last) {
                extra.clear()
                for (j in providers.indices) {
                    providers[j].getExtraStyles(i, extra)
                }
                for (s in extra) {
                    if (s is LineSideIcon) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Paint the view on given Canvas
     *
     * @param canvas Canvas you want to draw
     */
    open fun drawView(canvas: Canvas) {
        cursor.updateCache(editor.firstVisibleLine)

        val color = editor.colorScheme
        drawColor(canvas, color.getColor(EditorColorScheme.WHOLE_BACKGROUND), viewRect)

        val lineNumberWidth = editor.measureLineNumber()
        val sideIconWidth = if (hasSideHintIcons()) editor.rowHeight.toFloat() else 0f
        var offsetX = -editor.offsetX + editor.measureTextRegionOffset()
        val textOffset = offsetX

        val gutterWidth =
            (lineNumberWidth + sideIconWidth + editor.dividerWidth + editor.dividerMarginLeft + editor.dividerMarginRight).toInt()
        if (editor.isWordwrap) {
            if (cachedGutterWidth == 0) {
                cachedGutterWidth = gutterWidth
            } else if (cachedGutterWidth != gutterWidth && !editor.eventHandler.isScaling) {
                cachedGutterWidth = gutterWidth
                editor.postInLifecycle { editor.requestLayoutIfNeeded() }
                editor.createLayout(false)
            } else if (forcedRecreateLayout) {
                editor.createLayout()
                editor.postInLifecycle { editor.requestLayoutIfNeeded() }
            }
        } else {
            cachedGutterWidth = 0
            if (forcedRecreateLayout) {
                editor.createLayout()
            }
        }
        forcedRecreateLayout = false

        prepareLines(editor.firstVisibleLine, editor.lastVisibleLine)
        buildMeasureCacheForLines(
            editor.firstVisibleLine,
            editor.lastVisibleLine,
            displayTimestamp,
            true
        )
        val stuckLines = getStuckCodeBlocks()

        if (cursor.isSelected) {
            editor.insertHandleDescriptor.setEmpty()
        } else {
            editor.leftHandleDescriptor.setEmpty()
            editor.rightHandleDescriptor.setEmpty()
        }

        val lineNumberNotPinned =
            editor.isLineNumberEnabled && (editor.isWordwrap || !editor.isLineNumberPinned)

        val postDrawLineNumbers = this.postDrawLineNumbers
        postDrawLineNumbers.clear()
        val postDrawCurrentLines = this.postDrawCurrentLines
        postDrawCurrentLines.clear()
        val postDrawCursor = ArrayList<DrawCursorTask>(3)
        val firstLn =
            if (editor.isFirstLineNumberAlwaysVisible && editor.isWordwrap) MutableInt(-1) else null

        canvas.save()
        val stuckLineBottom = getStuckLineBottom(stuckLines)
        canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
        drawRows(
            canvas,
            textOffset,
            postDrawLineNumbers,
            postDrawCursor,
            postDrawCurrentLines,
            firstLn,
            stuckLines
        )
        patchHighlightedDelimiters(canvas, textOffset)
        drawDiagnosticIndicators(canvas, offsetX)
        canvas.restore()

        offsetX = -editor.offsetX.toFloat()

        val currentLineNumber = if (cursor.isSelected) -1 else cursor.leftLine

        if (lineNumberNotPinned) {
            drawLineNumberBackground(
                canvas,
                offsetX,
                lineNumberWidth + sideIconWidth + editor.dividerMarginLeft,
                color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND)
            )
            val lineNumberColor = editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER)
            val currentLineBgColor = editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE)
            if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine && editor.isEditable) {
                tmpRect.bottom =
                    (editor.cursorAnimator.animatedLineBottom() - editor.offsetY).toFloat()
                tmpRect.top = tmpRect.bottom - editor.cursorAnimator.animatedLineHeight()
                tmpRect.left = 0f
                tmpRect.right = (textOffset - editor.dividerMarginRight).toInt().toFloat()
                drawColor(canvas, currentLineBgColor, tmpRect)
            }

            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            for (i in 0 until postDrawCurrentLines.count()) {
                drawRowBackground(
                    canvas,
                    currentLineBgColor,
                    postDrawCurrentLines[i],
                    (textOffset - editor.dividerMarginRight).toInt()
                )
            }
            drawUserGutterBackground(canvas, (textOffset - editor.dividerMarginRight).toInt())
            drawSideIcons(canvas, offsetX + lineNumberWidth)
            canvas.restore()

            drawDivider(
                canvas,
                offsetX + lineNumberWidth + sideIconWidth + editor.dividerMarginLeft,
                color.getColor(EditorColorScheme.LINE_DIVIDER)
            )

            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            if (firstLn != null && firstLn.value != -1) {
                val bottom = editor.getRowBottom(0)
                val y: Float
                if (postDrawLineNumbers.size() == 0 || editor.getRowTop(
                        IntPair.getSecond(
                            postDrawLineNumbers[0]
                        )
                    ) - editor.offsetY > bottom
                ) {
                    y =
                        (editor.getRowBottom(0) + editor.getRowTop(0)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent
                } else {
                    val row = IntPair.getSecond(postDrawLineNumbers[0])
                    y =
                        (editor.getRowBottom(row - 1) + editor.getRowTop(row - 1)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent - editor.offsetY
                }
                paintOther.textAlign = editor.lineNumberAlign
                paintOther.color =
                    if (firstLn.value == currentLineNumber) color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lineNumberColor
                val text = (firstLn.value + 1).toString()
                when (editor.lineNumberAlign) {
                    AndroidPaint.Align.LEFT -> canvas.drawText(text, offsetX, y, paintOther)
                    AndroidPaint.Align.RIGHT -> canvas.drawText(
                        text,
                        offsetX + lineNumberWidth,
                        y,
                        paintOther
                    )

                    AndroidPaint.Align.CENTER -> canvas.drawText(
                        text,
                        offsetX + (lineNumberWidth + editor.dividerMarginLeft) / 2f,
                        y,
                        paintOther
                    )

                    else -> {}
                }
            }
            for (i in 0 until postDrawLineNumbers.size()) {
                val packed = postDrawLineNumbers[i]
                drawLineNumber(
                    canvas,
                    IntPair.getFirst(packed),
                    IntPair.getSecond(packed),
                    offsetX,
                    lineNumberWidth,
                    if (IntPair.getFirst(packed) == currentLineNumber) color.getColor(
                        EditorColorScheme.LINE_NUMBER_CURRENT
                    ) else lineNumberColor
                )
            }
            canvas.restore()
        }

        if (editor.isBlockLineEnabled) {
            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            if (editor.isWordwrap) {
                drawSideBlockLine(canvas)
            } else {
                drawBlockLines(canvas, textOffset)
            }
            canvas.restore()
        }

        if (!editor.cursorAnimator.isRunning()) {
            for (action in postDrawCursor) {
                action.execute(canvas)
            }
        } else {
            drawSelectionOnAnimation(canvas)
        }

        drawStuckLines(canvas, stuckLines, textOffset)

        if (editor.isLineNumberEnabled && !lineNumberNotPinned) {
            drawLineNumberBackground(
                canvas,
                0f,
                lineNumberWidth + sideIconWidth + editor.dividerMarginLeft,
                color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND)
            )

            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            val lineNumberColor = editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER)
            val currentLineBgColor = editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE)
            if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine && editor.isEditable) {
                tmpRect.bottom =
                    (editor.cursorAnimator.animatedLineBottom() - editor.offsetY).toFloat()
                tmpRect.top = tmpRect.bottom - editor.cursorAnimator.animatedLineHeight()
                tmpRect.left = 0f
                tmpRect.right = (textOffset - editor.dividerMarginRight).toInt().toFloat()
                drawColor(canvas, currentLineBgColor, tmpRect)
            }
            for (i in 0 until postDrawCurrentLines.count()) {
                drawRowBackground(
                    canvas,
                    currentLineBgColor,
                    postDrawCurrentLines[i],
                    (textOffset - editor.dividerMarginRight + editor.offsetX).toInt()
                )
            }
            drawUserGutterBackground(
                canvas,
                (textOffset - editor.dividerMarginRight + editor.offsetX).toInt()
            )
            drawSideIcons(canvas, lineNumberWidth)
            canvas.restore()

            drawDivider(
                canvas,
                lineNumberWidth + sideIconWidth + editor.dividerMarginLeft,
                color.getColor(EditorColorScheme.LINE_DIVIDER)
            )

            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            for (i in 0 until postDrawLineNumbers.size()) {
                val packed = postDrawLineNumbers[i]
                drawLineNumber(
                    canvas,
                    IntPair.getFirst(packed),
                    IntPair.getSecond(packed),
                    0f,
                    lineNumberWidth,
                    if (IntPair.getFirst(packed) == currentLineNumber) color.getColor(
                        EditorColorScheme.LINE_NUMBER_CURRENT
                    ) else lineNumberColor
                )
            }
            canvas.restore()
        }

        drawStuckLineNumbers(
            canvas,
            stuckLines,
            offsetX,
            lineNumberWidth,
            editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER)
        )
        drawScrollBarsAndMinimap(canvas)
        drawEdgeEffect(canvas)

        releasePreloadedData()
        lastStuckLines = stuckLines
        drawFormatTip(canvas)
    }

    protected open fun drawUserGutterBackground(canvas: Canvas, right: Int) {
        val firstVis = editor.firstVisibleLine
        val lastVis = editor.lastVisibleLine
        for (line in firstVis..lastVis) {
            val bg = getUserGutterBackgroundForLine(line)
            if (bg != null) {
                val bgColor = bg.resolve(editor.colorScheme)
                val top =
                    (editor.layout.getCharLayoutOffset(line, 0)[0] / editor.rowHeight).toInt() - 1
                val count = editor.layout.getRowCountForLine(line)
                for (i in 0 until count) {
                    drawRowBackground(canvas, bgColor, top + i, right)
                }
            }
        }
    }

    protected open fun drawStuckLineNumbers(
        canvas: Canvas,
        candidates: List<CodeBlock>?,
        offset: Float,
        lineNumberWidth: Float,
        lineNumberColor: Int
    ) {
        if (candidates == null || candidates.isEmpty() || !editor.isLineNumberEnabled) {
            return
        }
        val currentLine = if (cursor.isSelected) -1 else cursor.leftLine
        canvas.save()
        val offsetY = editor.offsetY.toFloat()
        canvas.translate(0f, offsetY)
        for (i in candidates.indices) {
            val block = candidates[i]
            val line = block.startLine
            val bg = getUserGutterBackgroundForLine(line)
            val color = bg?.resolve(editor.colorScheme) ?: 0
            val bottomOffset = editor.getRowBottom(i).toFloat()
            val endLineTop = editor.getRowTop(block.endLine) - editor.offsetY
            val shouldTranslate =
                endLineTop < bottomOffset && endLineTop >= bottomOffset - editor.rowHeight
            if (shouldTranslate) {
                canvas.save()
                canvas.clipRect(
                    0f,
                    editor.getRowTop(i) - offsetY,
                    editor.width.toFloat(),
                    editor.height.toFloat()
                )
                canvas.translate(0f, (endLineTop - bottomOffset).toFloat())
            }
            if (currentLine == line || color != 0) {
                tmpRect.top = (editor.getRowTop(i) - offsetY).toFloat()
                tmpRect.bottom = (editor.getRowBottom(i) - offsetY - editor.dpUnit).toFloat()
                tmpRect.left = if (editor.isLineNumberPinned) 0f else offset
                tmpRect.right = tmpRect.left + editor.measureTextRegionOffset()
                if (currentLine == line && editor.isHighlightCurrentLine) {
                    drawColor(
                        canvas,
                        editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE),
                        tmpRect
                    )
                }
                if (color != 0) {
                    drawColor(canvas, color, tmpRect)
                }
            }
            drawLineNumber(
                canvas,
                line,
                i,
                if (editor.isLineNumberPinned) 0f else offset,
                lineNumberWidth,
                if (currentLine == line) editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lineNumberColor
            )
            if (shouldTranslate) {
                canvas.restore()
            }
        }
        canvas.restore()
    }

    protected open fun getStuckLineBottom(candidates: List<CodeBlock>?): Float {
        if (candidates == null || candidates.isEmpty()) {
            return 0f
        }
        var bottomOffset = 0f
        var offsetLine = 0
        var previousLine = -1
        for (i in candidates.indices) {
            val block = candidates[i]
            if (block.startLine > previousLine) {
                bottomOffset = editor.getRowBottom(offsetLine).toFloat()
                val endLineTop = editor.getRowTop(block.endLine) - editor.offsetY
                val shouldTranslate =
                    endLineTop < bottomOffset && endLineTop >= bottomOffset - editor.rowHeight
                if (shouldTranslate) {
                    bottomOffset += (endLineTop - bottomOffset).toFloat()
                }
                previousLine = block.startLine
                offsetLine++
            }
        }
        return bottomOffset
    }

    protected open fun drawStuckLines(canvas: Canvas, candidates: List<CodeBlock>?, offset: Float) {
        if (candidates == null || candidates.isEmpty()) {
            return
        }
        val styles = editor.styles
        val spanMap = styles?.spans
        val spanReader = spanMap?.read()
        var previousLine = -1
        var offsetLine = 0
        val currentLine = if (cursor.isSelected) -1 else cursor.leftLine
        var bottomOffset = 0f
        for (i in candidates.indices) {
            val block = candidates[i]
            if (block.startLine > previousLine) {
                tmpRect.top = (editor.getRowTop(offsetLine)).toFloat()
                bottomOffset = (editor.getRowBottom(offsetLine)).toFloat()
                tmpRect.bottom = bottomOffset
                tmpRect.left = offset
                tmpRect.right = editor.width.toFloat()
                val endLineTop = editor.getRowTop(block.endLine) - editor.offsetY
                val shouldTranslate = endLineTop < tmpRect.bottom && endLineTop >= tmpRect.top
                if (shouldTranslate) {
                    canvas.save()
                    canvas.clipRect(
                        0f,
                        tmpRect.top,
                        editor.width.toFloat(),
                        editor.height.toFloat()
                    )
                    canvas.translate(0f, (endLineTop - tmpRect.bottom).toFloat())
                    bottomOffset += (endLineTop - tmpRect.bottom).toFloat()
                }
                var colorId = EditorColorScheme.WHOLE_BACKGROUND
                if (block.startLine == currentLine && editor.isHighlightCurrentLine) {
                    colorId = EditorColorScheme.CURRENT_LINE
                }
                drawColor(canvas, editor.colorScheme.getColor(colorId), tmpRect)
                if (canvas.isHardwareAccelerated && editor.isHardwareAcceleratedDrawAllowed &&
                    editor.renderContext.renderNodeHolder != null && !editor.eventHandler.isScaling &&
                    (editor.props.cacheRenderNodeForLongLines || getLine(block.startLine).length < 128)
                ) {
                    editor.renderContext.renderNodeHolder?.drawLineHardwareAccelerated(
                        canvas,
                        block.startLine,
                        offset,
                        (offsetLine * editor.rowHeight).toFloat()
                    )
                } else {
                    try {
                        spanReader?.moveToLine(block.startLine)
                        drawSingleTextLine(
                            canvas,
                            block.startLine,
                            offset,
                            (offsetLine * editor.rowHeight).toFloat(),
                            spanReader,
                            true
                        )
                    } finally {
                        spanReader?.moveToLine(-1)
                    }
                }
                previousLine = block.startLine
                offsetLine++
                if (shouldTranslate) {
                    canvas.restore()
                }
            }
        }
        if (bottomOffset > 0f) {
            tmpRect.top = bottomOffset - editor.dpUnit
            tmpRect.bottom = bottomOffset
            tmpRect.left = 0f
            tmpRect.right = editor.width.toFloat()
            val shadow =
                (editor.props.stickyLineIndicator and DirectAccessProps.STICKY_LINE_INDICATOR_SHADOW) != 0
            var showLine =
                (editor.props.stickyLineIndicator and DirectAccessProps.STICKY_LINE_INDICATOR_LINE) != 0
            if (!shadow && !showLine) {
                return
            }
            val lineColor = editor.colorScheme.getColor(EditorColorScheme.STICKY_SCROLL_DIVIDER)
            showLine = lineColor != 0
            if (shadow) {
                canvas.save()
                canvas.clipRect(
                    0f,
                    if (showLine) tmpRect.top else tmpRect.bottom,
                    editor.width.toFloat(),
                    editor.height.toFloat()
                )
                paintGeneral.setShadowLayer(
                    editor.dpUnit * RenderingConstants.DIVIDER_SHADOW_MAX_RADIUS_DIP,
                    0f,
                    0f,
                    Color.BLACK
                )
            }
            val color = if (!showLine && shadow) Color.BLACK else lineColor
            drawColor(canvas, color, tmpRect)
            if (shadow) {
                paintGeneral.setShadowLayer(0f, 0f, 0f, 0)
                canvas.restore()
            }
        }
    }

    protected open fun drawHardwrapMarker(canvas: Canvas, offset: Float) {
        val column = editor.props.hardwrapColumn
        if (!editor.isWordwrap && column > 0) {
            tmpRect.left = offset + paintGeneral.measureText("a") * column
            tmpRect.right = tmpRect.left + editor.dpUnit * 2f
            tmpRect.top = 0f
            tmpRect.bottom = viewRect.bottom.toFloat()
            drawColor(
                canvas,
                editor.colorScheme.getColor(EditorColorScheme.HARD_WRAP_MARKER),
                tmpRect
            )
        }
    }

    protected open fun drawSideIcons(canvas: Canvas, offset: Float) {
        if (!hasSideHintIcons()) {
            return
        }
        var row = editor.firstVisibleRow
        val itr = editor.layout.obtainRowIterator(row)
        val iconSizeFactor = editor.props.sideIconSizeFactor
        val size = (editor.rowHeight * iconSizeFactor).toInt()
        val offsetToLeftTop = (editor.rowHeight * (1f - iconSizeFactor) / 2f).toInt()
        while (row <= editor.lastVisibleRow && itr.hasNext()) {
            val rowInf = itr.next()
            if (rowInf.isLeadingRow) {
                val hint = getLineStyle(rowInf.lineIndex, LineSideIcon::class.java)
                if (hint != null) {
                    val drawable = hint.drawable
                    val rect = Rect(0, 0, size, size)
                    rect.offsetTo(
                        offset.toInt() + offsetToLeftTop,
                        editor.getRowTop(row) - editor.offsetY + offsetToLeftTop
                    )
                    drawable.bounds = rect
                    drawable.draw(canvas)
                }
            }
            row++
        }
    }

    protected open fun drawFormatTip(canvas: Canvas) {
        if (editor.isFormatting) {
            val text = editor.formatTip
            val baseline = editor.getRowBaseline(0).toFloat()
            val rightX = editor.width.toFloat()
            paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.TEXT_NORMAL)
            paintGeneral.isFakeBoldText = true
            paintGeneral.textAlign = AndroidPaint.Align.RIGHT
            canvas.drawText(text, rightX, baseline, paintGeneral)
            paintGeneral.textAlign = AndroidPaint.Align.LEFT
            paintGeneral.isFakeBoldText = false
        }
    }

    /**
     * Draw rect on screen
     * Will not do anything if color is zero
     */
    protected open fun drawColor(canvas: Canvas, color: Int, rect: RectF) {
        if (color != 0) {
            paintGeneral.color = color
            canvas.drawRect(rect, paintGeneral)
        }
    }

    /**
     * Draw rect on screen in a round rectangle
     */
    protected open fun drawColorRound(canvas: Canvas, color: Int, rect: RectF) {
        if (color != 0) {
            paintGeneral.color = color
            canvas.drawRoundRect(
                rect,
                rect.height() * RenderingConstants.ROUND_RECT_FACTOR,
                rect.height() * RenderingConstants.ROUND_RECT_FACTOR,
                paintGeneral
            )
        }
    }

    /**
     * Draw rect on screen
     */
    protected open fun drawColor(canvas: Canvas, color: Int, rect: Rect) {
        if (color != 0) {
            paintGeneral.color = color
            canvas.drawRect(rect, paintGeneral)
        }
    }

    /**
     * Draw background for whole row
     */
    protected open fun drawRowBackground(canvas: Canvas, color: Int, row: Int) {
        drawRowBackground(canvas, color, row, viewRect.right)
    }

    protected open fun drawRowBackground(canvas: Canvas, color: Int, row: Int, right: Int) {
        tmpRect.top = (editor.getRowTop(row) - editor.offsetY).toFloat()
        tmpRect.bottom = (editor.getRowBottom(row) - editor.offsetY).toFloat()
        tmpRect.left = 0f
        tmpRect.right = right.toFloat()
        drawColor(canvas, color, tmpRect)
    }

    /**
     * Draw single line number
     */
    protected open fun drawLineNumber(
        canvas: Canvas,
        line: Int,
        row: Int,
        offsetX: Float,
        width: Float,
        color: Int
    ) {
        if (width + offsetX <= 0) {
            return
        }
        if (paintOther.textAlign != editor.lineNumberAlign) {
            paintOther.textAlign = editor.lineNumberAlign
        }
        paintOther.color = color
        val y =
            (editor.getRowBottom(row) + editor.getRowTop(row)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent - editor.offsetY

        val buffer = TemporaryCharBuffer.obtain(20)
        val displayLine = line + 1
        val i = Numbers.stringSize(displayLine)
        Numbers.getChars(displayLine, i, buffer)

        when (editor.lineNumberAlign) {
            AndroidPaint.Align.LEFT -> canvas.drawText(buffer, 0, i, offsetX, y, paintOther)
            AndroidPaint.Align.RIGHT -> canvas.drawText(
                buffer,
                0,
                i,
                offsetX + width,
                y,
                paintOther
            )

            AndroidPaint.Align.CENTER -> canvas.drawText(
                buffer,
                0,
                i,
                offsetX + (width + editor.dividerMarginLeft) / 2f,
                y,
                paintOther
            )

            else -> {}
        }
        TemporaryCharBuffer.recycle(buffer)
    }

    /**
     * Draw line number background
     */
    protected open fun drawLineNumberBackground(
        canvas: Canvas,
        offsetX: Float,
        width: Float,
        color: Int
    ) {
        val right = offsetX + width
        if (right < 0) {
            return
        }
        val left = max(0f, offsetX)
        tmpRect.bottom = editor.height.toFloat()
        tmpRect.top = 0f
        val offY = editor.offsetY
        if (offY < 0) {
            tmpRect.bottom = tmpRect.bottom - offY
            tmpRect.top = tmpRect.top - offY
        }
        tmpRect.left = left
        tmpRect.right = right
        drawColor(canvas, color, tmpRect)
    }

    /**
     * Draw divider line
     */
    protected open fun drawDivider(canvas: Canvas, offsetX: Float, color: Int) {
        val shadow = editor.isLineNumberPinned && !editor.isWordwrap && editor.offsetX > 0
        val right = offsetX + editor.dividerWidth
        if (right < 0) {
            return
        }
        val left = max(0f, offsetX)
        tmpRect.bottom = editor.height.toFloat()
        tmpRect.top = 0f
        val offY = editor.offsetY
        if (offY < 0) {
            tmpRect.bottom = tmpRect.bottom - offY
            tmpRect.top = tmpRect.top - offY
        }
        tmpRect.left = left
        tmpRect.right = right
        if (shadow) {
            canvas.save()
            canvas.clipRect(tmpRect.left, tmpRect.top, editor.width.toFloat(), tmpRect.bottom)
            paintGeneral.setShadowLayer(
                min(
                    editor.dpUnit * RenderingConstants.DIVIDER_SHADOW_MAX_RADIUS_DIP,
                    editor.offsetX.toFloat()
                ), 0f, 0f, Color.BLACK
            )
        }
        drawColor(canvas, color, tmpRect)
        if (shadow) {
            canvas.restore()
            paintGeneral.setShadowLayer(0f, 0f, 0f, 0)
        }
    }

    private fun prepareLines(start: Int, end: Int) {
        releasePreloadedData()
        content.runReadActionsOnLines(
            max(0, start - 5),
            min(content.lineCount - 1, end + 5),
            Content.ContentLineConsumer { i, line, dirs ->
                preloadedLines.put(i, line)
                preloadedDirections.put(i, dirs)
            }
        )
    }

    private fun releasePreloadedData() {
        preloadedLines.clear()
        preloadedDirections.clear()
    }

    protected open fun getStuckCodeBlocks(): List<CodeBlock>? {
        if (editor.isWordwrap || !editor.props.stickyScroll) {
            return null
        }
        val styles = editor.styles ?: return null
        val codeBlocks = styles.blocks ?: return null
        var startLine = editor.firstVisibleLine
        var offsetY = editor.offsetY
        val rowHeight = editor.rowHeight

        val visibleBlocks = ArrayList<CodeBlock>()
        val first = editor.firstVisibleLine
        val last = editor.lastVisibleLine
        var mark = false
        var invalidCount = 0
        val maxCount = styles.suppressSwitch
        var mm = editor.binarySearchEndBlock(first, codeBlocks)
        if (mm == -1) {
            mm = 0
        }
        for (curr in mm until codeBlocks.size) {
            val block = codeBlocks[curr]
            if (CodeEditor.hasVisibleRegion(block.startLine, block.endLine, first, last)) {
                visibleBlocks.add(block)
                mark = true
            } else if (mark) {
                if (invalidCount >= maxCount) break
                invalidCount++
            }
        }
        visibleBlocks.sortWith(CodeBlock.COMPARATOR_START)
        var finalCandidates = ArrayList<CodeBlock>()
        for (i in visibleBlocks.indices) {
            val block = visibleBlocks[i]
            if (block.endLine > startLine && editor.getRowTop(block.startLine) - offsetY < 0) {
                finalCandidates.add(block)
                startLine++
                offsetY += rowHeight
            }
        }
        val maxLines = editor.props.stickyScrollMaxLines
        if (finalCandidates.size > maxLines) {
            if (maxLines <= 0) {
                return null
            }
            finalCandidates = if (editor.props.stickyScrollPreferInnerScope) {
                ArrayList(
                    finalCandidates.subList(
                        finalCandidates.size - maxLines,
                        finalCandidates.size
                    )
                )
            } else {
                ArrayList(finalCandidates.subList(0, maxLines))
            }
        }
        if (cursor.isSelected && editor.props.stickyScrollAutoCollapse) {
            val limitLine = cursor.leftLine
            val firstVis = editor.firstVisibleLine
            val lastSelectionLine = cursor.rightLine
            if (lastSelectionLine >= firstVis) {
                while (finalCandidates.isNotEmpty() && firstVis + finalCandidates.size >= limitLine) {
                    finalCandidates.removeAt(finalCandidates.size - 1)
                }
            }
        }
        return finalCandidates
    }

    private val coordinateLine = LineStyles(0)

    protected open fun getLineStyles(line: Int): LineStyles? {
        val styles = editor.styles
        val lineStylesList = styles?.lineStyles
        var result: LineStyles? = null
        if (lineStylesList != null) {
            coordinateLine.line = line
            val index = lineStylesList.binarySearch(coordinateLine)
            if (index >= 0 && index < lineStylesList.size) {
                result = lineStylesList[index]
            }
        }

        val providers = editor.extraStylesProviders
        if (providers.isNotEmpty()) {
            val extra = ArrayList<LineAnchorStyle>()
            for (j in providers.indices) {
                providers[j].getExtraStyles(line, extra)
            }
            if (extra.isNotEmpty()) {
                if (result == null) {
                    result = LineStyles(line)
                } else {
                    val copy = LineStyles(line)
                    for (i in 0 until result.getElementCount()) {
                        copy.addStyle(result.getElementAt(i))
                    }
                    result = copy
                }
                for (style in extra) {
                    result.addStyle(style)
                }
            }
        }
        return result
    }

    open fun <T : LineAnchorStyle> getLineStyle(line: Int, type: Class<T>): T? {
        val lineStyles = getLineStyles(line) ?: return null
        return lineStyles.findOne(type)
    }

    protected open fun getUserBackgroundForLine(line: Int): ResolvableColor? {
        val bg = getLineStyle(line, LineBackground::class.java)
        return bg?.color
    }

    protected open fun getUserGutterBackgroundForLine(line: Int): ResolvableColor? {
        val bg = getLineStyle(line, LineGutterBackground::class.java)
        return bg?.color
    }

    protected open fun drawAnimatedCurrentLineBackground(canvas: Canvas, currentLineBgColor: Int) {
        tmpRect.bottom = (editor.cursorAnimator.animatedLineBottom() - editor.offsetY).toFloat()
        tmpRect.top = tmpRect.bottom - editor.cursorAnimator.animatedLineHeight()
        tmpRect.left = 0f
        tmpRect.right = viewRect.right.toFloat()
        drawColor(canvas, currentLineBgColor, tmpRect)
    }

    open fun createTextRowParams(): TextRowParams {
        return TextRowParams(
            editor.tabWidth, textMetrics, editor.getRowTopOfText(0),
            editor.getRowBottomOfText(0), editor.rowHeightOfText, editor.getRowBaseline(0),
            editor.getRowTop(0), editor.getRowBottom(0),
            editor.rowHeight, editor.props.roundTextBackgroundFactor,
            editor, editor.colorScheme, paintOther, paintGraph, metricsGraph
        )
    }

    private fun setTextRow(
        row: TextRow,
        line: Int,
        text: ContentLine,
        start: Int,
        end: Int,
        spans: List<Span>,
        inlayHints: List<InlayHint>,
        directions: Directions,
        measureCache: TextAdvancesCache?
    ) {
        var composingSource: CharSequence? = null
        var composingColumn = 0
        val composing = editor.inputConnection.composingText
        if (line >= 0 && composing.isComposing && composing.text is android.text.Spanned &&
            composing.startIndex >= 0 && composing.startIndex <= editor.text.length
        ) {
            val position = editor.text.indexer.getCharPosition(composing.startIndex)
            if (position.line == line) {
                composingSource = composing.text
                composingColumn = position.column
            }
        }
        row.set(
            text,
            start,
            end,
            spans,
            inlayHints,
            directions,
            paintGeneral,
            measureCache,
            createTextRowParams(),
            composingSource,
            composingColumn
        )
    }

    protected open fun drawRows(
        canvas: Canvas,
        offset: Float,
        postDrawLineNumbers: LongArrayList,
        postDrawCursor: MutableList<DrawCursorTask>,
        postDrawCurrentLines: MutableIntList,
        requiredFirstLn: MutableInt?,
        stuckLines: List<CodeBlock>?
    ) {
        val firstVis = editor.firstVisibleRow
        val rowIterator = editor.layout.obtainRowIterator(firstVis, preloadedLines)
        val spans = editor.styles?.spans
        val matchedPositions = this.matchedPositions
        val highlightPositions = this.highlightPositions
        matchedPositions.clear()
        highlightPositions.clear()
        val currentLine = if (cursor.isSelected) -1 else cursor.leftLine
        val currentLineBgColor = editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE)
        val currentRow =
            if (cursor.isSelected) -1 else editor.layout.getRowIndexForPosition(cursor.left)
        val currentRowBorder = editor.colorScheme.getColor(EditorColorScheme.CURRENT_ROW_BORDER)
        var lastPreparedLine = -1
        var leadingWhitespaceEnd = 0
        var trailingWhitespaceStart = 0
        var circleRadius = 0f
        val miniGraphWidth =
            if (editor.isWordwrap && (editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) this.miniGraphWidth else 0f
        val composing = editor.inputConnection.composingText
        val composingPosition =
            if (composing.isComposing && composing.startIndex >= 0 && composing.startIndex < content.length) {
                content.indexer.getCharPosition(composing.startIndex)
            } else {
                null
            }
        val composingLength = composing.endIndex - composing.startIndex
        val draggingSelection = editor.eventHandler.draggingSelection
        if (editor.shouldInitializeNonPrintable()) {
            val spaceWidth = paintGeneral.spaceWidth
            circleRadius = min(
                editor.rowHeight.toFloat(),
                spaceWidth
            ) * RenderingConstants.NON_PRINTABLE_CIRCLE_RADIUS_FACTOR
        }
        if (!editor.isWordwrap && canvas.isHardwareAccelerated && editor.isHardwareAcceleratedDrawAllowed) {
            editor.renderContext.renderNodeHolder?.keepCurrentInDisplay(
                firstVis,
                editor.lastVisibleRow,
                stuckLines
            )
        }
        val offset2 = editor.offsetX - editor.measureTextRegionOffset()

        // Step 1 - Draw background of rows
        val lineBgOverlapBehavior = editor.props.cursorLineBgOverlapBehavior
        if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine && editor.isEditable &&
            (lineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_CURSOR || lineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_MIXED)
        ) {
            drawAnimatedCurrentLineBackground(canvas, currentLineBgColor)
        }

        var rowIdx = firstVis
        while (rowIdx <= editor.lastVisibleRow && rowIterator.hasNext()) {
            val rowInf = rowIterator.next()
            val line = rowInf.lineIndex
            if (lastPreparedLine != line) {
                prepareLine(line)
                lastPreparedLine = line
            }

            var drawCurrentLineBg = line == currentLine &&
                    !editor.cursorAnimator.isRunning() &&
                    editor.isHighlightCurrentLine &&
                    editor.isEditable

            val drawCustomLineBg = !drawCurrentLineBg ||
                    (editor.props.drawCustomLineBgOnCurrentLine && lineBgOverlapBehavior != CURSOR_LINE_BG_OVERLAP_CUSTOM)

            var isOverlapping = false

            if (drawCustomLineBg) {
                val customBackground = getUserBackgroundForLine(line)
                if (customBackground != null) {
                    val color = customBackground.resolve(editor.colorScheme)
                    if (line == currentLine) {
                        isOverlapping = true
                    }
                    drawRowBackground(canvas, color, rowIdx)
                }
            }

            if (isOverlapping) {
                drawCurrentLineBg =
                    drawCurrentLineBg && lineBgOverlapBehavior != CURSOR_LINE_BG_OVERLAP_CURSOR
            }

            if (drawCurrentLineBg) {
                var commitCurrentLineBg = currentLineBgColor
                if (isOverlapping && lineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_MIXED) {
                    commitCurrentLineBg = (commitCurrentLineBg and 0x00FFFFFF) or 0x80000000.toInt()
                }
                drawRowBackground(canvas, commitCurrentLineBg, rowIdx)
                postDrawCurrentLines.add(rowIdx)
            }
            rowIdx++
        }

        if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine &&
            lineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_CUSTOM
        ) {
            drawAnimatedCurrentLineBackground(canvas, currentLineBgColor)
        }
        rowIterator.reset()

        lastPreparedLine = -1
        rowIdx = firstVis
        while (rowIdx <= editor.lastVisibleRow && rowIterator.hasNext()) {
            val rowInf = rowIterator.next()
            canvas.save()
            canvas.translate(rowInf.renderTranslateX, 0f)
            val line = rowInf.lineIndex
            val columnCount = getColumnCount(line)
            if (lastPreparedLine != line) {
                editor.computeMatchedPositions(line, matchedPositions)
                editor.computeHighlightPositions(line, highlightPositions)
                prepareLine(line)
                lastPreparedLine = line
            }
            var paintingOffset = -offset2
            if (!rowInf.isLeadingRow) {
                paintingOffset += miniGraphWidth
            }

            // Draw matched text background
            if (matchedPositions.size() > 0) {
                val tr = createTextRow(rowIdx)
                for (i in 0 until matchedPositions.size()) {
                    val position = matchedPositions[i]
                    val start = IntPair.getFirst(position)
                    val end = IntPair.getSecond(position)
                    drawRowRegionBackground(
                        canvas,
                        rowIdx,
                        tr,
                        start,
                        end,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        editor.colorScheme.getColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND),
                        editor.colorScheme.getColor(EditorColorScheme.MATCHED_TEXT_BORDER)
                    )
                }
            }

            // Draw highlight text background
            if (highlightPositions.size > 0) {
                val finalRow = rowIdx
                val tr = createTextRow(rowIdx)
                highlightPositions.forEach { position, colorPair ->
                    val start = IntPair.getFirst(position)
                    val end = IntPair.getSecond(position)
                    drawRowRegionBackground(
                        canvas, finalRow, tr, start, end, rowInf.startColumn,
                        rowInf.endColumn, IntPair.getFirst(colorPair), IntPair.getSecond(colorPair)
                    )
                }
            }

            // Draw selected text background
            if (cursor.isSelected && line >= cursor.leftLine && line <= cursor.rightLine) {
                var selectionStart = 0
                var selectionEnd = columnCount
                if (line == cursor.leftLine) {
                    selectionStart = cursor.leftColumn
                }
                if (line == cursor.rightLine) {
                    selectionEnd = cursor.rightColumn
                }
                if (getColumnCount(line) == 0 && line != cursor.rightLine) {
                    tmpRect.top = (getRowTopForBackground(rowIdx) - editor.offsetY).toFloat()
                    tmpRect.bottom = (getRowBottomForBackground(rowIdx) - editor.offsetY).toFloat()
                    tmpRect.left = paintingOffset
                    tmpRect.right = tmpRect.left + paintGeneral.spaceWidth * 2f
                    drawRowBackgroundRectWithBorder(
                        canvas, tmpRect,
                        editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND),
                        editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BORDER)
                    )
                } else if (selectionStart < selectionEnd) {
                    drawRowRegionBackground(
                        canvas,
                        rowIdx,
                        null,
                        selectionStart,
                        selectionEnd,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND),
                        editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BORDER)
                    )
                }
            }
            canvas.restore()

            // Draw current row border
            if (rowIdx == currentRow && currentRowBorder != 0) {
                tmpRect.top = (editor.getRowTop(rowIdx) - editor.offsetY).toFloat()
                tmpRect.bottom = (editor.getRowBottom(rowIdx) - editor.offsetY).toFloat()
                tmpRect.left = max(0f, -offset2)
                tmpRect.right = editor.width.toFloat()
                paintGeneral.color = currentRowBorder
                paintGeneral.style = AndroidPaint.Style.STROKE
                paintGeneral.strokeWidth = editor.dpUnit
                canvas.drawRect(tmpRect, paintGeneral)
                paintGeneral.style = AndroidPaint.Style.FILL
            }
            rowIdx++
        }
        rowIterator.reset()

        patchSnippetRegions(canvas, offset)
        drawHardwrapMarker(canvas, offset)

        // Step 2 - Draw text and text decorations
        var reader: Spans.Reader = EmptyReader.getInstance()
        lastPreparedLine = -1
        var lineCache: TextAdvancesCache? = null
        rowIdx = firstVis
        while (rowIdx <= editor.lastVisibleRow && rowIterator.hasNext()) {
            val rowInf = rowIterator.next()
            val line = rowInf.lineIndex
            val contentLine = getLine(line)
            val columnCount = contentLine.length
            if (rowIdx == firstVis && requiredFirstLn != null) {
                requiredFirstLn.value = line
            } else if (rowInf.isLeadingRow) {
                postDrawLineNumbers.add(IntPair.pack(line, rowIdx))
            }

            if (lastPreparedLine != line) {
                lastPreparedLine = line
                val cache = editor.renderContext.cache.queryMeasureCache(line)
                lineCache = if (cache != null && cache.updateTimestamp == displayTimestamp &&
                    cache.widths != null && cache.widths!!.size > columnCount
                ) {
                    cache.widths
                } else {
                    null
                }
                prepareLine(line)
                if (reader !== EmptyReader.getInstance()) {
                    try {
                        reader.moveToLine(-1)
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "Failed to release SpanReader", e)
                    }
                }
                reader = spans?.read() ?: EmptyReader.getInstance()
                try {
                    reader.moveToLine(line)
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "Failed to read span", e)
                    reader = EmptyReader.getInstance()
                }
                if (reader.getSpanCount() == 0) {
                    reader = EmptyReader.getInstance()
                }
                if (editor.shouldInitializeNonPrintable()) {
                    val positions = editor.findLeadingAndTrailingWhitespacePos(lineBuf!!)
                    leadingWhitespaceEnd = IntPair.getFirst(positions)
                    trailingWhitespaceStart = IntPair.getSecond(positions)
                }
            }

            var paintingOffset = -offset2
            var offsetCopy = offset2

            paintingOffset += rowInf.renderTranslateX
            offsetCopy -= rowInf.renderTranslateX

            if (!rowInf.isLeadingRow) {
                if ((editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
                    drawMiniGraph(canvas, offset, rowIdx, softwrapLeftGraph)
                    paintingOffset += miniGraphWidth
                    offsetCopy -= miniGraphWidth
                }
            }

            val backupOffset = paintingOffset
            val nonPrintableFlags = editor.nonPrintablePaintingFlags

            // Draw text
            if (!editor.isHardwareAcceleratedDrawAllowed ||
                editor.eventHandler.isScaling ||
                !canvas.isHardwareAccelerated || editor.isWordwrap ||
                (rowInf.endColumn - rowInf.startColumn > 128 && !editor.props.cacheRenderNodeForLongLines)
            ) {
                val tr = TextRow()
                setTextRow(
                    tr,
                    line,
                    lineBuf!!,
                    rowInf.startColumn,
                    rowInf.endColumn,
                    SpansUtils.getSpansOnLine(reader, line),
                    rowInf.inlayHints ?: emptyList(),
                    getLineDirections(line),
                    lineCache
                )
                applySelectedTextRange(tr, line, lineBuf!!)

                canvas.save()
                canvas.translate(-offsetCopy, (editor.getRowTop(rowIdx) - editor.offsetY).toFloat())
                val beginOffset = max(0f, offsetCopy)
                val endOffset = beginOffset + editor.width
                val result = tr.draw(canvas, beginOffset, endOffset)
                canvas.restore()

                val exhausted = IntPair.getFirst(result) == 1
                paintingOffset += IntPair.getSecondAsFloat(result)

                if (exhausted && rowInf.isTrailingRow && (nonPrintableFlags and CodeEditor.FLAG_DRAW_LINE_SEPARATOR) != 0) {
                    drawMiniGraph(canvas, paintingOffset, rowIdx, lineBreakGraph)
                } else if (!rowInf.isTrailingRow && editor.isWordwrap && (nonPrintableFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
                    drawMiniGraph(canvas, paintingOffset, rowIdx, softwrapRightGraph)
                }
            } else {
                paintingOffset =
                    offset + editor.renderContext.renderNodeHolder!!.drawLineHardwareAccelerated(
                        canvas, line, offset, (editor.getRowTop(rowIdx) - editor.offsetY).toFloat()
                    )
                if (rowInf.isTrailingRow && (nonPrintableFlags and CodeEditor.FLAG_DRAW_LINE_SEPARATOR) != 0) {
                    drawMiniGraph(canvas, paintingOffset, rowIdx, lineBreakGraph)
                }
            }

            paintingOffset = backupOffset

            // Draw non-printable characters
            if (circleRadius != 0f && (leadingWhitespaceEnd != columnCount || (nonPrintableFlags and CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE) != 0)) {
                val tr = TextRow()
                setTextRow(
                    tr,
                    line,
                    lineBuf!!,
                    rowInf.startColumn,
                    rowInf.endColumn,
                    SpansUtils.getSpansOnLine(reader, line),
                    rowInf.inlayHints ?: emptyList(),
                    getLineDirections(line),
                    lineCache
                )
                canvas.save()
                canvas.translate(
                    paintingOffset,
                    (editor.getRowTopOfText(rowIdx) - editor.offsetY).toFloat()
                )
                bufferedDrawPoints.setOffsets(
                    paintingOffset,
                    (editor.getRowTopOfText(rowIdx) - editor.offsetY).toFloat()
                )
                val beginOffset = max(0f, offsetCopy)
                val endOffset = beginOffset + editor.width
                val wsLeadingEnd = leadingWhitespaceEnd
                val wsTrailingStart = trailingWhitespaceStart

                paintOther.color = editor.colorScheme.getColor(EditorColorScheme.NON_PRINTABLE_CHAR)
                tr.iterateDrawTextRegions(
                    rowInf.startColumn, rowInf.endColumn, canvas, beginOffset, endOffset, false
                ) { _canvas, text, index, count, contextIndex, contextCount, isRtl, horizontalOffset, width, params, span ->
                    val targetCanvas = _canvas ?: canvas
                    if ((nonPrintableFlags and CodeEditor.FLAG_DRAW_WHITESPACE_LEADING) != 0) {
                        drawWhitespaces(
                            targetCanvas,
                            tr,
                            text,
                            index,
                            count,
                            contextIndex,
                            contextCount,
                            isRtl,
                            horizontalOffset,
                            width,
                            0,
                            wsLeadingEnd
                        )
                    }
                    if ((nonPrintableFlags and CodeEditor.FLAG_DRAW_WHITESPACE_INNER) != 0) {
                        drawWhitespaces(
                            targetCanvas,
                            tr,
                            text,
                            index,
                            count,
                            contextIndex,
                            contextCount,
                            isRtl,
                            horizontalOffset,
                            width,
                            wsLeadingEnd,
                            wsTrailingStart
                        )
                    }
                    if ((nonPrintableFlags and CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING) != 0) {
                        drawWhitespaces(
                            targetCanvas,
                            tr,
                            text,
                            index,
                            count,
                            contextIndex,
                            contextCount,
                            isRtl,
                            horizontalOffset,
                            width,
                            wsTrailingStart,
                            columnCount
                        )
                    }
                    if ((nonPrintableFlags and CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION) != 0 && cursor.isSelected && line >= cursor.leftLine && line <= cursor.rightLine) {
                        var selectionStart = 0
                        var selectionEnd = columnCount
                        if (line == cursor.leftLine) {
                            selectionStart = cursor.leftColumn
                        }
                        if (line == cursor.rightLine) {
                            selectionEnd = cursor.rightColumn
                        }
                        if ((nonPrintableFlags and 0b1110) == 0) {
                            drawWhitespaces(
                                targetCanvas,
                                tr,
                                text,
                                index,
                                count,
                                contextIndex,
                                contextCount,
                                isRtl,
                                horizontalOffset,
                                width,
                                selectionStart,
                                selectionEnd
                            )
                        } else {
                            if ((nonPrintableFlags and CodeEditor.FLAG_DRAW_WHITESPACE_LEADING) == 0) {
                                drawWhitespaces(
                                    targetCanvas,
                                    tr,
                                    text,
                                    index,
                                    count,
                                    contextIndex,
                                    contextCount,
                                    isRtl,
                                    horizontalOffset,
                                    width,
                                    selectionStart,
                                    min(wsLeadingEnd, selectionEnd)
                                )
                            }
                            if ((nonPrintableFlags and CodeEditor.FLAG_DRAW_WHITESPACE_INNER) == 0) {
                                drawWhitespaces(
                                    targetCanvas,
                                    tr,
                                    text,
                                    index,
                                    count,
                                    contextIndex,
                                    contextCount,
                                    isRtl,
                                    horizontalOffset,
                                    width,
                                    max(wsLeadingEnd, selectionStart),
                                    min(wsTrailingStart, selectionEnd)
                                )
                            }
                            if ((nonPrintableFlags and CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING) == 0) {
                                drawWhitespaces(
                                    targetCanvas,
                                    tr,
                                    text,
                                    index,
                                    count,
                                    contextIndex,
                                    contextCount,
                                    isRtl,
                                    horizontalOffset,
                                    width,
                                    max(wsTrailingStart, selectionStart),
                                    selectionEnd
                                )
                            }
                        }
                    }
                }
                canvas.restore()
                bufferedDrawPoints.setOffsets(0f, 0f)
            }

            // Draw composing text underline
            if (composingPosition != null && line == composingPosition.line) {
                val composingStart = composingPosition.column
                val composingEnd = composingStart + composingLength
                val paintStart = min(max(composingStart, rowInf.startColumn), rowInf.endColumn)
                val paintEnd = min(max(composingEnd, rowInf.startColumn), rowInf.endColumn)

                if (paintStart < paintEnd) {
                    val tr = TextRow()
                    setTextRow(
                        tr,
                        line,
                        lineBuf!!,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        SpansUtils.getSpansOnLine(reader, line),
                        rowInf.inlayHints ?: emptyList(),
                        content.getLineDirections(line),
                        lineCache
                    )
                    tmpRect.top = (editor.getRowBottom(rowIdx) - editor.offsetY).toFloat()
                    tmpRect.bottom = tmpRect.top + editor.rowHeight * 0.06f
                    val finalOffset = paintingOffset
                    tr.iterateBackgroundRegions(paintStart, paintEnd, false, false) { left, right ->
                        tmpRect.left = finalOffset + left
                        tmpRect.right = finalOffset + right
                        if (tmpRect.right > 0f && tmpRect.left < editor.width) {
                            drawColor(
                                canvas,
                                editor.colorScheme.getColor(EditorColorScheme.UNDERLINE),
                                tmpRect
                            )
                        }
                        tmpRect.right < editor.width
                    }
                }
            }

            val layout = editor.layout
            if (cursor.isSelected) {
                if (cursor.leftLine == line && isInside(
                        cursor.leftColumn,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        rowInf.isTrailingRow
                    )
                ) {
                    val centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                        cursor.leftLine,
                        cursor.leftColumn
                    )[1] - editor.offsetX
                    val type = if (content.isRtlAt(
                            cursor.leftLine,
                            cursor.leftColumn
                        )
                    ) SelectionHandleStyle.HANDLE_TYPE_RIGHT else SelectionHandleStyle.HANDLE_TYPE_LEFT
                    val task = DrawCursorTask(
                        centerX,
                        (getRowBottomForBackground(rowIdx) - editor.offsetY).toFloat(),
                        type,
                        editor.leftHandleDescriptor
                    )
                    postDrawCursor.add(task)
                    applyBidiIndicatorAttrs(task, cursor.leftLine, cursor.leftColumn)
                }
                if (cursor.rightLine == line && isInside(
                        cursor.rightColumn,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        rowInf.isTrailingRow
                    )
                ) {
                    val centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                        cursor.rightLine,
                        cursor.rightColumn
                    )[1] - editor.offsetX
                    val type = if (content.isRtlAt(
                            cursor.rightLine,
                            cursor.rightColumn
                        )
                    ) SelectionHandleStyle.HANDLE_TYPE_LEFT else SelectionHandleStyle.HANDLE_TYPE_RIGHT
                    val task = DrawCursorTask(
                        centerX,
                        (getRowBottomForBackground(rowIdx) - editor.offsetY).toFloat(),
                        type,
                        editor.rightHandleDescriptor
                    )
                    postDrawCursor.add(task)
                    applyBidiIndicatorAttrs(task, cursor.rightLine, cursor.rightColumn)
                }
            } else if (cursor.leftLine == line && isInside(
                    cursor.leftColumn,
                    rowInf.startColumn,
                    rowInf.endColumn,
                    rowInf.isTrailingRow
                )
            ) {
                val centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                    cursor.leftLine,
                    cursor.leftColumn
                )[1] - editor.offsetX
                val task = DrawCursorTask(
                    centerX,
                    (getRowBottomForBackground(rowIdx) - editor.offsetY).toFloat(),
                    SelectionHandleStyle.HANDLE_TYPE_INSERT,
                    editor.insertHandleDescriptor
                )
                postDrawCursor.add(task)
                applyBidiIndicatorAttrs(task, cursor.leftLine, cursor.leftColumn)
            }

            if (draggingSelection != null) {
                if (draggingSelection.line == line && isInside(
                        draggingSelection.column,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        rowInf.isTrailingRow
                    )
                ) {
                    val centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                        draggingSelection.line,
                        draggingSelection.column
                    )[1] - editor.offsetX
                    val task = DrawCursorTask(
                        centerX,
                        (getRowBottomForBackground(rowIdx) - editor.offsetY).toFloat(),
                        SelectionHandleStyle.HANDLE_TYPE_UNDEFINED,
                        null
                    )
                    postDrawCursor.add(task)
                    applyBidiIndicatorAttrs(task, draggingSelection.line, draggingSelection.column)
                }
            } else if (editor.isInMouseMode && editor.isTextSelected) {
                val target = editor.selectingTarget
                if (target != null && target.line == line && isInside(
                        target.column,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        rowInf.isTrailingRow
                    )
                ) {
                    val centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                        target.line,
                        target.column
                    )[1] - editor.offsetX
                    val task = DrawCursorTask(
                        centerX,
                        (getRowBottomForBackground(rowIdx) - editor.offsetY).toFloat(),
                        SelectionHandleStyle.HANDLE_TYPE_UNDEFINED,
                        null
                    )
                    postDrawCursor.add(task)
                    applyBidiIndicatorAttrs(task, target.line, target.column)
                }
            }
            rowIdx++
        }

        if (reader !== EmptyReader.getInstance()) {
            try {
                reader.moveToLine(-1)
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Failed to release SpanReader", e)
            }
        }

        paintGeneral.isFakeBoldText = false
        paintGeneral.textSkewX = 0f
        paintOther.strokeWidth = circleRadius * 2f
        bufferedDrawPoints.commitPoints(canvas, paintOther)
    }

    private fun getBidiIndicatorAttrs(line: Int, column: Int): Long {
        val lineDirections = getLineDirections(line)
        val count = lineDirections.runCount
        if (count == 1) {
            return IntPair.pack(0, if (lineDirections.isRunRtl(0)) 1 else 0)
        }
        for (i in 0 until count) {
            if (i + 1 == count || (lineDirections.getRunStart(i) <= column && column < lineDirections.getRunEnd(
                    i
                ))
            ) {
                return IntPair.pack(
                    if (editor.props.showBidiDirectionIndicator) 1 else 0,
                    if (lineDirections.isRunRtl(i)) 1 else 0
                )
            }
        }
        return IntPair.pack(0, 0)
    }

    private fun applyBidiIndicatorAttrs(task: DrawCursorTask, line: Int, column: Int) {
        val bidiAttrs = getBidiIndicatorAttrs(line, column)
        task.setBidiIndicatorRequired(IntPair.getFirst(bidiAttrs) == 1)
        task.setRightToLeft(IntPair.getSecond(bidiAttrs) == 1)
    }

    private fun drawBidiSelectionIndicator(
        canvas: Canvas,
        x: Float,
        topY: Float,
        selectionHeight: Float,
        isRtl: Boolean
    ) {
        val height = selectionHeight * 0.2f
        val deltaX = height * 0.866f
        tmpPath.reset()
        tmpPath.moveTo(x, topY)
        tmpPath.lineTo(x + (if (isRtl) -deltaX else deltaX), topY + height / 2f)
        tmpPath.lineTo(x, topY + height)
        tmpPath.close()
        canvas.drawPath(tmpPath, paintGeneral)
    }

    protected open fun drawDiagnosticIndicator(
        canvas: Canvas,
        style: DiagnosticIndicatorStyle,
        i: Int,
        startX: Float,
        endX: Float
    ) {
        val waveLength = editor.dpUnit * editor.props.indicatorWaveLength
        val amplitude = editor.dpUnit * editor.props.indicatorWaveAmplitude
        val waveWidth = editor.dpUnit * editor.props.indicatorWaveWidth
        val centerY = (editor.getRowBottom(i) - editor.offsetY).toFloat()
        when (style) {
            DiagnosticIndicatorStyle.WAVY_LINE -> {
                val lineWidth = 0f - startX
                val waveCountInitial = ceil((lineWidth / waveLength).toDouble()).toInt()
                val phi = if (lineWidth < 0f) 0f else (waveLength * waveCountInitial - lineWidth)
                val totalLineWidth = endX - startX
                canvas.save()
                canvas.clipRect(startX, 0f, endX, canvas.height.toFloat())
                canvas.translate(startX, centerY)
                tmpPath.reset()
                tmpPath.moveTo(0f, 0f)
                val waveCount = ceil(((phi + totalLineWidth) / waveLength).toDouble()).toInt()
                for (j in 0 until waveCount) {
                    tmpPath.quadTo(
                        waveLength * j + waveLength / 4f,
                        amplitude,
                        waveLength * j + waveLength / 2f,
                        0f
                    )
                    tmpPath.quadTo(
                        waveLength * j + waveLength * 3f / 4f,
                        -amplitude,
                        waveLength * j + waveLength,
                        0f
                    )
                }
                paintOther.strokeWidth = waveWidth
                paintOther.style = AndroidPaint.Style.STROKE
                canvas.drawPath(tmpPath, paintOther)
                canvas.restore()
                paintOther.style = AndroidPaint.Style.FILL
            }

            DiagnosticIndicatorStyle.LINE -> {
                paintOther.strokeWidth = waveWidth
                canvas.drawLine(startX, centerY, endX, centerY, paintOther)
            }

            DiagnosticIndicatorStyle.DOUBLE_LINE -> {
                paintOther.strokeWidth = waveWidth / 3f
                canvas.drawLine(startX, centerY, endX, centerY, paintOther)
                canvas.drawLine(startX, centerY - waveWidth, endX, centerY - waveWidth, paintOther)
            }

            DiagnosticIndicatorStyle.NONE -> {}
        }
    }

    protected open fun drawDiagnosticIndicators(canvas: Canvas, offset: Float) {
        val diagnosticsContainer = editor.diagnostics
        val style = editor.diagnosticIndicatorStyle
        if (diagnosticsContainer != null && style != DiagnosticIndicatorStyle.NONE && style != null) {
            val text = content
            val firstVisRow = editor.firstVisibleRow
            val lastVisRow = editor.lastVisibleRow
            val firstIndex = text.getCharIndex(editor.firstVisibleLine, 0)
            val lastLine = min(text.lineCount - 1, editor.lastVisibleLine + 1)
            val lastIndex = text.getCharIndex(lastLine, 0) + text.getColumnCount(lastLine)
            diagnosticsContainer.queryInRegion(collectedDiagnostics, firstIndex, lastIndex)
            if (collectedDiagnostics.isEmpty()) {
                return
            }
            val start = CharPosition()
            val end = CharPosition()
            val indexer = cursor.indexer
            for (region in collectedDiagnostics) {
                val startIndex = max(firstIndex, region.startIndex)
                val endIndex = min(lastIndex, region.endIndex)
                indexer.getCharPosition(startIndex, start)
                indexer.getCharPosition(endIndex, end)
                val startRow = editor.layout.getRowIndexForPosition(startIndex)
                val endRow = editor.layout.getRowIndexForPosition(endIndex)
                val colorId =
                    if (region.severity in 0..3) sDiagnosticsColorMapping[region.severity.toInt()] else 0
                if (colorId == 0) {
                    break
                }
                paintOther.color = editor.colorScheme.getColor(colorId)
                val visStartRow = max(firstVisRow, startRow)
                val visEndRow = min(lastVisRow, endRow)
                for (i in visStartRow..visEndRow) {
                    val row = editor.layout.getRowAt(i)
                    val tr = createTextRow(i)
                    val startColumn = if (i == startRow) start.column else row.startColumn
                    val endColumn = if (i == endRow) end.column else row.endColumn
                    val finalOffset =
                        if (editor.isWordwrap && !row.isLeadingRow && (editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
                            offset + row.renderTranslateX + this.miniGraphWidth
                        } else {
                            offset + row.renderTranslateX
                        }
                    if (startColumn == endColumn) {
                        val startX = finalOffset + tr.getCursorOffsetForIndex(startColumn)
                        val endX = startX + paintGeneral.measureText("a")
                        drawDiagnosticIndicator(canvas, style, i, startX, endX)
                    } else {
                        val rowIndex = i
                        tr.iterateBackgroundRegions(
                            startColumn,
                            endColumn,
                            false,
                            false
                        ) { left, right ->
                            if (right > 0f) {
                                drawDiagnosticIndicator(
                                    canvas,
                                    style,
                                    rowIndex,
                                    finalOffset + left,
                                    finalOffset + right
                                )
                            }
                            finalOffset + right < editor.width
                        }
                    }
                }
            }
        }
        collectedDiagnostics.clear()
    }

    private fun drawWhitespaces(
        canvas: Canvas,
        tr: TextRow,
        chars: GetChars,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        isRtl: Boolean,
        horizontalOffset: Float,
        width: Float,
        min: Int,
        max: Int
    ) {
        var paintStart = max(index, min(index + count, min))
        val paintEnd = max(index, min(index + count, max))

        if (paintStart < paintEnd) {
            val spaceWidth = paintGeneral.spaceWidth
            val rowCenter = editor.rowHeightOfText / 2f + editor.getRowTopOfText(0)
            var offset = if (isRtl) horizontalOffset + width else horizontalOffset
            while (paintStart < paintEnd) {
                val ch = chars[paintStart]
                var paintCount = 0
                var paintLine = false
                if (ch == ' ' || ch == '\t') {
                    val advance = tr.measureAdvanceInRun(
                        paintStart,
                        index,
                        paintStart,
                        contextIndex,
                        contextIndex + contextCount,
                        isRtl
                    )
                    offset =
                        if (isRtl) horizontalOffset + width - advance else horizontalOffset + advance
                }
                if (ch == ' ') {
                    paintCount = 1
                } else if (ch == '\t') {
                    if ((editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_TAB_SAME_AS_SPACE) != 0) {
                        paintCount = editor.tabWidth
                    } else {
                        paintLine = true
                    }
                }
                for (i in 0 until paintCount) {
                    val charStartOffset = offset + spaceWidth * i
                    val charEndOffset = charStartOffset + spaceWidth
                    var centerOffset = (charStartOffset + charEndOffset) / 2f
                    if (isRtl) {
                        centerOffset -= spaceWidth
                    }
                    bufferedDrawPoints.drawPoint(centerOffset, rowCenter)
                }
                if (paintLine) {
                    val charWidth = editor.tabWidth * spaceWidth
                    val delta = charWidth * 0.05f
                    val rtlDelta = if (isRtl) -charWidth else 0f
                    canvas.drawLine(
                        offset + delta + rtlDelta,
                        rowCenter,
                        offset + charWidth + rtlDelta - delta,
                        rowCenter,
                        paintOther
                    )
                }

                if (ch == ' ' || ch == '\t') {
                    val charWidth = if (ch == ' ') spaceWidth else spaceWidth * editor.tabWidth
                    offset += if (isRtl) -charWidth else charWidth
                }
                paintStart++
            }
        }
    }

    val miniGraphWidth: Float
        get() {
            val height = editor.rowHeightOfText * editor.props.miniMarkerSizeFactor
            val graph = editor.context.getDrawable(R.drawable.line_break) ?: return 0f
            val w = graph.intrinsicWidth
            val h = graph.intrinsicHeight
            if (w <= 0 || h <= 0 || height <= 0f) {
                return 0f
            }
            return height * (w.toFloat() / h)
        }

    protected open fun drawMiniGraph(canvas: Canvas, offset: Float, row: Int, graph: Drawable?) {
        val graphBottom =
            if (row == -1) editor.getRowBottomOfText(0).toFloat() else (editor.getRowBottomOfText(
                row
            ) - editor.offsetY).toFloat()
        val height = editor.rowHeightOfText * editor.props.miniMarkerSizeFactor
        if (height <= 0f || graph == null) {
            return
        }
        val w = graph.intrinsicWidth
        val h = graph.intrinsicHeight
        if (w <= 0 || h <= 0) {
            return
        }
        val width = height * (w.toFloat() / h)
        graph.colorFilter = BlendModeColorFilter(
            editor.colorScheme.getColor(EditorColorScheme.NON_PRINTABLE_CHAR),
            BlendMode.SRC_ATOP
        )
        graph.setBounds(
            offset.toInt(),
            (graphBottom - height).toInt(),
            (offset + width).toInt(),
            graphBottom.toInt()
        )
        graph.draw(canvas)
    }

    protected open fun getRowTopForBackground(row: Int): Int {
        return if (!editor.props.textBackgroundWrapTextOnly) {
            editor.getRowTop(row)
        } else {
            editor.getRowTopOfText(row)
        }
    }

    protected open fun getRowBottomForBackground(row: Int): Int {
        return if (!editor.props.textBackgroundWrapTextOnly) {
            editor.getRowBottom(row)
        } else {
            editor.getRowBottomOfText(row)
        }
    }

    protected open fun drawRowRegionBackground(
        canvas: Canvas,
        row: Int,
        tr: TextRow?,
        highlightStart: Int,
        highlightEnd: Int,
        rowStart: Int,
        rowEnd: Int,
        color: Int,
        borderColor: Int
    ) {
        val actualStart = max(highlightStart, rowStart)
        val actualEnd = min(highlightEnd, rowEnd)
        if (actualStart < actualEnd) {
            tmpRect.top = (getRowTopForBackground(row) - editor.offsetY).toFloat()
            tmpRect.bottom = (getRowBottomForBackground(row) - editor.offsetY).toFloat()
            var offset = editor.measureTextRegionOffset() - editor.offsetX
            if (editor.isWordwrap && !editor.layout.getRowAt(row).isLeadingRow && (editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
                offset += this.miniGraphWidth
            }
            val finalOffset = offset
            val activeTr = tr ?: createTextRow(row)
            val width = editor.width
            activeTr.iterateBackgroundRegions(actualStart, actualEnd, false, false) { left, right ->
                tmpRect.left = finalOffset + left
                tmpRect.right = finalOffset + right
                if (tmpRect.right < 0 || tmpRect.left > width) {
                    return@iterateBackgroundRegions false
                }
                drawRowBackgroundRectWithBorder(canvas, tmpRect, color, borderColor)
                true
            }
        }
    }

    protected open fun drawRowBackgroundRectWithBorder(
        canvas: Canvas,
        rect: RectF,
        backgroundColor: Int,
        borderColor: Int
    ) {
        paintGeneral.color = backgroundColor
        drawRowBackgroundRect(canvas, rect)
        if (borderColor == 0) {
            return
        }
        paintGeneral.color = borderColor
        paintGeneral.style = AndroidPaint.Style.STROKE
        paintGeneral.strokeWidth = editor.textBorderWidth
        drawRowBackgroundRect(canvas, rect)
        paintGeneral.style = AndroidPaint.Style.FILL
    }

    protected open fun drawRowBackgroundRect(canvas: Canvas, rect: RectF) {
        drawRowBackgroundRect(canvas, rect, paintGeneral)
    }

    protected open fun drawRowBackgroundRect(canvas: Canvas, rect: RectF, p: Paint) {
        if (editor.props.enableRoundTextBackground) {
            canvas.drawRoundRect(
                rect,
                editor.rowHeight * editor.props.roundTextBackgroundFactor,
                editor.rowHeight * editor.props.roundTextBackgroundFactor,
                p
            )
        } else {
            canvas.drawRect(rect, p)
        }
    }

    private fun isInside(index: Int, start: Int, end: Int, isLastRow: Boolean): Boolean {
        return !(index == end && !isLastRow) && index in start..end
    }

    val timestamp: Long
        get() = displayTimestamp

    val lineNumberMetrics: AndroidPaint.FontMetricsInt
        get() = metricsLineNumber

    val textMetrics: AndroidPaint.FontMetricsInt
        get() = metricsText

    protected open fun drawEdgeEffect(canvas: Canvas) {
        var postDraw = false
        val verticalEdgeEffect = editor.verticalEdgeEffect
        val horizontalEdgeEffect = editor.horizontalEdgeEffect
        if (!verticalEdgeEffect.isFinished) {
            val bottom = editor.eventHandler.glowTopOrBottom
            if (bottom) {
                canvas.save()
                canvas.translate(-editor.measuredWidth.toFloat(), editor.measuredHeight.toFloat())
                canvas.rotate(180f, editor.measuredWidth.toFloat(), 0f)
            }
            postDraw = verticalEdgeEffect.draw(canvas)
            if (bottom) {
                canvas.restore()
            }
        }
        if (editor.isWordwrap) {
            horizontalEdgeEffect.finish()
        }
        if (!horizontalEdgeEffect.isFinished) {
            canvas.save()
            val right = editor.eventHandler.glowLeftOrRight
            if (right) {
                canvas.rotate(90f)
                canvas.translate(0f, -editor.measuredWidth.toFloat())
            } else {
                canvas.translate(0f, editor.measuredHeight.toFloat())
                canvas.rotate(-90f)
            }
            postDraw = horizontalEdgeEffect.draw(canvas) || postDraw
            canvas.restore()
        }
        val scroller = editor.scroller
        if (scroller.isOverScrolled) {
            if (verticalEdgeEffect.isFinished && (scroller.currY < 0 || scroller.currY > editor.scrollMaxY)) {
                editor.eventHandler.glowTopOrBottom = scroller.currY >= editor.scrollMaxY
                verticalEdgeEffect.onAbsorb(scroller.currVelocity.toInt())
                postDraw = true
            }
            if (horizontalEdgeEffect.isFinished && (scroller.currX < 0 || scroller.currX > editor.scrollMaxX)) {
                editor.eventHandler.glowLeftOrRight = scroller.currX >= editor.scrollMaxX
                horizontalEdgeEffect.onAbsorb(scroller.currVelocity.toInt())
                postDraw = true
            }
        }
        if (postDraw) {
            editor.postInvalidate()
        }
    }

    protected open fun drawBlockLines(canvas: Canvas, offsetX: Float) {
        val styles = editor.styles
        val blocks = styles?.blocks
        val indentMode = styles?.indentCountMode == true
        if (blocks == null || blocks.isEmpty()) {
            return
        }
        val first = editor.firstVisibleRow
        val last = editor.lastVisibleRow
        var mark = false
        var invalidCount = 0
        val maxCount = styles.suppressSwitch
        var mm = editor.binarySearchEndBlock(first, blocks)
        if (mm == -1) {
            mm = 0
        }
        val cursorIdx = editor.currentCursorBlock
        for (curr in mm until blocks.size) {
            val block = blocks[curr]
            if (CodeEditor.hasVisibleRegion(block.startLine, block.endLine, first, last)) {
                try {
                    var lineContent = getLine(block.endLine)
                    val offsetEnd =
                        if (indentMode) paintGeneral.spaceWidth * block.endColumn else createTextRow(
                            block.endLine
                        ).getCursorOffsetForIndex(min(block.endColumn, lineContent.length))
                    lineContent = getLine(block.startLine)
                    val offsetStart =
                        if (indentMode) paintGeneral.spaceWidth * block.startColumn else createTextRow(
                            block.startLine
                        ).getCursorOffsetForIndex(min(block.startColumn, lineContent.length))
                    val offset = min(offsetEnd, offsetStart)
                    val centerX = offset + offsetX
                    tmpRect.top =
                        max(0f, (editor.getRowBottom(block.startLine) - editor.offsetY).toFloat())
                    tmpRect.bottom = min(
                        editor.height.toFloat(),
                        ((if (block.toBottomOfEndLine) editor.getRowBottom(block.endLine) else editor.getRowTop(
                            block.endLine
                        )) - editor.offsetY).toFloat()
                    )
                    tmpRect.left = centerX - editor.dpUnit * editor.blockLineWidth / 2f
                    tmpRect.right = centerX + editor.dpUnit * editor.blockLineWidth / 2f
                    drawColor(
                        canvas,
                        editor.colorScheme.getColor(if (curr == cursorIdx) EditorColorScheme.BLOCK_LINE_CURRENT else EditorColorScheme.BLOCK_LINE),
                        tmpRect
                    )
                } catch (e: IndexOutOfBoundsException) {
                    // Ignored
                }
                mark = true
            } else if (mark) {
                if (invalidCount >= maxCount) break
                invalidCount++
            }
        }
    }

    protected open fun drawSideBlockLine(canvas: Canvas) {
        if (!editor.props.drawSideBlockLine) {
            return
        }
        val blocks = editor.styles?.blocks
        if (blocks == null || blocks.isEmpty()) {
            return
        }
        val current = editor.currentCursorBlock
        if (current in 0 until blocks.size) {
            val block = blocks[current]
            val layout = editor.layout
            try {
                val top = layout.getCharLayoutOffset(
                    block.startLine,
                    block.startColumn
                )[0] - editor.rowHeight - editor.offsetY
                val bottom =
                    layout.getCharLayoutOffset(block.endLine, block.endColumn)[0] - editor.offsetY
                val left = editor.measureLineNumber()
                val right = left + editor.dividerMarginLeft
                val center = (left + right) / 2f - editor.offsetX
                paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.SIDE_BLOCK_LINE)
                paintGeneral.strokeWidth = editor.dpUnit * editor.blockLineWidth
                canvas.drawLine(center, top, center, bottom, paintGeneral)
            } catch (e: IndexOutOfBoundsException) {
                // Ignored
            }
        }
    }

    protected open fun drawScrollBarsAndMinimap(canvas: Canvas) {
        val minimapWidth = minimapRenderer.onDrawToCanvas(canvas, editor.width, displayTimestamp)
        verticalScrollBarRect.setEmpty()
        horizontalScrollBarRect.setEmpty()
        val shouldShowScrollbars =
            editor.eventHandler.shouldDrawScrollBarForTouch() || (editor.isInMouseMode && editor.props.mouseModeAlwaysShowScrollbars)
        var percentage = editor.eventHandler.getScrollBarFadeOutPercentageForTouch()
        if (shouldShowScrollbars && editor.isInMouseMode && editor.props.mouseModeAlwaysShowScrollbars) {
            percentage = 0f
        }
        val size = editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
        if (shouldShowScrollbars && editor.isHorizontalScrollBarEnabled && !editor.isWordwrap && editor.scrollMaxX > editor.width * 3 / 4) {
            canvas.save()
            canvas.translate(0f, size * percentage)
            drawScrollBarTrackHorizontal(canvas)
            drawScrollBarHorizontal(canvas)
            canvas.restore()
        }
        val shouldShowVerticalScrollbar = shouldShowScrollbars && !editor.props.showMinimap
        if (shouldShowVerticalScrollbar && editor.isVerticalScrollBarEnabled && editor.scrollMaxY > editor.height / 2) {
            canvas.save()
            canvas.translate(size * percentage, 0f)
            drawScrollBarTrackVertical(canvas)
            drawScrollBarVertical(canvas)
            canvas.restore()
        }

        if (minimapWidth != 0) {
            getVerticalScrollBarRect(tmpRect, minimapWidth.toFloat())
            verticalScrollBarRect.set(tmpRect)
            if (editor.eventHandler.holdVerticalScrollBar()) {
                drawLineInfoPanel(
                    canvas,
                    tmpRect.top,
                    tmpRect.height(),
                    minimapWidth + 5f * editor.dpUnit
                )
            }
        }
    }

    protected open fun drawScrollBarTrackVertical(canvas: Canvas) {
        if (editor.eventHandler.holdVerticalScrollBar()) {
            tmpRect.right = editor.width.toFloat()
            tmpRect.left = editor.width - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
            tmpRect.top = 0f
            tmpRect.bottom = editor.height.toFloat()
            val track = verticalScrollbarTrackDrawable
            if (track != null) {
                track.setBounds(
                    tmpRect.left.toInt(),
                    tmpRect.top.toInt(),
                    tmpRect.right.toInt(),
                    tmpRect.bottom.toInt()
                )
                track.draw(canvas)
            } else {
                drawColor(
                    canvas,
                    editor.colorScheme.getColor(EditorColorScheme.SCROLL_BAR_TRACK),
                    tmpRect
                )
            }
        }
    }

    private fun getVerticalScrollBarRect(rect: RectF, width: Float) {
        val height = editor.height
        val all = editor.scrollMaxY + height
        val length = max(
            height.toFloat() / all * height,
            editor.dpUnit * RenderingConstants.SCROLLBAR_LENGTH_MIN_DIP
        )
        val topY = editor.offsetY * 1f / editor.scrollMaxY * (height - length)
        rect.right = editor.width.toFloat()
        rect.left = editor.width - width
        rect.top = topY
        rect.bottom = topY + length
    }

    protected open fun drawScrollBarVertical(canvas: Canvas) {
        getVerticalScrollBarRect(tmpRect, editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP)
        verticalScrollBarRect.set(tmpRect)
        if (editor.eventHandler.holdVerticalScrollBar()) {
            drawLineInfoPanel(canvas, tmpRect.top, tmpRect.height(), 30f * editor.dpUnit)
        }
        val thumb = verticalScrollbarThumbDrawable
        if (thumb != null) {
            thumb.state =
                if (editor.eventHandler.holdVerticalScrollBar()) PRESSED_DRAWABLE_STATE else DEFAULT_DRAWABLE_STATE
            thumb.setBounds(
                verticalScrollBarRect.left.toInt(),
                verticalScrollBarRect.top.toInt(),
                verticalScrollBarRect.right.toInt(),
                verticalScrollBarRect.bottom.toInt()
            )
            thumb.draw(canvas)
        } else {
            drawColor(
                canvas,
                editor.colorScheme.getColor(if (editor.eventHandler.holdVerticalScrollBar()) EditorColorScheme.SCROLL_BAR_THUMB_PRESSED else EditorColorScheme.SCROLL_BAR_THUMB),
                verticalScrollBarRect
            )
        }
    }

    protected open fun drawLineInfoPanel(
        canvas: Canvas,
        topY: Float,
        length: Float,
        rightMargin: Float
    ) {
        if (!editor.isDisplayLnPanel) {
            return
        }
        val mode = editor.lnPanelPositionMode
        val position = editor.lnPanelPosition
        val text = editor.lineNumberTipTextProvider.getCurrentText(editor)
        val backupSize = paintGeneral.textSize
        paintGeneral.textSkewX = 0f
        paintGeneral.isFakeBoldText = false
        paintGeneral.textSize = editor.lineInfoTextSize
        val backupMetrics = metricsText
        metricsText = paintGeneral.fontMetricsInt
        val expand = editor.dpUnit * 8f
        val textWidth = paintGeneral.measureText(text)
        var baseline: Float
        var textOffset = 0f
        if (mode == LineInfoPanelPositionMode.FIXED) {
            tmpRect.top = editor.height / 2f - editor.rowHeight / 2f - expand
            tmpRect.bottom = editor.height / 2f + editor.rowHeight / 2f + expand
            tmpRect.left = editor.width / 2f - textWidth / 2f - expand
            tmpRect.right = editor.width / 2f + textWidth / 2f + expand
            baseline = editor.height / 2f + 2f * expand
            val offset = 10f * editor.dpUnit
            if (position != LineInfoPanelPosition.CENTER) {
                if ((position or LineInfoPanelPosition.TOP) == position) {
                    tmpRect.top = offset
                    tmpRect.bottom = offset + editor.rowHeight + 2f * expand
                    baseline = offset + editor.getRowBaseline(0) + expand
                }
                if ((position or LineInfoPanelPosition.BOTTOM) == position) {
                    tmpRect.top = editor.height - offset - 2f * expand - editor.rowHeight
                    tmpRect.bottom = editor.height - offset
                    baseline =
                        editor.height - editor.rowHeight + editor.getRowBaseline(0) - offset - expand
                }
                if ((position or LineInfoPanelPosition.LEFT) == position) {
                    tmpRect.left = offset
                    tmpRect.right = offset + 2f * expand + textWidth
                }
                if ((position or LineInfoPanelPosition.RIGHT) == position) {
                    tmpRect.right = editor.width - offset
                    tmpRect.left = editor.width - offset - expand * 2f - textWidth
                }
            }
            drawColorRound(
                canvas,
                editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_PANEL),
                tmpRect
            )
        } else {
            var radii: FloatArray? = null
            tmpRect.right = editor.width - rightMargin
            tmpRect.left = tmpRect.right - expand * 2f - textWidth
            if (position == LineInfoPanelPosition.TOP) {
                tmpRect.top = topY
                tmpRect.bottom = topY + editor.rowHeight + 2f * expand
                baseline = topY + editor.getRowBaseline(0) + expand
                radii = FloatArray(8) { idx ->
                    if (idx != 5) tmpRect.height() * RenderingConstants.ROUND_BUBBLE_FACTOR else 0f
                }
            } else if (position == LineInfoPanelPosition.BOTTOM) {
                tmpRect.top = topY + length - editor.rowHeight - 2f * expand
                tmpRect.bottom = topY + length
                baseline = topY + length - editor.getRowBaseline(0) / 2f
                radii = FloatArray(8) { idx ->
                    if (idx != 3) tmpRect.height() * RenderingConstants.ROUND_BUBBLE_FACTOR else 0f
                }
            } else {
                val centerY = topY + length / 2f
                tmpRect.top = centerY - editor.rowHeight / 2f - expand
                tmpRect.bottom = centerY + editor.rowHeight / 2f + expand
                baseline = centerY - editor.rowHeight / 2f + editor.getRowBaseline(0).toFloat()
            }
            if (radii != null) {
                tmpPath.reset()
                tmpPath.addRoundRect(tmpRect, radii, Path.Direction.CW)
            } else {
                tmpRect.offset(-expand, 0f)
                tmpRect.right += expand
                textOffset = -expand / 2f
                BubbleHelper.buildBubblePath(tmpPath, tmpRect)
            }
            paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_PANEL)
            canvas.drawPath(tmpPath, paintGeneral)
        }
        val centerX = (tmpRect.left + tmpRect.right) / 2f + textOffset
        paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT)
        paintGeneral.textAlign = AndroidPaint.Align.CENTER
        canvas.drawText(text, centerX, baseline, paintGeneral)
        paintGeneral.textAlign = AndroidPaint.Align.LEFT
        paintGeneral.textSize = backupSize
        metricsText = backupMetrics
    }

    protected open fun drawScrollBarTrackHorizontal(canvas: Canvas) {
        if (editor.eventHandler.holdHorizontalScrollBar()) {
            tmpRect.set(
                0f,
                editor.height - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP,
                editor.width.toFloat(),
                editor.height.toFloat()
            )
            val track = horizontalScrollbarTrackDrawable
            if (track != null) {
                track.setBounds(
                    tmpRect.left.toInt(),
                    tmpRect.top.toInt(),
                    tmpRect.right.toInt(),
                    tmpRect.bottom.toInt()
                )
                track.draw(canvas)
            } else {
                drawColor(
                    canvas,
                    editor.colorScheme.getColor(EditorColorScheme.SCROLL_BAR_TRACK),
                    tmpRect
                )
            }
        }
    }

    protected open fun patchSnippetRegions(canvas: Canvas, textOffset: Float) {
        val controller = editor.snippetController
        if (controller.isInSnippet()) {
            val editing = controller.getEditingTabStop()
            if (editing != null) {
                patchTextRegionWithColor(
                    canvas,
                    textOffset,
                    editing.startIndex,
                    editing.endIndex,
                    0,
                    editor.colorScheme.getColor(EditorColorScheme.SNIPPET_BACKGROUND_EDITING),
                    0
                )
            }
            for (snippetItem in controller.getEditingRelatedTabStops()) {
                patchTextRegionWithColor(
                    canvas,
                    textOffset,
                    snippetItem.startIndex,
                    snippetItem.endIndex,
                    0,
                    editor.colorScheme.getColor(EditorColorScheme.SNIPPET_BACKGROUND_RELATED),
                    0
                )
            }
            for (snippetItem in controller.getInactiveTabStops()) {
                patchTextRegionWithColor(
                    canvas,
                    textOffset,
                    snippetItem.startIndex,
                    snippetItem.endIndex,
                    0,
                    editor.colorScheme.getColor(EditorColorScheme.SNIPPET_BACKGROUND_INACTIVE),
                    0
                )
            }
        }
    }

    protected open fun patchHighlightedDelimiters(canvas: Canvas, textOffset: Float) {
        if (editor.inputConnection.composingText.isComposing || !editor.props.highlightMatchingDelimiters || cursor.isSelected) {
            return
        }
        val paired = editor.styleDelegate.foundBracketPair
        if (paired != null) {
            val color =
                editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND)
            var backgroundColor =
                editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND)
            val underlineColor =
                editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE)
            val borderColor =
                editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BORDER)
            val borderWidth = editor.textBorderWidth
            if (isInvalidTextBounds(paired.leftIndex, paired.leftLength) || isInvalidTextBounds(
                    paired.rightIndex,
                    paired.rightLength
                )
            ) {
                return
            }

            val continuous = paired.leftIndex + paired.leftLength == paired.rightIndex
            if (color != 0 || underlineColor != 0) {
                if (continuous) {
                    patchTextRegionWithColor(
                        canvas,
                        textOffset,
                        paired.leftIndex,
                        paired.rightIndex + paired.rightLength,
                        color,
                        backgroundColor,
                        underlineColor
                    )
                } else {
                    patchTextRegionWithColor(
                        canvas,
                        textOffset,
                        paired.leftIndex,
                        paired.leftIndex + paired.leftLength,
                        color,
                        backgroundColor,
                        underlineColor
                    )
                    patchTextRegionWithColor(
                        canvas,
                        textOffset,
                        paired.rightIndex,
                        paired.rightIndex + paired.rightLength,
                        color,
                        backgroundColor,
                        underlineColor
                    )
                }
                backgroundColor = 0
            }
            if (backgroundColor != 0 || (borderColor != 0 && borderWidth > 0f)) {
                if (continuous) {
                    patchTextBackgroundRegions(
                        canvas,
                        textOffset,
                        paired.leftIndex,
                        paired.rightIndex + paired.rightLength,
                        backgroundColor,
                        borderWidth,
                        borderColor
                    )
                } else {
                    patchTextBackgroundRegions(
                        canvas,
                        textOffset,
                        paired.leftIndex,
                        paired.leftIndex + paired.leftLength,
                        backgroundColor,
                        borderWidth,
                        borderColor
                    )
                    patchTextBackgroundRegions(
                        canvas,
                        textOffset,
                        paired.rightIndex,
                        paired.rightIndex + paired.rightLength,
                        backgroundColor,
                        borderWidth,
                        borderColor
                    )
                }
            }
        }
    }

    protected open fun isInvalidTextBounds(index: Int, length: Int): Boolean {
        return index < 0 || length < 0 || index + length > content.length
    }

    protected open fun patchTextRegionWithColor(
        canvas: Canvas,
        textOffset: Float,
        start: Int,
        end: Int,
        color: Int,
        backgroundColor: Int,
        underlineColor: Int
    ) {
        paintGeneral.color = color
        paintOther.strokeWidth =
            editor.rowHeightOfText * RenderingConstants.MATCHING_DELIMITERS_UNDERLINE_WIDTH_FACTOR

        val useBoldStyle = editor.props.boldMatchingDelimiters
        paintGeneral.style =
            if (useBoldStyle) AndroidPaint.Style.FILL_AND_STROKE else AndroidPaint.Style.FILL
        paintGeneral.isFakeBoldText = useBoldStyle

        patchTextRegions(
            canvas,
            textOffset,
            start,
            end,
            { _, text, index, count, contextIndex, contextCount, isRtl, horizontalOffset, width, params, span ->
                if (span == null) {
                    return@patchTextRegions
                }
                if (backgroundColor != 0) {
                    tmpRect.top = getRowTopForBackground(0).toFloat()
                    tmpRect.bottom = getRowBottomForBackground(0).toFloat()
                    tmpRect.left = horizontalOffset
                    tmpRect.right = horizontalOffset + width
                    paintOther.color = backgroundColor
                    drawRowBackgroundRect(canvas, tmpRect, paintOther)
                }
                val style = span.style
                if (color != 0) {
                    paintGeneral.textSkewX =
                        if (TextStyle.isItalics(style)) RenderingConstants.TEXT_SKEW_X else 0f
                    paintGeneral.isStrikeThruText = TextStyle.isStrikeThrough(style)
                    GraphicsCompat.drawTextRun(
                        canvas,
                        text,
                        index,
                        count,
                        contextIndex,
                        contextCount,
                        horizontalOffset,
                        params.textBaseline.toFloat(),
                        isRtl,
                        paintGeneral
                    )
                }
                if (underlineColor != 0) {
                    paintOther.color = underlineColor
                    val bottom = params.textBottom - params.textHeight * 0.05f
                    canvas.drawLine(
                        horizontalOffset,
                        bottom,
                        horizontalOffset + width,
                        bottom,
                        paintOther
                    )
                }
            },
            null
        )
        paintGeneral.style = AndroidPaint.Style.FILL
        paintGeneral.isFakeBoldText = false
        paintGeneral.textSkewX = 0f
        paintGeneral.isStrikeThruText = false
    }

    protected open fun patchTextBackgroundRegions(
        canvas: Canvas,
        textOffset: Float,
        start: Int,
        end: Int,
        backgroundColor: Int,
        borderWidth: Float,
        borderColor: Int
    ) {
        if (backgroundColor == 0 && (borderWidth <= 0f || borderColor == 0)) {
            return
        }
        patchTextRegions(canvas, textOffset, start, end, null) { left, right ->
            if (textOffset + left < 0) {
                return@patchTextRegions true
            }
            tmpRect.top = getRowTopForBackground(0).toFloat()
            tmpRect.bottom = getRowBottomForBackground(0).toFloat()
            tmpRect.left = left
            tmpRect.right = right
            if (backgroundColor != 0) {
                paintOther.color = backgroundColor
                drawRowBackgroundRect(canvas, tmpRect, paintOther)
            }
            if (borderWidth > 0f && borderColor != 0) {
                paintOther.style = AndroidPaint.Style.STROKE
                paintOther.color = borderColor
                paintOther.strokeWidth = borderWidth
                drawRowBackgroundRect(canvas, tmpRect, paintOther)
                paintOther.style = AndroidPaint.Style.FILL
            }
            textOffset + right > editor.width
        }
    }

    protected open fun patchTextRegions(
        canvas: Canvas,
        textOffset: Float,
        start: Int,
        end: Int,
        patch: TextRow.DrawTextConsumer?,
        bgPatch: TextRow.BackgroundRegionConsumer?
    ) {
        if (patch == null && bgPatch == null) {
            return
        }
        val firstVisRow = editor.firstVisibleRow
        val lastVisRow = editor.lastVisibleRow

        val layout = editor.layout
        val startRow = layout.getRowIndexForPosition(start)
        val endRow = layout.getRowIndexForPosition(end)
        val posStart = cursor.indexer.getCharPosition(start)
        val posEnd = cursor.indexer.getCharPosition(end)
        val itr = layout.obtainRowIterator(startRow, preloadedLines)
        for (i in startRow..endRow) {
            if (!itr.hasNext()) break
            val row = itr.next()
            if (i !in firstVisRow..lastVisRow) {
                continue
            }
            val startOnRow = if (i == startRow) posStart.column else row.startColumn
            val endOnRow = if (i == endRow) posEnd.column else row.endColumn
            val tr = createTextRow(i)
            var horizontalOffset = textOffset
            if ((editor.nonPrintablePaintingFlags and CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0 && !row.isLeadingRow) {
                horizontalOffset += this.miniGraphWidth
            }
            val minHorizontalOffset = max(0f, -horizontalOffset)
            val maxHorizontalOffset = minHorizontalOffset + editor.width
            canvas.save()
            canvas.translate(
                horizontalOffset + row.renderTranslateX,
                (editor.getRowTop(i) - editor.offsetY).toFloat()
            )
            if (bgPatch != null) {
                tr.iterateBackgroundRegions(startOnRow, endOnRow, false, false, bgPatch)
            }
            if (patch != null) {
                tr.iterateDrawTextRegions(
                    startOnRow,
                    endOnRow,
                    canvas,
                    minHorizontalOffset,
                    maxHorizontalOffset,
                    true,
                    patch
                )
            }
            canvas.restore()
        }
    }

    protected open fun drawSelectionOnAnimation(canvas: Canvas) {
        if (!editor.isEditable) {
            return
        }
        tmpRect.bottom = (editor.cursorAnimator.animatedY() - editor.offsetY).toFloat()
        tmpRect.top =
            tmpRect.bottom - (if (editor.props.textBackgroundWrapTextOnly) editor.rowHeightOfText else editor.rowHeight).toFloat()
        val centerX = editor.cursorAnimator.animatedX() - editor.offsetX
        tmpRect.left = centerX - editor.insertSelectionWidth / 2f
        tmpRect.right = centerX + editor.insertSelectionWidth / 2f
        drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.SELECTION_INSERT), tmpRect)
        val bidiAttrs = getBidiIndicatorAttrs(cursor.leftLine, cursor.leftColumn)
        if (IntPair.getFirst(bidiAttrs) == 1) {
            drawBidiSelectionIndicator(
                canvas,
                centerX,
                tmpRect.top,
                tmpRect.height(),
                IntPair.getSecond(bidiAttrs) == 1
            )
        }
        if (editor.eventHandler.shouldDrawInsertHandle() && !editor.isInMouseMode) {
            editor.handleStyle.draw(
                canvas,
                SelectionHandleStyle.HANDLE_TYPE_INSERT,
                centerX,
                tmpRect.bottom,
                editor.rowHeight,
                editor.colorScheme.getColor(EditorColorScheme.SELECTION_HANDLE),
                editor.insertHandleDescriptor
            )
        }
    }

    protected open fun drawScrollBarHorizontal(canvas: Canvas) {
        val page = editor.width
        val all = editor.scrollMaxX.toFloat()
        var length = page / (all + editor.width) * editor.width
        val minLength = RenderingConstants.SCROLLBAR_LENGTH_MIN_DIP * editor.dpUnit
        if (length <= minLength) length = minLength
        val leftX = editor.offsetX / all * (editor.width - length)
        tmpRect.top = editor.height - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
        tmpRect.bottom = editor.height.toFloat()
        tmpRect.right = leftX + length
        tmpRect.left = leftX
        horizontalScrollBarRect.set(tmpRect)
        val thumb = horizontalScrollbarThumbDrawable
        if (thumb != null) {
            thumb.state =
                if (editor.eventHandler.holdHorizontalScrollBar()) PRESSED_DRAWABLE_STATE else DEFAULT_DRAWABLE_STATE
            thumb.setBounds(
                horizontalScrollBarRect.left.toInt(),
                horizontalScrollBarRect.top.toInt(),
                horizontalScrollBarRect.right.toInt(),
                horizontalScrollBarRect.bottom.toInt()
            )
            thumb.draw(canvas)
        } else {
            drawColor(
                canvas,
                editor.colorScheme.getColor(if (editor.eventHandler.holdHorizontalScrollBar()) EditorColorScheme.SCROLL_BAR_THUMB_PRESSED else EditorColorScheme.SCROLL_BAR_THUMB),
                horizontalScrollBarRect
            )
        }
    }

    // BEGIN Measure-------------------------------------

    /**
     * Build measure cache for the given lines, if the timestamp indicates that it is outdated.
     */
    open fun buildMeasureCacheForLines(
        startLine: Int,
        endLine: Int,
        timestamp: Long,
        useCachedContent: Boolean
    ) {
        val text = content
        val context = editor.renderContext
        var currentStart = startLine
        while (currentStart <= endLine && currentStart < text.lineCount) {
            val line = if (useCachedContent) getLine(currentStart) else getLineDirect(currentStart)
            val cache = context.cache.getOrCreateMeasureCache(currentStart)
            if (cache.updateTimestamp < timestamp) {
                var forced = false
                if (cache.widths == null || cache.widths!!.size < line.length) {
                    cache.widths = TextAdvancesCache(max(line.length + 8, 90))
                    forced = true
                }
                val spans = editor.getSpansForLine(currentStart)
                val hash = Objects.hash(
                    spans,
                    line.length,
                    editor.tabWidth,
                    paintGeneral.flags,
                    paintGeneral.textSize,
                    paintGeneral.textScaleX,
                    paintGeneral.letterSpacing,
                    paintGeneral.fontFeatureSettings,
                    paintGeneral.typeface.hashCode()
                )
                if (context.cache.getStyleHash(currentStart) != hash || forced) {
                    context.cache.setStyleHash(currentStart, hash)
                    val beginRowIndex =
                        editor.layout.getRowIndexForPosition(text.getCharIndex(currentStart, 0))
                    val itr = editor.layout.obtainRowIterator(beginRowIndex)
                    val tr = TextRow()
                    val lineText = text.getLine(currentStart)
                    val directions = text.getLineDirections(currentStart)
                    val requiredSize = lineText.length + 10
                    var widths = cache.widths
                    if (widths == null || widths.size < requiredSize) {
                        widths = TextAdvancesCache(requiredSize)
                        cache.widths = widths
                    }
                    while (itr.hasNext()) {
                        val row = itr.next()
                        if (row.lineIndex != currentStart) {
                            break
                        }
                        tr.set(
                            lineText,
                            row.startColumn,
                            row.endColumn,
                            spans,
                            row.inlayHints ?: emptyList(),
                            directions,
                            paintGeneral,
                            null,
                            createTextRowParams()
                        )
                        tr.buildMeasureCacheStep(widths)
                    }
                    tr.setRange(0, lineText.length)
                    tr.buildMeasureCacheTailor(widths)
                    cache.updateTimestamp = timestamp
                } else {
                    cache.updateTimestamp = timestamp
                }
            }
            currentStart++
        }
    }

    open fun buildMeasureCacheForLines(startLine: Int, endLine: Int) {
        buildMeasureCacheForLines(startLine, endLine, displayTimestamp, false)
    }

    open fun getRowWidth(row: Int): Float {
        return createTextRow(row).computeRowWidth()
    }

    // END Measure---------------------------------------

    protected inner class DrawCursorTask(
        @JvmField var x: Float,
        @JvmField var y: Float,
        @JvmField var handleType: Int,
        @JvmField var descriptor: SelectionHandleStyle.HandleDescriptor?
    ) {
        @JvmField
        var isBidiIndicatorRequired: Boolean = false

        @JvmField
        var isRightToLeft: Boolean = false

        fun setBidiIndicatorRequired(bidiIndicatorRequired: Boolean) {
            isBidiIndicatorRequired = bidiIndicatorRequired
        }

        fun setRightToLeft(rightToLeft: Boolean) {
            isRightToLeft = rightToLeft
        }

        private fun getActualHandleType(): Int {
            if (isRightToLeft && handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT) {
                return SelectionHandleStyle.HANDLE_TYPE_RIGHT
            }
            if (isRightToLeft && handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT) {
                return SelectionHandleStyle.HANDLE_TYPE_LEFT
            }
            return handleType
        }

        private fun drawSelForLeftRight(): Boolean {
            return (handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT || handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT) &&
                    editor.props.showSelectionWhenSelected && !editor.isInMouseMode
        }

        private fun drawSelForInsert(): Boolean {
            return !(handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT || handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT) &&
                    (editor.cursorBlink.visibility || editor.eventHandler.holdInsertHandle() || editor.isInLongSelect)
        }

        private fun isSelForLongSelect(): Boolean {
            return editor.isInLongSelect && !(handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT ||
                    handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT)
        }

        fun execute(canvas: Canvas) {
            if (handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED) {
                if (editor.inputConnection.imeConsumingInput || !editor.isFocused) {
                    return
                }
            }
            if (handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT && !editor.isEditable) {
                return
            }
            val descriptor = this.descriptor ?: TMP_DESC
            if (!descriptor.position.isEmpty) {
                if (!editor.isStickyTextSelection) {
                    if (editor.eventHandler.getTouchedHandleType() == getActualHandleType() &&
                        handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED &&
                        editor.eventHandler.isHandleMoving()
                    ) {
                        x =
                            editor.eventHandler.motionX + (if (descriptor.alignment != SelectionHandleStyle.ALIGN_CENTER) descriptor.position.width() else 0f) * (if (descriptor.alignment == SelectionHandleStyle.ALIGN_LEFT) 1 else -1)
                        y = editor.eventHandler.motionY - descriptor.position.height() * 2f / 3f
                    }
                }
            }

            if (drawSelForLeftRight() || drawSelForInsert() || handleType == SelectionHandleStyle.HANDLE_TYPE_UNDEFINED) {
                val startY =
                    y - (if (editor.props.textBackgroundWrapTextOnly) editor.rowHeightOfText else editor.rowHeight).toFloat()
                val stopY = y
                paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.SELECTION_INSERT)
                paintGeneral.strokeWidth = editor.insertSelectionWidth
                paintGeneral.style = AndroidPaint.Style.STROKE
                if (isSelForLongSelect()) {
                    paintGeneral.pathEffect = DashPathEffect(
                        floatArrayOf((stopY - startY) / 8f, (stopY - startY) / 8f),
                        (stopY - startY) / 16f
                    )
                    paintGeneral.strokeWidth = editor.insertSelectionWidth * 1.5f
                }
                canvas.drawLine(x, startY, x, stopY, paintGeneral)
                paintGeneral.style = AndroidPaint.Style.FILL
                paintGeneral.pathEffect = null
                if (drawSelForInsert() && isBidiIndicatorRequired) {
                    val height = stopY - startY
                    drawBidiSelectionIndicator(canvas, x, startY, height, isRightToLeft)
                }
            }
            var hType = this.handleType
            if (hType == SelectionHandleStyle.HANDLE_TYPE_INSERT && (editor.isInLongSelect || !editor.eventHandler.shouldDrawInsertHandle())) {
                hType = SelectionHandleStyle.HANDLE_TYPE_UNDEFINED
            }
            if (hType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED && !editor.isInMouseMode) {
                editor.handleStyle.draw(
                    canvas,
                    hType,
                    x,
                    y,
                    editor.rowHeight,
                    editor.colorScheme.getColor(EditorColorScheme.SELECTION_HANDLE),
                    descriptor
                )
                if (descriptor === TMP_DESC) {
                    descriptor.setEmpty()
                }
            } else {
                descriptor.setEmpty()
            }
        }
    }
}
