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
package io.github.rosemoe.sora.widget.compose

import android.content.ContextWrapper
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.base.findLifecycleOwner
import io.github.rosemoe.sora.widget.base.findSavedStateRegistryOwner
import io.github.rosemoe.sora.widget.base.findViewModelStoreOwner
import io.github.rosemoe.sora.widget.component.DiagnosticTooltipLayout
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlin.math.max
import kotlin.math.min

internal fun View.attachViewTreeOwnersFrom(anchor: View?) {
    val ctx = this.context
    val lifecycleOwner = (anchor?.findViewTreeLifecycleOwner())
        ?: (anchor?.context?.findLifecycleOwner())
        ?: (ctx.findLifecycleOwner())

    val viewModelStoreOwner = (anchor?.findViewTreeViewModelStoreOwner())
        ?: (anchor?.context?.findViewModelStoreOwner())
        ?: (ctx.findViewModelStoreOwner())

    val savedStateOwner = (anchor?.findViewTreeSavedStateRegistryOwner())
        ?: (anchor?.context?.findSavedStateRegistryOwner())
        ?: (ctx.findSavedStateRegistryOwner())

    fun applyOwners(target: View) {
        if (lifecycleOwner != null && target.findViewTreeLifecycleOwner() == null) {
            target.setViewTreeLifecycleOwner(lifecycleOwner)
        }
        if (viewModelStoreOwner != null && target.findViewTreeViewModelStoreOwner() == null) {
            target.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        }
        if (savedStateOwner != null && target.findViewTreeSavedStateRegistryOwner() == null) {
            target.setViewTreeSavedStateRegistryOwner(savedStateOwner)
        }
    }

    applyOwners(this)
    val root = this.rootView
    if (root != null && root !== this) {
        applyOwners(root)
    }

    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            applyOwners(v)
            val currentRoot = v.rootView
            if (currentRoot != null && currentRoot !== v) {
                applyOwners(currentRoot)
            }
        }

        override fun onViewDetachedFromWindow(v: View) {}
    })
}

/**
 * Adapter bridging Compose-rendered items to [io.github.rosemoe.sora.widget.component.EditorAutoCompletion].
 *
 * Allows developers to supply an arbitrary `@Composable` lambda for each auto-completion item,
 * receiving item data, selection highlight state, and an on-click selection trigger.
 */
class ComposeCompletionItemAdapter(
    private val itemHeightDp: Float = 45f,
    private val parentCompositionContext: CompositionContext? = null,
    itemContent: @Composable (item: CompletionItem, isSelected: Boolean, onClick: () -> Unit) -> Unit
) : EditorCompletionAdapter() {

    private var itemContentState by mutableStateOf(itemContent)
    var itemContent: @Composable (item: CompletionItem, isSelected: Boolean, onClick: () -> Unit) -> Unit
        get() = itemContentState
        set(value) {
            itemContentState = value
        }

    override fun getItemHeight(): Int {
        val density = runCatching { getContext().resources.displayMetrics.density }.getOrDefault(1f)
        return (itemHeightDp * density).toInt()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?,
        isCurrentCursorPosition: Boolean
    ): View {
        val context = runCatching { getContext() }.getOrNull()
            ?: completionWindow?.editor?.context
            ?: parent?.context
            ?: convertView?.context
            ?: throw IllegalStateException("Cannot resolve Context for ComposeCompletionItemAdapter")

        val composeView = (convertView as? ComposeView) ?: ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            if (parentCompositionContext != null) {
                setParentCompositionContext(parentCompositionContext)
            }
        }
        val editor = completionWindow?.editor ?: (parent as? View)
        composeView.attachViewTreeOwnersFrom(editor)
        if (parentCompositionContext != null) {
            composeView.setParentCompositionContext(parentCompositionContext)
        }
        val item = if (position in 0 until count) getItem(position) else null
        composeView.setContent {
            if (item != null) {
                itemContentState(item, isCurrentCursorPosition) {
                    completionWindow?.select(position)
                }
            }
        }
        return composeView
    }
}

/**
 * Scope providing contextual actions and metadata for text selection popups in Compose.
 */
interface TextActionScope {
    /** The hosting editor instance. */
    val editor: CodeEditor

    /** The currently selected text substring, or empty string if no selection is active. */
    val selectedText: String

    /** True if text is currently highlighted / selected in the editor. */
    val hasSelection: Boolean

    /** True if the current selection can be cut. */
    val canCut: Boolean

    /** True if the current selection can be copied. */
    val canCopy: Boolean

