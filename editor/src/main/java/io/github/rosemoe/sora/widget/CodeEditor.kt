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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint.Align
import android.graphics.Paint.FontMetricsInt
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.TransactionTooLargeException
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.ActionMode
import android.view.ContextMenu
import android.view.ContextThemeWrapper
import android.view.DragEvent
import android.view.GestureDetector
import android.view.InputDevice
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.PointerIcon
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.EdgeEffect
import android.widget.EditText
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import android.widget.Toast
import androidx.annotation.Px
import androidx.annotation.UiThread
import androidx.collection.MutableIntSet
import androidx.collection.MutableLongLongMap
import io.github.rosemoe.sora.I18nConfig.getResourceId
import io.github.rosemoe.sora.I18nConfig.getString
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage
import io.github.rosemoe.sora.event.BuildEditorInfoEvent
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.CreateContextMenuEvent
import io.github.rosemoe.sora.event.EditorAttachStateChangeEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.event.EditorFormatEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.Event
import io.github.rosemoe.sora.event.EventManager
import io.github.rosemoe.sora.event.EventManager.NoUnsubscribeReceiver
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.HoverEvent
import io.github.rosemoe.sora.event.LayoutStateChangeEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.graphics.inlayHint.InlayHintRenderer
import io.github.rosemoe.sora.graphics.inlayHint.InlayHintRendererProvider
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticProvider
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.format.Formatter.FormatResultReceiver
import io.github.rosemoe.sora.lang.format.FormatterProvider
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.ExtraStylesProvider
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer.HighlightText
import io.github.rosemoe.sora.lang.styling.HighlightTextProvider
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintProvider
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.lang.styling.inlayHint.IntSetUpdateRange
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.text.Cursor
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.text.TextUtils.countLeadingSpacesAndTabs
import io.github.rosemoe.sora.text.TextUtils.createIndent
import io.github.rosemoe.sora.text.TextUtils.getOffsetForBackspaceKey
import io.github.rosemoe.sora.text.TextUtils.trimToSize
import io.github.rosemoe.sora.text.method.KeyMetaStates
import io.github.rosemoe.sora.util.Chars.getWordRange
import io.github.rosemoe.sora.util.ClipDataUtils.clipDataToString
import io.github.rosemoe.sora.util.EditorHandler
import io.github.rosemoe.sora.util.Floats.withinDelta
import io.github.rosemoe.sora.util.IntPair.getFirst
import io.github.rosemoe.sora.util.IntPair.getSecond
import io.github.rosemoe.sora.util.IntPair.pack
import io.github.rosemoe.sora.util.KeyboardUtils.isHardKeyboardConnected
import io.github.rosemoe.sora.util.Logger.Companion.instance
import io.github.rosemoe.sora.util.LongArrayList
import io.github.rosemoe.sora.util.Numbers.clearBits
import io.github.rosemoe.sora.util.TemporaryFloatBuffer.obtain
import io.github.rosemoe.sora.util.TemporaryFloatBuffer.recycle
import io.github.rosemoe.sora.util.ThemeUtils
import io.github.rosemoe.sora.util.ViewUtils.getVerticalScrollFactor
import io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions
import io.github.rosemoe.sora.widget.SelectionMovement.MovingBasePosition
import io.github.rosemoe.sora.widget.SymbolPairMatch.SymbolPair
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorBuiltinComponent
import io.github.rosemoe.sora.widget.component.EditorContextMenuCreator
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.internal.util.SpansUtils.defaultLineSpans
import io.github.rosemoe.sora.widget.internal.util.SpansUtils.getSpansOnLine
import io.github.rosemoe.sora.widget.layout.Layout
import io.github.rosemoe.sora.widget.layout.Layout.VisualLocation
import io.github.rosemoe.sora.widget.layout.LineBreakLayout
import io.github.rosemoe.sora.widget.layout.ViewMeasureHelper
import io.github.rosemoe.sora.widget.layout.WordwrapLayout
import io.github.rosemoe.sora.widget.rendering.RenderContext
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.Companion.getDefault
import io.github.rosemoe.sora.widget.snippet.SnippetController
import io.github.rosemoe.sora.widget.style.CursorAnimator
import io.github.rosemoe.sora.widget.style.DiagnosticIndicatorStyle
import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode
import io.github.rosemoe.sora.widget.style.LineNumberTipTextProvider
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle.HandleDescriptor
import io.github.rosemoe.sora.widget.style.builtin.DefaultLineNumberTip
import io.github.rosemoe.sora.widget.style.builtin.HandleStyleSideDrop
import io.github.rosemoe.sora.widget.style.builtin.MoveCursorAnimator
import java.util.Objects
import java.util.Random
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * CodeEditor is an editor that can highlight text regions by doing basic syntax analyzing
 * This project in [GitHub](https://github.com/Rosemoe/sora-editor)
 * 
 * 
 * Note:
 * Row and line are different in this editor
 * When we say 'row', it means a line displayed on screen. It can be a part of a line in the text object.
 * When we say 'line', it means a real line in the original text.
 * 
 * @author Rosemoe
 */
@Suppress("unused")
open class CodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.codeEditorStyle,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), ContentListener, FormatResultReceiver,
    InlayHintRendererProvider {
    val keyEventHandler: EditorKeyEventHandler = EditorKeyEventHandler(this)
    var languageSymbolPairs: SymbolPairMatch? = null
    var textActionWindow: EditorTextActionWindow? = null
    var diagnosticTooltip: EditorDiagnosticTooltipWindow? = null
    var contextMenuCreator: EditorContextMenuCreator? = null

    protected var _styleDelegate: EditorStyleDelegate? = null
    open val styleDelegate: EditorStyleDelegate
        get() = _styleDelegate ?: error("StyleDelegate not initialized")

    val extraStylesProviders: MutableList<ExtraStylesProvider> = ArrayList<ExtraStylesProvider>()
    private val inlayHintProviders: MutableList<InlayHintProvider> = ArrayList<InlayHintProvider>()
    private val diagnosticProviders: MutableList<DiagnosticProvider> =
        ArrayList<DiagnosticProvider>()
    private val highlightTextProviders: MutableList<HighlightTextProvider> =
        ArrayList<HighlightTextProvider>()
    var startedActionMode: Int = 0
    var selectionAnchor: CharPosition? = null
    protected var _inputConnection: EditorInputConnection? = null
    open var inputConnection: EditorInputConnection
        get() = _inputConnection ?: error("inputConnection not initialized")
        set(value) {
            _inputConnection = value
        }
    open var eventManager: EventManager = EventManager()

    protected var _layout: Layout? = null

    /**
     * Get the layout of editor
     */
    @get:UnsupportedUserUsage
    open var layout: Layout
        get() = _layout ?: error("Layout not initialized")
        set(value) {
            _layout = value
        }

    var tabWidth: Int = 4
        set(width) {
            require(width >= 1) { "width can not be under 1" }
            field = width
            _renderContext?.invalidateRenderNodes()
            _renderer?.updateTimestamp()
            requestLayoutIfNeeded()
            invalidate()
        }

    /**
     * @see .findCursorBlock
     */
    /**
     * Get cursor code block index
     * 
     * @return index of cursor's code block
     */
    var blockIndex: Int = -1
    open val currentCursorBlock: Int
        get() = blockIndex

    private var downX = 0
    private var inputType = 0
    private var nonPrintableOptions = 0
    private var completionWndPosMode = 0
    private var availableFloatArrayRegion: Long = 0

    /**
     * Get 1dp = ?px
     * 
     * @return 1dp in pixel
     */
    var dpUnit: Float = 0f
        private set

    var dividerWidth: Float = 0f
        set(@Px dividerWidth) {
            require(dividerWidth >= 0) { "width can not be under zero" }
            field = dividerWidth
            requestLayoutIfNeeded()
            invalidate()
        }

    /**
     * @return Margin left of divider line
     * @see setDividerMargin
     */
    @get:Px
    var dividerMarginLeft: Float = 0f
        private set

    /**
     * @return Margin right of divider line
     * @see setDividerMargin
     */
    @get:Px
    var dividerMarginRight: Float = 0f
        private set

    @get:Px
    var insertSelectionWidth: Float = 0f
        private set

    var blockLineWidth: Float = 1f
        set(dp) {
            field = dp
            invalidate()
        }

    @get:Px
    var textBorderWidth: Float = 0f
        set(@Px width) {
            require(width >= 0) { "width can not be under zero" }
            field = width
            invalidate()
        }
    private var verticalScrollFactor = 0f

    /**
     * @see .setLineInfoTextSize
     */
    var lineInfoTextSize: Float = 0f
        /**
         * Set text size for line info panel
         * 
         * @param size Text size for line information, **unit is SP**
         */
        set(size) {
            require(!(size <= 0))
            field = size
        }
    private var lineSpacingMultiplier = 1f
    private var lineSpacingAdd = 0f
    private var lineNumberMarginLeft = 0f
    private var verticalExtraSpaceFactor = 0.5f
    private var waitForNextChange = false
    /**
     * @return Whether allow scaling
     * @see CodeEditor.setScalable
     */
    /**
     * Allow scale text size by thumb
     * 
     * @param scale Whether allow
     */
    var isScalable: Boolean = false
    private var editable = false
    private var wordwrap = false

    val isEditable: Boolean
        get() = editable && !layoutBusy && !this.isFormatting

    var isWordwrap: Boolean
        get() = wordwrap
        set(value) {
            setWordwrap(value, true)
        }

    private var undoEnabled = false
    private var mouseHover = false
    private var mouseButtonPressed = false
    private var lastAnchorIsSelLeft = false

    @Volatile
    private var layoutBusy = false

    /**
     * @return Enabled / disabled
     * @see CodeEditor.setDisplayLnPanel
     */
    var isDisplayLnPanel: Boolean = false
        /**
         * Whether display the line number panel beside vertical scroll bar
         * when the scroll bar is touched by user
         * 
         * @param displayLnPanel Enabled / disabled
         */
        set(displayLnPanel) {
            field = displayLnPanel
            invalidate()
        }

    /**
     * @return position
     * @see CodeEditor.setLnPanelPosition
     */
    var lnPanelPosition: Int = 0
        /**
         * Set display position the line number panel beside vertical scroll bar <br></br>
         * Only TOP,CENTER and BOTTOM will be effective when position mode is follow.
         * 
         * @param position default TOP|RIGHT
         * @see LineInfoPanelPosition
         */
        set(position) {
            field = position
            invalidate()
        }

    /**
     * @return LineInfoPanelPosition.FOLLOW or LineInfoPanelPosition.FIXED
     * @see CodeEditor.setLnPanelPosition
     */
    var lnPanelPositionMode: Int = 0
        /**
         * Set display position mode the line number panel beside vertical scroll bar
         * 
         * @param mode Default LineInfoPanelPosition.FOLLOW
         * @see LineInfoPanelPositionMode
         */
        set(mode) {
            field = mode
            invalidate()
        }
    private var rejectComposingCount = 0

    /**
     * Check if the editor is released.
     * When an editor is released, you are unexpected to make any changes to it.
     */
    var isReleased: Boolean = false
        private set

    /**
     * Check whether line numbers are shown
     * 
     * @return The state of line number displaying
     */
    var isLineNumberEnabled: Boolean = false
        /**
         * Set whether we should display line numbers
         * 
         * @param lineNumberEnabled The state of line number displaying
         */
        set(lineNumberEnabled) {
            if (lineNumberEnabled != field && isWordwrap) {
                createLayout()
            }
            field = lineNumberEnabled
            invalidate()
        }
    private var blockLineEnabled = false

    /**
     * @see setInterceptParentHorizontalScrollIfNeeded
     */
    var isInterceptParentHorizontalScrollEnabled: Boolean = false
        private set
    private var highlightCurrentBlock = false

    /**
     * @see CodeEditor.setHighlightCurrentLine
     */
    var isHighlightCurrentLine: Boolean = false
        /**
         * Specify whether the editor should use a different color to draw
         * the background of current line
         */
        set(highlightCurrentLine) {
            field = highlightCurrentLine
            invalidate()
        }
    private var verticalScrollBarEnabled = false
    private var horizontalScrollBarEnabled = false
    private var cursorAnimation = false

    /**
     * @see setPinLineNumber
     */
    var isLineNumberPinned: Boolean = false
        private set

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if anti word breaking is enabled in wordwrap mode.
     */
    var isAntiWordBreaking: Boolean = false
        private set

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if RTL-based text should display from right of the widget in wordwrap mode.
     */
    var isWordwrapRtlDisplaySupport: Boolean = false
        private set

    /**
     * @see CodeEditor.setFirstLineNumberAlwaysVisible
     */
    var isFirstLineNumberAlwaysVisible: Boolean = false
        /**
         * Show first line number in screen in word wrap mode
         * 
         * @see isFirstLineNumberAlwaysVisible
         */
        set(enabled) {
            field = enabled
            if (isWordwrap) {
                invalidate()
            }
        }

    /**
     * @see CodeEditor.setLigatureEnabled
     */
    var isLigatureEnabled: Boolean = false
        /**
         * Enable/disable ligature of all types(except 'rlig').
         */
        set(enabled) {
            field = enabled
            setFontFeatureSettings(if (enabled) null else "'liga' 0,'calt' 0,'hlig' 0,'dlig' 0,'clig' 0")
        }
    private var lastCursorState = false
    /**
     * Returns whether the cursor should stick to the text row while selecting the text
     * 
     * @see CodeEditor.setStickyTextSelection
     */
    /**
     * Whether the cursor should stick to the text row while selecting the text.
     * 
     * @param stickySelection value
     */
    var isStickyTextSelection: Boolean = false
    private var highlightBracketPair = false

    /**
     * Checks whether long select mode is started
     */
    var isInLongSelect: Boolean = false
        private set
    private var anyWrapContentSet = false
    private var renderFunctionCharacters = false
    private var isSoftKbdEnabled = false
    private var isDisableSoftKbdOnHardKbd = false

    /**
     * Get the rect of left selection handle painted on view
     * 
     * @return Descriptor of left handle
     */
    var leftHandleDescriptor: HandleDescriptor = HandleDescriptor()
        private set

    /**
     * Get the rect of right selection handle painted on view
     * 
     * @return Descriptor of right handle
     */
    var rightHandleDescriptor: HandleDescriptor = HandleDescriptor()
        private set

    /**
     * Get the rect of insert cursor handle on view
     * 
     * @return Rect of insert handle
     */
    var insertHandleDescriptor: HandleDescriptor = HandleDescriptor()
        private set
    var clipboardManager: ClipboardManager? = null

    /**
     * Get using [InputMethodManager]
     */
    var inputMethodManager: InputMethodManager? = null
        protected set

    protected var _text: Content? = null
    open var text: Content
        get() {
            var t = _text
            if (t == null) {
                t = Content()
                _text = t
                _cursor = t.cursor
            }
            return t
        }
        set(value) {
            setText(value)
        }

    protected var _cursor: Cursor? = null
    open val cursor: Cursor
        get() {
            var c = _cursor
            if (c == null) {
                text
                c = _cursor
            }
            return c!!
        }

    private var matrix: Matrix? = null

    protected var _colorScheme: EditorColorScheme? = null
    open var colorScheme: EditorColorScheme
        get() {
            var c = _colorScheme
            if (c == null) {
                c = getDefault()
                _colorScheme = c
                c.attachEditor(this)
            }
            return c
        }
        set(colors) {
            _colorScheme?.detachEditor(this)
            _colorScheme = colors
            colors.attachEditor(this)
            invalidate()
        }

    var lineNumberTipTextProvider: LineNumberTipTextProvider = DefaultLineNumberTip
        set(provider) {
            field = Objects.requireNonNull(provider, "Provider can not be null")
            invalidate()
        }

    var formatTip: String = ""
        set(formatTip) {
            field = Objects.requireNonNull(formatTip)
        }

    protected var _editorLanguage: Language = EmptyLanguage()

    var formatterProvider: FormatterProvider? = null
    private var diagnosticStyle = DiagnosticIndicatorStyle.WAVY_LINE
    private var lastMakeVisible: Long = 0
    private var completionWindow: EditorAutoCompletion? = null

    /**
     * Get [EditorTouchEventHandler] of the editor
     */
    protected var _eventHandler: EditorTouchEventHandler? = null
    open val eventHandler: EditorTouchEventHandler
        get() = _eventHandler ?: error("EventHandler not initialized")
    var lineNumberAlign: Align = Align.RIGHT
        set(align) {
            field = align
            invalidate()
        }
    private var basicDetector: GestureDetector? = null
    private var scaleDetector: ScaleGestureDetector? = null
    private var anchorInfoBuilder: CursorAnchorInfo.Builder? = null
    private var edgeEffectVertical: EdgeEffect? = null
    private var edgeEffectHorizontal: EdgeEffect? = null
    private var extractingTextRequest: ExtractedTextRequest? = null
    private var editorSearcher: EditorSearcher? = null

    protected var _cursorAnimator: CursorAnimator? = null
    open var cursorAnimator: CursorAnimator
        get() = _cursorAnimator ?: error("CursorAnimator not initialized")
        set(animator) {
            _cursorAnimator = animator
        }

    protected var _handleStyle: SelectionHandleStyle? = null
    open var handleStyle: SelectionHandleStyle
        get() = _handleStyle ?: error("HandleStyle not initialized")
        set(style) {
            _handleStyle = Objects.requireNonNull(style)
            invalidate()
        }

    protected var _cursorBlink: CursorBlink? = null
    open val cursorBlink: CursorBlink
        get() = _cursorBlink ?: error("CursorBlink not initialized")

    protected var _props: DirectAccessProps = DirectAccessProps()
    open val props: DirectAccessProps
        get() = _props

    protected var _extraArguments: Bundle = Bundle()
    open var extraArguments: Bundle
        get() = _extraArguments
        set(value) {
            _extraArguments = value
        }

    private var textStyles: Styles? = null
    var diagnostics: DiagnosticsContainer? = null
        private set
    var inlayHints: InlayHintsContainer? = null
        private set
    var highlightTexts: HighlightTextContainer? = null
        private set

    protected var _renderContext: RenderContext? = null
    open val renderContext: RenderContext
        get() = _renderContext ?: error("RenderContext not initialized")

    protected var _renderer: EditorRenderer? = null
    open val renderer: EditorRenderer
        get() = _renderer ?: error("Renderer not initialized")

    private var hardwareAccAllowed = false
    private var scrollerFinalX = 0f
    private var scrollerFinalY = 0f
    private var verticalAbsorb = false
    private var horizontalAbsorb = false

    var lineSeparator: LineSeparator = LineSeparator.LF
        set(lineSeparator) {
            require(Objects.requireNonNull(lineSeparator) != LineSeparator.NONE)
            field = lineSeparator
        }

    private var lastInsertion: TextRange? = null
    private var lastSelectedTextRange: TextRange? = null

    protected var _snippetController: SnippetController? = null
    open val snippetController: SnippetController
        get() = _snippetController ?: error("SnippetController not initialized")

    private val inlayHintRendererMap: MutableMap<String, InlayHintRenderer> =
        HashMap<String, InlayHintRenderer>()

    init {
        initialize(attrs, defStyleAttr, defStyleRes)
        applyAttributeSets(attrs, defStyleAttr, defStyleRes)
    }

    /**
     * Get builtin component so that you can enable/disable them or do some other actions.
     * 
     * @see io.github.rosemoe.sora.widget.component
     */
    fun <T : EditorBuiltinComponent> getComponent(clazz: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (clazz == EditorAutoCompletion::class.java) {
            return completionWindow as T
        } else if (clazz == Magnifier::class.java) {
            return eventHandler.magnifier as T
        } else if (clazz == EditorTextActionWindow::class.java) {
            return textActionWindow as T
        } else if (clazz == EditorDiagnosticTooltipWindow::class.java) {
            return diagnosticTooltip as T
        } else if (clazz == EditorContextMenuCreator::class.java) {
            return contextMenuCreator as T
        } else {
            throw IllegalArgumentException("Unknown component type")
        }
    }

    /**
     * Replace the built-in component to the given one.
     * The new component's enabled state will extend the old one.
     * 
     * @param clazz       Built-in class type. Such as `EditorAutoCompletion.class`
     * @param replacement The new component to apply
     * @param <T>         Type of built-in component
     */
    fun <T : EditorBuiltinComponent> replaceComponent(clazz: Class<T>, replacement: T) {
        val old: T = getComponent(clazz)
        val isEnabled = old.isEnabled()
        old.setEnabled(false)
        if (clazz == EditorAutoCompletion::class.java) {
            completionWindow = replacement as EditorAutoCompletion
        } else if (clazz == Magnifier::class.java) {
            eventHandler.magnifier = replacement as Magnifier
        } else if (clazz == EditorTextActionWindow::class.java) {
            textActionWindow = replacement as EditorTextActionWindow
        } else if (clazz == EditorDiagnosticTooltipWindow::class.java) {
            diagnosticTooltip = replacement as EditorDiagnosticTooltipWindow
        } else if (clazz == EditorContextMenuCreator::class.java) {
            contextMenuCreator = replacement as EditorContextMenuCreator
        } else {
            throw IllegalArgumentException("Unknown component type")
        }
        replacement.setEnabled(isEnabled)
    }

    fun registerInlayHintRenderers(vararg renderers: InlayHintRenderer) {
        var needLayout = false
        for (renderer in renderers) {
            val oldValue = inlayHintRendererMap.put(renderer.typeName, renderer)
            needLayout = needLayout or (oldValue !== renderer)
        }
        if (needLayout) {
            createLayout()
        }
    }

    fun registerInlayHintRenderer(renderer: InlayHintRenderer) {
        val oldValue = inlayHintRendererMap.put(renderer.typeName, renderer)
        if (oldValue !== renderer) {
            createLayout()
        }
    }

    fun removeInlayHintRenderer(renderer: InlayHintRenderer) {
        val oldValue = inlayHintRendererMap.get(renderer.typeName)
        if (oldValue === renderer) {
            inlayHintRendererMap.remove(renderer.typeName)
            createLayout()
        }
    }

    val inlayHintRenderers: MutableList<InlayHintRenderer>
        get() = ArrayList<InlayHintRenderer>(inlayHintRendererMap.values)

    override fun getInlayHintRendererForType(type: String): InlayHintRenderer? {
        return inlayHintRendererMap[type]
    }

    val keyMetaStates: KeyMetaStates
        /**
         * Get KeyMetaStates, which manages alt/shift state in editor
         */
        get() = keyEventHandler.keyMetaStates

    /**
     * Cancel the next animation for [ensurePositionVisible]
     */
    open fun cancelAnimation() {
        lastMakeVisible = System.currentTimeMillis()
    }

    /**
     * Get the width of line number and divider line
     * 
     * @return The width
     */
    fun measureTextRegionOffset(): Float {
        return if (this.isLineNumberEnabled) (measureLineNumber() + dividerMarginLeft + dividerMarginRight + dividerWidth +
                (if (renderer.hasSideHintIcons()) this.rowHeight else 0)) else dpUnit * 5
    }

    /**
     * Get the character's x offset on view
     * 
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    fun getOffset(line: Int, column: Int): Float {
        return layout.getCharLayoutOffset(
            line,
            column
        )[1] + measureTextRegionOffset() - this.offsetX
    }

    /**
     * Get the character's x offset on view
     * 
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    fun getCharOffsetX(line: Int, column: Int): Float {
        return layout.getCharLayoutOffset(
            line,
            column
        )[1] + measureTextRegionOffset() - this.offsetX
    }

    /**
     * Get the character's y offset on view
     * 
     * @param line   The line position of character
     * @param column The column position of character
     * @return The y offset on view
     */
    fun getCharOffsetY(line: Int, column: Int): Float {
        return layout.getCharLayoutOffset(line, column)[0] - this.offsetY
    }

    /**
     * Prepare editor
     * 
     * 
     * Initialize variants
     */
    protected fun initialize(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        Log.v(LOG_TAG, COPYRIGHT)

        eventManager = EventManager()
        renderFunctionCharacters = true
        _renderContext = RenderContext(this)
        _renderer = onCreateRenderer()

        _styleDelegate = EditorStyleDelegate(this)

        verticalScrollFactor = getVerticalScrollFactor(context)
        lineSeparator = LineSeparator.LF
        lineNumberTipTextProvider = DefaultLineNumberTip
        formatTip = getString(context, R.string.sora_editor_editor_formatting)
        _props = DirectAccessProps()
        dpUnit = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            10f,
            Resources.getSystem().displayMetrics
        ) / 10f
        dividerWidth = dpUnit
        insertSelectionWidth = dpUnit * 1.5f
        textBorderWidth = dpUnit
        dividerMarginRight = dpUnit * 2
        dividerMarginLeft = dividerMarginRight

        matrix = Matrix()
        handleStyle = HandleStyleSideDrop(context)
        editorSearcher = EditorSearcher(this)
        cursorAnimator = MoveCursorAnimator(this)
        setCursorBlinkPeriod(DEFAULT_CURSOR_BLINK_PERIOD)
        anchorInfoBuilder = CursorAnchorInfo.Builder()

        colorScheme = getDefault()
        colorScheme.attachEditor(this)
        this._eventHandler = EditorTouchEventHandler(this)
        basicDetector = GestureDetector(context, this.eventHandler)
        basicDetector!!.setOnDoubleTapListener(this.eventHandler)
        scaleDetector = ScaleGestureDetector(context, this.eventHandler)
        this.insertHandleDescriptor = HandleDescriptor()
        this.leftHandleDescriptor = HandleDescriptor()
        this.rightHandleDescriptor = HandleDescriptor()

        startedActionMode = ACTION_MODE_NONE
        setTextSize(DEFAULT_TEXT_SIZE.toFloat())
        this.lineInfoTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            DEFAULT_LINE_INFO_TEXT_SIZE.toFloat(),
            Resources.getSystem().displayMetrics
        )
        lineNumberAlign = Align.RIGHT
        waitForNextChange = false
        blockLineEnabled = true
        blockLineWidth = 1f
        inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        setUndoEnabled(true)
        this.blockIndex = -1
        this.isScalable = true
        setFocusable(true)
        setFocusableInTouchMode(true)
        highlightBracketPair = true
        inputConnection = EditorInputConnection(this)
        completionWindow = EditorAutoCompletion(this)
        edgeEffectVertical = EdgeEffect(context)
        edgeEffectHorizontal = EdgeEffect(context)
        textActionWindow = EditorTextActionWindow(this)
        diagnosticTooltip = EditorDiagnosticTooltipWindow(this)
        contextMenuCreator = EditorContextMenuCreator(this)
        setEditorLanguage(null)
        setText(null)
        this.tabWidth = 4
        this.isHighlightCurrentLine = true
        isVerticalScrollBarEnabled = true
        setHighlightCurrentBlock(true)
        this.isDisplayLnPanel = true
        isHorizontalScrollBarEnabled = true
        this.isFirstLineNumberAlwaysVisible = true
        this.isCursorAnimationEnabled = true
        setEditable(true)
        this.isLineNumberEnabled = true
        this.isHardwareAcceleratedDrawAllowed = true
        setInterceptParentHorizontalScrollIfNeeded(false)
        this.typefaceText = Typeface.DEFAULT
        this.isSoftKeyboardEnabled = true
        this.isDisableSoftKbdIfHardKbdAvailable = true

        defaultFocusHighlightEnabled = false
        if (context is ContextThemeWrapper) {
            this.edgeEffectColor =
                ThemeUtils.getColorPrimary((context as ContextThemeWrapper?)!!)
        }

        // Config scale detector
        scaleDetector!!.setQuickScaleEnabled(false)
        _snippetController = SnippetController(this)

        registerInlayHintProvider(styleDelegate)
        registerDiagnosticProvider(styleDelegate)
        registerHighlightTextProvider(styleDelegate)
    }

    /**
     * Apply attributes from XML
     */
    protected fun applyAttributeSets(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val array = context.obtainStyledAttributes(
            attrs,
            R.styleable.CodeEditor,
            defStyleAttr,
            defStyleRes
        )

        horizontalScrollbarThumbDrawable =
            array.getDrawable(R.styleable.CodeEditor_scrollbarThumbHorizontal)
        horizontalScrollbarTrackDrawable =
            array.getDrawable(R.styleable.CodeEditor_scrollbarTrackHorizontal)
        verticalScrollbarThumbDrawable =
            array.getDrawable(R.styleable.CodeEditor_scrollbarThumbVertical)
        verticalScrollbarTrackDrawable =
            array.getDrawable(R.styleable.CodeEditor_scrollbarTrackVertical)

        this.lnPanelPositionMode = array.getInt(
            R.styleable.CodeEditor_lnPanelPositionMode,
            LineInfoPanelPositionMode.FOLLOW
        )
        this.lnPanelPosition = array.getInt(
            R.styleable.CodeEditor_lnPanelPosition,
            LineInfoPanelPosition.CENTER
        )

        this.dividerWidth =
            array.getDimension(R.styleable.CodeEditor_dividerWidth, this.dividerWidth)
        setDividerMargin(
            array.getDimension(R.styleable.CodeEditor_dividerMargin, this.dividerMarginLeft),
            array.getDimension(
                R.styleable.CodeEditor_dividerMargin, this.dividerMarginRight
            )
        )
        setPinLineNumber(array.getBoolean(R.styleable.CodeEditor_pinLineNumber, false))

        setHighlightCurrentBlock(
            array.getBoolean(
                R.styleable.CodeEditor_highlightCurrentBlock,
                true
            )
        )
        this.isHighlightCurrentLine = array.getBoolean(
            R.styleable.CodeEditor_highlightCurrentLine,
            true
        )
        this.isHighlightBracketPair =
            array.getBoolean(R.styleable.CodeEditor_highlightBracketPair, true)

        this.isLigatureEnabled =
            array.getBoolean(R.styleable.CodeEditor_ligatures, true)
        this.isLineNumberEnabled = array.getBoolean(
            R.styleable.CodeEditor_lineNumberVisible,
            this.isLineNumberEnabled
        )
        getComponent(EditorAutoCompletion::class.java).setEnabled(
            array.getBoolean(
                R.styleable.CodeEditor_autoCompleteEnabled, true
            )
        )
        props.symbolPairAutoCompletion =
            array.getBoolean(R.styleable.CodeEditor_symbolCompletionEnabled, true)
        this.isRenderFunctionCharacters =
            array.getBoolean(
                R.styleable.CodeEditor_renderFunctionChars,
                this.isRenderFunctionCharacters
            )
        this.isScalable = array.getBoolean(
            R.styleable.CodeEditor_scalable,
            this.isScalable
        )

        this.textSizePx = array.getDimension(
            R.styleable.CodeEditor_textSize,
            this.textSizePx
        )
        setCursorBlinkPeriod(
            array.getInt(
                R.styleable.CodeEditor_cursorBlinkPeriod,
                this.cursorBlink.period
            )
        )
        this.tabWidth = array.getInt(R.styleable.CodeEditor_tabWidth, this.tabWidth)

        val wordwrapMode = array.getInt(R.styleable.CodeEditor_wordwrapMode, 0)
        if (wordwrapMode != 0) {
            setWordwrap(true, wordwrapMode > 1, false)
        }

        setText(array.getString(R.styleable.CodeEditor_text))

        array.recycle()
    }

    /**
     * Set whether line number region will scroll together with code region
     * 
     * @see isLineNumberPinned
     */
    fun setPinLineNumber(pinLineNumber: Boolean) {
        this.isLineNumberPinned = pinLineNumber
        if (this.isLineNumberEnabled) {
            invalidate()
        }
    }

    /**
     * Inserts the given text in the editor.
     * 
     * 
     * This method allows you to insert texts externally to the content of editor.
     * The content of {@param text} is not checked to be exactly characters of symbols.
     * 
     * 
     * Note that this still works when the editor is not editable. But you should not
     * call it at that time due to possible problems, especially when [.getEditable] returns
     * true but [.isEditable] returns false
     * 
     * 
     * @param text            Text to insert, usually a text of symbols
     * @param selectionOffset New selection position relative to the start of text to insert.
     * Ranging from 0 to text.length()
     * @throws IllegalArgumentException If the {@param selectionRegion} is invalid
     */
    fun insertText(text: String, selectionOffset: Int) {
        require(!(selectionOffset < 0 || selectionOffset > text.length)) { "selectionOffset is invalid" }
        val cur = this.text.cursor
        if (cur.isSelected) {
            deleteText()
            notifyIMEExternalCursorChange()
        }
        this.text.insert(cur.rightLine, cur.rightColumn, text)
        notifyIMEExternalCursorChange()
        if (selectionOffset != text.length) {
            val pos =
                this.text.indexer.getCharPosition(cur.right - (text.length - selectionOffset))
            setSelection(pos.line, pos.column)
        }
    }

    /**
     * Set cursor blinking period
     * If zero or negative period is passed, the cursor will always be shown.
     * 
     * @param period The period time of cursor blinking
     */
    fun setCursorBlinkPeriod(period: Int) {
        if (_cursorBlink == null) {
            _cursorBlink = CursorBlink(this, period)
        } else {
            val before = cursorBlink.period
            cursorBlink.setPeriod(period)
            if (before <= 0 && cursorBlink.valid && isAttachedToWindow) {
                postInLifecycle(cursorBlink)
            }
        }
    }

    /**
     * Set font feature settings for all paints used by editor
     * 
     * @see Paint.setFontFeatureSettings
     */
    fun setFontFeatureSettings(features: String?) {
        renderer.paint.setFontFeatureSettingsWrapped(features)
        renderer.paintOther.fontFeatureSettings = features
        renderer.paintGraph.fontFeatureSettings = features
        renderer.updateTimestamp()
        invalidate()
    }

    /**
     * Set the style of selection handler.
     * 
     * @see SelectionHandleStyle
     * 
     * @see HandleStyleDrop
     * 
     * @see HandleStyleSideDrop
     */
    fun setSelectionHandleStyle(style: SelectionHandleStyle) {
        handleStyle = Objects.requireNonNull<SelectionHandleStyle>(style)
        invalidate()
    }

    /**
     * Returns whether highlight current code block
     * 
     * @return This module enabled / disabled
     * @see setHighlightCurrentBlock
     */
    fun isHighlightCurrentBlock(): Boolean {
        return highlightCurrentBlock
    }

    /**
     * Whether the editor should use a different color to draw
     * the current code block line and this code block's start line and end line's
     * background.
     * 
     * @param highlightCurrentBlock Enabled / Disabled this module
     */
    fun setHighlightCurrentBlock(highlightCurrentBlock: Boolean) {
        this.highlightCurrentBlock = highlightCurrentBlock
        if (!this.highlightCurrentBlock) {
            this.blockIndex = -1
        } else {
            this.blockIndex = findCursorBlock()
        }
        invalidate()
    }

    /**
     * Set the editor's language.
     * A language is a tool for auto-completion,highlight and auto indent analysis.
     * 
     * @param lang New EditorLanguage for editor
     */
    open val editorLanguage: Language
        get() = _editorLanguage

    open fun setEditorLanguage(lang: Language?) {
        val newLang = lang ?: EmptyLanguage()

        // Destroy old one
        val old: Language = _editorLanguage
        if (old != null) {
            val formatter = this.formatter
            formatter.setReceiver(null)
            formatter.destroy()
            old.analyzeManager.setReceiver(null)
            old.analyzeManager.destroy()
            old.destroy()
        }

        styleDelegate.reset()
        this._editorLanguage = newLang
        this.textStyles = null

        if (this.diagnostics != null) {
            this.diagnostics!!.detachEditor()
        }

        this.diagnostics = null

        // Setup new one
        val mgr = newLang.analyzeManager
        mgr.setReceiver(styleDelegate)
        if (_text != null) {
            mgr.reset(ContentReference(_text!!), extraArguments)
        }

        // Symbol pairs
        if (languageSymbolPairs != null) {
            languageSymbolPairs!!.setParent(null)
        }
        languageSymbolPairs = _editorLanguage.symbolPairs
        if (languageSymbolPairs == null) {
            Log.w(
                LOG_TAG,
                "Language(" + _editorLanguage.toString() + ") returned null for symbol pairs. It is a mistake."
            )
            languageSymbolPairs = SymbolPairMatch()
        }
        languageSymbolPairs!!.setParent(props.overrideSymbolPairs)

        if (_snippetController != null) {
            _snippetController!!.stopSnippet()
        }
        renderContext.invalidateRenderNodes()
        invalidate()

        // reset inlay hints (partially re-layout required)
        if (this.inlayHints != null) {
            internalSetInlayHints(null)
        }
        if (this.highlightTexts != null) {
            internalSetHighlightTexts(null)
        }
    }

    /**
     * Internal callback to check if the editor is capable of handling the given
     * keybinding [KeyEvent]
     * 
     * @param keyCode      The keycode for the keybinding event.
     * @param ctrlPressed  Is 'Ctrl' key pressed?
     * @param shiftPressed Is 'Shift' key pressed?
     * @param altPressed   Is 'Alt' key pressed?
     * @return `true` if the editor can handle the keybinding, `false` otherwise.
     */
    open fun canHandleKeyBinding(
        keyCode: Int,
        ctrlPressed: Boolean,
        shiftPressed: Boolean,
        altPressed: Boolean
    ): Boolean {
        val isDpadKey =
            keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        val isHomeOrEnd =
            keyCode == KeyEvent.KEYCODE_MOVE_HOME || keyCode == KeyEvent.KEYCODE_MOVE_END

        if (ctrlPressed) {
            if (shiftPressed) {
                // Ctrl+Shift+[xx] keys
                return isDpadKey || isHomeOrEnd || keyCode == KeyEvent.KEYCODE_J
            }

            if (altPressed) {
                // Ctrl+Alt+[xx] keys
                return keyCode == KeyEvent.KEYCODE_ENTER
            }

            // Ctrl+[xx] keys
            return isDpadKey || isHomeOrEnd
                    || keyCode == KeyEvent.KEYCODE_A || keyCode == KeyEvent.KEYCODE_C || keyCode == KeyEvent.KEYCODE_X || keyCode == KeyEvent.KEYCODE_V || keyCode == KeyEvent.KEYCODE_U || keyCode == KeyEvent.KEYCODE_R || keyCode == KeyEvent.KEYCODE_D || keyCode == KeyEvent.KEYCODE_W || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE
        }


        if (shiftPressed) {
            // Shift+[xx] keys
            return isDpadKey || isHomeOrEnd || keyCode == KeyEvent.KEYCODE_ENTER
        }

        return false
    }

    /**
     * Set whether text in editor should be wrapped to fit its size
     * 
     * @param wordwrap         Whether to wrap words
     * @param antiWordBreaking Prevent English words to be split into two lines
     * @see .isWordwrap
     */
    fun setWordwrap(wordwrap: Boolean, antiWordBreaking: Boolean) {
        setWordwrap(wordwrap, antiWordBreaking, false)
    }

    /**
     * Set whether text in editor should be wrapped to fit its size
     * 
     * @param wordwrap         Whether to wrap words
     * @param antiWordBreaking Prevent English words to be split into two lines
     * @param supportRtlRow    Allow rows with RTL base direction to display from the right side of widget
     * @see .isWordwrap
     */
    fun setWordwrap(wordwrap: Boolean, antiWordBreaking: Boolean, supportRtlRow: Boolean) {
        if (this.wordwrap != wordwrap || this.isAntiWordBreaking != antiWordBreaking || this.isWordwrapRtlDisplaySupport != supportRtlRow) {
            this.wordwrap = wordwrap
            this.isAntiWordBreaking = antiWordBreaking
            this.isWordwrapRtlDisplaySupport = supportRtlRow
            requestLayoutIfNeeded()
            createLayout()
            if (!wordwrap) {
                renderContext.invalidateRenderNodes()
            }
            invalidate()
        }
    }

    var isCursorAnimationEnabled: Boolean
        /**
         * @see .setCursorAnimationEnabled
         */
        get() = cursorAnimation
        /**
         * Set cursor animation enabled
         */
        set(enabled) {
            if (!enabled) {
                cursorAnimator.cancel()
            }
            cursorAnimation = enabled
        }

    /**
     * Whether display vertical scroll bar when scrolling
     * 
     * @param enabled Enabled / disabled
     */
    fun setScrollBarEnabled(enabled: Boolean) {
        horizontalScrollBarEnabled = enabled
        verticalScrollBarEnabled = horizontalScrollBarEnabled
        invalidate()
    }

    override fun setHorizontalScrollbarThumbDrawable(drawable: Drawable?) {
        renderer.horizontalScrollbarThumbDrawable = drawable
    }

    override fun getHorizontalScrollbarThumbDrawable(): Drawable? {
        return renderer.horizontalScrollbarThumbDrawable
    }

    override fun setHorizontalScrollbarTrackDrawable(drawable: Drawable?) {
        renderer.horizontalScrollbarTrackDrawable = drawable
    }

    override fun getHorizontalScrollbarTrackDrawable(): Drawable? {
        return renderer.horizontalScrollbarTrackDrawable
    }

    override fun setVerticalScrollbarThumbDrawable(drawable: Drawable?) {
        renderer.verticalScrollbarThumbDrawable = drawable
    }

    override fun getVerticalScrollbarThumbDrawable(): Drawable? {
        return renderer.verticalScrollbarThumbDrawable
    }

    override fun setVerticalScrollbarTrackDrawable(drawable: Drawable?) {
        renderer.verticalScrollbarTrackDrawable = drawable
    }

    override fun getVerticalScrollbarTrackDrawable(): Drawable? {
        return renderer.verticalScrollbarTrackDrawable
    }

    /**
     * {@inheritDoc}
     */
    override fun isHorizontalScrollBarEnabled(): Boolean {
        return horizontalScrollBarEnabled
    }

    /**
     * {@inheritDoc}
     */
    override fun setHorizontalScrollBarEnabled(horizontalScrollBarEnabled: Boolean) {
        this.horizontalScrollBarEnabled = horizontalScrollBarEnabled
    }

    /**
     * {@inheritDoc}
     */
    override fun isVerticalScrollBarEnabled(): Boolean {
        return verticalScrollBarEnabled
    }

    /**
     * {@inheritDoc}
     */
    override fun setVerticalScrollBarEnabled(verticalScrollBarEnabled: Boolean) {
        this.verticalScrollBarEnabled = verticalScrollBarEnabled
    }

    @get:Px
    var textSizePx: Float
        /**
         * Get text size in pixel unit
         * 
         * @return Text size in pixel unit
         * @see setTextSize
         * @see CodeEditor.setTextSizePx
         */
        get() = renderer.paint.textSize
        /**
         * Set text size in pixel unit
         * 
         * @param size Text size in pixel unit
         */
        set(size) {
            setTextSizePxDirect(size)
            requestLayoutIfNeeded()
            createLayout()
            invalidate()
        }

    /**
     * Set text size directly without creating layout or invalidating view
     * 
     * @param size Text size in pixel unit
     */
    open fun setTextSizePxDirect(@Px size: Float) {
        val oldTextSize = this.textSizePx
        renderer.setTextSizePxDirect(size)
        dispatchEvent(TextSizeChangeEvent(this, oldTextSize, size))
    }

    open fun requestLayoutIfNeeded() {
        if (anyWrapContentSet) {
            requestLayout()
        }
    }

    protected fun checkForRelayout() {
        if (anyWrapContentSet) {
            val params = layoutParams
            if (params != null) {
                if (params.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    requestLayout()
                } else if (params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    if (height != layout.layoutHeight) {
                        requestLayout()
                    }
                }
            }
        }
    }

    val lineNumberMetrics: FontMetricsInt
        get() = renderer.lineNumberMetrics

    /**
     * Whether non-printable is to be drawn
     */
    open fun shouldInitializeNonPrintable(): Boolean {
        return clearBits(
            nonPrintableOptions,
            FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE or FLAG_DRAW_TAB_SAME_AS_SPACE or
                    FLAG_DRAW_LINE_SEPARATOR or FLAG_DRAW_SOFT_WRAP
        ) != 0
    }

    var isHardwareAcceleratedDrawAllowed: Boolean
        /**
         * @see .setHardwareAcceleratedDrawAllowed
         */
        get() = hardwareAccAllowed
        /**
         * Set whether allow the editor to use RenderNode to draw its text.
         * Enabling this can cause more memory usage, but the editor can display text
         * much quicker.
         * However, only when hardware accelerate is enabled on this view can the switch
         * make a difference.
         */
        set(acceleratedDraw) {
            hardwareAccAllowed = acceleratedDraw
            if (acceleratedDraw && !isWordwrap) {
                renderContext.invalidateRenderNodes()
            }
        }

    /**
     * As the name is, we find where leading spaces end and trailing spaces start
     * 
     * @param line The line to search
     */
    open fun findLeadingAndTrailingWhitespacePos(line: ContentLine): Long {
        val column = line.length
        var leading = 0
        var trailing = column
        while (leading < column && isWhitespace(line.get(leading))) {
            leading++
        }
        // Only when this action is needed
        if (leading != column && (nonPrintableOptions and (FLAG_DRAW_WHITESPACE_INNER or FLAG_DRAW_WHITESPACE_TRAILING)) != 0) {
            while (trailing > 0 && isWhitespace(line.get(trailing - 1))) {
                trailing--
            }
        }
        return pack(leading, trailing)
    }

    /**
     * A quick method to predicate whitespace character
     */
    private fun isWhitespace(ch: Char): Boolean {
        return ch == '\t' || ch == ' '
    }

    /**
     * Get matched text regions on the given line
     * 
     * @param line      Target line
     * @param positions Output start positions
     */
    open fun computeMatchedPositions(line: Int, positions: LongArrayList) {
        positions.clear()
        if (editorSearcher!!.currentPattern == null || editorSearcher!!.searchOptions == null) {
            return
        }
        if (!editorSearcher!!.isResultValid()) {
            return
        }
        val res = editorSearcher!!.lastResults
        if (res == null) {
            return
        }
        val lineLeft = text.getCharIndex(line, 0)
        val lineRight = lineLeft + text.getColumnCount(line)
        for (i in max(0, positions.lowerBoundByFirst(lineLeft) - 1)..<res.size()) {
            val region = res.get(i)
            val start = getFirst(region)
            val end = getSecond(region)
            val highlightStart = max(start, lineLeft)
            val highlightEnd = min(end, lineRight)
            if (highlightStart < highlightEnd) {
                positions.add(pack(highlightStart - lineLeft, highlightEnd - lineLeft))
            }
            if (start > lineRight) {
                break
            }
        }
    }


    open fun computeHighlightPositions(line: Int, positions: MutableLongLongMap) {
        positions.clear()
        if (this.highlightTexts == null) {
            return
        }
        val highlights: List<HighlightText> = highlightTexts!!.getForLine(line)
        if (highlights.isEmpty()) {
            return
        }
        val lineColumnCount = text.getColumnCount(line)
        val highlightBlankLine = false
        for (highlight in highlights) {
            if (line < highlight.startLine || line > highlight.endLine) {
                continue
            }
            var startColumn = if (line == highlight.startLine) highlight.startColumn else 0
            var endColumn =
                if (line == highlight.endLine) highlight.endColumn else lineColumnCount
            if (startColumn < 0) {
                startColumn = 0
            } else if (startColumn > lineColumnCount) {
                startColumn = lineColumnCount
            }
            if (endColumn < 0) {
                endColumn = 0
            } else if (endColumn > lineColumnCount) {
                endColumn = lineColumnCount
            }
            if (lineColumnCount == 0) {
                continue
            }
            if (startColumn < endColumn) {
                val backgroundColor = highlight.color.resolve(colorScheme)
                val borderColor = highlight.borderColor.resolve(colorScheme)
                positions.put(pack(startColumn, endColumn), pack(backgroundColor, borderColor))
            }
        }
    }

    var edgeEffectColor: Int
        /**
         * Get the color of EdgeEffect
         * 
         * @return The color of EdgeEffect.
         */
        get() = edgeEffectVertical!!.color
        /**
         * Set the color of EdgeEffect
         * 
         * @param color The color of EdgeEffect
         */
        set(color) {
            edgeEffectVertical!!.color = color
            edgeEffectHorizontal!!.setColor(color)
        }

    open val verticalEdgeEffect: EdgeEffect
        /**
         * Get EdgeEffect for vertical direction
         * 
         * @return EdgeEffect
         */
        get() = edgeEffectVertical!!

    open val horizontalEdgeEffect: EdgeEffect
        /**
         * Get EdgeEffect for horizontal direction
         * 
         * @return EdgeEffect
         */
        get() = edgeEffectHorizontal!!

    /**
     * Find the smallest code block that cursor is in
     * 
     * @return The smallest code block index.
     * If cursor is not in any code block,just -1.
     */
    private fun findCursorBlock(): Int {
        val blocks: List<CodeBlock>? = textStyles?.blocks
        if (blocks == null || blocks.isEmpty()) {
            return -1
        }
        return findCursorBlock(blocks)
    }

    /**
     * Find the cursor code block internal
     * 
     * @param blocks Current code blocks
     * @return The smallest code block index.
     * If cursor is not in any code block,just -1.
     */
    private fun findCursorBlock(blocks: List<CodeBlock>): Int {
        val line = cursor.leftLine
        var min = binarySearchEndBlock(line, blocks)
        if (min == -1) {
            min = 0
        }
        val max = blocks.size - 1
        var minDis = Int.MAX_VALUE
        var found = -1
        var invalidCount = 0
        var maxCount = Int.MAX_VALUE
        if (textStyles != null) {
            maxCount = textStyles!!.getSuppressSwitch()
        }
        for (i in min..max) {
            val block = blocks.get(i)
            if (block.endLine >= line && block.startLine <= line) {
                val dis = block.endLine - block.startLine
                if (dis < minDis) {
                    minDis = dis
                    found = i
                }
            } else if (minDis != Int.MAX_VALUE) {
                invalidCount++
                if (invalidCount >= maxCount) {
                    break
                }
            }
        }
        return found
    }

    /**
     * Find the first code block that maybe seen on screen Because the code blocks is sorted by its
     * end line position we can use binary search to quicken this process in order to decrease the
     * time we use on finding
     * 
     * @param firstVis The first visible line
     * @param blocks   Current code blocks
     * @return The index of the block we found or `-1` if no code block is found.
     */
    fun binarySearchEndBlock(firstVis: Int, blocks: List<CodeBlock>?): Int {
        return CodeBlock.binarySearchEndBlock(firstVis, blocks)
    }


    /**
     * Get spans on the given line
     */
    fun getSpansForLine(line: Int): List<Span> {
        val spanMap = textStyles?.spans
        if (spanMap != null) {
            return getSpansOnLine(spanMap.read(), line)
        } else {
            return defaultLineSpans
        }
    }

    /**
     * Get the width of line number region (include line number margin)
     * 
     * @return width of line number region
     */
    fun measureLineNumber(): Float {
        if (!this.isLineNumberEnabled) {
            return 0f
        }
        var count = 0
        var lineCount = this.lineCount
        while (lineCount > 0) {
            count++
            lineCount /= 10
        }
        val len: Int = NUMBER_DIGITS.length
        val buffer = obtain(len)
        renderer.paintOther.getTextWidths(NUMBER_DIGITS, buffer)
        recycle(buffer)
        var single = 0f
        var i = 0
        while (i < len) {
            single = max(single, buffer[i])
            i += 2
        }
        return single * count + lineNumberMarginLeft
    }

    /**
     * Create layout for text
     */
    open fun createLayout(clearWordwrapCache: Boolean = true) {
        if (_layout != null) {
            if (_layout is LineBreakLayout && !wordwrap) {
                (_layout as LineBreakLayout).reuse(text)
                return
            }
            val oldLayout = _layout
            if (oldLayout is WordwrapLayout && wordwrap) {
                val newLayout = WordwrapLayout(
                    this, text,
                    this.isAntiWordBreaking,
                    this.isWordwrapRtlDisplaySupport, oldLayout, clearWordwrapCache
                )
                oldLayout.destroyLayout()
                layout = newLayout
                return
            }
            oldLayout?.destroyLayout()
        }
        if (wordwrap) {
            renderer.setCachedLineNumberWidth(measureLineNumber().toInt())
            layout = WordwrapLayout(
                this, text,
                this.isAntiWordBreaking,
                this.isWordwrapRtlDisplaySupport, null, false
            )
        } else {
            layout = LineBreakLayout(this, text)
        }
        if (this.eventHandler != null) {
            eventHandler.scrollBy(0f, 0f)
        }
    }

    /**
     * Indents the selected lines. Does nothing if the text is not selected.
     */
    fun indentSelection() {
        indentLines(true)
    }

    /**
     * Indents the lines. Does nothing if the `onlyIfSelected` is `true` and
     * no text is selected.
     * 
     * @param onlyIfSelected Set to `true` if lines must be indented only if the text is
     * selected.
     */
    fun indentLines(onlyIfSelected: Boolean) {
        val cursor = this.cursor
        if (onlyIfSelected && !cursor.isSelected) {
            return
        }

        val tabString = createTabString()
        val text = this.text
        val tabWidth = this.tabWidth

        text.beginBatchEdit()
        for (i in cursor.leftLine..cursor.rightLine) {
            val line = text.getLine(i)
            val result = countLeadingSpacesAndTabs(line)
            val spaceCount = getFirst(result)
            val tabCount = getSecond(result)
            val spaces = spaceCount + (tabCount * tabWidth)
            val endColumn = spaceCount + tabCount

            val requiredSpaces = tabWidth - (spaces % tabWidth)
            if (spaceCount > 0 && tabCount > 0) {
                // indentation contains spaces as well as tabs
                // replace the leading indentation with appropriate indentation (according to language.useTabs())
                // this should be done while incrementing the indentation
                val finalSpaceCount =
                    ((if (requiredSpaces == 0) tabWidth else requiredSpaces) + spaces) / tabWidth
                text.replace(i, 0, i, endColumn, tabString.repeat(finalSpaceCount))
                continue
            }

            if (requiredSpaces == tabWidth) {
                // line is evenly indented
                // increase the indentation by \t or tabWidthSpaces
                text.insert(i, endColumn, tabString)
            } else {
                // line is oddly indented
                // We know that a line can never be oddly indented when it is indented only with tabs
                // therefore, we insert spaces to align the line
                text.insert(i, endColumn, " ".repeat(requiredSpaces))
            }
        }
        text.endBatchEdit()
    }

    /**
     * Removes indentation from the start of the selected lines. If the text is not selected, or if
     * the start and end selection is on the same line, only the line at the cursor position is
     * unindented.
     */
    fun unindentSelection() {
        val cursor = this.cursor
        val text = this.text
        val tabWidth = this.tabWidth
        val tabString = createTabString()

        text.beginBatchEdit()
        for (i in cursor.leftLine..cursor.rightLine) {
            val line = text.getLineString(i)
            val result = countLeadingSpacesAndTabs(line)
            val spaceCount = getFirst(result)
            val tabCount = getSecond(result)
            val spaces = spaceCount + (tabCount * tabWidth)
            if (spaces == 0) {
                // line is not indented
                continue
            }

            val endColumn = spaceCount + tabCount

            val extraSpaces = spaces % tabWidth
            if (spaceCount > 0 && tabCount > 0) {
                // indentation contains spaces as well as tabs
                // replace the leading indentation with appropriate indentation (according to language.useTabs())
                // this should be done while decrementing the indentation
                val finalSpaceCount =
                    abs(spaces - (if (extraSpaces == 0) tabWidth else extraSpaces)) / tabWidth
                text.replace(i, 0, i, endColumn, tabString.repeat(finalSpaceCount))
                continue
            }

            if (extraSpaces == 0) {
                // line is evenly indented
                // remove tabString.length() characters from the start

                // do not use tabString.length()

                text.delete(i, endColumn - (if (tabCount > 0) 1 else tabWidth), i, endColumn)
            } else {
                // line is oddly indented
                // We know that a line can never be oddly indented when it is indented only with tabs
                // therefore, we delete spaces to align the line
                text.delete(i, endColumn - extraSpaces, i, endColumn)
            }
        }
        text.endBatchEdit()
    }

    /**
     * Commit a tab to cursor
     */
    protected fun commitTab() {
        if (inputConnection != null && isEditable) {
            if (inputConnection.composingText.isComposing) {
                // This external change will damage the composing text (#784)
                restartInput()
            }
            inputConnection.commitTextInternal(createTabString(), true)
        }
    }

    /**
     * Indents the line if text is not selected and the cursor is at the start of the line. Inserts
     * an indentation string otherwise.
     */
    fun indentOrCommitTab() {
        val cursor = this.cursor
        if (cursor.isSelected) {
            indentSelection()
            return
        }

        val left = cursor.left()
        val line = this.text.getLine(left.line)

        val count = countLeadingSpacesAndTabs(line)
        val spaceCount = getFirst(count)
        val tabCount = getSecond(count)

        if (left.column > spaceCount + tabCount) {
            // there is text before the cursor
            commitTab()
            return
        }

        indentLines(false)
    }

    /**
     * Creates the string to insert when `KEYCODE_TAB` key event is received from the IME.
     * 
     * @return The string to insert for tab character.
     */
    protected fun createTabString(): String {
        val language = this.editorLanguage
        return createIndent(tabWidth, tabWidth, language.useTab())
    }

    /**
     * Update the information of cursor
     * Such as the position of cursor on screen(For input method that can go to any position on screen like PC input method)
     * 
     * @return The offset x of right cursor on view
     */
    fun updateCursorAnchor(): Float {
        val l = cursor.rightLine
        val column = cursor.rightColumn
        var visible = true
        val offsets = layout.getCharLayoutOffset(l, column)
        var x = measureTextRegionOffset() + offsets[1] - this.offsetX
        val charBottom = offsets[0] - this.offsetY
        val charTop = charBottom - this.rowHeight
        val charBaseline = getRowBaseline(Math.round(charTop / this.rowHeight.toFloat())).toFloat()
        if (x < 0) {
            visible = false
            x = 0f
        } else if (x > width) {
            visible = false
            x = width.toFloat()
        }
        val composingText = inputConnection.composingText
        if (composingText.preSetComposing) {
            return x
        }
        if (props.reportCursorAnchor) {
            val builder = anchorInfoBuilder!!
            builder.reset()
            matrix!!.set(getMatrix())
            val b = IntArray(2)
            getLocationOnScreen(b)
            matrix!!.postTranslate(b[0].toFloat(), b[1].toFloat())
            builder.setMatrix(matrix)
            builder.setSelectionRange(cursor.left, cursor.right)

            if (composingText.isComposing) {
                builder.setComposingText(
                    composingText.startIndex,
                    text.substring(composingText.startIndex, composingText.endIndex)
                )
            }
            builder.setInsertionMarkerLocation(
                x,
                charTop,
                charBaseline,
                charBottom,
                if (visible) CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION else CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
            )
            inputMethodManager!!.updateCursorAnchorInfo(this, builder.build())
        }
        return x
    }

    /**
     * Delete text before cursor or selected text (if there is)
     */
    fun deleteText() {
        val cur = cursor
        if (cur.isSelected) {
            text.delete(cur.leftLine, cur.leftColumn, cur.rightLine, cur.rightColumn)
        } else {
            val col = cur.leftColumn
            val line = cur.leftLine
            if (props.deleteEmptyLineFast || (props.deleteMultiSpaces != 1 && col > 0 && text.charAt(
                    line,
                    col - 1
                ) == ' ')
            ) {
                // Check whether selection is in leading spaces
                val text = this.text.getLine(cur.leftLine)
                var inLeading = true
                for (i in col - 1 downTo 0) {
                    val ch = text.get(i)
                    if (ch != ' ' && ch != '\t') {
                        inLeading = false
                        break
                    }
                }

                if (inLeading) {
                    // Check empty line
                    var emptyLine = true
                    val max = this.text.getColumnCount(line)
                    for (i in col..<max) {
                        val ch = text.get(i)
                        if (ch != ' ' && ch != '\t') {
                            emptyLine = false
                            break
                        }
                    }
                    if (props.deleteEmptyLineFast && emptyLine) {
                        if (line == 0) {
                            // Just delete whitespaces before
                            this.text.delete(line, 0, line, col)
                        } else {
                            this.text.delete(
                                line - 1,
                                this.text.getColumnCount(line - 1),
                                line,
                                max
                            )
                        }
                        return
                    }

                    if (props.deleteMultiSpaces != 1 && col > 0 && this.text.charAt(
                            line,
                            col - 1
                        ) == ' '
                    ) {
                        this.text.delete(
                            line,
                            max(
                                0,
                                col - (if (props.deleteMultiSpaces == -1) tabWidth else props.deleteMultiSpaces)
                            ),
                            line,
                            col
                        )
                        return
                    }
                }
            }
            // Do not put cursor inside combined characters
            var begin: Int
            begin = getOffsetForBackspaceKey(text.getLine(cur.leftLine), col)
            var end = cur.leftColumn
            if (begin > end) {
                val tmp = begin
                begin = end
                end = tmp
            }
            if (begin == end) {
                if (cur.leftLine > 0 && begin == 0) {
                    text.delete(
                        cur.leftLine - 1,
                        text.getColumnCount(cur.leftLine - 1),
                        cur.leftLine,
                        0
                    )
                }
            } else {
                text.delete(cur.leftLine, begin, cur.leftLine, end)
            }
        }
    }

    /**
     * Commit text with given options
     * 
     * @param text Text commit by InputConnection
     * @param applyAutoIndent Apply automatic indentation
     * @param applySymbolCompletion Apply symbol surroundings and completions
     */
    /**
     * Commit text at current state from IME
     * 
     * @param text            Text commit by InputConnection
     * @param applyAutoIndent Apply automatic indentation
     */
    /**
     * Commit text to the content from IME
     */
    @JvmOverloads
    fun commitText(
        text: CharSequence,
        applyAutoIndent: Boolean = true,
        applySymbolCompletion: Boolean = true
    ) {
        // replace text
        var text = text
        var pair: SymbolPair? = null
        if (applySymbolCompletion && props.symbolPairAutoCompletion && text.length > 0) {
            val endCharFromText = text.get(text.length - 1)

            var inputText: CharArray? = null

            //size > 1
            if (text.length > 1) {
                inputText = text.toString().toCharArray()
            }

            pair = languageSymbolPairs!!.matchBestPair(
                this, cursor.left(),
                inputText, endCharFromText
            )
        }

        val cur = cursor
        val editorText: Content = this.text
        val quoteHandler = LanguageHelper.getQuickQuoteHandler(editorLanguage)

        if (pair != null && pair !== SymbolPair.EMPTY_SYMBOL_PAIR) {
            // QuickQuoteHandler can easily implement the feature of AutoSurround
            // and is at a higher level (customizable),
            // so if the language implemented QuickQuoteHandler,
            // the AutoSurround feature is not considered needed because it can be implemented through QuickQuoteHandler

            if (pair.shouldDoAutoSurround(editorText) && quoteHandler == null) {
                editorText.beginBatchEdit()
                // insert left
                editorText.insert(cur.leftLine, cur.leftColumn, pair.open)
                // editorText.insert(editorCursor.getLeftLine(),editorCursor.getLeftColumn(),selectText);
                // insert right
                editorText.insert(cur.rightLine, cur.rightColumn, pair.close)
                editorText.endBatchEdit()

                // setSelection
                setSelectionRegion(
                    cur.leftLine, cur.leftColumn,
                    cur.rightLine, cur.rightColumn - pair.close.length
                )

                return
            } else if (cur.isSelected && quoteHandler != null) {
                if (text.length > 0 && text.length == 1) {
                    val result = quoteHandler.onHandleTyping(
                        text.toString(), this.text,
                        this.cursorRange,
                        this.styles
                    )
                    if (result.isConsumed) {
                        val range = result.newCursorRange
                        if (range != null) {
                            setSelectionRegion(
                                range.start.line,
                                range.start.column,
                                range.end.line,
                                range.end.column
                            )
                        }
                        return
                    }
                }
            } else {
                editorText.beginBatchEdit()

                val insertPosition = editorText
                    .indexer
                    .getCharPosition(pair.insertOffset)

                editorText.replace(
                    insertPosition.line, insertPosition.column,
                    cur.rightLine, cur.rightColumn, pair.open
                )
                editorText.insert(
                    insertPosition.line,
                    insertPosition.column + pair.open.length,
                    pair.close
                )
                editorText.endBatchEdit()

                val cursorPosition = editorText
                    .indexer
                    .getCharPosition(pair.cursorOffset)

                setSelection(cursorPosition.line, cursorPosition.column)

                return
            }
        }


        if (cur.isSelected) {
            editorText.replace(cur.leftLine, cur.leftColumn, cur.rightLine, cur.rightColumn, text)
        } else {
            if (props.autoIndent && text.length != 0 && applyAutoIndent) {
                val first = text.get(0)
                if (first == '\n' || first == '\r') {
                    val line = this.text.getLineString(cur.leftLine)
                    var p = 0
                    var spaceCount = 0
                    var tabCount = 0
                    while (p < cur.leftColumn) {
                        if (isWhitespace(line.get(p))) {
                            if (line.get(p) == '\t') {
                                ++tabCount
                            } else {
                                ++spaceCount
                            }
                            p++
                        } else {
                            break
                        }
                    }
                    var count = spaceCount + (tabCount * tabWidth)
                    try {
                        count += LanguageHelper.getIndentAdvance(
                            editorLanguage,
                            ContentReference(this.text),
                            cur.leftLine,
                            cur.leftColumn,
                            spaceCount,
                            tabCount
                        )
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "Language object error", e)
                    }
                    var index = 1
                    if (first == '\r' && text.length >= 2 && text.get(1) == '\n') {
                        index = 2
                    }
                    val sb = StringBuilder(text)
                    sb.insert(index, createIndent(count, tabWidth, editorLanguage.useTab()))
                    text = sb
                }
            }
            editorText.insert(cur.leftLine, cur.leftColumn, text)
        }
    }

    var nonPrintablePaintingFlags: Int
        /**
         * @see .setNonPrintablePaintingFlags
         * @see .FLAG_DRAW_WHITESPACE_LEADING
         * 
         * @see .FLAG_DRAW_WHITESPACE_INNER
         * 
         * @see .FLAG_DRAW_WHITESPACE_TRAILING
         * 
         * @see .FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
         * 
         * @see .FLAG_DRAW_LINE_SEPARATOR
         * 
         * @see .FLAG_DRAW_WHITESPACE_IN_SELECTION
         */
        get() = nonPrintableOptions
        /**
         * Sets non-printable painting flags.
         * Specify where they should be drawn and some other properties.
         * 
         * 
         * Flags can be mixed.
         * 
         * @param flags Flags
         * @see .FLAG_DRAW_WHITESPACE_LEADING
         * 
         * @see .FLAG_DRAW_WHITESPACE_INNER
         * 
         * @see .FLAG_DRAW_WHITESPACE_TRAILING
         * 
         * @see .FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
         * 
         * @see .FLAG_DRAW_LINE_SEPARATOR
         * 
         * @see .FLAG_DRAW_WHITESPACE_IN_SELECTION
         * 
         * @see .FLAG_DRAW_SOFT_WRAP
         */
        set(flags) {
            val oldFlags = nonPrintableOptions
            this.nonPrintableOptions = flags
            if ((oldFlags and FLAG_DRAW_SOFT_WRAP) != (flags and FLAG_DRAW_SOFT_WRAP)) {
                createLayout()
            }
            invalidate()
        }

    fun hasComposingText(): Boolean {
        return inputConnection.composingText.isComposing
    }

    /**
     * Make the selection visible
     */
    fun ensureSelectionVisible() {
        ensurePositionVisible(cursor.rightLine, cursor.rightColumn)
    }

    /**
     * Make the given character position visible
     * 
     * @param line        Line in text
     * @param column      Column in text
     * @param noAnimation true if no animation should be applied
     */
    /**
     * Make the given character position visible
     * 
     * @param line   Line in text
     * @param column Column in text
     */
    @JvmOverloads
    fun ensurePositionVisible(line: Int, column: Int, noAnimation: Boolean = false) {
        val scroller = this.scroller
        val layoutOffset = layout.getCharLayoutOffset(line, column)
        // x offset is the left of character
        val xOffset = layoutOffset[1] + measureTextRegionOffset()
        // y offset is the bottom of row
        val yOffset = layoutOffset[0]

        val currFinalY = (if (scroller.isFinished) this.offsetY else scroller.finalY).toFloat()
        val currFinalX = (if (scroller.isFinished) this.offsetX else scroller.finalX).toFloat()
        var targetY = currFinalY
        var targetX = currFinalX

        val topLines = if (props.stickyScroll) props.stickyScrollMaxLines else 2
        if (yOffset - this.rowHeight * topLines < currFinalY) {
            // top may be invisible
            targetY = yOffset - this.rowHeight * topLines
        }
        if (yOffset > height + currFinalY) {
            // bottom invisible
            targetY = yOffset - height + this.rowHeight * 1f
        }
        val charWidth = if (column == 0) 0f else this.textPaint.measureText("a")
        if (xOffset < currFinalX + (if (this.isLineNumberPinned) measureTextRegionOffset() else 0f)) {
            val backupX = targetX
            val scrollSlopX = width / 2
            targetX =
                xOffset + (if (this.isLineNumberPinned) -measureTextRegionOffset() else 0f) - charWidth
            if (abs(targetX - backupX) < scrollSlopX) {
                targetX = max(1f, backupX - scrollSlopX)
            }
        }
        if (xOffset + charWidth > currFinalX + width) {
            targetX = xOffset + charWidth * 0.8f - width
        }

        targetX = max(0f, min(this.scrollMaxX.toFloat(), targetX))
        targetY = max(0f, min(this.scrollMaxY.toFloat(), targetY))

        if (withinDelta(targetX, this.offsetX.toFloat(), 1f) && withinDelta(
                targetY,
                this.offsetY.toFloat(),
                1f
            )
        ) {
            invalidate()
            return
        }

        val animation = System.currentTimeMillis() - lastMakeVisible >= 100
        lastMakeVisible = System.currentTimeMillis()

        if (animation && !noAnimation) {
            scroller.forceFinished(true)
            scroller.startScroll(
                this.offsetX,
                this.offsetY, (targetX - this.offsetX).toInt(), (targetY - this.offsetY).toInt()
            )
            if (props.awareScrollbarWhenAdjust && abs(this.offsetY - targetY) > dpUnit * 100) {
                eventHandler.notifyScrolled()
            }
        } else {
            scroller.startScroll(
                this.offsetX,
                this.offsetY, (targetX - this.offsetX).toInt(), (targetY - this.offsetY).toInt(), 0
            )
            scroller.abortAnimation()
        }

        dispatchEvent(
            ScrollEvent(
                this,
                this.offsetX,
                this.offsetY,
                targetX.toInt(),
                targetY.toInt(),
                ScrollEvent.CAUSE_MAKE_POSITION_VISIBLE
            )
        )

        invalidate()
    }

    /**
     * Whether there is clip
     * 
     * @return whether clip in clip board
     */
    fun hasClip(): Boolean {
        return clipboardManager!!.hasPrimaryClip()
    }

    val scroller: EditorScroller
        /**
         * Get scroller from EventHandler
         * You would better not use it for your own scrolling
         * 
         * @return The scroller
         */
        get() = eventHandler.scroller

    /**
     * Checks whether the position is over max Y position
     * 
     * @param posOnScreen Y position on view
     * @return Whether over max Y
     */
    fun isOverMaxY(posOnScreen: Float): Boolean {
        return posOnScreen + this.offsetY > layout.layoutHeight
    }

    /**
     * Check if the point on editor view, is inside text region on that row
     */
    fun isScreenPointOnText(x: Float, y: Float): Boolean {
        val pos = getPointPositionOnScreen(x, y)
        val rowIdx =
            layout.getRowIndexForPosition(text.getCharIndex(getFirst(pos), getSecond(pos)))
        val layoutMax = renderer.getRowWidth(rowIdx)
        val textRegionX = measureTextRegionOffset()
        val rowRegionRightX = textRegionX + layoutMax
        val offset = this.offsetX + x
        return offset >= textRegionX && offset <= rowRegionRightX
    }

    /**
     * Determine character position using positions in scroll coordinate
     * 
     * @param xOffset Horizontal position in scroll coordinate
     * @param yOffset Vertical position in scroll coordinate
     * @return IntPair. first is line and second is column
     * @see IntPair
     */
    fun getPointPosition(xOffset: Float, yOffset: Float): Long {
        return layout.getCharPositionForLayoutOffset(xOffset - measureTextRegionOffset(), yOffset)
    }

    /**
     * Determine character position using positions on view
     * 
     * @param x X on view
     * @param y Y on view
     * @return IntPair. first is line and second is column
     * @see IntPair
     */
    fun getPointPositionOnScreen(x: Float, y: Float): Long {
        var y = y
        y = max(0f, y)
        val stuckLines = renderer.lastStuckLines
        if (stuckLines != null) {
            // position Y maybe negative
            val index = max(0f, (y / this.rowHeight)).toInt()
            if (y < stuckLines.size * this.rowHeight && index < stuckLines.size) {
                return getPointPosition(
                    x,
                    layout.getCharLayoutOffset(
                        stuckLines[index].startLine,
                        0
                    )[0] - this.rowHeight / 2f
                )
            }
        }
        return getPointPosition(x + this.offsetX, y + this.offsetY)
    }

    fun getPointVisualPosition(xOffset: Float, yOffset: Float): VisualLocation {
        return layout.getVisualPositionForLayoutOffset(
            xOffset - measureTextRegionOffset(),
            yOffset
        )
    }

    fun getPointVisualPositionOnScreen(x: Float, y: Float): VisualLocation {
        var y = y
        y = max(0f, y)
        val stuckLines = renderer.lastStuckLines
        if (stuckLines != null) {
            // position Y maybe negative
            val index = max(0f, (y / this.rowHeight)).toInt()
            if (y < stuckLines.size * this.rowHeight && index < stuckLines.size) {
                return getPointVisualPosition(
                    x,
                    layout.getCharLayoutOffset(
                        stuckLines[index].startLine,
                        0
                    )[0] - this.rowHeight / 2f
                )
            }
        }
        return getPointVisualPosition(x + this.offsetX, y + this.offsetY)
    }

    val scrollMaxY: Int
        /**
         * Get max scroll y
         * 
         * @return max scroll y
         */
        get() {
            val params = layoutParams
            return max(
                0,
                layout.layoutHeight - (if (params == null || params.height == ViewGroup.LayoutParams.WRAP_CONTENT) height.toFloat() else height * (1 - verticalExtraSpaceFactor)).toInt()
            )
        }

    val scrollMaxX: Int
        /**
         * Get max scroll x
         * 
         * @return max scroll x
         */
        get() = max(
            0f,
            layout.layoutWidth + measureTextRegionOffset() - width / 2f
        ).toInt()

    /**
     * Set the factor of extra space in vertical direction. The factor is multiplied with editor
     * height to compute the extra space of vertical viewport. Specially, when factor is zero, no
     * extra space is added.
     * 
     * @param extraSpaceFactor the factor. 0.5 by default.
     * @throws IllegalArgumentException if the factor is negative or bigger than 1.0f
     * @see .getVerticalExtraSpaceFactor
     */
    fun setVerticalExtraSpaceFactor(extraSpaceFactor: Float) {
        require(!(extraSpaceFactor < 0 || extraSpaceFactor > 1.0f)) { "the factor should be in range [0.0, 1.0]" }
        this.verticalExtraSpaceFactor = extraSpaceFactor
        // ensure offset is in scroll range
        eventHandler.scrollBy(0f, 0f)
    }

    /**
     * Get the factor used to compute extra space of vertical viewport.
     * 
     * @see .setVerticalExtraSpaceFactor
     */
    fun getVerticalExtraSpaceFactor(): Float {
        return verticalExtraSpaceFactor
    }

    val searcher: EditorSearcher
        /**
         * Get EditorSearcher
         * 
         * @return EditorSearcher
         */
        get() = editorSearcher!!

    /**
     * Set selection around the given position
     * It will try to set selection as near as possible (Exactly the position if that position exists)
     */
    open fun setSelectionAround(line: Int, column: Int) {
        var column = column
        if (line < this.lineCount) {
            val columnCount = text.getColumnCount(line)
            if (column > columnCount) {
                column = columnCount
            }
            setSelection(line, column)
        } else {
            setSelection(this.lineCount - 1, text.getColumnCount(this.lineCount - 1))
        }
    }

    /**
     * Format text Async
     * 
     * @return Whether the format task is scheduled
     */
    @Synchronized
    fun formatCodeAsync(): Boolean {
        if (this.isFormatting) {
            return false
        }
        val formatter = this.formatter
        formatter.setReceiver(this)
        val formatContent = text.copyText(false)
        formatContent.isUndoEnabled = false
        formatter.format(formatContent, this.cursorRange)
        postInvalidate()
        return true
    }

    /**
     * Format text in the given region.
     * 
     * 
     * Note: Make sure the given positions are valid (line, column and index). Typically, you should
     * obtain a position by an object of [io.github.rosemoe.sora.text.Indexer]
     * 
     * @param start Start position created by Indexer
     * @param end   End position created by Indexer
     * @return Whether the format task is scheduled
     */
    @Synchronized
    fun formatCodeAsync(start: CharPosition, end: CharPosition): Boolean {
        require(start.index <= end.index) { "start > end" }
        if (this.isFormatting) {
            return false
        }
        val formatter = this.formatter
        formatter.setReceiver(this)
        val formatContent = text.copyText(false)
        formatContent.isUndoEnabled = false
        formatter.formatRegion(
            formatContent, TextRange(start, end),
            this.cursorRange
        )
        postInvalidate()
        return true
    }

    private val formatter: Formatter
        get() {
            var formatter: Formatter? = null
            if (formatterProvider != null) {
                formatter = formatterProvider!!.getFormatter(this)
            }
            if (formatter == null) {
                formatter = editorLanguage.formatter
            }
            return formatter
        }

    val cursorRange: TextRange
        /**
         * Get the cursor range of editor
         */
        get() = cursor.range

    val isTextSelected: Boolean
        /**
         * If any text is selected
         */
        get() = cursor.isSelected

    /**
     * Set max and min text size that can be used by user zooming.
     * 
     * 
     * Unit is px.
     */
    fun setScaleTextSizes(minSize: Float, maxSize: Float) {
        require(!(minSize > maxSize)) { "min size can not be bigger than max size" }
        require(!(minSize < 2f)) { "min size must be at least 2px" }
        eventHandler.scaleMinSize = minSize
        eventHandler.scaleMaxSize = maxSize
    }

    /**
     * When the parent is a scrollable view group,
     * request it not to allow horizontal scrolling to be intercepted.
     * Until the code cannot scroll horizontally
     * 
     * @param forceHorizontalScrollable Whether force horizontal scrolling
     */
    fun setInterceptParentHorizontalScrollIfNeeded(forceHorizontalScrollable: Boolean) {
        this.isInterceptParentHorizontalScrollEnabled = forceHorizontalScrollable
        if (!forceHorizontalScrollable) {
            val parent = getParent()
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
    }

    /**
     * Whether to highlight brackets pairs
     */
    var isHighlightBracketPair: Boolean
        get() = highlightBracketPair
        set(highlightBracketPair) {
            this.highlightBracketPair = highlightBracketPair
            if (!highlightBracketPair) {
                styleDelegate.clearFoundBracketPair()
            } else {
                styleDelegate.postUpdateBracketPair()
            }
            invalidate()
        }

    /**
     * @see setInputType
     */
    fun getInputType(): Int {
        return inputType
    }

    /**
     * Specify input type for the editor
     * 
     * 
     * Zero for default input type
     * 
     * @see EditorInfo.inputType
     */
    fun setInputType(inputType: Int) {
        this.inputType = inputType
        restartInput()
    }

    /**
     * Undo last action
     */
    fun undo() {
        val range = text.undo()
        if (range != null) {
            try {
                setSelectionRegion(
                    range.start.line,
                    range.start.column,
                    range.end.line,
                    range.end.column,
                    true,
                    SelectionChangeEvent.CAUSE_TEXT_MODIFICATION
                )
            } catch (e: IndexOutOfBoundsException) {
                // Suppressed, typically because an invalid position is memorized.
            }
        }
        notifyIMEExternalCursorChange()
    }

    /**
     * Redo last action
     */
    fun redo() {
        text.redo()
        notifyIMEExternalCursorChange()
    }

    /**
     * Checks whether we can undo
     * 
     * @return true if we can undo
     */
    fun canUndo(): Boolean {
        return text.canUndo()
    }

    /**
     * Checks whether we can redo
     * 
     * @return true if we can redo
     */
    fun canRedo(): Boolean {
        return text.canRedo()
    }

    /**
     * @return Enabled/Disabled
     * @see setUndoEnabled
     */
    fun isUndoEnabled(): Boolean {
        return undoEnabled
    }

    /**
     * Enable / disabled undo manager
     * 
     * @param enabled Enable/Disable
     */
    fun setUndoEnabled(enabled: Boolean) {
        undoEnabled = enabled
        if (text != null) {
            text.isUndoEnabled = enabled
        }
    }

    var diagnosticIndicatorStyle: DiagnosticIndicatorStyle
        get() = diagnosticStyle
        set(diagnosticIndicatorStyle) {
            this.diagnosticStyle = diagnosticIndicatorStyle
            invalidate()
        }

    /**
     * Start search action mode
     */
    fun beginSearchMode() {
        class SearchActionMode : ActionMode.Callback {
            override fun onCreateActionMode(p1: ActionMode, p2: Menu): Boolean {
                startedActionMode = ACTION_MODE_SEARCH_TEXT
                p2.add(0, 0, 0, getResourceId(R.string.sora_editor_next)).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_IF_ROOM
                )
                p2.add(0, 1, 0, getResourceId(R.string.sora_editor_last)).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_NEVER
                )
                p2.add(0, 2, 0, getResourceId(R.string.sora_editor_replace)).setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_NEVER
                )
                p2.add(0, 3, 0, getResourceId(R.string.sora_editor_replaceAll))
                    .setShowAsActionFlags(
                        MenuItem.SHOW_AS_ACTION_NEVER
                    )
                val sv = SearchView(context)
                sv.setOnQueryTextListener(object : OnQueryTextListener {
                    override fun onQueryTextSubmit(text: String?): Boolean {
                        searcher.gotoNext()
                        return false
                    }

                    override fun onQueryTextChange(text: String?): Boolean {
                        if (text == null || text.isEmpty()) {
                            searcher.stopSearch()
                            return false
                        }
                        searcher.search(text, SearchOptions(false, false))
                        return false
                    }
                })
                p1.customView = sv
                sv.performClick()
                sv.setQueryHint(getString(context, R.string.sora_editor_text_to_search))
                sv.setIconifiedByDefault(false)
                sv.isIconified = false
                return true
            }

            override fun onPrepareActionMode(p1: ActionMode?, p2: Menu?): Boolean {
                return true
            }

            override fun onActionItemClicked(am: ActionMode, p2: MenuItem): Boolean {
                if (!searcher.hasQuery()) {
                    return false
                }
                when (p2.itemId) {
                    1 -> searcher.gotoPrevious()
                    0 -> searcher.gotoNext()
                    2, 3 -> {
                        val replaceAll = p2.itemId == 3
                        val et = EditText(context)
                        et.setHint(getResourceId(R.string.sora_editor_replacement))
                        AlertDialog.Builder(context)
                            .setTitle(getResourceId(if (replaceAll) R.string.sora_editor_replaceAll else R.string.sora_editor_replace))
                            .setView(et)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(
                                getResourceId(R.string.sora_editor_replace)
                            ) { dialog: DialogInterface?, which: Int ->
                                if (replaceAll) {
                                    searcher.replaceAll(
                                        et.getText().toString()
                                    ) { am.finish() }
                                } else {
                                    searcher.replaceCurrentMatch(et.getText().toString())
                                    am.finish()
                                }
                                dialog!!.dismiss()
                            }
                            .show()
                    }
                }
                return false
            }

            override fun onDestroyActionMode(p1: ActionMode?) {
                startedActionMode = ACTION_MODE_NONE
                searcher.stopSearch()
            }
        }

        val callback: ActionMode.Callback = SearchActionMode()
        startActionMode(callback)
    }

    /**
     * Set divider line's left and right margin
     * 
     * @param marginLeft  Margin left for divider line
     * @param marginRight Margin right for divider line
     */
    fun setDividerMargin(@Px marginLeft: Float, @Px marginRight: Float) {
        require(!(marginLeft < 0 || marginRight < 0)) { "margin can not be under zero" }
        dividerMarginLeft = marginLeft
        dividerMarginRight = marginRight
        requestLayoutIfNeeded()
        invalidate()
    }

    /**
     * Set divider line's left and right margin
     * 
     * @param margin Margin left and right for divider line
     */
    fun setDividerMargin(@Px margin: Float) {
        setDividerMargin(margin, margin)
    }

    /**
     * Set line number margin left
     */
    fun setLineNumberMarginLeft(@Px lineNumberMarginLeft: Float) {
        this.lineNumberMarginLeft = lineNumberMarginLeft
        requestLayoutIfNeeded()
        invalidate()
    }

    /**
     * @see .setLineNumberMarginLeft
     */
    @Px
    fun getLineNumberMarginLeft(): Float {
        return lineNumberMarginLeft
    }

    var typefaceLineNumber: Typeface?
        /**
         * @return Typeface of line number
         * @see CodeEditor.setTypefaceLineNumber
         */
        get() = renderer.paintOther.typeface
        /**
         * Set line number's typeface
         * 
         * @param typefaceLineNumber New typeface
         */
        set(typefaceLineNumber) {
            renderer.setTypefaceLineNumber(typefaceLineNumber)
            requestLayoutIfNeeded()
        }

    var typefaceText: Typeface
        /**
         * @return Typeface of text
         * @see CodeEditor.setTypefaceText
         */
        get() = renderer.paint.typeface ?: Typeface.DEFAULT
        /**
         * Set text's typeface
         * 
         * @param typefaceText New typeface
         */
        set(typefaceText) {
            renderer.setTypefaceText(typefaceText)
            requestLayoutIfNeeded()
        }

    var textScaleX: Float
        /**
         * @see .setTextScaleX
         */
        get() = renderer.paint.textScaleX
        /**
         * Set text scale x of Paint
         * 
         * @see Paint.setTextScaleX
         * @see .getTextScaleX
         */
        set(textScaleX) {
            renderer.setTextScaleX(textScaleX)
        }

    var textLetterSpacing: Float
        /**
         * @see .setTextLetterSpacing
         */
        get() = renderer.paint.letterSpacing
        /**
         * Set letter spacing of Paint
         * 
         * @see Paint.setLetterSpacing
         * @see .getTextLetterSpacing
         */
        set(textLetterSpacing) {
            renderer.setLetterSpacing(textLetterSpacing)
            requestLayoutIfNeeded()
        }

    /**
     * Width for insert cursor
     * 
     * @param width Cursor width
     */
    fun setCursorWidth(@Px width: Float) {
        require(!(width < 0)) { "width can not be under zero" }
        insertSelectionWidth = width
        invalidate()
    }

    val lineCount: Int
        /**
         * Get line count
         * 
         * @return line count
         */
        get() = text.lineCount

    val firstVisibleLine: Int
        /**
         * Get first visible line on screen
         * 
         * @return first visible line
         */
        get() {
            try {
                return layout.getLineNumberForRow(this.firstVisibleRow)
            } catch (e: IndexOutOfBoundsException) {
                return 0
            }
        }

    val firstVisibleRow: Int
        /**
         * Get first visible row on screen
         * 
         * @return first visible row
         */
        get() = max(0, this.offsetY / this.rowHeight)

    val lastVisibleRow: Int
        /**
         * Get last visible row on screen.
         * 
         * @return last visible row
         */
        get() = max(
            0,
            min(layout.rowCount - 1, (this.offsetY + height) / this.rowHeight)
        )

    val lastVisibleLine: Int
        /**
         * Get last visible line on screen
         * 
         * @return last visible line
         */
        get() {
            try {
                return layout.getLineNumberForRow(this.lastVisibleRow)
            } catch (e: IndexOutOfBoundsException) {
                return this.lineCount - 1
            }
        }

    /**
     * Checks whether this row is visible on screen
     * 
     * @param row Row to check
     * @return Whether visible
     */
    fun isRowVisible(row: Int): Boolean {
        return (this.firstVisibleRow <= row && row <= this.lastVisibleRow)
    }

    /**
     * Sets line spacing for this TextView.  Each line other than the last line will have its height
     * multiplied by `mult` and have `add` added to it.
     * 
     * @param add  The value in pixels that should be added to each line other than the last line.
     * This will be applied after the multiplier
     * @param mult The value by which each line height other than the last line will be multiplied
     * by
     */
    fun setLineSpacing(add: Float, mult: Float) {
        lineSpacingAdd = add
        lineSpacingMultiplier = mult
    }

    var lineSpacingExtra: Float
        /**
         * Gets the line spacing extra space
         * 
         * @return the extra space that is added to the height of each lines of this TextView.
         * @see .setLineSpacing
         * @see .getLineSpacingMultiplier
         */
        get() = lineSpacingAdd
        /**
         * @param lineSpacingExtra The value in pixels that should be added to each line other than the last line.
         * *            This will be applied after the multiplier
         */
        set(lineSpacingExtra) {
            lineSpacingAdd = lineSpacingExtra
            invalidate()
        }

    /**
     * @return the value by which each line's height is multiplied to get its actual height.
     * @see .setLineSpacingMultiplier
     */
    fun getLineSpacingMultiplier(): Float {
        return lineSpacingMultiplier
    }

    /**
     * @param lineSpacingMultiplier The value by which each line height other than the last line will be multiplied
     * *             by. Default 1.0f
     */
    fun setLineSpacingMultiplier(lineSpacingMultiplier: Float) {
        this.lineSpacingMultiplier = lineSpacingMultiplier
        invalidate()
    }

    val lineSpacingPixels: Int
        /**
         * Get actual line spacing in pixels.
         */
        get() {
            val metrics = renderer.metricsText
            return (((metrics.descent - metrics.ascent) * (lineSpacingMultiplier - 1f) + lineSpacingAdd).toInt()) / 2 * 2
        }

    /**
     * Get baseline directly
     * 
     * @param row Row
     * @return baseline y offset
     */
    fun getRowBaseline(row: Int): Int {
        val lineSpacing = this.lineSpacingPixels
        val metrics = renderer.metricsText
        return max(
            1,
            metrics.descent - metrics.ascent + lineSpacing
        ) * (row + 1) - metrics.descent - lineSpacing / 2
    }

    val rowHeight: Int
        /**
         * Get row height
         * 
         * @return height of single row
         */
        get() {
            val metrics = renderer.metricsText
            // Do not let the row height be zero...
            return max(1, metrics.descent - metrics.ascent + this.lineSpacingPixels)
        }

    /**
     * Get row top y offset
     * 
     * @param row Row
     * @return top y offset
     */
    fun getRowTop(row: Int): Int {
        return this.rowHeight * row
    }

    /**
     * Get row bottom y offset
     * 
     * @param row Row
     * @return Bottom y offset
     */
    fun getRowBottom(row: Int): Int {
        return this.rowHeight * (row + 1)
    }

    /**
     * Get the top of text in target row
     */
    fun getRowTopOfText(row: Int): Int {
        return getRowTop(row) + this.lineSpacingPixels / 2
    }

    /**
     * Get the bottom of text in target row
     */
    fun getRowBottomOfText(row: Int): Int {
        return getRowBottom(row) - this.lineSpacingPixels / 2
    }

    val rowHeightOfText: Int
        /**
         * Get the height of text in row
         */
        get() {
            val metrics = renderer.metricsText
            return metrics.descent - metrics.ascent
        }

    val offsetX: Int
        /**
         * Get scroll x
         * 
         * @return scroll x
         */
        get() = eventHandler.scroller.currX

    val offsetY: Int
        /**
         * Get scroll y
         * 
         * @return scroll y
         */
        get() = eventHandler.scroller.currY

    /**
     * Indicate whether the layout is working
     */
    @UnsupportedUserUsage
    fun setLayoutBusy(busy: Boolean) {
        if (layoutBusy && !busy) {
            if (wordwrap && eventHandler.positionNotApplied) {
                eventHandler.positionNotApplied = false
                val line = getFirst(eventHandler.memoryPosition)
                val column = getSecond(eventHandler.memoryPosition)
                // Compute new scroll position
                val row = (layout as WordwrapLayout).findRow(line, column)
                val afterScrollY = row * this.rowHeight - eventHandler.focusY
                val scroller = eventHandler.scroller
                dispatchEvent(
                    ScrollEvent(
                        this, scroller.currX,
                        scroller.currY, 0, afterScrollY.toInt(), ScrollEvent.CAUSE_SCALE_TEXT
                    )
                )
                scroller.startScroll(0, afterScrollY.toInt(), 0, 0, 0)
                scroller.abortAnimation()
            }
            // IMPORTANT restart input after clearing the busy flag
            // otherwise, the connection may fallback to inactive mode
            this.layoutBusy = false
            restartInput()
            postInvalidate()
            dispatchEvent(LayoutStateChangeEvent(this, false))
            return
        }
        if (layoutBusy == busy) {
            return
        }
        this.layoutBusy = busy
        dispatchEvent(LayoutStateChangeEvent(this, busy))
    }

    /**
     * @see .setEditable
     */
    fun getEditable(): Boolean {
        return editable
    }

    /**
     * Set whether text can be edited
     * 
     * @param editable Editable
     */
    fun setEditable(editable: Boolean) {
        this.editable = editable
        if (!editable) {
            hideSoftInput()
            snippetController.stopSnippet()
        }
    }

    var isBlockLineEnabled: Boolean
        get() = blockLineEnabled
        set(enabled) {
            blockLineEnabled = enabled
            invalidate()
        }

    /**
     * Begin a rejection on composing texts
     */
    fun beginComposingTextRejection() {
        rejectComposingCount++
    }

    /**
     * If the editor accepts composing text now, according to composing text rejection count
     */
    fun acceptsComposingText(): Boolean {
        return rejectComposingCount == 0
    }

    /**
     * End a rejection on composing texts
     */
    fun endComposingTextRejection() {
        rejectComposingCount--
        if (rejectComposingCount < 0) {
            rejectComposingCount = 0
        }
    }

    /**
     * Check if there is a mouse inside editor, hovering
     */
    fun hasMouseHovering(): Boolean {
        return mouseHover
    }

    /**
     * Check if there is a mouse inside editor with any button pressed
     */
    fun hasMousePressed(): Boolean {
        return mouseButtonPressed
    }

    val isInMouseMode: Boolean
        /**
         * Check if editor is in mouse mode.
         * 
         * @see DirectAccessProps.mouseMode
         */
        get() {
            when (props.mouseMode) {
                DirectAccessProps.MOUSE_MODE_ALWAYS -> {
                    return true
                }

                DirectAccessProps.MOUSE_MODE_NEVER -> {
                    return false
                }
            }
            // MOUSE_MODE_AUTO
            return hasMouseHovering() || hasMousePressed()
        }

    open val selectingTarget: CharPosition
        /**
         * Get the target cursor to move when shift is pressed
         */
        get() {
            if (cursor.left().equals(selectionAnchor)) {
                return cursor.right()
            } else {
                return cursor.left()
            }
        }

    /**
     * Make sure the moving selection is visible
     */
    protected fun ensureSelectingTargetVisible() {
        if (cursor.left().equals(selectionAnchor)) {
            // Ensure right selection visible
            ensureSelectionVisible()
        } else {
            ensurePositionVisible(cursor.leftLine, cursor.leftColumn)
        }
    }

    protected fun ensureSelectionAnchorAvailable() {
        if (selectionAnchor == null || !text.isValidPosition(selectionAnchor)) {
            selectionAnchor = cursor.right()
        }
    }

    /**
     * Move or extend selection, according to `extend` param.
     * 
     * @param extend True if you want to extend selection.
     */
    fun moveOrExtendSelection(movement: SelectionMovement, extend: Boolean) {
        if (extend) {
            extendSelection(movement)
        } else {
            moveSelection(movement)
        }
    }

    /**
     * Extend the selection, based on the selection anchor (select text)
     */
    fun extendSelection(movement: SelectionMovement) {
        ensureSelectionAnchorAvailable()
        val sel = movement.getPositionAfterMovement(this, this.selectingTarget)
        setSelectionRegion(
            selectionAnchor!!.line,
            selectionAnchor!!.column,
            sel.line,
            sel.column,
            false,
            SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE
        )
        if (movement == SelectionMovement.PAGE_UP) {
            eventHandler.scrollBy(0f, -height.toFloat(), true)
        } else if (movement == SelectionMovement.PAGE_DOWN) {
            eventHandler.scrollBy(0f, height.toFloat(), true)
        }
        ensureSelectingTargetVisible()
    }

    /**
     * Move the selection. Selected text will be de-selected.
     */
    fun moveSelection(movement: SelectionMovement) {
        if (cursor.isSelected) {
            if (movement == SelectionMovement.LEFT) {
                setSelection(
                    cursor.leftLine,
                    cursor.leftColumn,
                    SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE
                )
                return
            }
            if (movement == SelectionMovement.RIGHT) {
                setSelection(
                    cursor.rightLine,
                    cursor.rightColumn,
                    SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE
                )
                return
            }
        }
        val pos: CharPosition?
        when (movement.basePosition) {
            MovingBasePosition.LEFT_SELECTION -> pos = cursor.left()
            MovingBasePosition.RIGHT_SELECTION -> pos = cursor.right()
            else -> {
                ensureSelectionAnchorAvailable()
                pos = selectionAnchor
            }
        }
        val sel = movement.getPositionAfterMovement(this, pos!!)
        if (movement == SelectionMovement.PAGE_UP) {
            eventHandler.scrollBy(0f, -height.toFloat(), true)
        } else if (movement == SelectionMovement.PAGE_DOWN) {
            eventHandler.scrollBy(0f, height.toFloat(), true)
        }
        setSelection(sel.line, sel.column, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
    }

    /**
     * Move selection to given position
     * 
     * @param line   The line to move
     * @param column The column to move
     */
    fun setSelection(line: Int, column: Int) {
        setSelection(line, column, SelectionChangeEvent.CAUSE_UNKNOWN)
    }

    /**
     * Move selection to given position
     * 
     * @param line   The line to move
     * @param column The column to move
     */
    fun setSelection(line: Int, column: Int, cause: Int) {
        setSelection(line, column, true, cause)
    }

    /**
     * Move selection to given position
     * 
     * @param line          The line to move
     * @param column        The column to move
     * @param makeItVisible Make the character visible
     */
    fun setSelection(line: Int, column: Int, makeItVisible: Boolean) {
        setSelection(line, column, makeItVisible, SelectionChangeEvent.CAUSE_UNKNOWN)
    }

    /**
     * Move selection to given position
     * 
     * @param line          The line to move
     * @param column        The column to move
     * @param makeItVisible Make the character visible
     */
    fun setSelection(line: Int, column: Int, makeItVisible: Boolean, cause: Int) {
        var column = column
        cursorAnimator.markStartPos()
        if (column > 0 && Character.isHighSurrogate(text.charAt(line, column - 1))) {
            column++
            if (column > text.getColumnCount(line)) {
                column--
            }
        }
        cursor.set(line, column)
        if (highlightCurrentBlock) {
            this.blockIndex = findCursorBlock()
        }
        updateCursor()
        updateSelection()
        if (editable && !eventHandler.hasAnyHeldHandle() && acceptsComposingText()) {
            cursorAnimator.markEndPos()
            cursorAnimator.start()
        }

        // Update cursor anchor
        selectionAnchor = cursor.right()

        renderContext.invalidateRenderNodes()
        if (makeItVisible) {
            ensurePositionVisible(line, column)
        } else {
            invalidate()
        }
        onSelectionChanged(cause)
    }

    /**
     * Select all text
     */
    fun selectAll() {
        setSelectionRegion(0, 0, this.lineCount - 1, text.getColumnCount(this.lineCount - 1))
    }

    /**
     * Set selection region with a call to [ensureSelectionVisible]
     * 
     * @param lineLeft    Line left
     * @param columnLeft  Column Left
     * @param lineRight   Line right
     * @param columnRight Column right
     */
    fun setSelectionRegion(
        lineLeft: Int, columnLeft: Int, lineRight: Int,
        columnRight: Int, cause: Int
    ) {
        setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, true, cause)
    }

    /**
     * Set selection region with a call to [ensureSelectionVisible]
     * 
     * @param lineLeft    Line left
     * @param columnLeft  Column Left
     * @param lineRight   Line right
     * @param columnRight Column right
     */
    fun setSelectionRegion(
        lineLeft: Int, columnLeft: Int, lineRight: Int,
        columnRight: Int
    ) {
        setSelectionRegion(
            lineLeft,
            columnLeft,
            lineRight,
            columnRight,
            true,
            SelectionChangeEvent.CAUSE_UNKNOWN
        )
    }

    /**
     * Set selection region
     * 
     * @param lineLeft         Line left
     * @param columnLeft       Column Left
     * @param lineRight        Line right
     * @param columnRight      Column right
     * @param makeRightVisible Whether to make right cursor visible
     */
    fun setSelectionRegion(
        lineLeft: Int, columnLeft: Int, lineRight: Int,
        columnRight: Int, makeRightVisible: Boolean
    ) {
        setSelectionRegion(
            lineLeft,
            columnLeft,
            lineRight,
            columnRight,
            makeRightVisible,
            SelectionChangeEvent.CAUSE_UNKNOWN
        )
    }

    /**
     * Set selection region
     * 
     * @param lineLeft         Line left
     * @param columnLeft       Column Left
     * @param lineRight        Line right
     * @param columnRight      Column right
     * @param makeRightVisible Whether to make right cursor visible
     */
    fun setSelectionRegion(
        lineLeft: Int, columnLeft: Int, lineRight: Int,
        columnRight: Int, makeRightVisible: Boolean, cause: Int
    ) {
        var columnLeft = columnLeft
        var columnRight = columnRight
        requestFocus()
        val start = text.getCharIndex(lineLeft, columnLeft)
        val end = text.getCharIndex(lineRight, columnRight)
        if (start == end) {
            setSelection(lineLeft, columnLeft, makeRightVisible, cause)
            return
        }
        if (start > end) {
            setSelectionRegion(
                lineRight,
                columnRight,
                lineLeft,
                columnLeft,
                makeRightVisible,
                cause
            )
            Log.w(
                LOG_TAG,
                "setSelectionRegion() error: start > end:start = " + start + " end = " + end + " lineLeft = " + lineLeft + " columnLeft = " + columnLeft + " lineRight = " + lineRight + " columnRight = " + columnRight
            )
            return
        }
        cursorAnimator.cancel()
        val lastState = cursor.isSelected
        if (columnLeft > 0) {
            val column = columnLeft - 1
            val ch = text.charAt(lineLeft, column)
            if (Character.isHighSurrogate(ch)) {
                columnLeft++
                if (columnLeft > text.getColumnCount(lineLeft)) {
                    columnLeft--
                }
            }
        }
        if (columnRight > 0) {
            val column = columnRight - 1
            val ch = text.charAt(lineRight, column)
            if (Character.isHighSurrogate(ch)) {
                columnRight++
                if (columnRight > text.getColumnCount(lineRight)) {
                    columnRight--
                }
            }
        }
        cursor.setLeft(lineLeft, columnLeft)
        cursor.setRight(lineRight, columnRight)
        updateCursor()
        updateSelection()
        renderContext.invalidateRenderNodes()

        // Update selection anchor
        if (!cursor.left().equals(selectionAnchor) && !cursor.right().equals(selectionAnchor)) {
            selectionAnchor = cursor.right()
        }

        if (makeRightVisible) {
            if (cause == SelectionChangeEvent.CAUSE_SEARCH) {
                ensurePositionVisible(lineLeft, columnLeft)
                lastMakeVisible = 0
                ensurePositionVisible(lineRight, columnRight)
            } else {
                ensurePositionVisible(lineRight, columnRight)
            }
        } else {
            invalidate()
        }
        onSelectionChanged(cause)
    }

    fun registerExtraStylesProvider(provider: ExtraStylesProvider) {
        if (!extraStylesProviders.contains(provider)) {
            extraStylesProviders.add(provider)
            invalidate()
        }
    }

    fun unregisterExtraStylesProvider(provider: ExtraStylesProvider) {
        if (extraStylesProviders.remove(provider)) {
            invalidate()
        }
    }

    /**
     * Paste text from clip board
     */
    fun pasteText() {
        try {
            val clip = clipboardManager?.primaryClip ?: return
            pasteText(clipDataToString(clip))
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error pasting text to editor", e)
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Paste external text into editor
     */
    fun pasteText(text: CharSequence?) {
        if (text != null && inputConnection != null) {
            inputConnection.commitText(text, 1)
            if (props.formatPastedText) {
                formatCodeAsync(lastInsertion!!.start, lastInsertion!!.end)
            }
            notifyIMEExternalCursorChange()
        }
    }

    /**
     * Copy text to clipboard.
     * 
     * @param shouldCopyLine State whether the editor should select whole line if
     * cursor is not in selection mode.
     */
    /**
     * Copy text to clipboard.
     */
    @JvmOverloads
    fun copyText(shouldCopyLine: Boolean = true) {
        if (cursor.isSelected) {
            copyTextToClipboard(text, cursor.left, cursor.right)
        } else if (shouldCopyLine) {
            copyLine()
        } else {
            val text = lineSeparator.content
            copyTextToClipboard(text, 0, text.length)
        }
    }

    /**
     * Copy the given text region to clipboard, and follow editor's IPC properties.
     */
    protected fun copyTextToClipboard(text: CharSequence, start: Int, end: Int) {
        if (end < start) {
            return
        }
        if (end - start > props.clipboardTextLengthLimit) {
            Toast.makeText(
                context,
                getResourceId(R.string.sora_editor_clip_text_length_too_large),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        try {
            val clip =
                if (text is Content) text.substring(start, end) else text.subSequence(start, end)
                    .toString()
            clipboardManager?.setPrimaryClip(ClipData.newPlainText(clip, clip))
        } catch (e: RuntimeException) {
            if (e.cause is TransactionTooLargeException) {
                Toast.makeText(
                    context,
                    getResourceId(R.string.sora_editor_clip_text_length_too_large),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.w(LOG_TAG, e)
                Toast.makeText(context, e.javaClass.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Copies the current line to clipboard.
     */
    private fun copyLine() {
        val cursor = this.cursor
        if (cursor.isSelected) {
            copyText()
            return
        }

        val line = cursor.left().line
        setSelectionRegion(line, 0, line, this.text.getColumnCount(line))
        copyText(false)
    }

    /**
     * Copy text to clipboard and delete them
     */
    fun cutText() {
        if (cursor.isSelected) {
            copyText()
            deleteText()
            notifyIMEExternalCursorChange()
        } else {
            cutLine()
        }
    }

    /**
     * Copy the current line to clipboard and delete it.
     */
    fun cutLine() {
        val cursor = this.cursor
        if (cursor.isSelected) {
            cutText()
            return
        }

        val left = cursor.left()
        val line = left.line
        val column = this.text.getColumnCount(left.line)

        if (line + 1 == this.lineCount) {
            val columnCount = this.text.getColumnCount(line)
            if (columnCount == 0) {
                // copy line separator
                copyText(false)
                return
            }
            setSelectionRegion(line, 0, line, this.text.getColumnCount(line))
        } else {
            setSelectionRegion(line, 0, line + 1, 0)
        }

        cutText()
        if (props.placeSelOnPreviousLineAfterCut) {
            moveSelection(SelectionMovement.LEFT)
        }
    }

    /**
     * Duplicates the current line.
     * Does not selects the duplicated line.
     */
    fun duplicateLine() {
        val cursor = this.cursor
        if (cursor.isSelected) {
            duplicateSelection()
            return
        }

        val left = cursor.left()
        setSelectionRegion(left.line, 0, left.line, this.text.getColumnCount(left.line), true)
        duplicateSelection("\n", false)
    }

    /**
     * Copies the current selection and pastes it at the right selection handle.
     * 
     * @param selectDuplicate Whether to select the duplicated content.
     */
    /**
     * Copies the current selection and pastes it at the right selection handle,
     * then selects the duplicated content.
     */
    @JvmOverloads
    fun duplicateSelection(selectDuplicate: Boolean = true) {
        duplicateSelection("", selectDuplicate)
    }

    /**
     * Copies the current selection, add the `prefix` to it
     * and pastes it at the right selection handle.
     * 
     * @param prefix          The prefix for the selected content.
     * @param selectDuplicate Whether to select the duplicated content.
     */
    fun duplicateSelection(prefix: String?, selectDuplicate: Boolean) {
        val cursor = this.cursor
        if (!cursor.isSelected) {
            return
        }

        val left = cursor.left()
        val right = cursor.right().fromThis()
        val sub = this.text.subContent(left.line, left.column, right.line, right.column)

        setSelection(right.line, right.column)
        commitText(prefix + sub, false)

        if (selectDuplicate) {
            val r = cursor.right()
            setSelectionRegion(right.line, right.column, r.line, r.column)
        }
    }

    /**
     * Selects the word at the left selection handle.
     */
    fun selectCurrentWord() {
        val left = cursor.left()
        selectWord(left.line, left.column)
    }

    /**
     * Selects the word at the given character position.
     * 
     * @param line   The line.
     * @param column The column.
     */
    fun selectWord(line: Int, column: Int) {
        val range = getWordRange(line, column)
        val start = range.start
        val end = range.end
        setSelectionRegion(
            start.line,
            start.column,
            end.line,
            end.column,
            SelectionChangeEvent.CAUSE_LONG_PRESS
        )
    }

    /**
     * @see .getWordRange
     */
    fun getWordRange(line: Int, column: Int): TextRange {
        return getWordRange(line, column, props.useICULibToSelectWords)
    }

    /**
     * Get the range of the word at given character position.
     * 
     * @param line   The line.
     * @param column The column.
     * @param useIcu Whether to use the ICU library to get word edges.
     * @return The word range.
     */
    fun getWordRange(line: Int, column: Int, useIcu: Boolean): TextRange {
        return getWordRange(this.text, line, column, useIcu)
    }

    /**
     * Set the text to be displayed.
     * With no extra arguments.
     * 
     * @param text the new text you want to display
     */
    fun setText(text: CharSequence?) {
        setText(text, true, null)
    }

    /**
     * Sets the text to be displayed.
     * 
     * @param text           the new text you want to display
     * @param extraArguments Extra arguments for the document. This [Bundle] object is passed
     * to all languages and plugins in editor.
     */
    fun setText(text: CharSequence?, extraArguments: Bundle?) {
        setText(text, true, extraArguments)
    }

    /**
     * Sets the text to be displayed.
     * 
     * @param text               the new text you want to display
     * @param reuseContentObject If the given `text` is an instance of [Content], reuse it.
     * @param extraArguments     Extra arguments for the document. This [Bundle] object is passed
     * to all languages and plugins in editor.
     */
    fun setText(
        text: CharSequence?, reuseContentObject: Boolean,
        extraArguments: Bundle?
    ) {
        var text = text
        if (text == null) {
            text = ""
        }

        if (this._text != null) {
            this._text!!.removeContentListener(this)
            this._text!!.resetBatchEdit()
        }
        this._extraArguments = extraArguments ?: Bundle()
        lastInsertion = null
        if (reuseContentObject && text is Content) {
            this._text = text
            this._text!!.resetBatchEdit()
            renderer.updateTimestamp()
        } else {
            this._text = Content(text)
        }
        styleDelegate.reset()
        textStyles = null
        _cursor = this._text!!.cursor
        selectionAnchor = cursor.right()
        eventHandler.reset()
        this._text!!.addContentListener(this)
        this._text!!.isUndoEnabled = undoEnabled
        this._text!!.isBidiEnabled = true
        renderContext.reset(this._text!!.lineCount)
        renderer.onEditorFullTextUpdate()

        if (editorLanguage != null) {
            editorLanguage.analyzeManager.reset(
                ContentReference(this.text),
                this.extraArguments
            )
            editorLanguage.formatter.cancel()
        }
        inlayHints = null

        dispatchEvent(
            ContentChangeEvent(
                this,
                ContentChangeEvent.ACTION_SET_NEW_TEXT,
                CharPosition(),
                this.text.indexer.getCharPosition(
                    this.lineCount - 1, this.text.getColumnCount(this.lineCount - 1)
                ),
                this.text,
                false
            )
        )
        createLayout()
        if (inputMethodManager != null) {
            inputMethodManager!!.restartInput(this)
        }
        requestLayout()
        renderContext.invalidateRenderNodes()
        invalidate()
    }

    /**
     * Set the editor's text size in sp unit. This value must be greater than 0
     * 
     * @param textSize the editor's text size in **Sp** units.
     */
    fun setTextSize(textSize: Float) {
        val context = getContext()
        val res: Resources

        if (context == null) {
            res = Resources.getSystem()
        } else {
            res = context.resources
        }

        this.textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            textSize,
            res.displayMetrics
        )
    }

    /**
     * Render ASCII Function characters
     */
    var isRenderFunctionCharacters: Boolean
        get() = renderFunctionCharacters
        set(value) {
            if (renderFunctionCharacters != value) {
                renderFunctionCharacters = value
                renderer.onTextStyleUpdate()
                requestLayoutIfNeeded()
                createLayout()
                invalidate()
            }
        }

    /**
     * Subscribe event of the given type.
     * 
     * @see EventManager.subscribeEvent
     */
    fun <T : Event> subscribeEvent(
        eventType: Class<T>,
        receiver: EventReceiver<T>
    ): SubscriptionReceipt<T> {
        return eventManager.subscribeEvent(eventType, receiver)
    }

    /**
     * Subscribe event of the given type, without [io.github.rosemoe.sora.event.Unsubscribe].
     * 
     * @see EventManager.subscribeEvent
     */
    fun <T : Event> subscribeAlways(
        eventType: Class<T>,
        receiver: NoUnsubscribeReceiver<T>
    ): SubscriptionReceipt<T> {
        return eventManager.subscribeAlways(eventType, receiver)
    }

    /**
     * Dispatch the given event
     * 
     * @see EventManager.dispatchEvent
     */
    fun <T : Event> dispatchEvent(event: T): Int {
        return eventManager.dispatchEvent(event)
    }

    /**
     * Create a new [EventManager] instance that can be used to subscribe events in editor,
     * as a child instance of editor.
     * 
     * @return Child EventManager instance
     */
    fun createSubEventManager(): EventManager {
        return EventManager(eventManager)
    }

    val isFormatting: Boolean
        /**
         * Check whether the editor is currently performing a format operation
         * 
         * @return whether the editor is currently formatting
         */
        get() = this.formatter.isRunning()

    val textPaint: Paint
        /**
         * Get the paint of the editor
         * You should not change text size and other attributes that are related to text measuring by the object
         * 
         * @return The paint which is used by the editor now
         */
        get() = renderer.paint

    val otherPaint: Paint
        get() = renderer.paintOther

    val graphPaint: Paint
        get() = renderer.paintGraph

    /**
     * Move selection to line start with scrolling
     * 
     * @param line Line index to jump
     */
    fun jumpToLine(line: Int) {
        setSelection(line, 0)
    }

    /**
     * Mark current selection position as a point of cursor range.
     * When user taps to select another point in text, the text between the marked point and
     * newly chosen point is selected.
     * 
     * @see .isInLongSelect
     * @see .endLongSelect
     */
    fun beginLongSelect() {
        if (!isEditable) {
            return
        }
        if (cursor.isSelected) {
            setSelection(cursor.leftLine, cursor.leftColumn)
        }
        isInLongSelect = true
        invalidate()
    }

    /**
     * Marks long select mode is end.
     * This does nothing but set the flag to false.
     */
    fun endLongSelect() {
        isInLongSelect = false
    }


    //-------------------------------------------------------------------------------
    //-------------------------IME Interaction---------------------------------------
    //-------------------------------------------------------------------------------
    /**
     * Rerun analysis forcibly
     */
    fun rerunAnalysis() {
        if (editorLanguage != null) {
            editorLanguage.analyzeManager.rerun()
        }
    }

    @set:UiThread
    var styles: Styles?
        /**
         * Get analyze result.
         * **Do not make changes to it or read concurrently**
         */
        get() = textStyles
        set(styles) {
            textStyles = styles
            if (highlightCurrentBlock) {
                this.blockIndex = findCursorBlock()
            }
            renderContext.invalidateRenderNodes()
            renderer.updateTimestamp()
            invalidate()
        }

    @UiThread
    fun updateStyles(styles: Styles, range: StyleUpdateRange?) {
        if (textStyles !== styles || range == null) {
            this.styles = styles
            return
        }
        if (highlightCurrentBlock) {
            this.blockIndex = findCursorBlock()
        }
        renderContext.updateForRange(range)
        renderer.updateTimestamp()
        invalidate()
    }


    fun registerDiagnosticProvider(provider: DiagnosticProvider) {
        synchronized(diagnosticProviders) {
            if (!diagnosticProviders.contains(provider)) {
                diagnosticProviders.add(provider)
                invalidateDiagnostics()
            }
        }
    }

    fun unregisterDiagnosticProvider(provider: DiagnosticProvider) {
        synchronized(diagnosticProviders) {
            if (diagnosticProviders.remove(provider)) {
                invalidateDiagnostics()
            }
        }
    }

    fun invalidateDiagnostics() {
        synchronized(diagnosticProviders) {
            val merged = DiagnosticsContainer(false)
            for (provider in diagnosticProviders) {
                provider.provideDiagnostics(merged)
            }
            internalSetDiagnostics(if (merged.getRegions().isEmpty()) null else merged)
        }
    }

    private fun internalSetDiagnostics(diagnostics: DiagnosticsContainer?) {
        if (this.diagnostics != null) {
            this.diagnostics!!.detachEditor()
        }

        this.diagnostics = diagnostics

        if (this.diagnostics != null) {
            this.diagnostics!!.attachEditor(this)
        }
        invalidate()
    }

    fun registerInlayHintProvider(provider: InlayHintProvider) {
        synchronized(inlayHintProviders) {
            if (!inlayHintProviders.contains(provider)) {
                inlayHintProviders.add(provider)
                invalidateInlayHints()
            }
        }
    }

    fun unregisterInlayHintProvider(provider: InlayHintProvider) {
        synchronized(inlayHintProviders) {
            if (inlayHintProviders.remove(provider)) {
                invalidateInlayHints()
            }
        }
    }

    fun invalidateInlayHints() {
        synchronized(inlayHintProviders) {
            val merged = InlayHintsContainer()
            for (provider in inlayHintProviders) {
                provider.provideInlayHints(merged)
            }
            internalSetInlayHints(if (merged.isEmpty()) null else merged)
        }
    }

    private fun internalSetInlayHints(inlayHints: InlayHintsContainer?) {
        val affectedLines = MutableIntSet()
        val oldInlayHints = this.inlayHints
        if (oldInlayHints != null) {
            affectedLines.addAll(oldInlayHints.getLineNumbers())
        }
        if (inlayHints != null) {
            affectedLines.addAll(inlayHints.getLineNumbers())
        }
        this.inlayHints = inlayHints
        val range = IntSetUpdateRange(affectedLines)
        if (!layoutBusy) {
            layout.invalidateLines(range)
        } else {
            createLayout()
        }
        renderContext.invalidateRenderNodes()
    }

    fun registerHighlightTextProvider(provider: HighlightTextProvider) {
        synchronized(highlightTextProviders) {
            if (!highlightTextProviders.contains(provider)) {
                highlightTextProviders.add(provider)
                invalidateHighlightTexts()
            }
        }
    }

    fun unregisterHighlightTextProvider(provider: HighlightTextProvider) {
        synchronized(highlightTextProviders) {
            if (highlightTextProviders.remove(provider)) {
                invalidateHighlightTexts()
            }
        }
    }

    fun invalidateHighlightTexts() {
        synchronized(highlightTextProviders) {
            val merged = HighlightTextContainer()
            for (provider in highlightTextProviders) {
                provider.provideHighlightTexts(merged)
            }
            internalSetHighlightTexts(if (merged.isEmpty()) null else merged)
        }
    }

    private fun internalSetHighlightTexts(highlightTexts: HighlightTextContainer?) {
        val affectedLines = MutableIntSet()
        val oldHighlights = this.highlightTexts
        if (oldHighlights != null) {
            val lines = oldHighlights.lineNumbers
            for (line in lines) {
                affectedLines.add(line)
            }
        }
        this.highlightTexts = highlightTexts
        if (highlightTexts != null) {
            val lines = highlightTexts.lineNumbers
            for (line in lines) {
                affectedLines.add(line)
            }
        }
        val range = IntSetUpdateRange(affectedLines)
        if (!layoutBusy) {
            layout.invalidateLines(range)
        } else {
            createLayout()
        }
        renderContext.invalidateRenderNodes()
    }


    /**
     * Hide auto complete window if shown
     */
    fun hideAutoCompleteWindow() {
        if (completionWindow != null) {
            completionWindow!!.hide()
        }
    }

    /**
     * Display soft input method for self
     */
    fun showSoftInput() {
        if (isEditable && isEnabled) {
            // Note that we always try to get focused at this time.
            if (isInTouchMode && !isFocused) {
                requestFocusFromTouch()
            }
            if (!isFocused) {
                requestFocus()
            }

            if (checkSoftInputEnabled()) inputMethodManager!!.showSoftInput(this, 0)
        }
        invalidate()
    }

    /**
     * Hide soft input
     */
    fun hideSoftInput() {
        inputMethodManager!!.hideSoftInputFromWindow(windowToken, 0)
    }

    /**
     * Check whether the soft keyboard is enabled for this editor. Unlike [.isSoftKeyboardEnabled],
     * this method also checks whether a hardware keyboard is connected.
     * 
     * @return Whether the editor should show soft keyboard.
     * @see .isSoftKeyboardEnabled
     * @see .isDisableSoftKbdIfHardKbdAvailable
     */
    protected fun checkSoftInputEnabled(): Boolean {
        return !(this.isDisableSoftKbdIfHardKbdAvailable
                && isHardKeyboardConnected(context)) && this.isSoftKeyboardEnabled
    }

    var isSoftKeyboardEnabled: Boolean
        /**
         * Returns whether the soft keyboard is enabled.
         * 
         * @return Whether the soft keyboard is enabled.
         */
        get() = this.isSoftKbdEnabled
        /**
         * Set whether the soft keyboard is enabled for this editor. Set to `true` by default.
         * 
         * @param isEnabled Whether the soft keyboard is enabled.
         */
        set(isEnabled) {
            if (this.isSoftKbdEnabled == isEnabled) {
                // no need to do anything
                return
            }

            this.isSoftKbdEnabled = isEnabled
            hideSoftInput()
            restartInput()
        }

    var isDisableSoftKbdIfHardKbdAvailable: Boolean
        /**
         * Returns whether the soft keyboard should be enabled if hardware keyboard is connected.
         * 
         * @return Whether the soft keyboard should be enabled if hardware keyboard is connected.
         */
        get() = isDisableSoftKbdOnHardKbd
        /**
         * Set whether the soft keyboard should be disabled for this editor if a hardware keyboard is
         * connected to the device. Set to `true` by default.
         * 
         * @param isDisabled Whether the soft keyboard should be enabled if hardware keyboard is connected.
         */
        set(isDisabled) {
            if (this.isDisableSoftKbdOnHardKbd == isDisabled) {
                // no need to do anything
                return
            }

            this.isDisableSoftKbdOnHardKbd = isDisabled
            hideSoftInput()
            restartInput()
        }

    /**
     * Send current selection position to input method
     */
    open fun updateSelection() {
        if (props.disallowSuggestions) {
            val index = Random().nextInt()
            inputMethodManager!!.updateSelection(this, index, index, -1, -1)
            return
        }
        if (inputConnection.composingText.preSetComposing) {
            return
        }
        var candidatesStart = -1
        var candidatesEnd = -1
        if (inputConnection.composingText.isComposing) {
            try {
                candidatesStart = inputConnection.composingText.startIndex
                candidatesEnd = inputConnection.composingText.endIndex
            } catch (e: IndexOutOfBoundsException) {
                // Ignored
            }
        }
        inputMethodManager!!.updateSelection(
            this,
            cursor.left,
            cursor.right,
            candidatesStart,
            candidatesEnd
        )
    }

    /**
     * Update request result for monitoring request
     */
    protected fun updateExtractedText() {
        if (extractingTextRequest != null) {
            val text = extractText(extractingTextRequest!!)
            inputMethodManager!!.updateExtractedText(this, extractingTextRequest!!.token, text)
        }
    }

    //-------------------------------------------------------------------------------
    //------------------------Internal Callbacks-------------------------------------
    //-------------------------------------------------------------------------------
    /**
     * Set request needed to update when editor updates selection
     */
    open fun setExtracting(request: ExtractedTextRequest?) {
        if (props.disallowSuggestions) {
            extractingTextRequest = null
            return
        }
        extractingTextRequest = request
    }

    /**
     * Extract text in editor for input method
     */
    open fun extractText(request: ExtractedTextRequest): ExtractedText? {
        if (props.disallowSuggestions || props.disableTextExtracting) {
            return null
        }
        val cur = cursor
        val text = ExtractedText()
        val selBegin = cur.left
        val selEnd = cur.right
        var startOffset = 0
        if (request.hintMaxChars == 0) {
            request.hintMaxChars = props.maxIPCTextLength
        }
        if (startOffset + request.hintMaxChars < selBegin) {
            startOffset = selBegin - request.hintMaxChars / 2
            startOffset = min(startOffset, selBegin) // Ensure not negative
        }
        text.text = inputConnection.getTextRegion(
            startOffset,
            startOffset + request.hintMaxChars,
            request.flags
        )
        text.startOffset = startOffset
        text.selectionStart = selBegin - startOffset
        text.selectionEnd = selEnd - startOffset
        if (this.keyMetaStates.isSelecting) {
            text.flags = text.flags or ExtractedText.FLAG_SELECTING
        }
        return text
    }

    /**
     * Notify input method that text has been changed for external reason
     */
    fun notifyIMEExternalCursorChange() {
        updateExtractedText()
        updateSelection()
        updateCursorAnchor()
        // Restart if composing
        if (inputConnection.composingText.isComposing) {
            restartInput()
        }
    }

    /**
     * Restart the input connection.
     * Do not call this method randomly. Please refer to documentation first.
     * 
     * @see InputConnection
     */
    fun restartInput() {
        if (inputConnection != null) inputConnection.reset()
        if (inputMethodManager != null) inputMethodManager!!.restartInput(this)
    }

    /**
     * Send cursor position in text and on screen to input method
     */
    fun updateCursor() {
        updateCursorAnchor()
        updateExtractedText()
        if (text.nestedBatchEdit <= 1 && !inputConnection.composingText.isComposing) {
            updateSelection()
        }
    }

    /**
     * Release some resources held by editor.
     * This will stop completion threads and destroy using [Language] object.
     * Also it prevents future editor tasks (such as posted Runnable) to be executed.
     * 
     * 
     * You are expected to call this method when the editor instance is no longer used, especially when
     * your activity is to be destroyed. Invoking this method repeatedly will not generated errors.
     */
    fun release() {
        hideEditorWindows()
        if (!this.isReleased) {
            dispatchEvent(EditorReleaseEvent(this))
        } else {
            return
        }
        this.isReleased = true
        if (editorLanguage != null) {
            editorLanguage.analyzeManager.destroy()
            val formatter = this.formatter
            formatter.setReceiver(null)
            formatter.destroy()
            editorLanguage.destroy()
            _editorLanguage = EmptyLanguage()
        }

        // avoid access to language related after releasing
        textStyles = null
        diagnostics = null
        styleDelegate.reset()

        val text: Content = this.text
        if (text != null) {
            text.removeContentListener(this)
        }
        colorScheme.detachEditor(this)
    }

    /**
     * Hide all built-in windows of the editor
     */
    fun hideEditorWindows() {
        completionWindow!!.cancelCompletion()
        completionWindow!!.hide()
        textActionWindow!!.dismiss()
        eventHandler.magnifier.dismiss()
        diagnosticTooltip!!.dismiss()
    }

    /**
     * Called by ColorScheme to notify invalidate
     * 
     * @param type Color type changed
     */
    fun onColorUpdated(type: Int) {
        dispatchEvent(ColorSchemeUpdateEvent(this))
        renderContext.invalidateRenderNodes()
        invalidate()
    }

    /**
     * Called by color scheme to init colors
     */
    fun onColorFullUpdate() {
        dispatchEvent(ColorSchemeUpdateEvent(this))
        renderContext.invalidateRenderNodes()
        invalidate()
    }

    /**
     * Called by [EditorInputConnection]
     */
    open fun onCloseConnection() {
        setExtracting(null)
        invalidate()
    }

    /**
     * This method is called once when the editor is created.
     */
    protected fun onCreateRenderer(): EditorRenderer {
        return EditorRenderer(this)
    }

    /**
     * Called when the text is edited or [setSelection] is called
     */
    protected fun onSelectionChanged(cause: Int) {
        var oldLeft: CharPosition? = null
        var oldRight: CharPosition? = null
        val lastTextRange = this.lastSelectedTextRange
        if (lastTextRange != null) {
            oldLeft = lastTextRange.start
            oldRight = lastTextRange.end
        }
        dispatchEvent(SelectionChangeEvent(this, oldLeft, oldRight, cause))
        this.lastSelectedTextRange = this.cursorRange
    }

    /**
     * Release active edge effects on thumbs up
     */
    open fun releaseEdgeEffects() {
        edgeEffectHorizontal!!.onRelease()
        edgeEffectVertical!!.onRelease()
    }

    //-------------------------------------------------------------------------------
    //-------------------------Override methods--------------------------------------
    //-------------------------------------------------------------------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        renderer.draw(canvas)

        // Update magnifier
        if ((lastCursorState != cursorBlink.visibility || !eventHandler.scroller.isFinished) && eventHandler.magnifier.isShowing) {
            lastCursorState = cursorBlink.visibility
            postInLifecycle(Runnable { eventHandler.magnifier.updateDisplay() })
        }
    }

    override fun createAccessibilityNodeInfo(): AccessibilityNodeInfo {
        val info = super.createAccessibilityNodeInfo()
        if (isEnabled) {
            val maxTextLength = props.maxAccessibilityTextLength
            if (maxTextLength > 0) {
                info.isEditable = isEditable
                info.setTextSelection(cursor.left, cursor.right)
                info.setInputType(InputType.TYPE_CLASS_TEXT)
                info.isMultiLine = true
                info.setText(trimToSize(text, maxTextLength).toString())
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY)
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT)
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE)
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)
            }

            info.isLongClickable = true
            val scrollRange = this.scrollMaxY
            if (scrollRange > 0) {
                info.isScrollable = true
                val scrollY = this.offsetY
                if (scrollY > 0) {
                    info.addAction(
                        AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD
                    )
                    info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP)
                }
                if (scrollY < scrollRange) {
                    info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
                    info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN)
                }
            }
        }
        return info
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        val maxScrollY = this.scrollMaxY
        event.setScrollable(maxScrollY > 0)
        event.setMaxScrollX(this.scrollMaxX)
        event.setMaxScrollY(maxScrollY)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        when (action) {
            AccessibilityNodeInfo.ACTION_COPY -> {
                copyText()
                return true
            }

            AccessibilityNodeInfo.ACTION_CUT -> {
                cutText()
                return true
            }

            AccessibilityNodeInfo.ACTION_PASTE -> {
                pasteText()
                return true
            }

            AccessibilityNodeInfo.ACTION_SET_TEXT -> {
                setText(arguments?.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE))
                return true
            }

            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
            android.R.id.accessibilityActionScrollUp -> {
                moveSelection(SelectionMovement.PAGE_DOWN)
                return true
            }

            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
            android.R.id.accessibilityActionScrollDown -> {
                moveSelection(SelectionMovement.PAGE_UP)
                return true
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    override fun getAccessibilityClassName(): CharSequence {
        return CodeEditor::class.java.getName()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                if (this.isInterceptParentHorizontalScrollEnabled) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - downX
                if (this.isInterceptParentHorizontalScrollEnabled && !eventHandler.hasAnyHeldHandle()) {
                    if (deltaX > 0 && this.scroller.currX == 0
                        || deltaX < 0 && this.scroller.currX == this.scrollMaxX
                    ) {
                        parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onCheckIsTextEditor(): Boolean {
        return isEnabled && isEditable
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        if (!isEditable || !isEnabled) {
            return null
        }
        if (checkSoftInputEnabled()) {
            outAttrs.inputType =
                if (inputType != 0) inputType else EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        } else {
            outAttrs.inputType = InputType.TYPE_NULL
        }
        outAttrs.initialSelStart = cursor.left
        outAttrs.initialSelEnd = cursor.right
        outAttrs.initialCapsMode = inputConnection.getCursorCapsMode(0)

        // Prevent fullscreen when the screen height is too small
        // Especially in landscape mode
        if (!props.allowFullscreen) {
            outAttrs.imeOptions =
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        }

        dispatchEvent(BuildEditorInfoEvent(this, outAttrs))
        inputConnection.reset()
        text.resetBatchEdit()
        setExtracting(null)
        return inputConnection
    }

    override fun onResolvePointerIcon(event: MotionEvent, pointerIndex: Int): PointerIcon? {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (this.isFormatting || layoutBusy) {
                return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_WAIT)
            }
            if (eventHandler.hasAnyHeldHandle()) {
                return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_GRABBING)
            }
            if (this.leftHandleDescriptor.position.contains(event.x, event.y)
                || this.rightHandleDescriptor.position.contains(event.x, event.y)
                || this.insertHandleDescriptor.position.contains(event.x, event.y)
            ) {
                return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_GRAB)
            }
            val res = this.resolveTouchRegion(event, pointerIndex)
            val region = getFirst(res)
            val inbound = getSecond(res) == IN_BOUND
            if (region == REGION_TEXT && inbound) {
                if (eventHandler.mouseCanMoveText && !eventHandler.mouseClick) {
                    return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_GRABBING)
                }
                if (renderer.lastStuckLines != null) {
                    val stickyLineCount = renderer.lastStuckLines!!.size
                    if (stickyLineCount > 0 && event.y < getRowBottom(stickyLineCount - 1)) {
                        return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_HAND)
                    }
                }
                return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_TEXT)
            } else if (region == REGION_LINE_NUMBER) {
                when (props.actionWhenLineNumberClicked) {
                    DirectAccessProps.LN_ACTION_SELECT_LINE, DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME -> {
                        return PointerIcon.getSystemIcon(context, PointerIcon.TYPE_HAND)
                    }
                }
            }
            return super.onResolvePointerIcon(event, pointerIndex)
        }
        return super.onResolvePointerIcon(event, pointerIndex)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) && props.mouseMode != DirectAccessProps.MOUSE_MODE_NEVER) {
            return eventHandler.onMouseEvent(event)
        }
        if (this.isFormatting) {
            eventHandler.reset2()
            scaleDetector!!.onTouchEvent(event)
            return basicDetector!!.onTouchEvent(event)
        }
        val handlingBefore = eventHandler.handlingMotions()
        val res = eventHandler.onTouchEvent(event)
        val handling = eventHandler.handlingMotions()
        var res2 = false
        val res3 = scaleDetector!!.onTouchEvent(event)
        if (!handling && !handlingBefore) {
            res2 = basicDetector!!.onTouchEvent(event)
        }
        if (event.action == MotionEvent.ACTION_UP) {
            edgeEffectVertical!!.onRelease()
            edgeEffectHorizontal!!.onRelease()
        }
        return (res3 || res2 || res)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventHandler.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventHandler.onKeyUp(keyCode, event)
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        return keyEventHandler.onKeyMultiple(keyCode, repeatCount, event)
    }

    fun onSuperKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    fun onSuperKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    fun onSuperKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent?): Boolean {
        return super.onKeyMultiple(keyCode, repeatCount, event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
            MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        ) {
            Log.w(LOG_TAG, "use wrap_content in editor may cause layout lags")
            val specs = ViewMeasureHelper.getDesiredSize(
                widthMeasureSpec, heightMeasureSpec, measureTextRegionOffset(),
                this.rowHeight.toFloat(), wordwrap, tabWidth, text, renderer.paintGeneral
            )
            widthMeasureSpec = getFirst(specs)
            heightMeasureSpec = getSecond(specs)
            anyWrapContentSet = true
        } else {
            anyWrapContentSet = false
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDragEvent(event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                return true
            }

            DragEvent.ACTION_DRAG_LOCATION -> {
                val pos = getPointPositionOnScreen(event.x, event.y)
                val line = getFirst(pos)
                val column = getSecond(pos)
                eventHandler.draggingSelection = text.indexer.getCharPosition(line, column)
                postInvalidate()
                eventHandler.scrollIfReachesEdge(null, event.x, event.y)
                return true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                eventHandler.draggingSelection = null
                postInvalidate()
                return true
            }

            DragEvent.ACTION_DROP -> {
                val targetPos = eventHandler.draggingSelection
                if (targetPos == null) {
                    return false
                }
                eventHandler.draggingSelection = null
                setSelection(targetPos.line, targetPos.column)
                pasteText(clipDataToString(event.clipData))
                requestFocus()
                postInvalidate()
                // Call super for notifying listeners
                super.onDragEvent(event)
                return true
            }
        }
        return super.onDragEvent(event)
    }

    override fun onCreateContextMenu(menu: ContextMenu) {
        super.onCreateContextMenu(menu)
        val pos = eventHandler.lastContextClickPosition
        if (pos == null) {
            return
        }
        val charPos = getPointPositionOnScreen(pos.x, pos.y)
        dispatchEvent(
            CreateContextMenuEvent(
                this, menu, text.indexer.getCharPosition(
                    getFirst(charPos), getSecond(charPos)
                )
            )
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        eventHandler.resetMouse()
        mouseButtonPressed = false
        mouseHover = mouseButtonPressed
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (event.action == MotionEvent.ACTION_HOVER_ENTER) {
                mouseHover = true
            } else if (event.action == MotionEvent.ACTION_HOVER_EXIT) {
                mouseHover = false
            }
            if (event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS
                || event.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE
            ) {
                mouseButtonPressed = event.buttonState != 0
            }
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_EXIT -> {
                    eventHandler.dispatchEditorMotionEvent({ editor: CodeEditor, position: CharPosition, event: MotionEvent, span: Span?, spanRange: TextRange?, motionRegion: Int, motionBound: Int ->
                        HoverEvent(
                            editor,
                            position,
                            event,
                            span,
                            spanRange,
                            motionRegion,
                            motionBound
                        )
                    }, null, event)
                    return true
                }
            }
        }
        if (event.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_CLASS_POINTER) && !keyEventHandler.keyMetaStates.isCtrlPressed) {
            val v_scroll = -event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val h_scroll = -event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            var distanceX = h_scroll * verticalScrollFactor * props.mouseWheelScrollFactor
            var distanceY = v_scroll * verticalScrollFactor * props.mouseWheelScrollFactor
            if (keyEventHandler.keyMetaStates.isAltPressed) {
                val multiplier = props.fastScrollSensitivity
                distanceX *= multiplier
                distanceY *= multiplier
            }
            if (keyEventHandler.keyMetaStates.isShiftPressed) {
                val tmp = distanceX
                distanceX = distanceY
                distanceY = tmp
            }
            eventHandler.onScroll(event, event, distanceX, distanceY)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(w, h, oldWidth, oldHeight)
        renderer.onSizeChanged(w, h)
        this.verticalEdgeEffect.setSize(w, h)
        this.horizontalEdgeEffect.setSize(h, w)
        this.verticalEdgeEffect.finish()
        this.horizontalEdgeEffect.finish()
        if (_layout == null || (isWordwrap && w != oldWidth)) {
            createLayout()
        } else {
            eventHandler.scrollBy(
                (if (this.offsetX > this.scrollMaxX) this.scrollMaxX - this.offsetX else 0).toFloat(),
                (if (this.offsetY > this.scrollMaxY) this.scrollMaxY - this.offsetY else 0).toFloat()
            )
        }
        verticalAbsorb = false
        horizontalAbsorb = false
        if (oldHeight > h && props.adjustToSelectionOnResize) {
            ensureSelectionVisible()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dispatchEvent(EditorAttachStateChangeEvent(this, false))
        cursorBlink.valid = false
        removeCallbacks(cursorBlink)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        dispatchEvent(EditorAttachStateChangeEvent(this, true))
    }

    override fun onFocusChanged(
        gainFocus: Boolean, direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (gainFocus) {
            cursorBlink.valid = cursorBlink.period > 0
            if (cursorBlink.valid) {
                postInLifecycle(cursorBlink)
            }
        } else {
            cursorBlink.valid = false
            cursorBlink.visibility = false
            eventHandler.hideInsertHandle()
            removeCallbacks(cursorBlink)
        }
        dispatchEvent(EditorFocusChangeEvent(this, gainFocus))
        invalidate()
    }

    override fun computeScroll() {
        val scroller = eventHandler.scroller
        if (scroller.computeScrollOffset()) {
            if (!scroller.isFinished && (scroller.startX != scroller.finalX || scroller.startY != scroller.finalY)) {
                scrollerFinalX = scroller.finalX.toFloat()
                scrollerFinalY = scroller.finalY.toFloat()
                horizontalAbsorb = abs(scroller.startX - scroller.finalX) > this.dpUnit * 5
                verticalAbsorb = abs(scroller.startY - scroller.finalY) > this.dpUnit * 5
            }
            if (scroller.currX <= 0 && scrollerFinalX <= 0 && edgeEffectHorizontal!!.isFinished && horizontalAbsorb) {
                edgeEffectHorizontal!!.onAbsorb(scroller.currVelocity.toInt())
                eventHandler.glowLeftOrRight = false
            } else {
                val max = this.scrollMaxX
                if (scroller.currX >= max && scrollerFinalX >= max && edgeEffectHorizontal!!.isFinished && horizontalAbsorb) {
                    edgeEffectHorizontal!!.onAbsorb(scroller.currVelocity.toInt())
                    eventHandler.glowLeftOrRight = true
                }
            }
            if (scroller.currY <= 0 && scrollerFinalY <= 0 && edgeEffectVertical!!.isFinished && verticalAbsorb) {
                edgeEffectVertical!!.onAbsorb(scroller.currVelocity.toInt())
                eventHandler.glowTopOrBottom = false
            } else {
                val max = this.scrollMaxY
                if (scroller.currY >= max && scrollerFinalY >= max && edgeEffectVertical!!.isFinished && verticalAbsorb) {
                    edgeEffectVertical!!.onAbsorb(scroller.currVelocity.toInt())
                    eventHandler.glowTopOrBottom = true
                }
            }
            postInvalidateOnAnimation()
        }
    }

    override fun computeVerticalScrollRange(): Int {
        return this.scrollMaxY
    }

    override fun computeVerticalScrollOffset(): Int {
        return max(0, min(this.scrollMaxY, this.offsetY))
    }

    override fun computeHorizontalScrollRange(): Int {
        return this.scrollMaxX
    }

    override fun computeHorizontalScrollOffset(): Int {
        return max(0, min(this.scrollMaxX, this.offsetX))
    }

    override fun computeHorizontalScrollExtent(): Int {
        return 0
    }

    override fun computeVerticalScrollExtent(): Int {
        return 0
    }

    override fun removeCallbacks(action: Runnable): Boolean {
        EditorHandler.removeCallbacks(action)
        return super.removeCallbacks(action)
    }

    /**
     * Post the given action to message queue. Run the action if editor is not released.
     * 
     * @param action The Runnable to be executed.
     * @return Returns true if the Runnable was successfully placed in to the message queue.
     * Returns false on failure, usually because the looper processing the message queue is exiting.
     * @see View.post
     */
    fun postInLifecycle(action: Runnable): Boolean {
        return EditorHandler.post {
            if (this.isReleased) {
                return@post
            }
            action.run()
        }
    }

    /**
     * Post the given action to message queue. Run the action if editor is not released.
     * 
     * @param action      The Runnable to be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable will be executed.
     * @return Returns true if the Runnable was successfully placed in to the message queue.
     * Returns false on failure, usually because the looper processing the message queue is exiting.
     * @see View.postDelayed
     */
    fun postDelayedInLifecycle(action: Runnable, delayMillis: Long): Boolean {
        return EditorHandler.postDelayed({
            if (this.isReleased) {
                return@postDelayed
            }
            action.run()
        }, delayMillis)
    }

    override fun beforeReplace(content: Content) {
        waitForNextChange = true
        layout.beforeReplace(content)
    }

    override fun afterInsert(
        content: Content, startLine: Int, startColumn: Int, endLine: Int,
        endColumn: Int, insertedContent: CharSequence
    ) {
        renderContext.updateForInsertion(startLine, endLine)
        renderer.updateTimestamp()
        styleDelegate.onTextChange()
        val start = text.indexer.getCharPosition(startLine, startColumn)
        val end = text.indexer.getCharPosition(endLine, endColumn)

        // Update spans
        try {
            if (textStyles != null) {
                textStyles!!.adjustOnInsert(start, end)
            }
            if (diagnostics != null) {
                diagnostics!!.shiftOnInsert(start.index, end.index)
            }
            if (inlayHints != null) {
                inlayHints!!.updateOnInsertion(startLine, startColumn, endLine, endColumn)
            }
            if (this.highlightTexts != null) {
                highlightTexts!!.updateOnInsertion(startLine, startColumn, endLine, endColumn)
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Update failure", e)
        }

        layout.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        renderer.buildMeasureCacheForLines(startLine, endLine)
        checkForRelayout()

        editorLanguage.analyzeManager.insert(start, end, insertedContent)
        eventHandler.hideInsertHandle()
        if (editable && !cursor.isSelected && !inputConnection.composingText.isComposing && acceptsComposingText()) {
            cursorAnimator.markEndPos()
            cursorAnimator.start()
        }
        selectionAnchor = if (lastAnchorIsSelLeft) cursor.left() else cursor.right()
        dispatchEvent(
            ContentChangeEvent(
                this,
                ContentChangeEvent.ACTION_INSERT,
                start,
                end,
                insertedContent,
                text.isUndoManagerWorking
            )
        )
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION)
        lastInsertion = TextRange(start.fromThis(), end.fromThis())
        waitForNextChange = false
        ensureSelectionVisible()

        // Notify input method
        updateCursor()
    }

    override fun afterDelete(
        content: Content, startLine: Int, startColumn: Int, endLine: Int,
        endColumn: Int, deletedContent: CharSequence
    ) {
        renderContext.updateForDeletion(startLine, endLine)
        renderer.updateTimestamp()
        styleDelegate.onTextChange()
        val start = text.indexer.getCharPosition(startLine, startColumn)
        val end = start.fromThis()
        end.column = endColumn
        end.line = endLine
        end.index = start.index + deletedContent.length

        try {
            if (textStyles != null) {
                textStyles!!.adjustOnDelete(start, end)
            }
            if (diagnostics != null) {
                diagnostics!!.shiftOnDelete(start.index, end.index)
            }
            if (inlayHints != null) {
                inlayHints!!.updateOnDeletion(startLine, startColumn, endLine, endColumn)
            }
            if (this.highlightTexts != null) {
                highlightTexts!!.updateOnDeletion(startLine, startColumn, endLine, endColumn)
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Update failure", e)
        }

        layout.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        renderer.buildMeasureCacheForLines(startLine, startLine + 1)
        checkForRelayout()

        if (editable && !cursor.isSelected && !waitForNextChange && !inputConnection.composingText.isComposing && acceptsComposingText()) {
            cursorAnimator.markEndPos()
            cursorAnimator.start()
        }
        editorLanguage.analyzeManager.delete(start, end, deletedContent)
        selectionAnchor = if (lastAnchorIsSelLeft) cursor.left() else cursor.right()
        dispatchEvent(
            ContentChangeEvent(
                this,
                ContentChangeEvent.ACTION_DELETE,
                start,
                end,
                deletedContent,
                text.isUndoManagerWorking
            )
        )
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION)

        if (!waitForNextChange) {
            updateCursor()
            ensureSelectionVisible()
            eventHandler.hideInsertHandle()
        }
    }

    override fun beforeModification(content: Content) {
        if (props.checkModificationThread && isAttachedToWindow) {
            val handler = getHandler()
            if (handler != null) {
                if (handler.looper.thread !== Thread.currentThread()) {
                    throw RuntimeException("text is changed in wrong thread")
                }
            }
        }
        cursorAnimator.markStartPos()
        lastAnchorIsSelLeft = cursor.left().equals(selectionAnchor)
    }

    override fun onFormatSucceed(applyContent: CharSequence, cursorRange: TextRange?) {
        postInLifecycle(Runnable {
            val line = cursor.leftLine
            val column = cursor.leftColumn
            val x = this.offsetX
            val y = this.offsetY
            val string =
                if (applyContent is Content) applyContent.toStringBuilder() else applyContent
            text.beginBatchEdit()
            text.delete(
                0, 0, text.lineCount - 1,
                text.getColumnCount(text.lineCount - 1)
            )
            text.insert(0, 0, string)
            text.endBatchEdit()
            inputConnection.markInvalid()
            if (cursorRange == null) {
                setSelectionAround(line, column)
            } else {
                try {
                    val start = cursorRange.start
                    val end = cursorRange.end
                    setSelectionRegion(start.line, start.column, end.line, end.column)
                } catch (e: IndexOutOfBoundsException) {
                    Log.w(LOG_TAG, e)
                }
            }
            this.scroller.forceFinished(true)
            this.scroller.startScroll(x, y, 0, 0, 0)
            this.scroller.abortAnimation()
            // Ensure the scroll offset is valid
            eventHandler.scrollBy(0f, 0f)
            inputConnection.reset()
            restartInput()
            dispatchEvent(EditorFormatEvent(this, true))
        })
    }

    override fun onFormatFail(throwable: Throwable?) {
        postInLifecycle(Runnable {
            Toast.makeText(context, "Format:" + throwable, Toast.LENGTH_SHORT).show()
            dispatchEvent(EditorFormatEvent(this, false))
        })
    }

    companion object {
        /**
         * The default text size when creating the editor object. Unit is sp.
         */
        const val DEFAULT_TEXT_SIZE: Int = 18

        /**
         * The default line info text size when creating the editor object. Unit is sp.
         */
        const val DEFAULT_LINE_INFO_TEXT_SIZE: Int = 21

        /**
         * The default cursor blinking period
         */
        const val DEFAULT_CURSOR_BLINK_PERIOD: Int = 500

        /**
         * Draw whitespace characters before line content start
         * **Whitespace here only means space and tab**
         * 
         * @see .setNonPrintablePaintingFlags
         */
        const val FLAG_DRAW_WHITESPACE_LEADING: Int = 1

        /**
         * Draw whitespace characters inside line content
         * **Whitespace here only means space and tab**
         * 
         * @see .setNonPrintablePaintingFlags
         */
        val FLAG_DRAW_WHITESPACE_INNER: Int = 1 shl 1

        /**
         * Draw whitespace characters after line content end
         * **Whitespace here only means space and tab**
         * 
         * @see .setNonPrintablePaintingFlags
         */
        val FLAG_DRAW_WHITESPACE_TRAILING: Int = 1 shl 2

        /**
         * Draw whitespace characters even if it is a line full of whitespaces
         * To apply this, you must enable [.FLAG_DRAW_WHITESPACE_LEADING]
         * **Whitespace here only means space and tab**
         * 
         * @see .setNonPrintablePaintingFlags
         */
        val FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE: Int = 1 shl 3

        /**
         * Draw newline signals in text
         * 
         * @see .setNonPrintablePaintingFlags
         */
        val FLAG_DRAW_LINE_SEPARATOR: Int = 1 shl 4

        /**
         * Draw the tab character the same as space.
         * If not set, tab will be display to be a line.
         * 
         * @see .setNonPrintablePaintingFlags
         */
        val FLAG_DRAW_TAB_SAME_AS_SPACE: Int = 1 shl 5

        /**
         * Draw the whitespaces in selected text
         * 
         * @see .setNonPrintablePaintingFlags
         */
        val FLAG_DRAW_WHITESPACE_IN_SELECTION: Int = 1 shl 6

        /**
         * Draw soft-wrap indicator in text
         * 
         * @see .setNonPrintablePaintingFlags
         */
        val FLAG_DRAW_SOFT_WRAP: Int = 1 shl 7

        /*
     * Internal state identifiers of action mode
     */
        const val ACTION_MODE_NONE: Int = 0
        const val ACTION_MODE_SEARCH_TEXT: Int = 1
        const val ACTION_MODE_SELECT_TEXT: Int = 2
        private val logger = instance("CodeEditor")

        /**
         * Digits for line number measuring
         */
        private const val NUMBER_DIGITS = "0 1 2 3 4 5 6 7 8 9"
        private const val LOG_TAG = "CodeEditor"
        private const val COPYRIGHT =
            "sora-editor\nCopyright (C) Rosemoe roses2020@qq.com\nThis project is distributed under the LGPL v2.1 license"

        /**
         * Checks whether this region has visible region on screen
         * 
         * @param begin The start line of code block
         * @param end   The end line of code block
         * @param first The first visible line on screen
         * @param last  The last visible line on screen
         * @return Whether this block can be seen
         */
        fun hasVisibleRegion(begin: Int, end: Int, first: Int, last: Int): Boolean {
            return (end > first && begin < last)
        }
    }
}
