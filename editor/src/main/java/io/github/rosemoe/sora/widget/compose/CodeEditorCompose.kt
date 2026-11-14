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

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.DefaultCompletionItemAdapter
import io.github.rosemoe.sora.widget.component.DefaultDiagnosticTooltipLayout
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow

/**
 * A Jetpack Compose native wrapper for [CodeEditor] providing two-way observable state
 * synchronization, lifecycle awareness, and first-class Compose component customization
 * for AutoCompletion items, Selection Action popups, Diagnostic tooltips, and editor overlays.
 *
 * ### 1. Basic Usage
 * ```kotlin
 * @Composable
 * fun SimpleEditorScreen() {
 *     val editorState = rememberCodeEditorState(
 *         initialText = "fun main() {\n    println(\"Hello, Compose!\")\n}",
 *         isWordwrap = false,
 *         textSizeSp = 14f
 *     )
 *
 *     Column(modifier = Modifier.fillMaxSize()) {
 *         // Read state dynamically
 *         Text("Lines: ${editorState.lineCount} | Cursor: ${editorState.cursorPosition.line}:${editorState.cursorPosition.column}")
 *
 *         CodeEditorCompose(
 *             state = editorState,
 *             modifier = Modifier.weight(1f).fillMaxWidth()
 *         )
 *
 *         // Imperative actions
 *         Row {
 *             Button(onClick = { editorState.undo() }, enabled = editorState.canUndo) { Text("Undo") }
 *             Button(onClick = { editorState.redo() }, enabled = editorState.canRedo) { Text("Redo") }
 *             Button(onClick = { editorState.insertText("// Inserted\n") }) { Text("Insert") }
 *         }
 *     }
 * }
 * ```
 *
 * ### 2. Custom Compose AutoCompletion
 * You can supply a custom `@Composable` slot to render the complete suggestions popup:
 * ```kotlin
 * CodeEditorCompose(
 *     state = editorState,
 *     modifier = Modifier.fillMaxSize(),
 *     autoCompletion = { isAboveTheCursor, items, onItemClick, selectedItem ->
 *         val listState = rememberLazyListState()
 *         LaunchedEffect(selectedItem) {
 *             if (selectedItem != null) {
 *                 val idx = items.indexOf(selectedItem)
 *                 if (idx in items.indices) listState.animateScrollToItem(idx)
 *             }
 *         }
 *         Column(
 *             modifier = Modifier
 *                 .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
 *                 .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
 *                 .padding(4.dp)
 *         ) {
 *             if (isAboveTheCursor) {
 *                 Text("Suggestions (Above Cursor)", style = MaterialTheme.typography.labelSmall)
 *             }
 *             LazyColumn(
 *                 state = listState,
 *                 modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
 *             ) {
 *                 items(items, key = { "${it.label}_${it.desc ?: ""}" }) { item ->
 *                     val isSelected = item == selectedItem
 *                     Row(
 *                         modifier = Modifier
 *                             .fillMaxWidth()
 *                             .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
 *                             .clickable { onItemClick(item) }
 *                             .padding(horizontal = 8.dp, vertical = 4.dp),
 *                         verticalAlignment = Alignment.CenterVertically
 *                     ) {
 *                         Text(text = item.label.toString(), fontWeight = FontWeight.Bold)
 *                         item.desc?.let { Text(text = it.toString()) }
 *                     }
 *                 }
 *             }
 *             if (!isAboveTheCursor) {
 *                 Text("Suggestions (Below Cursor)", style = MaterialTheme.typography.labelSmall)
 *             }
 *         }
 *     }
 * )
 * ```
 *
 * ### 3. Custom Selection Action Floating Window
 * Replace the standard copy/cut/paste popup with a custom Compose floating toolbar:
 * ```kotlin
 * CodeEditorCompose(
 *     state = editorState,
 *     modifier = Modifier.fillMaxSize(),
 *     selectionActionContent = { scope ->
 *         Surface(
 *             shape = RoundedCornerShape(8.dp),
 *             shadowElevation = 4.dp,
 *             color = MaterialTheme.colorScheme.surface
 *         ) {
 *             Row(modifier = Modifier.padding(4.dp)) {
 *                 if (scope.canCopy) {
 *                     IconButton(onClick = { scope.copy() }) {
 *                         Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
 *                     }
 *                 }
 *                 if (scope.canPaste) {
 *                     IconButton(onClick = { scope.paste() }) {
 *                         Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
 *                     }
 *                 }
 *                 // Custom action (e.g. AI explain)
 *                 Button(
 *                     onClick = {
 *                         explainCodeWithAI(scope.selectedText)
 *                         scope.dismiss()
 *                     }
 *                 ) {
 *                     Text("Explain")
 *                 }
 *             }
 *         }
 *     }
 * )
 * ```
 *
 * ### 4. Custom Diagnostic Tooltip Window
 * Supply custom Compose rendering for syntax and linter diagnostics:
 * ```kotlin
 * CodeEditorCompose(
 *     state = editorState,
 *     modifier = Modifier.fillMaxSize(),
 *     diagnosticTooltipContent = { scope ->
 *         Card(
 *             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
 *         ) {
 *             Column(modifier = Modifier.padding(8.dp)) {
 *                 Text(
 *                     text = scope.diagnostic?.message ?: "Unknown issue",
 *                     color = MaterialTheme.colorScheme.onErrorContainer
 *                 )
 *                 Button(onClick = { scope.dismiss() }) { Text("Close") }
 *             }
 *         }
 *     }
 * )
 * ```
 *
 * ### 5. Floating Overlay Content
 * Place floating Compose UI (such as search & replace headers, minimaps, or status badges)
 * layered directly above the editor:
 * ```kotlin
 * CodeEditorCompose(
 *     state = editorState,
 *     modifier = Modifier.fillMaxSize(),
 *     overlayContent = { state ->
 *         FloatingSearchBar(
 *             modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
 *             onSearch = { query -> state.setSelection(...) }
 *         )
 *     }
 * )
 * ```
 *
 * @param state The [CodeEditorState] instance controlling and observing the editor.
 * @param modifier The Compose modifier applied to the parent container.
 * @param editorFactory Optional factory to construct a customized [CodeEditor] instance.
 * @param autoCompletion Composable slot customizing the auto-completion popup, receiving `isAboveTheCursor`, `items`, `onItemClick`, and `selectedItem`.
 * @param selectionActionContent Composable slot customizing the text selection floating action menu.
 * @param diagnosticTooltipContent Composable slot customizing the diagnostic tooltip popup.
 * @param overlayContent Composable slot rendered directly over the editor viewport inside a [BoxScope].
 * @param onEditorCreated Invoked once when the underlying native [CodeEditor] view is instantiated.
 */
