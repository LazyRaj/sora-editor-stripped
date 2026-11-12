/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
 *    Modified by Raj Dave (aka LazyRaj)
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

package io.github.rosemoe.sora.lang

import android.os.Bundle
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolPairMatch

/**
 * Language for editor
 *
 * A Language instance helps editor to highlight text and provide auto-completion.
 * Implement this interface when you want to add new language support for editor.
 *
 * **NOTE:** A language must not be single instance.
 * One language instance should always serve for only one editor.
 * It means that you should not pass a language object to multiple editor instances.
 *
 * @author Rosemoe
 */
interface Language {

    companion object {
        /**
         * Set the thread's interrupted flag by calling [Thread.interrupt].
         *
         * Throw [CompletionCancelledException] exceptions
         * from [ContentReference] and [CompletionPublisher].
         *
         * Set thread's flag for abortion.
         */
        const val INTERRUPTION_LEVEL_STRONG = 0

        /**
         * Throw [CompletionCancelledException] exceptions
         * from [ContentReference] and [CompletionPublisher].
         *
         * Set thread's flag for abortion.
         */
        const val INTERRUPTION_LEVEL_SLIGHT = 1

        /**
         * Throw [CompletionCancelledException] exceptions
         * from [ContentReference]
         *
         * Set thread's flag for abortion.
         */
        const val INTERRUPTION_LEVEL_NONE = 2
    }

    /**
     * Get [AnalyzeManager] of the language.
     * The result instance is expected to be the same in language lifecycle.
     */
    val analyzeManager: AnalyzeManager
        @UiThread get

    /**
     * Get the interruption level for auto-completion.
     *
     * @see INTERRUPTION_LEVEL_STRONG
     * @see INTERRUPTION_LEVEL_SLIGHT
     * @see INTERRUPTION_LEVEL_NONE
     */
    val interruptionLevel: Int

    /**
     * Get the code formatter for the current language.
     * The formatter is expected to be the same one during the lifecycle of a language instance.
     *
     * @return The code formatter for the current language.
     */
    val formatter: Formatter
        @UiThread get

    /**
     * Returns language specified symbol pairs.
     * The method is called only once when the language is applied.
     */
    val symbolPairs: SymbolPairMatch?
        @UiThread get

    /**
     * Get newline handlers of this language.
     * This method is called each time the user presses ENTER key.
     *
     * Pay attention to the performance as this method is called frequently
     *
     * @return NewlineHandlers, maybe null
     */
    val newlineHandlers: Array<NewlineHandler>?
        @UiThread get

    /**
     * Get quick quote handler of this language.
     * This method is called each time the user types a single character (or a single code point)
     * and some text is currently selected.
     *
     * Pay attention to the performance as this method is called frequently
     *
     * @return QuickQuoteHandler, maybe null
     */
    val quickQuoteHandler: QuickQuoteHandler?
        @UiThread get() = null

    /**
     * Request to auto-complete the code at the given [position].
     * Note that if you did not correctly set the spans for the text, the auto-completion will not be triggered.
     * This is called in a worker thread other than UI thread.
     *
     * @param content Read-only reference of content
     * @param position The position for auto-complete
     * @param publisher The publisher used to update items
     * @param extraArguments Arguments set by [CodeEditor.setText]
     * @throws CompletionCancelledException This thread can be abandoned
     * by the editor framework because the auto-completion items of
     * this invocation are no longer needed by the user.
     * @see ContentReference
     * @see CompletionPublisher
     * @see interruptionLevel
     * @see CompletionHelper.checkCancelled
     */
    @WorkerThread
    @Throws(CompletionCancelledException::class)
    fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    )

    /**
     * Get delta indent spaces count.
     *
     * @param content Content of given line.
     * @param line 0-indexed line number. The indentation is applied on line index: `line + 1`.
     * @param column Column on the given line, where a line separator is inserted.
     * @return Delta count of indent spaces. It can be a negative/positive number or zero.
     */
    @UiThread
    fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int

    /**
     * Get delta indent spaces count.
     *
     * @param content Content of given line.
     * @param line 0-indexed line number. The indentation is applied on line index: `line + 1`.
     * @param column Column on the given line, where a line separator is inserted.
     * @param spaceCountOnLine The number of spaces on `line`.
     * @param tabCountOnLine The number of tabs on `line`.
     * @return Delta count of indent spaces. It can be a negative/positive number or zero.
     */
    @UiThread
    fun getIndentAdvance(
        content: ContentReference,
        line: Int,
        column: Int,
        spaceCountOnLine: Int,
        tabCountOnLine: Int
    ): Int {
        return getIndentAdvance(content, line, column)
    }

    /**
     * Use tab to format
     */
    @UiThread
    fun useTab(): Boolean

    /**
     * Destroy this [Language] object.
     *
     * When called, you should stop your resource-taking actions and remove any reference
     * of editor or other objects related to editor (such as references to text in editor) to avoid
     * memory leaks and resource waste.
     */
    @UiThread
    fun destroy()
}
