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

import android.content.res.Resources
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage
import io.github.rosemoe.sora.event.ClickEvent
import io.github.rosemoe.sora.event.ContextClickEvent
import io.github.rosemoe.sora.event.DoubleClickEvent
import io.github.rosemoe.sora.event.DragSelectStopEvent
import io.github.rosemoe.sora.event.EditorMotionEvent
import io.github.rosemoe.sora.event.HandleStateChangeEvent
import io.github.rosemoe.sora.event.InlayHintClickEvent
import io.github.rosemoe.sora.event.InterceptTarget
import io.github.rosemoe.sora.event.LongPressEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SideIconClickEvent
import io.github.rosemoe.sora.graphics.RectUtils
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.StylesUtils
import io.github.rosemoe.sora.lang.styling.line.LineSideIcon
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.layout.RowElementTypes
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Handles touch events of editor
 *
 * @author Rosemoe
 */
class EditorTouchEventHandler(private val editor: CodeEditor) :
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    ScaleGestureDetector.OnScaleGestureListener {

    companion object {
        const val HANDLE_LEFT = HandleStateChangeEvent.HANDLE_TYPE_LEFT
        const val HANDLE_RIGHT = HandleStateChangeEvent.HANDLE_TYPE_RIGHT
        const val HANDLE_BOTH = HandleStateChangeEvent.HANDLE_TYPE_INSERT

        private const val HIDE_DELAY = 3000L
        private const val HIDE_DELAY_HANDLE = 3500L
        private const val SCROLLBAR_FADE_ANIMATION_TIME = 200L
        private const val MAGNIFIER_TOUCH_SLOP = 4
        private const val LEFT_EDGE = 1
        private const val RIGHT_EDGE = 1 shl 1
        private const val TOP_EDGE = 1 shl 2
        private const val BOTTOM_EDGE = 1 shl 3
        private const val MAX_FACTOR = 32
        private const val INCREASE_FACTOR = 1.06f
    }

    val scroller: EditorScroller = EditorScroller(editor)
    private val insertHandle: SelectionHandle

    @JvmField
    var magnifier: Magnifier

    @JvmField
    var selHandleType: Int = -1

    @JvmField
    var selHandleMoving: Boolean = false

    @JvmField
    var motionX: Float = 0f

    @JvmField
    var motionY: Float = 0f

    @JvmField
    var glowTopOrBottom: Boolean = false // true for bottom

    @JvmField
    var glowLeftOrRight: Boolean = false // true for right

    @JvmField
    var isScaling: Boolean = false

    @JvmField
    var scaleMaxSize: Float

    @JvmField
    var scaleMinSize: Float

    private var textSizeStart: Float = 0f
    private var timeLastScroll: Long = 0L
    private var timeLastSetSelection: Long = 0L
    private var holdingScrollbarVertical: Boolean = false
    private var holdingScrollbarHorizontal: Boolean = false
    private var thumbDownY: Float = 0f
    private var thumbDownX: Float = 0f
    private var leftHandle: SelectionHandle
    private var rightHandle: SelectionHandle
    private var edgeFieldSize: Float
    private var edgeFlags: Int = 0
    private val touchSlop: Int
    private var thumbMotionRecord: MotionEvent? = null
    private var mouseDownX: Float = 0f
    private var mouseDownY: Float = 0f
    private var mouseDownButtonState: Int = 0
    private var lastTimeMousePrimaryClickUp: Long = 0L
    private var mouseDoubleClick: Boolean = false

    var lastContextClickPosition: PointF? = null
        private set

    @JvmField
    var mouseClick: Boolean = false

    @JvmField
    var mouseCanMoveText: Boolean = false

    @JvmField
    var draggingSelection: CharPosition? = null

    /* dragging selection fields */
    private var dragSelectActive: Boolean = false
    private var dragSelectStarted: Boolean = false
    private var dragSelectInitialCharIndex: Int = -1
    private var dragSelectInitialLeftIndex: Int = -1
    private var dragSelectInitialRightIndex: Int = -1
    private var dragSelectLastDragIndex: Int = -1

    @JvmField
    var memoryPosition: Long = 0L

    @JvmField
    var positionNotApplied: Boolean = false

    @JvmField
    var focusY: Float = 0f

    init {
        edgeFieldSize = editor.dpUnit * 18f
        scaleMaxSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            26f,
            Resources.getSystem().displayMetrics
        )
        scaleMinSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            8f,
            Resources.getSystem().displayMetrics
        )
        magnifier = Magnifier(editor)
        leftHandle = SelectionHandle(HANDLE_LEFT)
        rightHandle = SelectionHandle(HANDLE_RIGHT)
        insertHandle = SelectionHandle(HANDLE_BOTH)
        val config = ViewConfiguration.get(editor.context)
        touchSlop = config.scaledTouchSlop
    }

    fun hasAnyHeldHandle(): Boolean = selHandleType != -1

    fun isHandleMoving(): Boolean = selHandleMoving

    /**
     * Whether we should draw scroll bars
     *
     * @return whether draw scroll bars
     */
    fun shouldDrawScrollBarForTouch(): Boolean {
        return System.currentTimeMillis() - timeLastScroll < HIDE_DELAY + SCROLLBAR_FADE_ANIMATION_TIME ||
                holdingScrollbarVertical || holdingScrollbarHorizontal
    }

    @UnsupportedUserUsage
    fun getScrollBarFadeOutPercentageForTouch(): Float {
        val elapsed = System.currentTimeMillis() - timeLastScroll
        if (elapsed < HIDE_DELAY || holdingScrollbarVertical || holdingScrollbarHorizontal) {
            return 0f
        } else if (elapsed in HIDE_DELAY until (HIDE_DELAY + SCROLLBAR_FADE_ANIMATION_TIME)) {
            editor.postInvalidateOnAnimation()
            return (elapsed - HIDE_DELAY).toFloat() / SCROLLBAR_FADE_ANIMATION_TIME
        }
        return 1f
    }

    /**
     * Hide the insert handle at once
     */
    fun hideInsertHandle() {
        if (!shouldDrawInsertHandle()) {
            return
        }
        timeLastSetSelection = 0L
        editor.invalidate()
    }

    /**
     * Whether the vertical scroll bar is touched
     *
     * @return Whether touched
     */
    fun holdVerticalScrollBar(): Boolean = holdingScrollbarVertical

    /**
     * Whether the horizontal scroll bar is touched
     *
     * @return Whether touched
     */
    fun holdHorizontalScrollBar(): Boolean = holdingScrollbarHorizontal

    /**
     * Whether insert handle is touched
     *
     * @return Whether touched
     */
    fun holdInsertHandle(): Boolean = selHandleType == HANDLE_BOTH

    /**
     * Whether the editor should draw insert handler
     *
     * @return Whether to draw
     */
    fun shouldDrawInsertHandle(): Boolean {
        return (System.currentTimeMillis() - timeLastSetSelection < HIDE_DELAY_HANDLE || holdInsertHandle())
    }

    /**
     * Notify the editor later to hide scroll bars
     */
    fun notifyScrolled() {
        timeLastScroll = System.currentTimeMillis()
        editor.postDelayedInLifecycle({
            if (System.currentTimeMillis() - timeLastScroll >= HIDE_DELAY) {
                editor.invalidate()
            }
        }, HIDE_DELAY)
    }

    /**
     * Notify the editor later to hide insert handle
     */
    fun notifyLater() {
        timeLastSetSelection = System.currentTimeMillis()
        editor.postDelayedInLifecycle({
            if (System.currentTimeMillis() - timeLastSetSelection >= HIDE_DELAY_HANDLE) {
                editor.invalidate()
            }
        }, HIDE_DELAY_HANDLE)
    }

    /**
     * Called by editor
     * Whether this class is handling motions by user
     *
     * @return Whether handling
     */
    fun handlingMotions(): Boolean {
        return holdHorizontalScrollBar() || holdVerticalScrollBar() || hasAnyHeldHandle()
    }

    /**
     * Reset states of handler
     */
    fun reset() {
        scroller.startScroll(0, 0, 0, 0, 0)
        reset2()
    }

    /**
     * Reset states of handler, except scrolling state
     */
    fun reset2() {
        holdingScrollbarHorizontal = false
        holdingScrollbarVertical = false
        selHandleType = -1
        finishDragSelect()
        dismissMagnifier()
    }

    private fun getHandleDescriptorByType(type: Int): SelectionHandleStyle.HandleDescriptor? {
        return when (type) {
            HANDLE_BOTH -> editor.insertHandleDescriptor
            HANDLE_LEFT -> editor.leftHandleDescriptor
            HANDLE_RIGHT -> editor.rightHandleDescriptor
            else -> null
        }
    }

    fun updateMagnifier(e: MotionEvent) {
        if (edgeFlags != 0 || !hasAnyHeldHandle() || !magnifier.isEnabled()) {
            dismissMagnifier()
            return
        }
        val descriptor = getHandleDescriptorByType(selHandleType) ?: return
        val pos = descriptor.position
        val height = pos.height()
        val x: Int
        val y: Int
        if (editor.isStickyTextSelection) {
            x = min(e.x, pos.right).toInt()
            y = (pos.top - height / 2f).toInt()
        } else {
            x = e.x.toInt()
            y = (e.y - height / 2f - editor.rowHeight).toInt()
        }
        magnifier.show(x, y)
    }

    fun dismissMagnifier() {
        magnifier.dismiss()
    }

    private fun beginDragSelect(line: Int, column: Int) {
        if (!editor.props.dragSelectAfterLongPress) {
            return
        }
        val text = editor.text
        dragSelectInitialCharIndex = text.getCharIndex(line, column)
        val cursor = editor.cursor
        dragSelectInitialLeftIndex = text.getCharIndex(cursor.leftLine, cursor.leftColumn)
        dragSelectInitialRightIndex = text.getCharIndex(cursor.rightLine, cursor.rightColumn)
        dragSelectLastDragIndex = dragSelectInitialCharIndex
        dragSelectActive = true
        dragSelectStarted = false
    }

    val isDragSelecting: Boolean
        get() = dragSelectActive

    private fun updateDragSelectMagnifier(e: MotionEvent) {
        if (!editor.props.dragSelectAfterLongPress ||
            edgeFlags != 0 || !magnifier.isEnabled() || !dragSelectStarted
        ) {
            dismissMagnifier()
            return
        }
        if (!magnifier.isShowing) {
            val dx = (e.x - thumbDownX).toDouble()
            val dy = (e.y - thumbDownY).toDouble()
            if (hypot(dx, dy) < MAGNIFIER_TOUCH_SLOP) {
                return
            }
        }
        val x = e.x.toInt()
        val y = (e.y - editor.rowHeight).toInt()
        magnifier.show(x, y)
    }

    private fun handleDragSelect(e: MotionEvent, fromEdgeScroll: Boolean): Boolean {
        if (!editor.props.dragSelectAfterLongPress || !dragSelectActive) {
            return false
        }
        val text = editor.text
        if (text.length == 0) {
            return true
        }
        val res = editor.getPointPositionOnScreen(e.x, e.y)
        val line = IntPair.getFirst(res)
        val column = IntPair.getSecond(res)
        val currentIndex = text.getCharIndex(line, column)
        if (!dragSelectStarted) {
            if (currentIndex == dragSelectInitialCharIndex) {
                if (!fromEdgeScroll) {
                    scrollIfThumbReachesEdge(e)
                }
                return true
            }
            dragSelectStarted = true
        }
        if (currentIndex == dragSelectLastDragIndex) {
            updateDragSelectMagnifier(e)
            if (!fromEdgeScroll) {
                scrollIfThumbReachesEdge(e)
            }
            return true
        }
        val rawAnchor = if (currentIndex <= dragSelectInitialCharIndex) {
            dragSelectInitialRightIndex
        } else {
            dragSelectInitialLeftIndex
        }
        val anchorIndex = rawAnchor.coerceIn(0, text.length)
        val startIndex = min(anchorIndex, currentIndex)
        val endIndex = max(anchorIndex, currentIndex)
        val indexer = text.indexer
        if (startIndex == endIndex) {
            val pos = indexer.getCharPosition(startIndex)
            editor.setSelection(
                pos.line,
                pos.column,
                false,
                SelectionChangeEvent.CAUSE_SELECTION_HANDLE
            )
        } else {
            val startPos = indexer.getCharPosition(startIndex)
            val endPos = indexer.getCharPosition(endIndex)
            editor.setSelectionRegion(
                startPos.line,
                startPos.column,
                endPos.line,
                endPos.column,
                false,
                SelectionChangeEvent.CAUSE_SELECTION_HANDLE
            )
        }
        dragSelectLastDragIndex = currentIndex
        updateDragSelectMagnifier(e)
        if (!fromEdgeScroll) {
            scrollIfThumbReachesEdge(e)
        }
        return true
    }

    private fun finishDragSelect() {
        val startedBefore = dragSelectStarted
        dragSelectActive = false
        dragSelectStarted = false
        dragSelectInitialCharIndex = -1
        dragSelectInitialLeftIndex = -1
        dragSelectInitialRightIndex = -1
        dragSelectLastDragIndex = -1
        if (startedBefore) {
            editor.dispatchEvent(DragSelectStopEvent(editor))
        }
    }

    /**
     * Handle events apart from detectors
     *
     * @param e The event editor received
     * @return Whether this touch event is handled by this class
     */
    fun onTouchEvent(e: MotionEvent): Boolean {
        motionY = e.y
        motionX = e.x
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                finishDragSelect()
                thumbDownY = e.y
                thumbDownX = e.x
                holdingScrollbarHorizontal = false
                holdingScrollbarVertical = false
                var rect = editor.renderer.verticalScrollBarRect
                if (RectUtils.contains(rect, e.x, e.y, editor.dpUnit * 10f)) {
                    holdingScrollbarVertical = true
                }
                rect = editor.renderer.horizontalScrollBarRect
                if (rect.contains(e.x, e.y)) {
                    holdingScrollbarHorizontal = true
                }
                if (holdingScrollbarVertical || holdingScrollbarHorizontal) {
                    if (holdingScrollbarVertical && holdingScrollbarHorizontal) {
                        holdingScrollbarHorizontal = false
                    }
                    editor.invalidate()
                } else {
                    val allowedDistance = editor.dpUnit * 7f
                    if (shouldDrawInsertHandle() &&
                        RectUtils.almostContains(
                            editor.insertHandleDescriptor.position,
                            e.x,
                            e.y,
                            allowedDistance
                        )
                    ) {
                        selHandleType = HANDLE_BOTH
                    }
                    val left = RectUtils.almostContains(
                        editor.leftHandleDescriptor.position,
                        e.x,
                        e.y,
                        allowedDistance
                    )
                    val right = RectUtils.almostContains(
                        editor.rightHandleDescriptor.position,
                        e.x,
                        e.y,
                        allowedDistance
                    )
                    if (left) {
                        selHandleType = HANDLE_LEFT
                    } else if (right) {
                        selHandleType = HANDLE_RIGHT
                    }
                    if (selHandleType != -1) {
                        selHandleMoving = false
                        dispatchHandleStateChange(selHandleType, true)
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (holdingScrollbarVertical) {
                    val movedDis = e.y - thumbDownY
                    thumbDownY = e.y
                    val all = editor.scrollMaxY.toFloat()
                    val dy =
                        movedDis / (editor.height - editor.renderer.verticalScrollBarRect.height()) * all
                    scrollBy(0f, dy)
                    return true
                }
                if (holdingScrollbarHorizontal) {
                    val movedDis = e.x - thumbDownX
                    thumbDownX = e.x
                    val all = (editor.scrollMaxX + editor.width).toFloat()
                    val dx =
                        if (editor.renderer.horizontalScrollBarRect.width() <= 60f * editor.dpUnit) {
                            movedDis / (editor.width - editor.renderer.horizontalScrollBarRect.width()) * all
                        } else {
                            movedDis / editor.width * all
                        }
                    scrollBy(dx, 0f)
                    return true
                }
                if (handleDragSelect(e, false)) {
                    return true
                }
                if (!selHandleMoving && (abs(e.x - thumbDownX) > touchSlop || abs(e.y - thumbDownY) > touchSlop)) {
                    selHandleMoving = true
                }
                if (selHandleMoving && handleSelectionChange(e)) {
                    if (magnifier.isShowing ||
                        hypot(
                            (e.x - thumbDownX).toDouble(),
                            (e.y - thumbDownY).toDouble()
                        ) >= MAGNIFIER_TOUCH_SLOP
                    ) {
                        updateMagnifier(e)
                    }
                    editor.invalidate()
                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (holdingScrollbarVertical || holdingScrollbarHorizontal) {
                    holdingScrollbarHorizontal = false
                    holdingScrollbarVertical = false
                    timeLastScroll = System.currentTimeMillis()
                    notifyScrolled()
                }
                finishDragSelect()
                if (selHandleType != -1) {
                    dispatchHandleStateChange(selHandleType, false)
                    if (selHandleType == HANDLE_BOTH) {
                        notifyLater()
                    }
                    selHandleType = -1
                }
                editor.invalidate()
                stopEdgeScroll()
                dismissMagnifier()
            }
        }
        return false
    }

    private fun shouldForwardToTouch(): Boolean {
        return holdingScrollbarHorizontal || holdingScrollbarVertical
    }

    /**
     * Entry for mouse motion events
     */
    fun onMouseEvent(event: MotionEvent): Boolean {
        if (editor.isFormatting) {
            resetMouse()
            return false
        }
        if (shouldForwardToTouch()) {
            return onTouchEvent(event)
        }
        lastContextClickPosition = null
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mouseDownX = event.x
                mouseDownY = event.y
                mouseDownButtonState = event.buttonState
                mouseClick = true
                if ((mouseDownButtonState and MotionEvent.BUTTON_PRIMARY) != 0) {
                    if (onTouchEvent(event) && shouldForwardToTouch()) {
                        return true
                    }
                    if (SystemClock.uptimeMillis() - lastTimeMousePrimaryClickUp < ViewConfiguration.getDoubleTapTimeout()) {
                        mouseDoubleClick = true
                        onDoubleTap(event)
                        return true
                    }
                    val pos = editor.getPointPositionOnScreen(mouseDownX, mouseDownY)
                    val line = IntPair.getFirst(pos)
                    val column = IntPair.getSecond(pos)
                    val charPos = editor.text.indexer.getCharPosition(line, column)
                    if (editor.isTextSelected && editor.cursorRange.isPositionInside(charPos) && editor.isScreenPointOnText(
                            mouseDownX,
                            mouseDownY
                        )
                    ) {
                        mouseCanMoveText = true
                    } else {
                        mouseCanMoveText = false
                        editor.setSelection(line, column, SelectionChangeEvent.CAUSE_MOUSE_INPUT)
                        editor.requestFocus()
                    }
                    draggingSelection = charPos
                    editor.postInvalidate()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (mouseDoubleClick) {
                    return true
                }
                if (abs(event.x - mouseDownX) > touchSlop || abs(event.y - mouseDownY) > touchSlop) {
                    mouseClick = false
                }
                if ((mouseDownButtonState and MotionEvent.BUTTON_PRIMARY) != 0) {
                    val pos = editor.getPointPositionOnScreen(event.x, event.y)
                    val line = IntPair.getFirst(pos)
                    val column = IntPair.getSecond(pos)
                    val charPos = editor.text.indexer.getCharPosition(line, column)
                    if (!mouseClick && !mouseCanMoveText) {
                        val anchor = editor.selectionAnchor
                        if (anchor != null) {
                            editor.setSelectionRegion(
                                anchor.line,
                                anchor.column,
                                line,
                                column,
                                SelectionChangeEvent.CAUSE_MOUSE_INPUT
                            )
                        }
                    }
                    draggingSelection = charPos
                    editor.postInvalidate()
                    scrollIfThumbReachesEdge(event)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (event.eventTime - event.downTime > ViewConfiguration.getTapTimeout() * 2f) {
                    mouseClick = false
                }
                if (!mouseDoubleClick) {
                    if (mouseCanMoveText && !mouseClick && (mouseDownButtonState and MotionEvent.BUTTON_PRIMARY) != 0) {
                        val pos = editor.getPointPositionOnScreen(event.x, event.y)
                        val line = IntPair.getFirst(pos)
                        val column = IntPair.getSecond(pos)
                        val dest = editor.text.indexer.getCharPosition(line, column)
                        val curRange = editor.cursorRange
                        if (!curRange.isPositionInside(dest) && (editor.keyMetaStates.isCtrlPressed || curRange.end != dest)) {
                            val length = curRange.endIndex - curRange.startIndex
                            val insIndex = if (editor.keyMetaStates.isCtrlPressed) {
                                dest.index
                            } else if (dest.index < curRange.startIndex) {
                                dest.index
                            } else {
                                dest.index - length
                            }
                            val text = editor.text
                            val insText =
                                text.subSequence(curRange.startIndex, curRange.endIndex).toString()
                            val insPos: CharPosition
                            if (editor.keyMetaStates.isCtrlPressed) {
                                text.insert(dest.line, dest.column, insText)
                                insPos = dest
                            } else {
                                text.beginBatchEdit()
                                editor.deleteText()
                                insPos = text.indexer.getCharPosition(insIndex)
                                text.insert(insPos.line, insPos.column, insText)
                                text.endBatchEdit()
                            }
                            val endPos = text.indexer.getCharPosition(insIndex + length)
                            editor.setSelectionRegion(
                                insPos.line,
                                insPos.column,
                                endPos.line,
                                endPos.column,
                                SelectionChangeEvent.CAUSE_MOUSE_INPUT
                            )
                        }
                    }
                    if (mouseClick) {
                        if ((mouseDownButtonState and MotionEvent.BUTTON_PRIMARY) != 0) {
                            onSingleTapUp(event)
                            lastTimeMousePrimaryClickUp = event.eventTime
                        } else if ((mouseDownButtonState and MotionEvent.BUTTON_SECONDARY) != 0) {
                            onContextClick(event)
                        }
                    }
                }
                resetMouse()
                stopEdgeScroll()
            }

            MotionEvent.ACTION_CANCEL -> {
                resetMouse()
                stopEdgeScroll()
            }
        }
        return true
    }

    /**
     * Reset mouse handling state
     */
    fun resetMouse() {
        mouseDownX = 0f
        mouseDownY = 0f
        mouseClick = false
        mouseCanMoveText = false
        draggingSelection = null
        if (mouseDoubleClick) {
            mouseDoubleClick = false
            lastTimeMousePrimaryClickUp = 0L
        }
    }

    /**
     * Context click
     */
    fun onContextClick(event: MotionEvent) {
        lastContextClickPosition = PointF(event.x, event.y)
        if ((dispatchEditorMotionEvent(
                ::ContextClickEvent,
                null,
                event
            ) and InterceptTarget.TARGET_EDITOR) != 0
        ) {
            return
        }
        editor.performContextClick(event.x, event.y)
        if (editor.props.mouseContextMenu) {
            editor.showContextMenu(event.x, event.y)
        }
    }

    private fun dispatchHandleStateChange(type: Int, held: Boolean) {
        editor.dispatchEvent(HandleStateChangeEvent(editor, type, held))
    }

    fun dispatchEditorMotionEvent(
        constructor: (CodeEditor, CharPosition, MotionEvent, Span?, TextRange?, Int, Int) -> EditorMotionEvent,
        pos: CharPosition?,
        event: MotionEvent
    ): Int {
        val region = editor.resolveTouchRegion(event)
        return dispatchEditorMotionEvent(
            constructor,
            pos,
            event,
            IntPair.getFirst(region),
            IntPair.getSecond(region)
        )
    }

    fun dispatchEditorMotionEvent(
        constructor: (CodeEditor, CharPosition, MotionEvent, Span?, TextRange?, Int, Int) -> EditorMotionEvent,
        pos: CharPosition?,
        event: MotionEvent,
        motionRegion: Int,
        motionBound: Int
    ): Int {
        val actualPos = pos ?: run {
            val pt = editor.getPointPositionOnScreen(event.x, event.y)
            editor.text.indexer.getCharPosition(IntPair.getFirst(pt), IntPair.getSecond(pt))
        }
        val styles = editor.styles
        val text = editor.text
        val span = StylesUtils.getSpanForPosition(styles, actualPos)
        val nextSpan = StylesUtils.getFollowingSpanForPosition(styles, actualPos)
        var range: TextRange? = null
        if (span != null) {
            val startPos = text.indexer.getCharPosition(
                actualPos.line,
                span.column.coerceIn(0, text.getColumnCount(actualPos.line))
            )
            val endPos = if (nextSpan != null) {
                text.indexer.getCharPosition(
                    actualPos.line,
                    nextSpan.column.coerceIn(0, text.getColumnCount(actualPos.line))
                )
            } else {
                text.indexer.getCharPosition(actualPos.line, text.getColumnCount(actualPos.line))
            }
            range = TextRange(startPos, endPos)
        }
        return editor.dispatchEvent(
            constructor(editor, actualPos, event, span, range, motionRegion, motionBound)
        )
    }

    private fun handleSelectionChange(e: MotionEvent): Boolean {
        when (selHandleType) {
            HANDLE_BOTH -> {
                insertHandle.applyPosition(e)
                scrollIfThumbReachesEdge(e)
                return true
            }

            HANDLE_LEFT -> {
                editor.selectionAnchor = editor.cursor.right()
                leftHandle.applyPosition(e)
                scrollIfThumbReachesEdge(e)
                return true
            }

            HANDLE_RIGHT -> {
                editor.selectionAnchor = editor.cursor.left()
                rightHandle.applyPosition(e)
                scrollIfThumbReachesEdge(e)
                return true
            }
        }
        return false
    }

    private fun handleSelectionChange2(e: MotionEvent) {
        when (selHandleType) {
            HANDLE_BOTH -> insertHandle.applyPosition(e)
            HANDLE_LEFT -> leftHandle.applyPosition(e)
            HANDLE_RIGHT -> rightHandle.applyPosition(e)
        }
    }

    private fun computeEdgeFlags(x: Float, y: Float): Int {
        var flags = 0
        if (x < edgeFieldSize) {
            flags = flags or LEFT_EDGE
        }
        if (y < edgeFieldSize) {
            flags = flags or TOP_EDGE
        }
        if (x > editor.width - edgeFieldSize) {
            flags = flags or RIGHT_EDGE
        }
        if (y > editor.height - edgeFieldSize) {
            flags = flags or BOTTOM_EDGE
        }
        return flags
    }

    fun scrollIfThumbReachesEdge(e: MotionEvent?) {
        scrollIfReachesEdge(e, 0f, 0f)
    }

    fun scrollIfReachesEdge(e: MotionEvent?, x: Float = 0f, y: Float = 0f) {
        var actualX = x
        var actualY = y
        if (e != null) {
            actualX = e.x
            actualY = e.y
        }
        val flag = computeEdgeFlags(actualX, actualY)
        if (flag != 0) {
            val oldFlags = edgeFlags
            edgeFlags = flag
            thumbMotionRecord = e?.let { MotionEvent.obtain(it) }
            if (oldFlags == 0) {
                val initialDelta = (8f * editor.dpUnit).toInt()
                editor.postInLifecycle(EdgeScrollRunnable(initialDelta))
            }
        } else {
            stopEdgeScroll()
        }
    }

    private fun isSameSign(a: Float, b: Float): Boolean {
        return !(abs(a) < 1e-5f || abs(b) < 1e-5f) && ((a < 0f && b < 0f) || (a > 0f && b > 0f))
    }

    fun stopEdgeScroll() {
        edgeFlags = 0
    }

    @JvmOverloads
    fun scrollBy(distanceX: Float, distanceY: Float, smooth: Boolean = false) {
        var endX = scroller.currX + distanceX.toInt()
        var endY = scroller.currY + distanceY.toInt()
        endX = max(endX, 0)
        endY = max(endY, 0)
        endY = min(endY, editor.scrollMaxY)
        endX = min(endX, editor.scrollMaxX)
        editor.dispatchEvent(
            ScrollEvent(
                editor,
                scroller.currX,
                scroller.currY,
                endX,
                endY,
                ScrollEvent.CAUSE_USER_DRAG
            )
        )
        if (smooth) {
            scroller.startScroll(
                scroller.currX,
                scroller.currY,
                endX - scroller.currX,
                endY - scroller.currY
            )
        } else {
            scroller.startScroll(
                scroller.currX,
                scroller.currY,
                endX - scroller.currX,
                endY - scroller.currY,
                0
            )
            scroller.abortAnimation()
        }
        editor.invalidate()
    }

    fun getTouchedHandleType(): Int = selHandleType

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        scroller.forceFinished(true)
        if (editor.isFormatting) {
            return true
        }
        val resolved = editor.resolveTouchRegion(e)
        val region = IntPair.getFirst(resolved)
        val regionBound = IntPair.getSecond(resolved)
        val visualPosition = editor.getPointVisualPositionOnScreen(e.x, e.y)
        val line = visualPosition.line
        val column = visualPosition.column
        editor.performClick()
        if (region == REGION_SIDE_ICON) {
            var row = ((e.y + editor.offsetX) / editor.rowHeight).toInt()
            row = row.coerceIn(0, editor.layout.rowCount - 1)
            val inf = editor.layout.getRowAt(row)
            if (inf.isLeadingRow) {
                val style = editor.renderer.getLineStyle(inf.lineIndex, LineSideIcon::class.java)
                if (style != null) {
                    if ((editor.dispatchEvent(
                            SideIconClickEvent(
                                editor,
                                style
                            )
                        ) and InterceptTarget.TARGET_EDITOR) != 0
                    ) {
                        return true
                    }
                }
            }
        }
        val position = editor.text.indexer.getCharPosition(line, column)
        val elem = visualPosition.element
        if (elem != null && elem.type == RowElementTypes.INLAY_HINT && elem.inlayHint != null && visualPosition.isInElementBounds) {
            if ((editor.dispatchEvent(
                    InlayHintClickEvent(
                        editor,
                        elem.inlayHint!!,
                        position
                    )
                ) and InterceptTarget.TARGET_EDITOR) != 0
            ) {
                return true
            }
        }
        if ((dispatchEditorMotionEvent(
                ::ClickEvent,
                position,
                e,
                region,
                regionBound
            ) and InterceptTarget.TARGET_EDITOR) != 0
        ) {
            return true
        }
        editor.showSoftInput()
        notifyLater()
        val lnAction = editor.props.actionWhenLineNumberClicked
        if (region == REGION_TEXT) {
            if (editor.isInLongSelect) {
                val cursor = editor.cursor
                editor.setSelectionRegion(
                    cursor.leftLine,
                    cursor.leftColumn,
                    line,
                    column,
                    false,
                    SelectionChangeEvent.CAUSE_TAP
                )
                editor.endLongSelect()
            } else {
                editor.setSelection(line, column, SelectionChangeEvent.CAUSE_TAP)
            }
        } else if (region == REGION_LINE_NUMBER) {
            when (lnAction) {
                DirectAccessProps.LN_ACTION_SELECT_LINE -> {
                    editor.setSelectionRegion(
                        line,
                        0,
                        line,
                        editor.text.getColumnCount(line),
                        false,
                        SelectionChangeEvent.CAUSE_TAP
                    )
                }

                DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME -> {
                    editor.setSelection(line, column, SelectionChangeEvent.CAUSE_TAP)
                }

                DirectAccessProps.LN_ACTION_NOTHING -> {
                    // do nothing
                }
            }
        }
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        scroller.forceFinished(true)
        editor.releaseEdgeEffects()
        if (editor.isFormatting) {
            return
        }
        val res = editor.getPointPositionOnScreen(e.x, e.y)
        val line = IntPair.getFirst(res)
        val column = IntPair.getSecond(res)
        if ((dispatchEditorMotionEvent(
                ::LongPressEvent,
                editor.text.indexer.getCharPosition(line, column),
                e
            ) and InterceptTarget.TARGET_EDITOR) != 0
        ) {
            return
        }
        if ((!editor.props.reselectOnLongPress && editor.cursor.isSelected) || e.pointerCount != 1) {
            return
        }
        editor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        editor.selectWord(line, column)
        if (editor.cursor.isSelected) {
            beginDragSelect(line, column)
        }
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        var dx = distanceX
        var dy = distanceY
        if (editor.props.singleDirectionDragging) {
            if (abs(dx) > abs(dy)) {
                dy = 0f
            } else {
                dx = 0f
            }
        }
        var endX = scroller.currX + dx.toInt()
        var endY = scroller.currY + dy.toInt()
        endX = endX.coerceIn(0, editor.scrollMaxX)
        endY = endY.coerceIn(0, editor.scrollMaxY)
        var notifyY = true
        var notifyX = true
        if (!editor.verticalEdgeEffect.isFinished) {
            val displacement = (e2.x / editor.width).coerceIn(0f, 1f)
            val distance = (if (glowTopOrBottom) dy else -dy) / editor.measuredHeight
            if (distance > 0f) {
                endY = scroller.currY
                editor.verticalEdgeEffect.onPull(
                    distance,
                    if (!glowTopOrBottom) displacement else 1f - displacement
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val edgeEffect = editor.verticalEdgeEffect
                edgeEffect.onPullDistance(
                    distance,
                    if (!glowTopOrBottom) displacement else 1f - displacement
                )
                if (edgeEffect.distance != 0f) {
                    endY = scroller.currY
                }
            } else {
                editor.verticalEdgeEffect.finish()
            }
            notifyY = false
        }
        if (!editor.horizontalEdgeEffect.isFinished) {
            val displacement = (e2.y / editor.height).coerceIn(0f, 1f)
            val distance = (if (glowLeftOrRight) dx else -dx) / editor.measuredWidth
            if (distance > 0f) {
                endX = scroller.currX
                editor.horizontalEdgeEffect.onPull(
                    distance,
                    if (!glowLeftOrRight) 1f - displacement else displacement
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val edgeEffect = editor.horizontalEdgeEffect
                edgeEffect.onPullDistance(
                    distance,
                    if (!glowLeftOrRight) 1f - displacement else displacement
                )
                if (edgeEffect.distance != 0f) {
                    endX = scroller.currX
                }
            } else {
                editor.horizontalEdgeEffect.finish()
            }
            notifyX = false
        }
        scroller.startScroll(
            scroller.currX,
            scroller.currY,
            endX - scroller.currX,
            endY - scroller.currY,
            0
        )
        val minOverPull = 2f
        if (notifyY && scroller.currY + dy < -minOverPull) {
            editor.verticalEdgeEffect.onPull(
                -dy / editor.measuredHeight,
                (e2.x / editor.width).coerceIn(0f, 1f)
            )
            glowTopOrBottom = false
        }
        if (notifyY && scroller.currY + dy > editor.scrollMaxY + minOverPull) {
            editor.verticalEdgeEffect.onPull(
                dy / editor.measuredHeight,
                (e2.x / editor.width).coerceIn(0f, 1f)
            )
            glowTopOrBottom = true
        }
        if (notifyX && scroller.currX + dx < -minOverPull) {
            editor.horizontalEdgeEffect.onPull(
                -dx / editor.measuredWidth,
                (e2.y / editor.height).coerceIn(0f, 1f)
            )
            glowLeftOrRight = false
        }
        if (notifyX && scroller.currX + dx > editor.scrollMaxX + minOverPull) {
            editor.horizontalEdgeEffect.onPull(
                dx / editor.measuredWidth,
                (e2.y / editor.height).coerceIn(0f, 1f)
            )
            glowLeftOrRight = true
        }
        editor.invalidate()
        editor.dispatchEvent(
            ScrollEvent(
                editor,
                scroller.currX,
                scroller.currY,
                endX,
                endY,
                ScrollEvent.CAUSE_USER_DRAG
            )
        )
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        var vx = velocityX
        var vy = velocityY
        if (editor.props.singleDirectionFling) {
            if (abs(vx) > abs(vy)) {
                vy = 0f
            } else {
                vx = 0f
            }
        }
        if (!editor.props.scrollFling) {
            return false
        }
        scroller.forceFinished(true)
        scroller.fling(
            scroller.currX,
            scroller.currY,
            (-vx).toInt(),
            (-vy).toInt(),
            0,
            editor.scrollMaxX,
            0,
            editor.scrollMaxY,
            if (editor.props.overScrollEnabled && !editor.isWordwrap) (20f * editor.dpUnit).toInt() else 0,
            if (editor.props.overScrollEnabled) (20f * editor.dpUnit).toInt() else 0
        )
        val minVe = editor.dpUnit * 2000f
        if (abs(vx) >= minVe || abs(vy) >= minVe) {
            notifyScrolled()
        }
        editor.releaseEdgeEffects()
        editor.dispatchEvent(
            ScrollEvent(
                editor,
                scroller.currX,
                scroller.currY,
                scroller.finalX,
                scroller.finalY,
                ScrollEvent.CAUSE_USER_FLING
            )
        )
        editor.postInvalidateOnAnimation()
        return false
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        if (editor.isFormatting) {
            return true
        }
        if (editor.isScalable) {
            val newSize = editor.textSizePx * detector.scaleFactor
            if (newSize < scaleMinSize || newSize > scaleMaxSize) {
                return true
            }
            val focusX = detector.focusX
            val focusY = detector.focusY
            val originHeight = editor.rowHeight
            editor.setTextSizePxDirect(newSize)
            val heightFactor = editor.rowHeight.toFloat() / originHeight
            var afterScrollY = (scroller.currY + focusY) * heightFactor - focusY
            var afterScrollX = (scroller.currX + focusX) * detector.scaleFactor - focusX
            afterScrollX = afterScrollX.coerceIn(0f, editor.scrollMaxX.toFloat())
            afterScrollY = afterScrollY.coerceIn(0f, editor.scrollMaxY.toFloat())
            editor.dispatchEvent(
                ScrollEvent(
                    editor,
                    scroller.currX,
                    scroller.currY,
                    afterScrollX.toInt(),
                    afterScrollY.toInt(),
                    ScrollEvent.CAUSE_SCALE_TEXT
                )
            )
            scroller.startScroll(afterScrollX.toInt(), afterScrollY.toInt(), 0, 0, 0)
            scroller.abortAnimation()
            isScaling = true
            editor.invalidate()
            return true
        }
        return false
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        scroller.forceFinished(true)
        textSizeStart = editor.textSizePx
        return editor.isScalable && !editor.isFormatting && !hasAnyHeldHandle()
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        isScaling = false
        if (textSizeStart == editor.textSizePx) {
            return
        }
        editor.renderer.forcedRecreateLayout = true
        if (editor.isWordwrap) {
            focusY = detector.focusY
            memoryPosition = editor.getPointPositionOnScreen(detector.focusX, detector.focusY)
            positionNotApplied = true
        } else {
            positionNotApplied = false
        }
        editor.renderContext.invalidateRenderNodes()
        editor.renderer.updateTimestamp()
        editor.invalidate()
    }

    override fun onDown(e: MotionEvent): Boolean = editor.isEnabled

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean = true

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (editor.isFormatting) {
            return true
        }
        val res = editor.getPointPositionOnScreen(e.x, e.y)
        val line = IntPair.getFirst(res)
        val column = IntPair.getSecond(res)
        if ((dispatchEditorMotionEvent(
                ::DoubleClickEvent,
                editor.text.indexer.getCharPosition(line, column),
                e
            ) and InterceptTarget.TARGET_EDITOR) != 0
        ) {
            return true
        }
        if (editor.cursor.isSelected || e.pointerCount != 1) {
            return true
        }
        editor.selectWord(line, column)
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean = true

    /**
     * This is a helper for EventHandler to control handles
     */
    internal inner class SelectionHandle(var type: Int) {

        private fun checkNoIntersection(
            one: SelectionHandleStyle.HandleDescriptor,
            another: SelectionHandleStyle.HandleDescriptor
        ): Boolean {
            return !RectF.intersects(one.position, another.position)
        }

        /**
         * Handle the event
         *
         * @param e Event sent by EventHandler
         */
        fun applyPosition(e: MotionEvent) {
            val descriptor = when (type) {
                HANDLE_LEFT -> editor.leftHandleDescriptor
                HANDLE_RIGHT -> editor.rightHandleDescriptor
                else -> editor.insertHandleDescriptor
            }
            val anotherDesc =
                if (type == HANDLE_LEFT) editor.rightHandleDescriptor else editor.leftHandleDescriptor
            val targetX =
                scroller.currX + e.x + (if (descriptor.alignment != SelectionHandleStyle.ALIGN_CENTER) descriptor.position.width() else 0f) * (if (descriptor.alignment == SelectionHandleStyle.ALIGN_LEFT) 1 else -1)
            val targetY = scroller.currY + e.y - descriptor.position.height()
            val line = IntPair.getFirst(editor.getPointPosition(0f, targetY))
            if (line in 0 until editor.lineCount) {
                val column = IntPair.getSecond(editor.getPointPosition(targetX, targetY))
                val lastLine =
                    if (type == HANDLE_RIGHT) editor.cursor.rightLine else editor.cursor.leftLine
                val lastColumn =
                    if (type == HANDLE_RIGHT) editor.cursor.rightColumn else editor.cursor.leftColumn
                val anotherLine =
                    if (type != HANDLE_RIGHT) editor.cursor.rightLine else editor.cursor.leftLine
                val anotherColumn =
                    if (type != HANDLE_RIGHT) editor.cursor.rightColumn else editor.cursor.leftColumn

                if ((line != lastLine || column != lastColumn) && (type == HANDLE_BOTH || (line != anotherLine || column != anotherColumn))) {
                    when (type) {
                        HANDLE_BOTH -> {
                            editor.cancelAnimation()
                            editor.setSelection(
                                line,
                                column,
                                false,
                                SelectionChangeEvent.CAUSE_SELECTION_HANDLE
                            )
                        }

                        HANDLE_RIGHT -> {
                            if (anotherLine > line || (anotherLine == line && anotherColumn > column)) {
                                // Swap type
                                if (checkNoIntersection(descriptor, anotherDesc)) {
                                    dispatchHandleStateChange(selHandleType, false)
                                    this@EditorTouchEventHandler.selHandleType = HANDLE_LEFT
                                    dispatchHandleStateChange(selHandleType, true)
                                    this.type = HANDLE_LEFT
                                    leftHandle.type = HANDLE_RIGHT
                                    val tmp = rightHandle
                                    rightHandle = leftHandle
                                    leftHandle = tmp
                                    editor.setSelectionRegion(
                                        line,
                                        column,
                                        anotherLine,
                                        anotherColumn,
                                        false,
                                        SelectionChangeEvent.CAUSE_SELECTION_HANDLE
                                    )
                                }
                            } else {
                                editor.setSelectionRegion(
                                    anotherLine,
                                    anotherColumn,
                                    line,
                                    column,
                                    false,
                                    SelectionChangeEvent.CAUSE_SELECTION_HANDLE
                                )
                            }
                        }

                        HANDLE_LEFT -> {
                            if (anotherLine < line || (anotherLine == line && anotherColumn < column)) {
                                // Swap type
                                if (checkNoIntersection(descriptor, anotherDesc)) {
                                    dispatchHandleStateChange(selHandleType, false)
                                    this@EditorTouchEventHandler.selHandleType = HANDLE_RIGHT
                                    dispatchHandleStateChange(selHandleType, true)
                                    this.type = HANDLE_RIGHT
                                    rightHandle.type = HANDLE_LEFT
                                    val tmp = rightHandle
                                    rightHandle = leftHandle
                                    leftHandle = tmp
                                    editor.setSelectionRegion(
                                        anotherLine,
                                        anotherColumn,
                                        line,
                                        column,
                                        false,
                                        SelectionChangeEvent.CAUSE_SELECTION_HANDLE
                                    )
                                }
                            } else {
                                editor.setSelectionRegion(
                                    line,
                                    column,
                                    anotherLine,
                                    anotherColumn,
                                    false,
                                    SelectionChangeEvent.CAUSE_SELECTION_HANDLE
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Runnable for controlling auto-scrolling when thumb reaches the edges of editor
     */
    private inner class EdgeScrollRunnable(initDelta: Int) : Runnable {

        private val initialDelta: Float = initDelta.toFloat()
        private var deltaHorizontal: Float = initialDelta
        private var deltaVertical: Float = initialDelta
        private var lastDx: Float = 0f
        private var lastDy: Float = 0f
        private var factorX: Float = 0f
        private var factorY: Float = 0f
        private var postTimes: Long = 0L

        override fun run() {
            var dx = (if ((edgeFlags and LEFT_EDGE) != 0) -deltaHorizontal else 0f) +
                    (if ((edgeFlags and RIGHT_EDGE) != 0) deltaHorizontal else 0f)
            val dy = (if ((edgeFlags and TOP_EDGE) != 0) -deltaVertical else 0f) +
                    (if ((edgeFlags and BOTTOM_EDGE) != 0) deltaVertical else 0f)
            if (dx > 0f) {
                // Check whether there is content at right
                val line = if (selHandleType == HANDLE_BOTH || selHandleType == HANDLE_LEFT) {
                    editor.cursor.leftLine
                } else {
                    editor.cursor.rightLine
                }
                val column = editor.text.getColumnCount(line)
                // Do not scroll too far from text region of this line
                val maxOffset = editor.measureTextRegionOffset() +
                        editor.layout.getCharLayoutOffset(line, column)[1] -
                        editor.width * 0.85f
                if (scroller.currX > maxOffset) {
                    dx = 0f
                }
            }
            scrollBy(dx, dy)
            if (magnifier.isShowing) {
                magnifier.dismiss()
            }

            // Speed up if we are scrolling in the direction
            if (isSameSign(dx, lastDx)) {
                if (factorX < MAX_FACTOR && (postTimes and 1L) == 0L) {
                    factorX++
                    deltaHorizontal *= INCREASE_FACTOR
                }
            } else {
                deltaHorizontal = initialDelta
                factorX = 0f
            }
            if (isSameSign(dy, lastDy)) {
                if (factorY < MAX_FACTOR && (postTimes and 1L) == 0L) {
                    factorY++
                    deltaVertical *= INCREASE_FACTOR
                }
            } else {
                deltaVertical = initialDelta
                factorY = 0f
            }
            lastDx = dx
            lastDy = dy

            // Update selection
            val motionRecord = thumbMotionRecord
            if (motionRecord != null) {
                if (!handleDragSelect(motionRecord, true)) {
                    handleSelectionChange2(motionRecord)
                }
            }

            postTimes++
            // Post for animation
            if (edgeFlags != 0) {
                editor.postDelayedInLifecycle(this, 10)
            }
        }
    }
}
