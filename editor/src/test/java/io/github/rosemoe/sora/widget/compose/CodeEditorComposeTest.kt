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

import android.view.LayoutInflater
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.replaceComponent
import io.github.rosemoe.sora.widget.subscribeEvent
import io.github.rosemoe.sora.widget.textString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class CodeEditorComposeTest {

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun testCodeEditorStateInitializationAndDefaults() {
        val state = CodeEditorState(
            initialText = "Hello Sora",
            initialSelection = TextRange(0, 5)
        )

        assertThat(state.text).isEqualTo("Hello Sora")
        assertThat(state.selection).isEqualTo(TextRange(0, 5))
        assertThat(state.isEditable).isTrue()
        assertThat(state.isWordwrap).isFalse()
        assertThat(state.lineNumbersEnabled).isTrue()
        assertThat(state.textSizeSp).isEqualTo(14f)
        assertThat(state.tabWidth).isEqualTo(4)
        assertThat(state.isUndoEnabled).isTrue()
        assertThat(state.isAttached).isFalse()
    }

    @Test
    fun testStateAttachmentAndTwoWaySyncWithEditor() {
        val editor = CodeEditor(context)
        val state = CodeEditorState(initialText = "val x = 10\nval y = 20")

        state.attachEditor(editor)
        assertThat(state.isAttached).isTrue()
        assertThat(editor.text.toString()).isEqualTo("val x = 10\nval y = 20")
        assertThat(state.lineCount).isEqualTo(2)

        // Mutate state -> reflected in editor
        state.text = "print(1)"
        assertThat(editor.text.toString()).isEqualTo("print(1)")

        // Mutate editor -> reflected in state
        editor.setText("println(42)")
        assertThat(state.text).isEqualTo("println(42)")

        // Selection sync
        state.selection = TextRange(0, 7)
        assertThat(editor.cursor.left).isEqualTo(0)
        assertThat(editor.cursor.right).isEqualTo(7)

        // Detach
        state.detachEditor(editor)
        assertThat(state.isAttached).isFalse()
        assertThat(state.editor).isNull()
    }

    @Test
    fun testImperativeActionsOnState() {
        val editor = CodeEditor(context)
        val state = CodeEditorState(initialText = "Initial")
        state.attachEditor(editor)

        state.selectAll()
        assertThat(state.selection).isEqualTo(TextRange(0, 7))

        state.insertText("Replaced")
        assertThat(state.text).isEqualTo("Replaced")

        state.undo()
        assertThat(state.canRedo).isTrue()

        state.redo()
        assertThat(state.canUndo).isTrue()

        state.detachEditor(editor)
    }

    @Test
    fun testComposeCompletionItemAdapter() {
        val adapter = ComposeCompletionItemAdapter(itemHeightDp = 40f) { item, isSelected, onClick ->
            // Composable slot
        }

        assertThat(adapter.count).isEqualTo(0)
    }

    @Test
    fun testComposeTextActionWindowIntegration() {
        val editor = CodeEditor(context)
        editor.setText("Selection Action Test")
        editor.setSelectionRegion(0, 0, 0, 9)

        var invoked = false
        val composeWindow = ComposeTextActionWindow(editor) { scope ->
            invoked = true
            assertThat(scope.selectedText).isEqualTo("Selection")
            assertThat(scope.hasSelection).isTrue()
            assertThat(scope.canCopy).isTrue()
        }

        editor.replaceComponent(composeWindow)
        val component = editor.getComponent<EditorTextActionWindow>()
        assertThat(component).isSameInstanceAs(composeWindow)
    }

    @Test
    fun testComposeDiagnosticTooltipLayout() {
        val layout = ComposeDiagnosticTooltipLayout { scope ->
            // Composable slot
        }

        val view = layout.createView(LayoutInflater.from(context))
        assertThat(view).isNotNull()

        val diagnostic = DiagnosticDetail("Syntax error at line 1", "Detailed message")
        val region = DiagnosticRegion(0, 5, DiagnosticRegion.SEVERITY_ERROR)
        layout.renderDiagnostic(diagnostic, region)

        val size = layout.measureContent(300, 200)
        assertThat(size.first).isAtLeast(0)
        assertThat(size.second).isAtLeast(0)
    }

    @Test
    fun testCodeEditorExtensions() {
        val editor = CodeEditor(context)
        editor.textString = "Kotlin DSL Extension"
        assertThat(editor.textString).isEqualTo("Kotlin DSL Extension")

        var contentChanged = false
        val unsubscribe = editor.subscribeEvent<ContentChangeEvent> { event, _ ->
            contentChanged = true
        }

        editor.insertText("!", 1)
        assertThat(contentChanged).isTrue()
        unsubscribe.unsubscribe()

        val autoComp = editor.getComponent<EditorAutoCompletion>()
        assertThat(autoComp).isNotNull()
    }

    @Test
    fun testBidirectionalSingleSourceOfTruth() {
        val editor = CodeEditor(context)
        val state = CodeEditorState(initialText = "Test bidirectional synchronization")
        state.attachEditor(editor)

        // 1. Direct mutation on editor -> must immediately sync to state without lag
        editor.isWordwrap = true
        assertThat(state.isWordwrap).isTrue()

        editor.tabWidth = 8
        assertThat(state.tabWidth).isEqualTo(8)

        editor.textSize = 22f
        assertThat(state.textSizeSp).isEqualTo(22f)

        editor.isEditable = false
        assertThat(state.isEditable).isFalse()

        editor.isLineNumberEnabled = false
        assertThat(state.lineNumbersEnabled).isFalse()

        editor.isLineNumberPinned = true
        assertThat(state.pinLineNumbers).isTrue()

        editor.isHighlightCurrentLine = false
        assertThat(state.highlightCurrentLine).isFalse()

        editor.isDividerEnabled = false
        assertThat(state.dividerEnabled).isFalse()

        editor.cursorBlinkPeriod = 0
        assertThat(state.cursorBlinkEnabled).isFalse()

        // 2. Recomposition simulation: updateEditor must NOT revert direct editor changes!
        state.updateEditor(editor)
        assertThat(editor.isWordwrap).isTrue()
        assertThat(editor.tabWidth).isEqualTo(8)
        assertThat(editor.textSize).isEqualTo(22f)
        assertThat(editor.isEditable).isFalse()
        assertThat(editor.isLineNumberEnabled).isFalse()
        assertThat(editor.isLineNumberPinned).isTrue()
        assertThat(editor.isHighlightCurrentLine).isFalse()
        assertThat(editor.isDividerEnabled).isFalse()
        assertThat(editor.cursorBlinkPeriod).isEqualTo(0)

        // 3. Mutation on state -> must reflect on editor
        state.isWordwrap = false
        assertThat(editor.isWordwrap).isFalse()

        state.tabWidth = 2
        assertThat(editor.tabWidth).isEqualTo(2)

        state.textSizeSp = 18f
        assertThat(editor.textSize).isEqualTo(18f)

        state.isEditable = true
        assertThat(editor.isEditable).isTrue()

        state.detachEditor(editor)
    }

    @Test
    fun testStateSaverPersistence() {
        val state = CodeEditorState(
            initialText = "Persistent Code",
            initialSelection = TextRange(5, 10)
        ).apply {
            isWordwrap = true
            textSizeSp = 20f
            tabWidth = 2
            isEditable = false
            lineNumbersEnabled = false
            pinLineNumbers = true
            isUndoEnabled = false
            cursorBlinkEnabled = false
            highlightCurrentLine = false
            dividerEnabled = false
        }

        val saverScope = SaverScope { true }
        val saved = with(CodeEditorState.Saver) { saverScope.save(state) } as List<*>
        val restored = CodeEditorState.Saver.restore(saved) as CodeEditorState

        assertThat(restored.text).isEqualTo("Persistent Code")
        assertThat(restored.selection).isEqualTo(TextRange(5, 10))
        assertThat(restored.isWordwrap).isTrue()
        assertThat(restored.textSizeSp).isEqualTo(20f)
        assertThat(restored.tabWidth).isEqualTo(2)
        assertThat(restored.isEditable).isFalse()
        assertThat(restored.lineNumbersEnabled).isFalse()
        assertThat(restored.pinLineNumbers).isTrue()
        assertThat(restored.isUndoEnabled).isFalse()
        assertThat(restored.cursorBlinkEnabled).isFalse()
        assertThat(restored.highlightCurrentLine).isFalse()
        assertThat(restored.dividerEnabled).isFalse()
    }
}
