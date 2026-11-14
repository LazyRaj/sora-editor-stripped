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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import io.github.rosemoe.sora.event.ClickEvent
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.event.EditorFormatEvent
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.event.EditorLanguageChangeEvent
import io.github.rosemoe.sora.event.EditorPropertyChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.Event
import io.github.rosemoe.sora.event.EventManager
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SnippetEvent
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.highlightMatchLabel
import io.github.rosemoe.sora.lang.styling.StylesUtils
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.text.TextReference
import io.github.rosemoe.sora.util.KeyboardUtils
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.base.EditorPopupWindow
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Auto complete window for editing code quicker
 *
 * @author Rosemoe
 */
open class EditorAutoCompletion(
    editor: CodeEditor
) : EditorPopupWindow(editor, FEATURE_HIDE_WHEN_FAST_SCROLL),
    EditorBuiltinComponent {

    companion object {
        /**
         * Adjust the completion window's position scheme according to the device's screen size.
         */
        const val WINDOW_POS_MODE_AUTO = 0

        /**
         * Completion window always follow the cursor
         */
        const val WINDOW_POS_MODE_FOLLOW_CURSOR_ALWAYS = 1

        /**
         * Completion window always stay at the bottom of view and occupies the
         * horizontal viewport
         */
        const val WINDOW_POS_MODE_FULL_WIDTH_ALWAYS = 2

        private const val SHOW_PROGRESS_BAR_DELAY = 50L
    }

    protected var cancelShowUp: Boolean = false

    protected var requestTime: Long = 0L

    open var maxHeight: Int = 0

    /**
     * Whether the completion window is currently positioned above the active cursor line
     * (e.g. when typing near the bottom of the editor or above the soft keyboard).
     */
    open var isPositionedAbove: Boolean = false
        protected set

    /**
     * Whether the completion list items should be reversed when the window is positioned
     * above the cursor line, placing primary matches closest to the cursor.
     */
    open var reverseListWhenAbove: Boolean
        get() = editor.props.reverseListWhenAbove
        set(value) {
            editor.props.reverseListWhenAbove = value
        }

    protected var completionThread: CompletionThread? = null

    protected var publisher: CompletionPublisher? = null

    protected var lastAttachedItems: WeakReference<List<CompletionItem>>? = null

    protected var currentSelection: Int = -1

    open var adapter: EditorCompletionAdapter = DefaultCompletionItemAdapter().also {
        it.attachValues(this, emptyList())
    }
        set(value) {
            field = value
            value.attachValues(this, value.currentItems ?: emptyList())
            if (::_layout.isInitialized) {
                runCatching { layout.getCompletionList().adapter = value }
            }
        }

    fun getItemHeight(): Int {
        if (adapter.completionWindow == null) {
            adapter.attachValues(this, adapter.currentItems ?: emptyList())
        }
        return runCatching { adapter.getItemHeight() }.getOrDefault((editor.dpUnit * 45).toInt()).coerceAtLeast(1)
    }

    protected lateinit var _layout: CompletionLayout

    val layout: CompletionLayout
        get() = _layout

    protected var eventManager: EventManager

    var completionWndPositionMode: Int = WINDOW_POS_MODE_AUTO
        set(value) {
            field = value
            updateCompletionWindowPosition()
        }

    var highlightMatchedLabel: Boolean = true

    val context: Context
        get() = editor.context

    val currentPosition: Int
        get() = currentSelection

    open val isCompletionInProgress: Boolean
        get() {
            val thread = completionThread
            return super.isShowing || requestShow > requestHide || (thread != null && thread.isAlive)
        }

    private var previousSelection: CharPosition? = null
    private var requestShow: Long = 0L
    private var requestHide: Long = -1L
    private var _enabled: Boolean = true
    private var loading: Boolean = false
    private val editorLayoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        if (isShowing) {
            updateCompletionWindowPosition(false)
        }
    }

    init {
        adapter = DefaultCompletionItemAdapter()
        setLayout(DefaultCompletionLayout())
        editor.addOnLayoutChangeListener(editorLayoutChangeListener)
        eventManager = editor.createSubEventManager()
        eventManager.subscribeEvent(ColorSchemeUpdateEvent::class.java, this::onColorSchemeUpdate)
        eventManager.subscribeEvent(ContentChangeEvent::class.java, this::onContentChange)
        eventManager.subscribeEvent(ScrollEvent::class.java, this::onEditorScroll)
        eventManager.subscribeEvent(EditorKeyEvent::class.java, this::onKeyEvent)
        eventManager.subscribeEvent(SelectionChangeEvent::class.java, this::onSelectionChange)
        eventManager.subscribeEvent(EditorReleaseEvent::class.java) { _, _ ->
            editor.removeOnLayoutChangeListener(editorLayoutChangeListener)
            isEnabled = false
        }
        subscribeEventForHide(EditorFormatEvent::class.java) { it.isSuccess }
        subscribeEventForHide(ClickEvent::class.java, null)
        subscribeEventForHide(EditorLanguageChangeEvent::class.java, null)
        subscribeEventForHide(EditorFocusChangeEvent::class.java) { !it.isGainFocus }
        subscribeEventForHide(SnippetEvent::class.java) { it.action == SnippetEvent.ACTION_SHIFT }
    }

    protected open fun <T : Event> subscribeEventForHide(
        clazz: Class<T>,
        predicate: ((T) -> Boolean)?
    ) {
        eventManager.subscribeEvent(clazz) { event, _ ->
            if (predicate == null || predicate(event)) {
                hide()
            }
        }
    }

    protected open fun onColorSchemeUpdate(
        event: ColorSchemeUpdateEvent,
        unsubscribe: Unsubscribe
    ) {
        applyColorScheme()
    }

    protected open fun onContentChange(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
        if (event.isCausedByUndoManager || !isEnabled()
            || event.action == ContentChangeEvent.ACTION_SET_NEW_TEXT
        ) {
            hide()
            return
        }
        val start = event.changeStart
        val end = event.changeEnd
        var needCompletion = false
        when (event.action) {
            ContentChangeEvent.ACTION_INSERT -> {
                if ((!editor.hasComposingText() || editor.props.autoCompletionOnComposing) && end.column != 0 && start.line == end.line) {
                    needCompletion = true
                } else {
                    hide()
                }
                updateCompletionWindowPosition(isShowing)
            }

            ContentChangeEvent.ACTION_DELETE -> {
                if (!editor.hasComposingText() && isShowing) {
                    if (start.line != end.line || start.column != end.column - 1) {
                        hide()
                    } else {
                        needCompletion = true
                        updateCompletionWindowPosition()
                    }
                }
            }
        }
        if (needCompletion) {
            requireCompletion()
        }
    }

    protected open fun onSelectionChange(event: SelectionChangeEvent, unsubscribe: Unsubscribe) {
        if (event.isSelected || event.cause == SelectionChangeEvent.CAUSE_IME
            || event.cause == SelectionChangeEvent.CAUSE_SELECTION_HANDLE || event.cause == SelectionChangeEvent.CAUSE_TAP
            || event.cause == SelectionChangeEvent.CAUSE_SEARCH || event.cause == SelectionChangeEvent.CAUSE_UNKNOWN
        ) {
            hide()
            return
        }
        val prev = previousSelection
        if (prev == null) {
            previousSelection = event.left.fromThis()
            return
        }
        if (event.cause == SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE) {
            if (prev.line != event.left.line) {
                hide()
                return
            }
            if (isShowing && abs(prev.column - event.left.column) <= 1) {
                if (event.left.column > 0) {
                    requireCompletion()
                } else {
                    hide()
                }
            }
        }
    }

    protected open fun onEditorScroll(event: ScrollEvent, unsubscribe: Unsubscribe) {
        if (event.cause == ScrollEvent.CAUSE_USER_DRAG) {
            updateCompletionWindowPosition(false)
        } else if (event.cause == ScrollEvent.CAUSE_USER_FLING) {
            val minVe = editor.dpUnit * 2000
            val velocityX = event.flingVelocityX
            val velocityY = event.flingVelocityY
            if (abs(velocityX) >= minVe || abs(velocityY) >= minVe) {
                hide()
            }
        }
    }

    protected open fun onKeyEvent(event: EditorKeyEvent, unsubscribe: Unsubscribe) {
        if (event.eventType == EditorKeyEvent.Type.DOWN && !event.isAltPressed
            && !event.isCtrlPressed && !event.isShiftPressed && isShowing
        ) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    moveUp()
                    event.markAsConsumed()
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    moveDown()
                    event.markAsConsumed()
                }

                KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_PAGE_UP -> hide()
                KeyEvent.KEYCODE_TAB -> {
                    if (currentSelection == -1) {
                        moveDown()
                    }
                    if (select()) {
                        event.markAsConsumed()
                    }
                }

                KeyEvent.KEYCODE_ENTER -> {
                    if (currentSelection == -1 && editor.props.selectCompletionItemOnEnterForSoftKbd) {
                        moveDown()
                    }
                    if (select()) {
                        event.markAsConsumed()
                    }
                }
            }
        }
    }

    open fun updateCompletionWindowPosition() {
        updateCompletionWindowPosition(true, -1)
    }

    open fun updateCompletionWindowPosition(scrollEditor: Boolean) {
        updateCompletionWindowPosition(scrollEditor, -1)
    }

    open fun updateCompletionWindowPosition(scrollEditor: Boolean, expectedHeight: Int) {
        val dp = editor.dpUnit
        val cursor = editor.cursor
        val rowHeight = editor.rowHeight
        val rightLayoutOffset =
            editor.layout.getCharLayoutOffset(cursor.rightLine, cursor.rightColumn)

        val cursorRowBottom = rightLayoutOffset[0] - editor.offsetY
        val cursorRowTop = cursorRowBottom - rowHeight
        val margin = dp * 2f

        val effectiveEditorHeight = editor.height - (if (parentView === editor) editor.currentImeBottomInset else 0)
        val spaceBelow = effectiveEditorHeight - (cursorRowBottom + margin)
        val spaceAbove = cursorRowTop - margin
        val targetMaxHeight = dp * 200
        val minThreshold = dp * 100

        val canScrollDown = scrollEditor && (editor.offsetY + rowHeight <= editor.scrollMaxY)
        val shouldShowAbove = if (spaceBelow >= minThreshold) {
            false
        } else if (spaceAbove >= minThreshold) {
            true
        } else if (canScrollDown && (spaceBelow + (editor.scrollMaxY - editor.offsetY) >= minThreshold)) {
            false
        } else {
            spaceAbove > spaceBelow
        }

        val wasAbove = isPositionedAbove
        isPositionedAbove = shouldShowAbove
        if (::_layout.isInitialized) {
            layout.onPositionChanged(shouldShowAbove)
        }

        if (wasAbove != shouldShowAbove && reverseListWhenAbove) {
            val pub = publisher
            if (pub != null && pub.hasData()) {
                val rawItems = pub.items
                val displayItems = if (shouldShowAbove) rawItems.asReversed() else rawItems
                adapter.attachValues(this, displayItems)
                adapter.notifyDataSetChanged()
            }
        }

        val width =
            if ((editor.width < 500 * dp && completionWndPositionMode == WINDOW_POS_MODE_AUTO) || completionWndPositionMode == WINDOW_POS_MODE_FULL_WIDTH_ALWAYS) {
                (editor.width * 7 / 8)
            } else {
                min(300 * dp, editor.width / 2f).toInt()
            }
        var panelX =
            if ((editor.width < 500 * dp && completionWndPositionMode == WINDOW_POS_MODE_AUTO) || completionWndPositionMode == WINDOW_POS_MODE_FULL_WIDTH_ALWAYS) {
                editor.width / 8f / 2f
            } else {
                editor.updateCursorAnchor() + dp * 20
            }
        if (panelX + width > editor.width) {
            panelX = (editor.width - width).toFloat().coerceAtLeast(0f)
        }

        val h: Int
        val panelY: Float

        if (shouldShowAbove) {
            isPositionedAbove = true
            maxHeight = min(targetMaxHeight, spaceAbove.coerceAtLeast(0f)).toInt()
            h = when {
                expectedHeight > 0 -> min(expectedHeight, maxHeight)
                height > 0 -> min(height, maxHeight)
                adapter.count > 0 -> min(getItemHeight() * adapter.count, maxHeight)
                else -> maxHeight
            }
            panelY = (cursorRowTop - margin - h).coerceAtLeast(0f)
        } else {
            isPositionedAbove = false
            var candidatePanelY = cursorRowBottom + margin
            var restY = effectiveEditorHeight - candidatePanelY
            if (restY > targetMaxHeight) {
                restY = targetMaxHeight
            } else if (restY < minThreshold && scrollEditor) {
                var offset = 0f
                while (restY < minThreshold && editor.offsetY + offset + rowHeight <= editor.scrollMaxY) {
                    restY += rowHeight
                    candidatePanelY -= rowHeight
                    offset += rowHeight
                }
                if (offset > 0f) {
                    editor.scroller.startScroll(editor.offsetX, editor.offsetY, 0, offset.toInt(), 0)
                }
            }
            maxHeight = restY.coerceAtLeast(0f).toInt()
            h = when {
                expectedHeight > 0 -> min(expectedHeight, maxHeight)
                height > 0 -> min(height, maxHeight)
                adapter.count > 0 -> min(getItemHeight() * adapter.count, maxHeight)
                else -> maxHeight
            }
            panelY = candidatePanelY
        }

        setLocation(panelX.toInt() + editor.offsetX, panelY.toInt() + editor.offsetY)
        super.setSize(width, h)
    }

    override fun setSize(width: Int, height: Int) {
        if (isPositionedAbove && this.height != height && height > 0) {
            val dp = editor.dpUnit
            val margin = dp * 2f
            val cursor = editor.cursor
            val rightLayoutOffset =
                editor.layout.getCharLayoutOffset(cursor.rightLine, cursor.rightColumn)
            val cursorRowBottom = rightLayoutOffset[0] - editor.offsetY
            val cursorRowTop = cursorRowBottom - editor.rowHeight
            val panelY = (cursorRowTop - margin - height).coerceAtLeast(0f)
            setLocation(windowX, panelY.toInt() + editor.offsetY)
        }
        super.setSize(width, height)
    }

    override fun onImeAnimationProgress(imeBottom: Int) {
        if (isShowing) {
            updateCompletionWindowPosition(false)
        }
    }

    open fun setLayout(layout: CompletionLayout) {
        this._layout = layout
        layout.setEditorCompletion(this)
        setContentView(layout.inflate(editor.context))
        applyColorScheme()
        layout.getCompletionList().adapter = adapter
    }

    override fun isEnabled(): Boolean = _enabled

    override fun setEnabled(enabled: Boolean) {
        if (_enabled != enabled) {
            _enabled = enabled
            eventManager.isEnabled = enabled
            if (!enabled) {
                cancelCompletion()
                hide()
            }
            editor.dispatchEvent(EditorPropertyChangeEvent(editor, "autoCompletionEnabled", enabled))
        }
    }

    open fun setEnabledAnimation(enabledAnimation: Boolean) {
        layout.setEnabledAnimation(enabledAnimation)
    }

    override fun show() {
        if (cancelShowUp || !isEnabled()) {
            return
        }
        requestShow = System.currentTimeMillis()
        val requireRequest = requestTime
        editor.postDelayedInLifecycle({
            if (requestHide < requestShow && requestTime == requireRequest) {
                super.show()
            }
        }, 70)
    }

    open fun hide() {
        super.dismiss()
        cancelCompletion()
        requestHide = System.currentTimeMillis()
    }

    open fun applyColorScheme() {
        val colors = editor.colorScheme
        layout.onApplyColorScheme(colors)
    }

    open fun setLoading(state: Boolean) {
        loading = state
        if (state) {
            editor.postDelayedInLifecycle({
                if (loading) {
                    layout.setLoading(true)
                }
            }, SHOW_PROGRESS_BAR_DELAY)
        } else {
            layout.setLoading(false)
        }
    }

    open fun moveDown() {
        val count = adapter.count
        if (currentSelection + 1 >= count) {
            return
        }
        currentSelection++
        adapter.notifyDataSetChanged()
        ensurePosition()
    }

    open fun moveUp() {
        if (currentSelection - 1 < 0) {
            return
        }
        currentSelection--
        adapter.notifyDataSetChanged()
        ensurePosition()
    }

    private fun ensurePosition() {
        if (currentSelection != -1) {
            layout.ensureListPositionVisible(currentSelection, getItemHeight())
        }
    }

    open fun select(): Boolean = select(currentSelection)

    open fun select(pos: Int): Boolean {
        if (pos == -1) {
            return false
        }
        val adpView = layout.getCompletionList()
        val item = (adpView.adapter as? EditorCompletionAdapter)?.getItem(pos)
            ?: runCatching { adapter.getItem(pos) }.getOrNull()
            ?: return false
        return select(item)
    }

    open fun select(item: CompletionItem): Boolean {
        val cursor = editor.cursor
        val currentCompletionThread = this.completionThread
        if (!cursor.isSelected && currentCompletionThread != null) {
            cancelShowUp = true
            editor.beginComposingTextRejection()
            editor.text.beginBatchEdit()
            editor.restartInput()
            try {
                item.performCompletion(editor, editor.text, currentCompletionThread.requestPosition)
                editor.updateCursor()
            } finally {
                editor.text.endBatchEdit()
                editor.endComposingTextRejection()
                cancelShowUp = false
            }
            editor.restartInput()
        }
        hide()
        return true
    }

    open fun cancelCompletion() {
        val previous = completionThread
        if (previous != null && previous.isAlive) {
            previous.cancel()
            previous.requestTimestamp = -1L
        }
        completionThread = null
    }

    open fun checkNoCompletion(): Boolean {
        val pos = editor.cursor.left()
        val styles = editor.styles
        return StylesUtils.checkNoCompletion(styles, pos)
    }

    open fun requireCompletion() {
        if (cancelShowUp || !isEnabled()) {
            return
        }
        val text = editor.text
        if (text.cursor.isSelected || checkNoCompletion()) {
            hide()
            return
        }
        if (System.nanoTime() - requestTime < editor.props.cancelCompletionNs) {
            hide()
            requestTime = System.nanoTime()
            return
        }
        cancelCompletion()
        requestTime = System.nanoTime()
        currentSelection = -1
        val handler = editor.handler ?: android.os.Handler(android.os.Looper.getMainLooper())
        publisher = CompletionPublisher(handler, {
            val pub = publisher ?: return@CompletionPublisher
            val rawItems = pub.items
            if (highlightMatchedLabel) {
                rawItems.highlightMatchLabel(editor.colorScheme)
            }
            val extraHeight = if (::_layout.isInitialized) layout.getExtraHeight() else 0
            val estimatedHeight = getItemHeight() * rawItems.size + extraHeight
            updateCompletionWindowPosition(true, estimatedHeight)

            val displayItems = if (isPositionedAbove && reverseListWhenAbove) {
                rawItems.asReversed()
            } else {
                rawItems
            }
            if (lastAttachedItems == null || lastAttachedItems?.get() !== displayItems) {
                adapter.attachValues(this, displayItems)
                adapter.notifyDataSetInvalidated()
                lastAttachedItems = WeakReference(displayItems)
            } else {
                adapter.notifyDataSetChanged()
            }
            if (editor.props.moveSelectionToFirstForKeyboard &&
                KeyboardUtils.isHardKeyboardConnected(context) &&
                currentSelection == -1
            ) {
                moveDown()
            }
            val newHeight = getItemHeight() * adapter.count + extraHeight
            if (newHeight == 0) {
                hide()
            }
            setSize(width, min(newHeight, maxHeight))
            resetScrollPosition()
            if (!isShowing) {
                show()
            }
        }, editor.editorLanguage.interruptionLevel)
        val thread = CompletionThread(requestTime, publisher!!)
        completionThread = thread
        setLoading(true)
        thread.start()
    }

    /**
     * Scroll the completion list back to its first item.
     *
     * The adapter is set again so that rows of the previous result are dropped. Otherwise,
     * if the list is resized before its next layout (window height, loading bar, layout
     * animations), [android.widget.AbsListView] restores the old first visible row.
     */
    open fun resetScrollPosition() {
        val list = layout.getCompletionList()
        if (list.firstVisiblePosition > 0 || (list.childCount > 0
                && list.getChildAt(0).top < list.paddingTop)) {
            list.adapter = adapter
        }
    }

    /**
     * Auto-completion Analyzing thread
     *
     * @author Rosemoe
     */
    open inner class CompletionThread(
        requestTime: Long,
        private val localPublisher: CompletionPublisher
    ) : Thread(), TextReference.Validator {

        private val extraData: Bundle = editor.extraArguments
        val requestPosition: CharPosition = editor.cursor.left()
        private val targetLanguage: Language = editor.editorLanguage
        private val contentRef: ContentReference = ContentReference(editor.text).also {
            it.setValidator(this)
        }
        var requestTimestamp: Long = requestTime
        private var aborted: Boolean = false

        open val isCancelled: Boolean
            get() = aborted

        open fun cancel() {
            aborted = true
            val level = targetLanguage.interruptionLevel
            if (level == Language.INTERRUPTION_LEVEL_STRONG) {
                interrupt()
            }
            localPublisher.cancel()
        }

        override fun validate() {
            if (this@EditorAutoCompletion.requestTime != requestTimestamp || aborted) {
                throw CompletionCancelledException()
            }
        }

        override fun run() {
            try {
                targetLanguage.requireAutoComplete(
                    contentRef,
                    requestPosition,
                    localPublisher,
                    extraData
                )
                if (localPublisher.hasData()) {
                    if (this@EditorAutoCompletion.completionThread === currentThread()) {
                        localPublisher.updateList(true)
                    }
                } else {
                    editor.postInLifecycle { hide() }
                }
                editor.postInLifecycle { setLoading(false) }
            } catch (e: Exception) {
                if (e is CompletionCancelledException) {
                    Log.v("CompletionThread", "Completion is cancelled")
                } else {
                    Log.e("CompletionThread", "Completion failed", e)
                }
            }
        }
    }
}
