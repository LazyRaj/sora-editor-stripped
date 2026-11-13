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
package io.github.rosemoe.sora.widget.base

import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Base class for all editor popup windows.
 */
open class EditorPopupWindow(
    val editor: CodeEditor,
    private val features: Int
) {

    companion object {
        /**
         * Update the position of this window when user scrolls the editor
         */
        const val FEATURE_SCROLL_AS_CONTENT = 1

        /**
         * Allow the window to be displayed outside the view's rectangle.
         * Otherwise, the window's size will be adjusted to force it to display in the view.
         * If the space can't display it, it will get hidden.
         */
        const val FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED = 1 shl 1

        /**
         * Hide this window when the user scrolls fast. Such as the selection handle
         * is currently near the edge of screen.
         */
        const val FEATURE_HIDE_WHEN_FAST_SCROLL = 1 shl 2

        /**
         * Dismiss the window if it covers the current caret.
         */
        const val FEATURE_DISMISS_WHEN_OBSCURING_CURSOR = 1 shl 3
    }

    private val window: PopupWindow = PopupWindow().apply {
        elevation = editor.dpUnit * 8
    }
    private val locationBuffer = IntArray(2)
    private val scrollListener: EventReceiver<ScrollEvent>
    private val editorLayoutChangeListener: View.OnLayoutChangeListener
    private var registerFlag = false
    private var registered = false
    private var layoutChangeListenerRegistered = false
    var parentView: View = editor

    private var offsetX = 0
    private var offsetY = 0
    private var windowX = 0
    private var windowY = 0
    var width: Int = 0
        private set
    var height: Int = 0
        private set

    init {
        editorLayoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (isShowing) {
                applyWindowAttributes(false)
            }
        }
        scrollListener = EventReceiver { event, unsubscribe ->
            if (!registerFlag) {
                unsubscribe.unsubscribe()
                registered = false
                return@EventReceiver
            }
            when (event.cause) {
                ScrollEvent.CAUSE_MAKE_POSITION_VISIBLE,
                ScrollEvent.CAUSE_TEXT_SELECTING,
                ScrollEvent.CAUSE_USER_FLING,
                ScrollEvent.CAUSE_SCALE_TEXT -> {
                    if (isFeatureEnabled(FEATURE_HIDE_WHEN_FAST_SCROLL) &&
                        (abs(event.endX - event.startX) > 80 || abs(event.endY - event.startY) > 80)
                    ) {
                        if (isShowing) {
                            dismiss()
                            return@EventReceiver
                        }
                    }
                }
            }
            if (isFeatureEnabled(FEATURE_SCROLL_AS_CONTENT)) {
                applyWindowAttributes(false)
            }
        }
        register()
    }

    /**
     * Checks whether a single feature is enabled
     */
    fun isFeatureEnabled(feature: Int): Boolean {
        require(Integer.bitCount(feature) == 1) { "Not a valid feature integer" }
        return (features and feature) != 0
    }

    /**
     * Register this window in target editor.
     */
    fun register() {
        if (!registered) {
            editor.subscribeEvent(ScrollEvent::class.java, scrollListener)
            registered = true
        }
        if (isFeatureEnabled(FEATURE_DISMISS_WHEN_OBSCURING_CURSOR) && !layoutChangeListenerRegistered) {
            editor.addOnLayoutChangeListener(editorLayoutChangeListener)
            layoutChangeListenerRegistered = true
        }
        registerFlag = true
    }

    /**
     * Unregister this window in target editor.
     */
    fun unregister() {
        registerFlag = false
        if (layoutChangeListenerRegistered) {
            editor.removeOnLayoutChangeListener(editorLayoutChangeListener)
            layoutChangeListenerRegistered = false
        }
    }

    open val isShowing: Boolean
        get() = window.isShowing

    /**
     * Get the actual [PopupWindow] instance.
     */
    val popup: PopupWindow
        get() = window

    /**
     * @see PopupWindow.setContentView
     */
    open fun setContentView(view: View) {
        window.contentView = view
    }

    private fun wrapHorizontal(horizontal: Int): Int {
        return max(0, min(horizontal, editor.width))
    }

    private fun wrapVertical(vertical: Int): Int {
        return max(0, min(vertical, editor.height))
    }

    private fun applyWindowAttributes(show: Boolean) {
        if (!show && !isShowing) {
            return
        }
        val autoScroll = isFeatureEnabled(FEATURE_SCROLL_AS_CONTENT)
        var left = if (autoScroll) (windowX - editor.offsetX) else (windowX - offsetX)
        var top = if (autoScroll) (windowY - editor.offsetY) else (windowY - offsetY)
        var right = left + width
        var bottom = top + height
        if (!isFeatureEnabled(FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED)) {
            left = wrapHorizontal(left)
            right = wrapHorizontal(right)
            top = wrapVertical(top)
            bottom = wrapVertical(bottom)
            if (top >= bottom || left >= right) {
                dismiss()
                return
            }
        }
        if (isCursorObscured(left, top, right, bottom)) {
            dismiss()
            return
        }
        editor.getLocationInWindow(locationBuffer)
        val w = right - left
        val h = bottom - top
        left += locationBuffer[0]
        top += locationBuffer[1]
        if (window.isShowing) {
            window.update(left, top, w, h)
        } else if (show) {
            window.height = h
            window.width = w
            window.showAtLocation(parentView, Gravity.START or Gravity.TOP, left, top)
        }
    }



    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        applyWindowAttributes(false)
    }

    fun setLocation(x: Int, y: Int) {
        windowX = x
        windowY = y
        offsetY = editor.offsetY
        offsetX = editor.offsetX
        applyWindowAttributes(false)
    }

    fun setLocationAbsolutely(x: Int, y: Int) {
        setLocation(x + editor.offsetX, y + editor.offsetY)
    }

    private fun isCursorObscured(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        if (!isFeatureEnabled(FEATURE_DISMISS_WHEN_OBSCURING_CURSOR)) {
            return false
        }
        return try {
            val cursor = editor.cursor ?: return false
            val line = cursor.leftLine
            val column = cursor.leftColumn
            val cursorLeft = editor.getCharOffsetX(line, column)
            val cursorTop = editor.getCharOffsetY(line, column)
            if (cursorLeft.isNaN() || cursorTop.isNaN()) {
                return false
            }
            val cursorRight = cursorLeft + max(1f, editor.insertSelectionWidth)
            val cursorBottom = cursorTop + editor.rowHeight
            cursorLeft < right && cursorRight > left && cursorTop < bottom && cursorBottom > top
        } catch (_: Throwable) {
            false
        }
    }

    open fun show() {
        if (isShowing) {
            return
        }
        applyWindowAttributes(true)
    }

    open fun dismiss() {
        if (isShowing) {
            window.dismiss()
        }
    }
}