    /** True if clipboard content can be pasted into the editor. */
    val canPaste: Boolean

    /** True if editor content is available to select all. */
    val canSelectAll: Boolean

    /** Cut the selected text to clipboard. */
    fun cut()

    /** Copy the selected text to clipboard. */
    fun copy()

    /** Paste clipboard text into current selection/cursor position. */
    fun paste()

    /** Select all content in the editor. */
    fun selectAll()

    /** Dismiss the selection action window. */
    fun dismiss()
}

/**
 * Compose-native implementation of [EditorTextActionWindow].
 *
 * Replaces the traditional Android view hierarchy with a [ComposeView] displaying
 * arbitrary `@Composable` content inside [TextActionScope].
 */
class ComposeTextActionWindow(
    editor: CodeEditor,
    private val parentCompositionContext: CompositionContext? = null,
    actionContent: @Composable (TextActionScope) -> Unit
) : EditorTextActionWindow(editor) {

    private var actionContentState by mutableStateOf(actionContent)
    var actionContent: @Composable (TextActionScope) -> Unit
        get() = actionContentState
        set(value) {
            actionContentState = value
        }

    private var lastMeasuredW: Int = (editor.dpUnit * 220).toInt().coerceAtLeast(100)
    private var lastMeasuredH: Int = (editor.dpUnit * 48).toInt().coerceAtLeast(40)

    private val composeView: ComposeView = ComposeView(editor.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        if (parentCompositionContext != null) {
            setParentCompositionContext(parentCompositionContext)
        }
        attachViewTreeOwnersFrom(editor)
    }

    private var scopeState by mutableStateOf<TextActionScope>(createScope())

    init {
        rootView = composeView
        setContentView(composeView)
        setSize(lastMeasuredW, lastMeasuredH)

        composeView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val w = right - left
            val h = bottom - top
            if (w > 0 && h > 0 && (w != lastMeasuredW || h != lastMeasuredH)) {
                lastMeasuredW = w
                lastMeasuredH = h
                if (isShowing) {
                    repositionWindow(w, h)
                }
            }
        }

        composeView.setContent {
            actionContentState(scopeState)
        }
    }

    private fun createScope(): TextActionScope = object : TextActionScope {
        override val editor: CodeEditor get() = this@ComposeTextActionWindow.editor
        override val selectedText: String
            get() {
                val cursor = editor.cursor
                return if (cursor.isSelected && cursor.left >= 0 && cursor.right <= editor.text.length) {
                    editor.text.substring(cursor.left, cursor.right)
                } else {
                    ""
                }
            }
        override val hasSelection: Boolean get() = editor.cursor.isSelected
        override val canCut: Boolean get() = editor.cursor.isSelected && editor.isEditable
        override val canCopy: Boolean get() = editor.cursor.isSelected
        override val canPaste: Boolean get() = editor.isEditable && editor.hasClip()
        override val canSelectAll: Boolean get() = editor.text.isNotEmpty()

        override fun cut() {
            if (editor.cursor.isSelected && editor.isEditable) {
                editor.cutText()
            }
            dismiss()
        }

        override fun copy() {
            if (editor.cursor.isSelected) {
                editor.copyText()
                editor.setSelection(editor.cursor.rightLine, editor.cursor.rightColumn)
            }
            dismiss()
        }

        override fun paste() {
            if (editor.isEditable) {
                editor.pasteText()
                editor.setSelection(editor.cursor.rightLine, editor.cursor.rightColumn)
            }
            dismiss()
        }

        override fun selectAll() {
            editor.selectAll()
            scopeState = createScope()
        }

        override fun dismiss() {
            this@ComposeTextActionWindow.dismiss()
        }
    }

    override fun applyColorScheme() {
        // Compose manages its own styling/theming internally
    }

    protected fun repositionWindow(w: Int, h: Int) {
        setSize(w, h)
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
        val maxBoundedTop = max(0, editor.height - h - 5)
        val boundedTop = max(0, min(top, maxBoundedTop))

        val handleLeftX = editor.getOffset(cursor.leftLine, cursor.leftColumn)
        val handleRightX = editor.getOffset(cursor.rightLine, cursor.rightColumn)
        val panelX = ((handleLeftX + handleRightX) / 2f - w / 2f).toInt()
        val maxBoundedX = max(0, editor.width - w)
        val boundedX = max(0, min(panelX, maxBoundedX))

        setLocationAbsolutely(boundedX, boundedTop)
    }

    override fun displayWindow() {
        if (!isEnabled() || editor.snippetController.isInSnippet() || editor.isInMouseMode) {
            dismiss()
            return
        }

        if (!editor.hasFocus() && editor.cursor.isSelected) {
            editor.requestFocus()
        }

        scopeState = createScope()
        try {
            val maxW = if (editor.width > 0) editor.width else (editor.dpUnit * 300).toInt()
            val maxH = if (editor.height > 0) editor.height else (editor.dpUnit * 200).toInt()
            composeView.measure(
                View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST)
            )
            if (composeView.measuredWidth > 0 && composeView.measuredHeight > 0) {
                lastMeasuredW = composeView.measuredWidth
                lastMeasuredH = composeView.measuredHeight
            }
        } catch (_: Throwable) {
            // Headless unit test fallback
        }

        repositionWindow(lastMeasuredW, lastMeasuredH)
        show()
    }

    override fun show() {
        if (!isEnabled() || editor.snippetController.isInSnippet() || editor.isInMouseMode) {
            return
        }
        if (!editor.hasFocus() && editor.cursor.isSelected) {
            editor.requestFocus()
        }
        if (isShowing) {
            return
        }
        try {
            applyWindowAttributes(true)
        } catch (_: Throwable) {
            // E.g. BadTokenException if window is detaching or in headless environment
        }
    }
}

