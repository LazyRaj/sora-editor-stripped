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
package io.github.rosemoe.sora.widget

import android.os.Looper
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticProvider
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer
import io.github.rosemoe.sora.lang.styling.HighlightTextProvider
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintProvider
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import java.lang.ref.WeakReference

open class EditorStyleDelegate(
    editor: CodeEditor
) : StyleReceiver, InlayHintProvider, DiagnosticProvider, HighlightTextProvider {

    private val editorRef: WeakReference<CodeEditor> = WeakReference(editor)
    private var foundPair: PairedBracket? = null
    private var bracketsProvider: BracketsProvider? = null
    private var diagnostics: DiagnosticsContainer? = null
    private var inlayHints: InlayHintsContainer? = null
    private var highlightTexts: HighlightTextContainer? = null

    init {
        editor.subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
            if (!event.isSelected) {
                postUpdateBracketPair()
            }
        }
    }

    open fun onTextChange() {
        // bracketsProvider = null;
        // foundPair = null;
    }

    open fun postUpdateBracketPair() {
        runOnUiThread {
            val provider = bracketsProvider
            val editor = editorRef.get()
            if (provider != null && editor != null && !editor.cursor.isSelected && editor.isHighlightBracketPair) {
                foundPair = provider.getPairedBracketAt(editor.text, editor.cursor.left)
                editor.invalidate()
            }
        }
    }

    open val foundBracketPair: PairedBracket?
        get() = foundPair

    open fun reset() {
        foundPair = null
        bracketsProvider = null
        diagnostics = null
        inlayHints = null
        highlightTexts = null
    }

    private fun runOnUiThread(operation: Runnable) {
        val editor = editorRef.get() ?: return
        if (Looper.getMainLooper().thread === Thread.currentThread()) {
            operation.run()
        } else {
            editor.postInLifecycle(operation)
        }
    }

    override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?) {
        setStyles(sourceManager, styles, null)
    }

    override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?, action: Runnable?) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage.analyzeManager) {
            runOnUiThread {
                action?.run()
                editor.styles = styles
            }
        }
    }

    override fun setDiagnostics(sourceManager: AnalyzeManager, diagnostics: DiagnosticsContainer?) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage.analyzeManager) {
            this.diagnostics = diagnostics
            runOnUiThread { editor.invalidateDiagnostics() }
        }
    }

    override fun setInlayHints(sourceManager: AnalyzeManager, inlayHints: InlayHintsContainer?) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage.analyzeManager) {
            this.inlayHints = inlayHints
            runOnUiThread { editor.invalidateInlayHints() }
        }
    }

    open fun setHighlightTexts(
        sourceManager: AnalyzeManager,
        highlightTexts: HighlightTextContainer?
    ) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage.analyzeManager) {
            this.highlightTexts = highlightTexts
            runOnUiThread { editor.invalidateHighlightTexts() }
        }
    }

    override fun provideInlayHints(container: InlayHintsContainer) {
        val hints = inlayHints
        if (hints != null) {
            container.addAll(hints)
        }
    }

    override fun provideDiagnostics(container: DiagnosticsContainer) {
        val diag = diagnostics
        if (diag != null) {
            container.addDiagnostics(diag.getRegions())
        }
    }

    override fun provideHighlightTexts(container: HighlightTextContainer) {
        val texts = highlightTexts
        if (texts != null) {
            container.addAll(texts.asList())
        }
    }

    override fun updateBracketProvider(sourceManager: AnalyzeManager, provider: BracketsProvider?) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage.analyzeManager && bracketsProvider !== provider) {
            this.bracketsProvider = provider
            postUpdateBracketPair()
        }
    }

    override fun updateStyles(
        sourceManager: AnalyzeManager,
        styles: Styles,
        range: StyleUpdateRange
    ) {
        val editor = editorRef.get()
        if (editor != null && sourceManager === editor.editorLanguage.analyzeManager) {
            runOnUiThread { editor.updateStyles(styles, range) }
        }
    }

    open fun clearFoundBracketPair() {
        this.foundPair = null
    }
}
