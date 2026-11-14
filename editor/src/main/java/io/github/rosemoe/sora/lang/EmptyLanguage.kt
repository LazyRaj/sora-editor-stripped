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
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lang.util.BaseAnalyzeManager
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.widget.SymbolPairMatch

/**
 * Empty language
 *
 * @author Rosemoe
 */
open class EmptyLanguage : Language {

    companion object {
        val EMPTY_SYMBOL_PAIRS = SymbolPairMatch()
    }

    override val formatter: Formatter
        get() = EmptyFormatter.INSTANCE

    override val symbolPairs: SymbolPairMatch
        get() = EMPTY_SYMBOL_PAIRS

    override val newlineHandlers: Array<NewlineHandler>?
        get() = emptyArray()

    override val analyzeManager: AnalyzeManager
        get() = EmptyAnalyzeManager.INSTANCE

    override val interruptionLevel: Int
        get() = Language.INTERRUPTION_LEVEL_STRONG

    override val quickQuoteHandler: QuickQuoteHandler?
        get() = null

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
    }

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int = 0

    override fun destroy() {
    }

    override fun useTab(): Boolean = false

    open class EmptyFormatter : Formatter {
        companion object {
            val INSTANCE = EmptyFormatter()
        }

        override fun format(text: Content, cursorRange: TextRange) {
        }

        override fun formatRegion(text: Content, rangeToFormat: TextRange, cursorRange: TextRange) {
        }

        override fun setReceiver(receiver: Formatter.FormatResultReceiver?) {
        }

        override fun isRunning(): Boolean = false

        override fun destroy() {
        }
    }

    open class EmptyAnalyzeManager : BaseAnalyzeManager() {
        companion object {
            val INSTANCE = EmptyAnalyzeManager()
        }

        override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {
        }

        override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {
        }

        override fun rerun() {
        }
    }
}
