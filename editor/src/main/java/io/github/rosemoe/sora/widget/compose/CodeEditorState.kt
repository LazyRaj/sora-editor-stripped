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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.widget.CodeEditor
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
    initialLanguage: Language? = null,
    initialColorScheme: EditorColorScheme = EditorColorScheme()
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

    /** Whether the editor text can be modified by the user. */
    var isEditable: Boolean by mutableStateOf(true)

    /** Whether automatic soft word-wrapping is enabled. */
    var isWordwrap: Boolean by mutableStateOf(false)

    /** Whether line numbers are rendered in the gutter. */
    var lineNumbersEnabled: Boolean by mutableStateOf(true)

    /** Whether line numbers stay pinned to the screen edge during horizontal scrolling. */
    var pinLineNumbers: Boolean by mutableStateOf(false)

    /** Editor font size in SP units. */
    var textSizeSp: Float by mutableStateOf(14f)

    /** Tab size in space count. */
    var tabWidth: Int by mutableIntStateOf(4)

    /** Whether undo and redo tracking is active. */
    var isUndoEnabled: Boolean by mutableStateOf(true)

    /** Whether cursor blinking animation is enabled. */
    var cursorBlinkEnabled: Boolean by mutableStateOf(true)

    /** Whether the line containing the cursor is highlighted. */
    var highlightCurrentLine: Boolean by mutableStateOf(true)

    /** Whether dividing lines between text and gutter are visible. */
    var dividerEnabled: Boolean by mutableStateOf(true)

    /** Whether built-in code auto-completion is active. */
    var autoCompletionEnabled: Boolean by mutableStateOf(true)

    /** Whether the magnifying glass handle is enabled on touch hold. */
    var magnifierEnabled: Boolean by mutableStateOf(true)

    /** The syntax language engine providing parsing, styling, and completion. */
    var language: Language? by mutableStateOf(initialLanguage)

    /** The active editor color scheme. */
    var colorScheme: EditorColorScheme by mutableStateOf(initialColorScheme)

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

    internal fun attachEditor(editor: CodeEditor) {
        if (_editor === editor) return
        detachEditor()

        _editor = editor
        isAttached = true

        // Push initial state into editor
        editor.setText(_text)
        editor.isEditable = isEditable
        editor.isWordwrap = isWordwrap
        editor.isLineNumberEnabled = lineNumbersEnabled
        editor.isLineNumberPinned = pinLineNumbers
        editor.textSize = textSizeSp
        editor.tabWidth = tabWidth
        editor.isUndoEnabled = isUndoEnabled
        editor.cursorBlinkPeriod = if (cursorBlinkEnabled) 500 else 0
        editor.isHighlightCurrentLine = highlightCurrentLine
        editor.isDividerEnabled = dividerEnabled
        editor.getComponent(EditorAutoCompletion::class.java).setEnabled(autoCompletionEnabled)
        editor.getComponent(Magnifier::class.java).setEnabled(magnifierEnabled)
        editor.colorScheme = colorScheme
        if (language != null) {
            editor.setEditorLanguage(language)
        }

        if (_selection != TextRange.Zero) {
            applySelectionToEditor(editor, _selection)
        }

        lineCount = editor.lineCount
        canUndo = editor.canUndo()
        canRedo = editor.canRedo()
        cursorPosition = CharPosition(editor.cursor.leftLine, editor.cursor.leftColumn)

        // Subscribe to editor events
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
            editor.subscribeEvent(EditorReleaseEvent::class.java) { _, _ ->
                detachEditor()
            }
        )
    }

    internal fun updateEditor(editor: CodeEditor) {
        if (editor.isEditable != isEditable) editor.isEditable = isEditable
        if (editor.isWordwrap != isWordwrap) editor.isWordwrap = isWordwrap
        if (editor.isLineNumberEnabled != lineNumbersEnabled) editor.isLineNumberEnabled = lineNumbersEnabled
        if (editor.isLineNumberPinned != pinLineNumbers) editor.isLineNumberPinned = pinLineNumbers
        if (editor.tabWidth != tabWidth) editor.tabWidth = tabWidth
        if (editor.isUndoEnabled != isUndoEnabled) editor.isUndoEnabled = isUndoEnabled
        if (editor.isHighlightCurrentLine != highlightCurrentLine) editor.isHighlightCurrentLine = highlightCurrentLine
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

    internal fun detachEditor(editor: CodeEditor? = null) {
        for (u in unsubscribes) {
            try {
                u.unsubscribe()
            } catch (_: Throwable) {}
        }
        unsubscribes.clear()
        if (editor == null || _editor === editor) {
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
        val Saver: Saver<CodeEditorState, *> = listSaver(
            save = { state ->
                listOf(
                    state.text,
                    state.selection.start,
                    state.selection.end,
                    state.isEditable,
                    state.isWordwrap,
                    state.lineNumbersEnabled,
                    state.pinLineNumbers,
                    state.textSizeSp,
                    state.tabWidth,
                    state.isUndoEnabled
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