@Composable
fun CodeEditorCompose(
    state: CodeEditorState,
    modifier: Modifier = Modifier,
    editorFactory: ((Context) -> CodeEditor)? = null,
    autoCompletion: (@Composable (isAboveTheCursor: Boolean, items: List<CompletionItem>, onItemClick: (CompletionItem) -> Unit, selectedItem: CompletionItem?) -> Unit)? = null,
    selectionActionContent: (@Composable (TextActionScope) -> Unit)? = null,
    diagnosticTooltipContent: (@Composable (DiagnosticTooltipScope) -> Unit)? = null,
    overlayContent: (@Composable BoxScope.(state: CodeEditorState) -> Unit)? = null,
    onEditorCreated: ((CodeEditor) -> Unit)? = null,
    onEditorUpdated: ((CodeEditor) -> Unit)? = null
) {
    val compositionContext = rememberCompositionContext()

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val editor = editorFactory?.invoke(context) ?: CodeEditor(context)

                if (autoCompletion != null) {
                    editor.getComponent(EditorAutoCompletion::class.java).setLayout(
                        ComposeCompletionLayout(
                            parentCompositionContext = compositionContext,
                            content = autoCompletion
                        )
                    )
                }

                if (selectionActionContent != null) {
                    val composeActionWindow = ComposeTextActionWindow(
                        editor = editor,
                        actionContent = selectionActionContent,
                        parentCompositionContext = compositionContext
                    )
                    editor.replaceComponent(EditorTextActionWindow::class.java, composeActionWindow)
                }

                if (diagnosticTooltipContent != null) {
                    editor.diagnosticTooltip?.layout =
                        ComposeDiagnosticTooltipLayout(
                            tooltipContent = diagnosticTooltipContent,
                            parentCompositionContext = compositionContext
                        )
                }

                state.attachEditor(editor)
                onEditorCreated?.invoke(editor)
                editor
            },
            update = { editor ->
                if (selectionActionContent != null) {
                    val currentActionWindow =
                        editor.getComponent(EditorTextActionWindow::class.java)
                    if (currentActionWindow is ComposeTextActionWindow) {
                        currentActionWindow.actionContent = selectionActionContent
                    } else {
                        val composeActionWindow = ComposeTextActionWindow(
                            editor = editor,
                            actionContent = selectionActionContent,
                            parentCompositionContext = compositionContext
                        )
                        editor.replaceComponent(
                            EditorTextActionWindow::class.java,
                            composeActionWindow
                        )
                    }
                } else {
                    val currentActionWindow =
                        editor.getComponent(EditorTextActionWindow::class.java)
                    if (currentActionWindow is ComposeTextActionWindow) {
                        editor.replaceComponent(
                            EditorTextActionWindow::class.java,
                            EditorTextActionWindow(editor)
                        )
                    }
                }

                if (autoCompletion != null) {
                    val autoComp = editor.getComponent(EditorAutoCompletion::class.java)
                    val currentLayout = autoComp.layout
                    if (currentLayout is ComposeCompletionLayout) {
                        currentLayout.content = autoCompletion
                    } else {
                        autoComp.setLayout(
                            ComposeCompletionLayout(
                                parentCompositionContext = compositionContext,
                                content = autoCompletion
                            )
                        )
                    }
                } else {
                    val autoComp = editor.getComponent(EditorAutoCompletion::class.java)
                    if (autoComp.layout is ComposeCompletionLayout) {
                        autoComp.setLayout(io.github.rosemoe.sora.widget.component.DefaultCompletionLayout())
                    }
                }

                if (diagnosticTooltipContent != null) {
                    val currentLayout = editor.diagnosticTooltip?.layout
                    if (currentLayout is ComposeDiagnosticTooltipLayout) {
                        currentLayout.tooltipContent = diagnosticTooltipContent
                    } else {
                        editor.diagnosticTooltip?.layout =
                            ComposeDiagnosticTooltipLayout(
                                tooltipContent = diagnosticTooltipContent,
                                parentCompositionContext = compositionContext
                            )
                    }
                } else {
                    val currentLayout = editor.diagnosticTooltip?.layout
                    if (currentLayout is ComposeDiagnosticTooltipLayout) {
                        editor.diagnosticTooltip?.layout = DefaultDiagnosticTooltipLayout()
                    }
                }

                state.updateEditor(editor)

                onEditorUpdated?.invoke(editor)
            },
            onRelease = { editor ->
                state.detachEditor(editor)
                editor.release()
            }
        )

        overlayContent?.invoke(this, state)
    }
}
