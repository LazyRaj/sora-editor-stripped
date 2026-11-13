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
package io.github.rosemoe.sora.widget.component

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.DragSelectStopEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.EventManager
import io.github.rosemoe.sora.event.HandleStateChangeEvent
import io.github.rosemoe.sora.event.InterceptTarget
import io.github.rosemoe.sora.event.LongPressEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorTouchEventHandler
import io.github.rosemoe.sora.widget.base.EditorPopupWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlin.math.max
import kotlin.math.min

/**
 * This window will show when selecting text to present text actions.
 *
 * @author Rosemoe
 */
open class EditorTextActionWindow(
    editor: CodeEditor
) : EditorPopupWindow(editor, FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED),
    View.OnClickListener,
    EditorBuiltinComponent {

    companion object {
        private const val DELAY = 200L
        private const val CHECK_FOR_DISMISS_INTERVAL = 100L
    }

    private val selectAllBtn: ImageButton
    private val pasteBtn: ImageButton
    private val copyBtn: ImageButton
    private val cutBtn: ImageButton
    private val longSelectBtn: ImageButton
    private val rootView: View
    private val handler: EditorTouchEventHandler = editor.eventHandler
    private val eventManager: EventManager = editor.createSubEventManager()
    private var lastScroll: Long = 0L
    private var lastPosition: Int = 0
    private var lastCause: Int = 0
    private var _enabled: Boolean = true

    init {
        @SuppressLint("InflateParams")
        val root = LayoutInflater.from(editor.context).inflate(R.layout.text_compose_panel, null)
        this.rootView = root
        selectAllBtn = root.findViewById(R.id.panel_btn_select_all)
        cutBtn = root.findViewById(R.id.panel_btn_cut)
        copyBtn = root.findViewById(R.id.panel_btn_copy)
        longSelectBtn = root.findViewById(R.id.panel_btn_long_select)
        pasteBtn = root.findViewById(R.id.panel_btn_paste)

        selectAllBtn.setOnClickListener(this)
        cutBtn.setOnClickListener(this)
        copyBtn.setOnClickListener(this)
        pasteBtn.setOnClickListener(this)
        longSelectBtn.setOnClickListener(this)

        applyColorScheme()
        setContentView(root)
        setSize(0, (editor.dpUnit * 48).toInt())
        popup.animationStyle = R.style.text_action_popup_animation

        subscribeEvents()
    }

    protected open fun applyColorFilter(btn: ImageButton, color: Int) {
        val drawable = btn.drawable ?: return
        btn.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    protected open fun applyColorScheme() {
        val gd = GradientDrawable().apply {
            cornerRadius = 5 * editor.dpUnit
            setColor(editor.colorScheme.getColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND))
        }
        rootView.background = gd
        val color = editor.colorScheme.getColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR)
        applyColorFilter(selectAllBtn, color)
        applyColorFilter(cutBtn, color)
        applyColorFilter(copyBtn, color)
        applyColorFilter(pasteBtn, color)
        applyColorFilter(longSelectBtn, color)
    }

    protected open fun subscribeEvents() {
        eventManager.subscribeAlways(SelectionChangeEvent::class.java, this::onSelectionChange)
        eventManager.subscribeAlways(ScrollEvent::class.java, this::onEditorScroll)
        eventManager.subscribeAlways(HandleStateChangeEvent::class.java, this::onHandleStateChange)
        eventManager.subscribeAlways(LongPressEvent::class.java, this::onEditorLongPress)
        eventManager.subscribeAlways(EditorFocusChangeEvent::class.java, this::onEditorFocusChange)
        eventManager.subscribeAlways(EditorReleaseEvent::class.java, this::onEditorRelease)
        eventManager.subscribeAlways(ColorSchemeUpdateEvent::class.java, this::onEditorColorChange)
        eventManager.subscribeAlways(DragSelectStopEvent::class.java, this::onDragSelectingStop)
    }

    protected open fun onEditorColorChange(event: ColorSchemeUpdateEvent) {
        applyColorScheme()
    }

    protected open fun onEditorFocusChange(event: EditorFocusChangeEvent) {
        if (!event.isGainFocus) {
            dismiss()
        }
    }

    protected open fun onDragSelectingStop(event: DragSelectStopEvent) {
        displayWindow()
    }

    protected open fun onEditorRelease(event: EditorReleaseEvent) {
        isEnabled = false
    }

    protected open fun onEditorLongPress(event: LongPressEvent) {
        if (editor.cursor.isSelected && lastCause == SelectionChangeEvent.CAUSE_SEARCH) {
            val idx = event.index
            if (idx >= editor.cursor.left && idx <= editor.cursor.right) {
                lastCause = 0
                displayWindow()
            }
            event.intercept(InterceptTarget.TARGET_EDITOR)
        }
    }

    protected open fun onEditorScroll(event: ScrollEvent) {
        val last = lastScroll
        lastScroll = System.currentTimeMillis()
        if (lastScroll - last < DELAY && lastCause != SelectionChangeEvent.CAUSE_SEARCH) {
            postDisplay()
        }
    }

    protected open fun onHandleStateChange(event: HandleStateChangeEvent) {
        if (event.isHeld) {
            postDisplay()
        }
        if (!event.editor.cursor.isSelected
            && event.handleType == HandleStateChangeEvent.HANDLE_TYPE_INSERT
            && !event.isHeld
        ) {
            displayWindow()
            // Also, post to hide the window on handle disappearance
            editor.postDelayedInLifecycle(object : Runnable {
                override fun run() {
                    if (!editor.eventHandler.shouldDrawInsertHandle()
                        && !editor.cursor.isSelected
                    ) {
                        dismiss()
                    } else if (!editor.cursor.isSelected) {
                        editor.postDelayedInLifecycle(this, CHECK_FOR_DISMISS_INTERVAL)
                    }
                }
            }, CHECK_FOR_DISMISS_INTERVAL)
        }
    }

    protected open fun onSelectionChange(event: SelectionChangeEvent) {
        if (handler.hasAnyHeldHandle() || event.cause == SelectionChangeEvent.CAUSE_DEAD_KEYS) {
            return
        }
        if (handler.isDragSelecting) {
            dismiss()
            return
        }
        lastCause = event.cause
        if (event.isSelected || event.cause == SelectionChangeEvent.CAUSE_LONG_PRESS && editor.text.length == 0) {
            // Always post show. See #193
            if (event.cause != SelectionChangeEvent.CAUSE_SEARCH) {
                editor.postInLifecycle(this::displayWindow)
            } else {
                dismiss()
            }
            lastPosition = -1
        } else {
            var show = false
            if (event.cause == SelectionChangeEvent.CAUSE_TAP && event.left.index == lastPosition && !isShowing && !editor.text.isInBatchEdit && editor.isEditable) {
                editor.postInLifecycle(this::displayWindow)
                show = true
            } else {
                dismiss()
            }
            if (event.cause == SelectionChangeEvent.CAUSE_TAP && !show) {
                lastPosition = event.left.index
            } else {
                lastPosition = -1
            }
        }
    }

    override fun isEnabled(): Boolean = _enabled

    override fun setEnabled(enabled: Boolean) {
        _enabled = enabled
        eventManager.isEnabled = enabled
        if (!enabled) {
            dismiss()
        }
    }

    open fun getView(): ViewGroup = popup.contentView as ViewGroup

    private fun postDisplay() {
        if (!isShowing) {
            return
        }
        dismiss()
        if (!editor.cursor.isSelected) {
            return
        }
        editor.postDelayedInLifecycle(object : Runnable {
            override fun run() {
                if (!handler.hasAnyHeldHandle() && !editor.snippetController.isInSnippet() && System.currentTimeMillis() - lastScroll > DELAY
                    && editor.scroller.isFinished
                ) {
                    displayWindow()
                } else {
                    editor.postDelayedInLifecycle(this, DELAY)
                }
            }
        }, DELAY)
    }

    private fun selectTop(rect: RectF): Int {
        val rowHeight = editor.rowHeight
        return if (rect.top - rowHeight * 3 / 2f > height) {
            (rect.top - rowHeight * 3 / 2f - height).toInt()
        } else {
            (rect.bottom + rowHeight / 2f).toInt()
        }
    }

    open fun displayWindow() {
        updateBtnState()
        val cursor = editor.cursor
        val top = if (cursor.isSelected) {
            val leftRect = editor.leftHandleDescriptor.position
            val rightRect = editor.rightHandleDescriptor.position
            val top1 = selectTop(leftRect)
            val top2 = selectTop(rightRect)
            min(top1, top2)
        } else {
            selectTop(editor.insertHandleDescriptor.position)
        }
        val boundedTop = max(0, min(top, editor.height - height - 5))
        val handleLeftX = editor.getOffset(editor.cursor.leftLine, editor.cursor.leftColumn)
        val handleRightX = editor.getOffset(editor.cursor.rightLine, editor.cursor.rightColumn)
        val panelX = ((handleLeftX + handleRightX) / 2f - rootView.measuredWidth / 2f).toInt()
        setLocationAbsolutely(panelX, boundedTop)
        show()
    }

    private fun updateBtnState() {
        pasteBtn.isEnabled = editor.hasClip()
        copyBtn.visibility = if (editor.cursor.isSelected) View.VISIBLE else View.GONE
        pasteBtn.visibility = if (editor.isEditable) View.VISIBLE else View.GONE
        cutBtn.visibility = if (editor.cursor.isSelected && editor.isEditable) View.VISIBLE else View.GONE
        longSelectBtn.visibility = if (!editor.cursor.isSelected && editor.isEditable) View.VISIBLE else View.GONE
        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(1000000, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(100000, View.MeasureSpec.AT_MOST)
        )
        setSize(min(rootView.measuredWidth, (editor.dpUnit * 230).toInt()), height)
    }

    override fun show() {
        if (!_enabled || editor.snippetController.isInSnippet() || !editor.hasFocus() || editor.isInMouseMode) {
            return
        }
        super.show()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.panel_btn_select_all -> {
                editor.selectAll()
                return
            }
            R.id.panel_btn_cut -> {
                if (editor.cursor.isSelected) {
                    editor.cutText()
                }
            }
            R.id.panel_btn_paste -> {
                editor.pasteText()
                editor.setSelection(editor.cursor.rightLine, editor.cursor.rightColumn)
            }
            R.id.panel_btn_copy -> {
                editor.copyText()
                editor.setSelection(editor.cursor.rightLine, editor.cursor.rightColumn)
            }
            R.id.panel_btn_long_select -> {
                editor.beginLongSelect()
            }
        }
        dismiss()
    }
}
