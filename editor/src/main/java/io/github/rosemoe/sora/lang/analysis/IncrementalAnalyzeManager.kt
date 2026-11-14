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
package io.github.rosemoe.sora.lang.analysis

import io.github.rosemoe.sora.lang.styling.Span

/**
 * Interface for line based analyze managers
 *
 * @param <S> State type at line endings
 * @param <T> Token type
 */
interface IncrementalAnalyzeManager<S, T> : AnalyzeManager {

    /**
     * Get the initial state at document start
     */
    fun getInitialState(): S?

    /**
     * Get recorded state for subclass
     */
    fun getState(line: Int): LineTokenizeResult<S, T>?

    /**
     * Compare the two states.
     * Return true if they equal
     */
    fun stateEquals(state: S?, another: S?): Boolean

    /**
     * Tokenize for the given line
     *
     * @param lineIndex -1 for unknown
     */
    fun tokenizeLine(line: CharSequence, state: S?, lineIndex: Int): LineTokenizeResult<S, T>

    /**
     * Generate spans for the line
     */
    fun generateSpansForLine(tokens: LineTokenizeResult<S, T>): List<Span>?

    /**
     * Called when a State object is to be abandoned
     */
    fun onAbandonState(state: S) {}

    /**
     * Called when a State object is to be added
     */
    fun onAddState(state: S) {}

    /**
     * Saved state
     */
    class LineTokenizeResult<S_, T_>(
        var state: S_,
        var tokens: List<T_>? = null,
        var spans: List<Span>? = null
    ) {
        operator fun component1(): S_ = state
        operator fun component2(): List<T_>? = tokens
        operator fun component3(): List<Span>? = spans

        fun clearSpans(): LineTokenizeResult<S_, T_> {
            spans = null
            return this
        }
    }
}
