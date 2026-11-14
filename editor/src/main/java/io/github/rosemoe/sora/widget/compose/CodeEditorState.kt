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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorLanguageChangeEvent
import io.github.rosemoe.sora.event.EditorPropertyChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.DefaultCompletionLayout
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * State holder for [CodeEditorCompose] managing observable editor state, two-way bindings,
 * and imperative editor controls.
 *
 * @param initialText Initial text displayed in the editor.
 * @param initialSelection Initial selection or cursor location.
 * @param initialLanguage Initial language syntax/highlighting provider.
 * @param initialColorScheme Initial color scheme theme.
 */
@Stable
class CodeEditorState(
    initialText: String = "",
    initialSelection: TextRange = TextRange.Zero,
    private val initialLanguage: Language? = null,
    private val initialColorScheme: EditorColorScheme = EditorColorScheme()
) {

    private var _editor: CodeEditor? = null

    /** Direct reference to the attached [CodeEditor], or null if unattached. */
    val editor: CodeEditor?
        get() = _editor

    private var isUpdatingFromEditor = false
    private var isUpdatingFromState = false
    private val unsubscribes = mutableListOf<SubscriptionReceipt<*>>()

    // -----------------------------------------------------------------------------------------
    // Observable Two-Way State Properties
    // -----------------------------------------------------------------------------------------

    private var _text by mutableStateOf(initialText)

    /**
     * The current text content of the editor.
     * Reading this creates a Compose snapshot read dependency. Mutating it updates the underlying
     * editor buffer while avoiding circular re-entrancy.
     */
    var text: String
        get() = _text
        set(value) {
            if (_text != value) {
                _text = value
                if (!isUpdatingFromEditor) {
                    _editor?.let { ed ->
                        isUpdatingFromState = true
                        try {
                            ed.setText(value)
                        } finally {
                            isUpdatingFromState = false
                        }
                    }
                }
            }
        }

    private var _selection by mutableStateOf(initialSelection)

    /**
     * The current selection range or cursor index in the editor.
     */
    var selection: TextRange
        get() = _selection
        set(value) {
            if (_selection != value) {
                _selection = value
                if (!isUpdatingFromEditor) {
                    _editor?.let { ed ->
                        applySelectionToEditor(ed, value)
                    }
                }
            }
        }

    private var _isEditable by mutableStateOf(true)

    /** Whether the editor text can be modified by the user. */
    var isEditable: Boolean
        get() {
            val snap = _isEditable
            return _editor?.isEditable ?: snap
        }
        set(value) {
            if (_isEditable != value) {
                _isEditable = value
                _editor?.let { ed ->
                    if (ed.isEditable != value) ed.isEditable = value
                }
            }
        }

    private var _isWordwrap by mutableStateOf(false)

    /** Whether automatic soft word-wrapping is enabled. */
    var isWordwrap: Boolean
        get() {
            val snap = _isWordwrap
            return _editor?.isWordwrap ?: snap
        }
        set(value) {
            if (_isWordwrap != value) {
                _isWordwrap = value
                _editor?.let { ed ->
                    if (ed.isWordwrap != value) ed.isWordwrap = value
                }
            }
        }

    private var _lineNumbersEnabled by mutableStateOf(true)

    /** Whether line numbers are rendered in the gutter. */
    var lineNumbersEnabled: Boolean
        get() {
            val snap = _lineNumbersEnabled
            return _editor?.isLineNumberEnabled ?: snap
        }
        set(value) {
            if (_lineNumbersEnabled != value) {
                _lineNumbersEnabled = value
                _editor?.let { ed ->
                    if (ed.isLineNumberEnabled != value) ed.isLineNumberEnabled = value
                }
            }
        }

    private var _pinLineNumbers by mutableStateOf(false)

    /** Whether line numbers stay pinned to the screen edge during horizontal scrolling. */
    var pinLineNumbers: Boolean
        get() {
            val snap = _pinLineNumbers
            return _editor?.isLineNumberPinned ?: snap
        }
        set(value) {
            if (_pinLineNumbers != value) {
                _pinLineNumbers = value
                _editor?.let { ed ->
                    if (ed.isLineNumberPinned != value) ed.isLineNumberPinned = value
                }
            }
        }

    private var _textSizeSp by mutableFloatStateOf(14f)

    /** Editor font size in SP units. */
    var textSizeSp: Float
        get() {
            val snap = _textSizeSp
            return _editor?.textSize ?: snap
        }
        set(value) {
            if (_textSizeSp != value) {
                _textSizeSp = value
                _editor?.let { ed ->
                    if (ed.textSize != value) ed.textSize = value
                }
            }
        }

    private var _isScalable by mutableStateOf(true)

    /** Whether pinch-to-zoom text scaling is enabled. */
    var isScalable: Boolean
        get() {
            val snap = _isScalable
            return _editor?.isScalable ?: snap
        }
        set(value) {
            if (_isScalable != value) {
                _isScalable = value
                _editor?.let { ed ->
                    if (ed.isScalable != value) ed.isScalable = value
                }
            }
        }

    private var _tabWidth by mutableIntStateOf(4)

    /** Tab size in space count. */
    var tabWidth: Int
        get() {
            val snap = _tabWidth
            return _editor?.tabWidth ?: snap
        }
        set(value) {
            if (_tabWidth != value) {
                _tabWidth = value
                _editor?.let { ed ->
                    if (ed.tabWidth != value) ed.tabWidth = value
                }
            }
        }

    private var _isUndoEnabled by mutableStateOf(true)

    /** Whether undo and redo tracking is active. */
    var isUndoEnabled: Boolean
        get() {
            val snap = _isUndoEnabled
            return _editor?.isUndoEnabled ?: snap
        }
        set(value) {
            if (_isUndoEnabled != value) {
                _isUndoEnabled = value
                _editor?.let { ed ->
                    if (ed.isUndoEnabled != value) ed.isUndoEnabled = value
                }
            }
        }

    private var _cursorBlinkEnabled by mutableStateOf(true)

    /** Whether cursor blinking animation is enabled. */
    var cursorBlinkEnabled: Boolean
        get() {
            val snap = _cursorBlinkEnabled
            return _editor?.let { it.cursorBlinkPeriod > 0 } ?: snap
        }
        set(value) {
            if (_cursorBlinkEnabled != value) {
                _cursorBlinkEnabled = value
                _editor?.let { ed ->
                    val desired = if (value) 500 else 0
                    if (ed.cursorBlinkPeriod != desired) ed.cursorBlinkPeriod = desired
                }
            }
        }

    private var _highlightCurrentLine by mutableStateOf(true)

    /** Whether the line containing the cursor is highlighted. */
    var highlightCurrentLine: Boolean
        get() {
            val snap = _highlightCurrentLine
            return _editor?.isHighlightCurrentLine ?: snap
        }
        set(value) {
            if (_highlightCurrentLine != value) {
                _highlightCurrentLine = value
                _editor?.let { ed ->
                    if (ed.isHighlightCurrentLine != value) ed.isHighlightCurrentLine = value
                }
            }
        }

    private var _dividerEnabled by mutableStateOf(true)

    /** Whether dividing lines between text and gutter are visible. */
    var dividerEnabled: Boolean
        get() {
            val snap = _dividerEnabled
            return _editor?.isDividerEnabled ?: snap
        }
        set(value) {
            if (_dividerEnabled != value) {
                _dividerEnabled = value
                _editor?.let { ed ->
                    if (ed.isDividerEnabled != value) ed.isDividerEnabled = value
                }
            }
        }

    private var _autoCompletionEnabled by mutableStateOf(true)

    /** Whether built-in code auto-completion is active. */
    var autoCompletionEnabled: Boolean
        get() {
            val snap = _autoCompletionEnabled
            return _editor?.getComponent(EditorAutoCompletion::class.java)?.isEnabled() ?: snap
        }
        set(value) {
            if (_autoCompletionEnabled != value) {
                _autoCompletionEnabled = value
                _editor?.getComponent(EditorAutoCompletion::class.java)?.let { comp ->
                    if (comp.isEnabled() != value) comp.setEnabled(value)
                }
            }
        }

    private var _magnifierEnabled by mutableStateOf(true)

    /** Whether the magnifying glass handle is enabled on touch hold. */
    var magnifierEnabled: Boolean
        get() {
            val snap = _magnifierEnabled
            return _editor?.getComponent(Magnifier::class.java)?.isEnabled() ?: snap
        }
        set(value) {
            if (_magnifierEnabled != value) {
                _magnifierEnabled = value
                _editor?.getComponent(Magnifier::class.java)?.let { comp ->
                    if (comp.isEnabled() != value) comp.setEnabled(value)
                }
            }
        }

    private var _language: Language? by mutableStateOf(initialLanguage)

    /** The syntax language engine providing parsing, styling, and completion. */
    var language: Language?
        get() {
            val snap = _language
            return _editor?.editorLanguage ?: snap
        }
        set(value) {
            if (_language != value) {
                _language = value
                _editor?.let { ed ->
                    if (ed.editorLanguage != value) ed.setEditorLanguage(value)
                }
            }
        }

    private var _colorScheme: EditorColorScheme by mutableStateOf(initialColorScheme)

    /** The active editor color scheme. */
    var colorScheme: EditorColorScheme
        get() {
            val snap = _colorScheme
            return _editor?.colorScheme ?: snap
        }
        set(value) {
            if (_colorScheme !== value) {
                _colorScheme = value
                _editor?.let { ed ->
                    if (ed.colorScheme !== value) ed.colorScheme = value
                }
            }
        }

    private var _scrollX by mutableIntStateOf(0)

    /**
     * Viewport horizontal scroll offset in pixels.
     */
    var scrollX: Int
        get() = _editor?.offsetX ?: _scrollX
        set(value) {
            if (_scrollX != value) {
                _scrollX = value
                _editor?.let { ed ->
                    if (ed.offsetX != value) {
                        ed.scrollTo(value, ed.offsetY)
                    }
                }
            }
        }

    private var _scrollY by mutableIntStateOf(0)

    /**
     * Viewport vertical scroll offset in pixels.
     */
    var scrollY: Int
        get() = _editor?.offsetY ?: _scrollY
        set(value) {
            if (_scrollY != value) {
                _scrollY = value
                _editor?.let { ed ->
                    if (ed.offsetY != value) {
                        ed.scrollTo(ed.offsetX, value)
                    }
                }
            }
        }

    // -----------------------------------------------------------------------------------------
    // Scrollbar Indication Customization
    // -----------------------------------------------------------------------------------------

    private var _scrollbarThumbColor by mutableIntStateOf(0)
    /** Normal fill color for scrollbar indicator. 0 falls back to color scheme. */
    var scrollbarThumbColor: Int
        get() = _editor?.props?.scrollbarThumbColor ?: _scrollbarThumbColor
        set(value) {
            _scrollbarThumbColor = value
            _editor?.let { ed ->
                ed.props.scrollbarThumbColor = value
                ed.postInvalidate()
            }
        }

    private var _scrollbarThumbHoldColor by mutableIntStateOf(0)
    /** Pressed / holding fill color for scrollbar indicator. 0 falls back to color scheme. */
    var scrollbarThumbHoldColor: Int
        get() = _editor?.props?.scrollbarThumbHoldColor ?: _scrollbarThumbHoldColor
        set(value) {
            _scrollbarThumbHoldColor = value
            _editor?.let { ed ->
                ed.props.scrollbarThumbHoldColor = value
                ed.postInvalidate()
            }
        }

    private var _scrollbarBorderEnabled by mutableStateOf(false)
    /** Whether border rendering is enabled for scrollbar indicator. */
    var scrollbarBorderEnabled: Boolean
        get() = _editor?.props?.scrollbarBorderEnabled ?: _scrollbarBorderEnabled
        set(value) {
            _scrollbarBorderEnabled = value
            _editor?.let { ed ->
                ed.props.scrollbarBorderEnabled = value
                ed.postInvalidate()
            }
        }

    private var _scrollbarBorderColor by mutableIntStateOf(0)
    /** Border color for scrollbar indicator. */
    var scrollbarBorderColor: Int
        get() = _editor?.props?.scrollbarBorderColor ?: _scrollbarBorderColor
        set(value) {
            _scrollbarBorderColor = value
            _editor?.let { ed ->
                ed.props.scrollbarBorderColor = value
                ed.postInvalidate()
            }
        }

    private var _scrollbarBorderRadiusDip by mutableFloatStateOf(0f)
    /** Corner/border radius for scrollbar indicator in DIP. */
    var scrollbarBorderRadiusDip: Float
        get() = _editor?.props?.scrollbarBorderRadiusDip ?: _scrollbarBorderRadiusDip
        set(value) {
            _scrollbarBorderRadiusDip = value
            _editor?.let { ed ->
                ed.props.scrollbarBorderRadiusDip = value
                ed.postInvalidate()
            }
        }

    // -----------------------------------------------------------------------------------------
    // AutoCompletion Background Box Customization
    // -----------------------------------------------------------------------------------------

    private var _completionCornerRadiusDip by mutableFloatStateOf(8f)
    /** Corner radius of auto-completion box in DIP. */
    var completionCornerRadiusDip: Float
        get() = _editor?.props?.completionCornerRadiusDip ?: _completionCornerRadiusDip
        set(value) {
            _completionCornerRadiusDip = value
            _editor?.let { ed ->
                ed.props.completionCornerRadiusDip = value
                (ed.getComponent(EditorAutoCompletion::class.java).layout as? DefaultCompletionLayout)?.onApplyColorScheme(ed.colorScheme)
            }
        }

    private var _completionBorderWidthDip by mutableFloatStateOf(1f)
    /** Border stroke width of auto-completion box in DIP. */
    var completionBorderWidthDip: Float
        get() = _editor?.props?.completionBorderWidthDip ?: _completionBorderWidthDip
        set(value) {
            _completionBorderWidthDip = value
            _editor?.let { ed ->
                ed.props.completionBorderWidthDip = value
                (ed.getComponent(EditorAutoCompletion::class.java).layout as? DefaultCompletionLayout)?.onApplyColorScheme(ed.colorScheme)
            }
        }

    private var _completionBorderColor by mutableIntStateOf(0)
    /** Border stroke color of auto-completion box. */
    var completionBorderColor: Int
        get() = _editor?.props?.completionBorderColor ?: _completionBorderColor
        set(value) {
            _completionBorderColor = value
            _editor?.let { ed ->
                ed.props.completionBorderColor = value
                (ed.getComponent(EditorAutoCompletion::class.java).layout as? DefaultCompletionLayout)?.onApplyColorScheme(ed.colorScheme)
            }
        }

    private var _completionBackgroundColor by mutableIntStateOf(0)
    /** Background color of auto-completion box. */
    var completionBackgroundColor: Int
        get() = _editor?.props?.completionBackgroundColor ?: _completionBackgroundColor
        set(value) {
            _completionBackgroundColor = value
            _editor?.let { ed ->
                ed.props.completionBackgroundColor = value
                (ed.getComponent(EditorAutoCompletion::class.java).layout as? DefaultCompletionLayout)?.onApplyColorScheme(ed.colorScheme)
            }
        }

    private var _reverseListWhenAbove by mutableStateOf(true)
    /** Whether completion list items are reversed when displayed above the cursor line. */
    var reverseListWhenAbove: Boolean
        get() = _editor?.props?.reverseListWhenAbove ?: _reverseListWhenAbove
        set(value) {
            _reverseListWhenAbove = value
            _editor?.let { ed ->
                ed.props.reverseListWhenAbove = value
            }
        }

    // -----------------------------------------------------------------------------------------
    // Read-Only Observable States
    // -----------------------------------------------------------------------------------------

    /** Current line count of the editor content. */
    var lineCount: Int by mutableIntStateOf(1)
        private set

    /** True if an undoable modification exists in history. */
    var canUndo: Boolean by mutableStateOf(false)
        private set

    /** True if a redoable modification exists in history. */
    var canRedo: Boolean by mutableStateOf(false)
        private set

    /** True if an Android [CodeEditor] view is actively attached and bound to this state. */
    var isAttached: Boolean by mutableStateOf(false)
        private set

    /** Current line and column position of the primary cursor. */
    var cursorPosition: CharPosition by mutableStateOf(CharPosition())
        private set

    // -----------------------------------------------------------------------------------------
    // Imperative Actions
    // -----------------------------------------------------------------------------------------

    /** Reverts the last text edit action. */
    fun undo() {
        _editor?.let { ed ->
            ed.undo()
            canUndo = ed.canUndo()
            canRedo = ed.canRedo()
            _text = ed.text.toString()
        }
    }

    /** Re-applies the previously reverted text edit action. */
    fun redo() {
        _editor?.let { ed ->
            ed.redo()
            canUndo = ed.canUndo()
            canRedo = ed.canRedo()
            _text = ed.text.toString()
        }
    }

    /** Selects all content currently present in the editor. */
    fun selectAll() {
        _editor?.selectAll()
    }

    /** Sets the selection range using character offsets. */
    fun setSelection(start: Int, end: Int) {
        selection = TextRange(start, end)
    }

    /** Sets the cursor location to the specified line and column. */
    fun setCursorPosition(line: Int, column: Int) {
        _editor?.let { ed ->
            ed.setSelection(line, column)
            _selection = TextRange(ed.cursor.left, ed.cursor.right)
            cursorPosition = CharPosition(line, column)
        }
    }

    /** Sets the selection region between start and end line/column coordinates. */
    fun setSelectionRegion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        _editor?.let { ed ->
            ed.setSelectionRegion(startLine, startColumn, endLine, endColumn)
            _selection = TextRange(ed.cursor.left, ed.cursor.right)
            cursorPosition = CharPosition(startLine, startColumn)
        }
    }

    /** Clears any active selection, placing cursor at selection start. */
    fun clearSelection() {
        _editor?.let { ed ->
            val pos = ed.cursor.left
            ed.setSelection(ed.cursor.leftLine, ed.cursor.leftColumn)
            _selection = TextRange(pos, pos)
        }
    }

    /** Cancels any active selection and collapses the cursor to the selection end, dismissing action popups. */
    fun cancelSelection() {
        _editor?.let { ed ->
            ed.cancelSelection()
            _selection = TextRange(ed.cursor.left, ed.cursor.right)
            cursorPosition = CharPosition(ed.cursor.leftLine, ed.cursor.leftColumn)
        } ?: run {
            selection = TextRange(selection.end, selection.end)
        }
    }

    /** Inserts text at the current cursor position or replaces active selection. */
    fun insertText(textToInsert: String, selectionOffset: Int = textToInsert.length) {
        _editor?.let { ed ->
            ed.insertText(textToInsert, selectionOffset)
        }
    }

    /** Replaces the current selection with given text. */
    fun replaceSelection(newText: String) {
        insertText(newText, newText.length)
    }

    /** Triggers asynchronous code formatting using the active language provider. */
    fun formatCodeAsync(): Boolean {
        return _editor?.formatCodeAsync() ?: false
    }

    /** Reruns syntax analysis and re-renders code spans. */
    fun rerunAnalysis() {
        _editor?.rerunAnalysis()
    }

    /** Scrolls the editor viewport until the given line is visible. */
    fun jumpToLine(line: Int) {
        _editor?.let { ed ->
            val targetLine = line.coerceIn(0, (ed.lineCount - 1).coerceAtLeast(0))
            ed.setSelection(targetLine, 0)
        }
    }

    /** Ensures the specified line and column is visible in the viewport. */
    fun ensurePositionVisible(line: Int, column: Int) {
        _editor?.ensurePositionVisible(line, column)
    }

    /** Executes a custom lambda with the attached [CodeEditor] instance safely. */
    inline fun withEditor(block: (CodeEditor) -> Unit) {
        editor?.let(block)
    }

    // -----------------------------------------------------------------------------------------
    // View Lifecycle & Synchronization Hooks
    // -----------------------------------------------------------------------------------------

    fun attachEditor(editor: CodeEditor) {
        if (_editor === editor) return
        detachEditor()

        _editor = editor
        isAttached = true

        // Push initial state into editor
        if (_text.isNotEmpty()) {
            editor.setText(_text)
        } else if (editor.text.isNotEmpty()) {
            _text = editor.text.toString()
        }
        editor.isEditable = isEditable
        editor.isWordwrap = isWordwrap
        editor.isLineNumberEnabled = lineNumbersEnabled
        editor.isLineNumberPinned = pinLineNumbers
        editor.textSize = textSizeSp
        editor.isScalable = isScalable
        editor.tabWidth = tabWidth
        editor.isUndoEnabled = isUndoEnabled
        editor.cursorBlinkPeriod = if (cursorBlinkEnabled) 500 else 0
        editor.isHighlightCurrentLine = highlightCurrentLine
        editor.isDividerEnabled = dividerEnabled
        editor.props.scrollbarThumbColor = _scrollbarThumbColor
        editor.props.scrollbarThumbHoldColor = _scrollbarThumbHoldColor
        editor.props.scrollbarBorderEnabled = _scrollbarBorderEnabled
        editor.props.scrollbarBorderColor = _scrollbarBorderColor
        editor.props.scrollbarBorderRadiusDip = _scrollbarBorderRadiusDip
        editor.props.completionCornerRadiusDip = _completionCornerRadiusDip
        editor.props.completionBorderWidthDip = _completionBorderWidthDip
        editor.props.completionBorderColor = _completionBorderColor
        editor.props.completionBackgroundColor = _completionBackgroundColor
        editor.props.reverseListWhenAbove = _reverseListWhenAbove
        editor.getComponent(EditorAutoCompletion::class.java).setEnabled(autoCompletionEnabled)
        editor.getComponent(Magnifier::class.java).setEnabled(magnifierEnabled)
        if (language != null) {
            editor.setEditorLanguage(language)
        } else {
            _language = editor.editorLanguage
        }
        if (colorScheme !== initialColorScheme) {
            editor.colorScheme = colorScheme
        } else {
            _colorScheme = editor.colorScheme
        }

        if (_selection != TextRange.Zero) {
            applySelectionToEditor(editor, _selection)
        }

        if (_scrollX != 0 || _scrollY != 0) {
            val targetX = _scrollX
            val targetY = _scrollY
            editor.scrollTo(targetX, targetY)
            editor.post {
                if (_editor === editor) {
                    editor.scrollTo(targetX, targetY)
                }
            }
        } else if (_selection != TextRange.Zero) {
            editor.post {
                if (_editor === editor) {
                    editor.ensureSelectionVisible()
                }
            }
        }

        lineCount = editor.lineCount
        canUndo = editor.canUndo()
        canRedo = editor.canRedo()
        cursorPosition = CharPosition(editor.cursor.leftLine, editor.cursor.leftColumn)

        // Subscribe to editor events
        unsubscribes.add(
            editor.subscribeEvent(ScrollEvent::class.java) { event, _ ->
                _scrollX = event.endX
                _scrollY = event.endY
            }
        )
        unsubscribes.add(
            editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                if (isUpdatingFromState) return@subscribeEvent
                isUpdatingFromEditor = true
                try {
                    val currentText = editor.text.toString()
                    if (_text != currentText) {
                        _text = currentText
                    }
                    lineCount = editor.lineCount
                    canUndo = editor.canUndo()
                    canRedo = editor.canRedo()
                } finally {
                    isUpdatingFromEditor = false
                }
            }
        )

        unsubscribes.add(
            editor.subscribeEvent(SelectionChangeEvent::class.java) { _, _ ->
                if (isUpdatingFromState) return@subscribeEvent
                isUpdatingFromEditor = true
                try {
                    _selection = TextRange(editor.cursor.left, editor.cursor.right)
                    cursorPosition = CharPosition(editor.cursor.leftLine, editor.cursor.leftColumn)
                } finally {
                    isUpdatingFromEditor = false
                }
            }
        )

        unsubscribes.add(
            editor.subscribeEvent(EditorPropertyChangeEvent::class.java) { event, _ ->
                when (event.propertyName) {
                    "isWordwrap" -> (event.value as? Boolean)?.let { _isWordwrap = it }
                    "isEditable" -> (event.value as? Boolean)?.let { _isEditable = it }
                    "lineNumbersEnabled" -> (event.value as? Boolean)?.let {
                        _lineNumbersEnabled = it
                    }

                    "pinLineNumbers" -> (event.value as? Boolean)?.let { _pinLineNumbers = it }
                    "highlightCurrentLine" -> (event.value as? Boolean)?.let {
                        _highlightCurrentLine = it
                    }

                    "dividerEnabled" -> (event.value as? Boolean)?.let { _dividerEnabled = it }
                    "isUndoEnabled" -> (event.value as? Boolean)?.let { _isUndoEnabled = it }
                    "tabWidth" -> (event.value as? Int)?.let { _tabWidth = it }
                    "cursorBlinkPeriod" -> (event.value as? Int)?.let {
                        _cursorBlinkEnabled = it > 0
                    }

                    "autoCompletionEnabled" -> (event.value as? Boolean)?.let {
                        _autoCompletionEnabled = it
                    }

                    "magnifierEnabled" -> (event.value as? Boolean)?.let { _magnifierEnabled = it }
                    "isScalable" -> (event.value as? Boolean)?.let { _isScalable = it }
                }
            }
        )

        unsubscribes.add(
            editor.subscribeEvent(TextSizeChangeEvent::class.java) { event, _ ->
                _textSizeSp = event.newTextSizeSp
            }
        )

        unsubscribes.add(
            editor.subscribeEvent(ColorSchemeUpdateEvent::class.java) { event, _ ->
                _colorScheme = event.editor.colorScheme
            }
        )

        unsubscribes.add(
            editor.subscribeEvent(EditorLanguageChangeEvent::class.java) { event, _ ->
                _language = event.newLanguage
            }
        )

        unsubscribes.add(
            editor.subscribeEvent(EditorReleaseEvent::class.java) { _, _ ->
                detachEditor()
            }
        )
    }

    fun updateEditor(editor: CodeEditor) {
        if (editor.isEditable != isEditable) editor.isEditable = isEditable
        if (editor.isWordwrap != isWordwrap) editor.isWordwrap = isWordwrap
        if (editor.isLineNumberEnabled != lineNumbersEnabled) editor.isLineNumberEnabled =
            lineNumbersEnabled
        if (editor.isLineNumberPinned != pinLineNumbers) editor.isLineNumberPinned = pinLineNumbers
        if (kotlin.math.abs(editor.textSize - textSizeSp) > 0.05f) editor.textSize = textSizeSp
        if (editor.isScalable != isScalable) editor.isScalable = isScalable
        if (editor.tabWidth != tabWidth) editor.tabWidth = tabWidth
        if (editor.isUndoEnabled != isUndoEnabled) editor.isUndoEnabled = isUndoEnabled
        if (editor.isHighlightCurrentLine != highlightCurrentLine) editor.isHighlightCurrentLine =
            highlightCurrentLine
        if (editor.isDividerEnabled != dividerEnabled) editor.isDividerEnabled = dividerEnabled
        if (editor.colorScheme !== colorScheme) editor.colorScheme = colorScheme
        if (editor.editorLanguage !== language) editor.setEditorLanguage(language)

        val desiredBlink = if (cursorBlinkEnabled) 500 else 0
        if (editor.cursorBlinkPeriod != desiredBlink) editor.cursorBlinkPeriod = desiredBlink

        val autoComp = editor.getComponent(EditorAutoCompletion::class.java)
        if (autoComp.isEnabled() != autoCompletionEnabled) autoComp.setEnabled(autoCompletionEnabled)

        val magnifier = editor.getComponent(Magnifier::class.java)
        if (magnifier.isEnabled() != magnifierEnabled) magnifier.setEnabled(magnifierEnabled)

        if (!isUpdatingFromEditor && editor.text.toString() != _text) {
            isUpdatingFromState = true
            try {
                editor.setText(_text)
            } finally {
                isUpdatingFromState = false
            }
        }
    }

    fun detachEditor(editor: CodeEditor? = null) {
        val currentEditor = _editor
        if (editor == null || currentEditor === editor) {
            currentEditor?.let { ed ->
                _text = ed.text.toString()
                _selection = TextRange(ed.cursor.left, ed.cursor.right)
                _scrollX = ed.offsetX
                _scrollY = ed.offsetY
                _scrollbarThumbColor = ed.props.scrollbarThumbColor
                _scrollbarThumbHoldColor = ed.props.scrollbarThumbHoldColor
                _scrollbarBorderEnabled = ed.props.scrollbarBorderEnabled
                _scrollbarBorderColor = ed.props.scrollbarBorderColor
                _scrollbarBorderRadiusDip = ed.props.scrollbarBorderRadiusDip
                _completionCornerRadiusDip = ed.props.completionCornerRadiusDip
                _completionBorderWidthDip = ed.props.completionBorderWidthDip
                _completionBorderColor = ed.props.completionBorderColor
                _completionBackgroundColor = ed.props.completionBackgroundColor
                _reverseListWhenAbove = ed.props.reverseListWhenAbove
            }
            for (u in unsubscribes) {
                try {
                    u.unsubscribe()
                } catch (_: Throwable) {
                }
            }
            unsubscribes.clear()
            _editor = null
            isAttached = false
        }
    }

    private fun applySelectionToEditor(ed: CodeEditor, range: TextRange) {
        val len = ed.text.length
        val start = range.start.coerceIn(0, len)
        val end = range.end.coerceIn(0, len)
        if (start == end) {
            val pos = ed.text.indexer.getCharPosition(start)
            ed.setSelection(pos.line, pos.column)
        } else {
            val startPos = ed.text.indexer.getCharPosition(start)
            val endPos = ed.text.indexer.getCharPosition(end)
            ed.setSelectionRegion(startPos.line, startPos.column, endPos.line, endPos.column)
        }
    }

    companion object {
        /**
         * Standard [Saver] implementation enabling state persistence across configuration changes.
         */
        val Saver: Saver<CodeEditorState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.editor?.text?.toString() ?: state.text,
                    state.selection.start,
                    state.selection.end,
                    state.editor?.isEditable ?: state.isEditable,
                    state.editor?.isWordwrap ?: state.isWordwrap,
                    state.lineNumbersEnabled,
                    state.pinLineNumbers,
                    state.textSizeSp,
                    state.editor?.tabWidth ?: state.tabWidth,
                    state.editor?.isUndoEnabled ?: state.isUndoEnabled,
                    state.cursorBlinkEnabled,
                    state.highlightCurrentLine,
                    state.dividerEnabled,
                    state.editor?.isScalable ?: state.isScalable,
                    state.scrollX,
                    state.scrollY,
                    state.scrollbarThumbColor,
                    state.scrollbarThumbHoldColor,
                    state.scrollbarBorderEnabled,
                    state.scrollbarBorderColor,
                    state.scrollbarBorderRadiusDip,
                    state.completionCornerRadiusDip,
                    state.completionBorderWidthDip,
                    state.completionBorderColor,
                    state.completionBackgroundColor,
                    state.reverseListWhenAbove
                )
            },
            restore = { savedList ->
                CodeEditorState(
                    initialText = savedList[0] as String,
                    initialSelection = TextRange(savedList[1] as Int, savedList[2] as Int)
                ).apply {
                    isEditable = savedList[3] as Boolean
                    isWordwrap = savedList[4] as Boolean
                    lineNumbersEnabled = savedList[5] as Boolean
                    pinLineNumbers = savedList[6] as Boolean
                    textSizeSp = savedList[7] as Float
                    tabWidth = savedList[8] as Int
                    isUndoEnabled = savedList[9] as Boolean
                    if (savedList.size > 10) cursorBlinkEnabled = savedList[10] as Boolean
                    if (savedList.size > 11) highlightCurrentLine = savedList[11] as Boolean
                    if (savedList.size > 12) dividerEnabled = savedList[12] as Boolean
                    if (savedList.size > 13) isScalable = savedList[13] as Boolean
                    if (savedList.size > 14) scrollX = savedList[14] as Int
                    if (savedList.size > 15) scrollY = savedList[15] as Int
                    if (savedList.size > 16) scrollbarThumbColor = savedList[16] as Int
                    if (savedList.size > 17) scrollbarThumbHoldColor = savedList[17] as Int
                    if (savedList.size > 18) scrollbarBorderEnabled = savedList[18] as Boolean
                    if (savedList.size > 19) scrollbarBorderColor = savedList[19] as Int
                    if (savedList.size > 20) scrollbarBorderRadiusDip = savedList[20] as Float
                    if (savedList.size > 21) completionCornerRadiusDip = savedList[21] as Float
                    if (savedList.size > 22) completionBorderWidthDip = savedList[22] as Float
                    if (savedList.size > 23) completionBorderColor = savedList[23] as Int
                    if (savedList.size > 24) completionBackgroundColor = savedList[24] as Int
                    if (savedList.size > 25) reverseListWhenAbove = savedList[25] as Boolean
                }
            }
        )
    }
}

/**
 * Creates and remembers a [CodeEditorState] instance that persists across recompositions and
 * Android configuration changes.
 */
@Composable
fun rememberCodeEditorState(
    initialText: String = "",
    initialSelection: TextRange = TextRange.Zero,
    initialLanguage: Language? = null,
    initialColorScheme: EditorColorScheme = EditorColorScheme(),
    isEditable: Boolean = true,
    isWordwrap: Boolean = false,
    lineNumbersEnabled: Boolean = true,
    textSizeSp: Float = 14f,
    tabWidth: Int = 4
): CodeEditorState {
    return rememberSaveable(saver = CodeEditorState.Saver) {
        CodeEditorState(
            initialText = initialText,
            initialSelection = initialSelection,
            initialLanguage = initialLanguage,
            initialColorScheme = initialColorScheme
        ).apply {
            this.isEditable = isEditable
            this.isWordwrap = isWordwrap
            this.lineNumbersEnabled = lineNumbersEnabled
            this.textSizeSp = textSizeSp
            this.tabWidth = tabWidth
        }
    }
}