/**
 * Scope providing contextual diagnostic metadata for diagnostic tooltips in Compose.
 */
interface DiagnosticTooltipScope {
    /** The hosting editor instance. */
    val editor: CodeEditor

    /** The diagnostic detail to present. */
    val diagnostic: DiagnosticDetail?

    /** The associated source code region for the diagnostic. */
    val region: DiagnosticRegion?

    /** Dismiss the diagnostic tooltip window. */
    fun dismiss()
}

/**
 * Compose-native implementation of [DiagnosticTooltipLayout].
 *
 * Replaces XML layout inflation with a [ComposeView] displaying arbitrary `@Composable` content
 * inside [DiagnosticTooltipScope].
 */
class ComposeDiagnosticTooltipLayout(
    private val parentCompositionContext: CompositionContext? = null,
    tooltipContent: @Composable (DiagnosticTooltipScope) -> Unit
) : DiagnosticTooltipLayout {

    private var hostWindow: EditorDiagnosticTooltipWindow? = null
    private var composeView: ComposeView? = null
    private var currentDiagnostic by mutableStateOf<DiagnosticDetail?>(null)
    private var currentRegion by mutableStateOf<DiagnosticRegion?>(null)
    private var tooltipContentState by mutableStateOf(tooltipContent)

    var tooltipContent: @Composable (DiagnosticTooltipScope) -> Unit
        get() = tooltipContentState
        set(value) {
            tooltipContentState = value
        }

    override fun attach(window: EditorDiagnosticTooltipWindow) {
        this.hostWindow = window
    }

    override fun createView(inflater: LayoutInflater): View {
        val view = ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            if (parentCompositionContext != null) {
                setParentCompositionContext(parentCompositionContext)
            }
            attachViewTreeOwnersFrom(hostWindow?.editor)
            setContent {
                val scope = object : DiagnosticTooltipScope {
                    override val editor: CodeEditor get() = hostWindow!!.editor
                    override val diagnostic: DiagnosticDetail? get() = currentDiagnostic
                    override val region: DiagnosticRegion? get() = currentRegion
                    override fun dismiss() {
                        hostWindow?.dismiss()
                    }
                }
                tooltipContentState(scope)
            }
        }
        composeView = view
        return view
    }

    override fun applyColorScheme(colorScheme: EditorColorScheme) {
        // Handled by Compose
    }

    override fun renderDiagnostic(diagnostic: DiagnosticDetail?) {
        renderDiagnostic(diagnostic, null)
    }

    override fun renderDiagnostic(diagnostic: DiagnosticDetail?, region: DiagnosticRegion?) {
        currentDiagnostic = diagnostic
        currentRegion = region
    }

    override fun measureContent(maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        val view = composeView ?: return Pair(0, 0)
        return try {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
            )
            Pair(view.measuredWidth, view.measuredHeight)
        } catch (_: IllegalStateException) {
            Pair(maxWidth, maxHeight)
        }
    }

    override fun isPointerOverPopup(): Boolean = false

    override fun isMenuShowing(): Boolean = false

    override fun onWindowDismissed() {
        currentDiagnostic = null
        currentRegion = null
    }
}
