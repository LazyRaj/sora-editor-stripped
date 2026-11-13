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
// Modified by Raj Dave (aka LazyRaj)
package io.github.rosemoe.sora.lang.styling

import io.github.rosemoe.sora.text.CharPosition

/**
 * Spans object saves spans in editor.
 */
interface Spans {

    /**
     * Get line count of the spans
     */
    fun getLineCount(): Int

    /**
     * Adjust spans on insert.
     * Must be implemented.
     */
    fun adjustOnInsert(start: CharPosition, end: CharPosition)

    /**
     * Adjust spans on delete.
     * Must be implemented.
     */
    fun adjustOnDelete(start: CharPosition, end: CharPosition)

    /**
     * Read spans.
     * Must be implemented.
     */
    fun read(): Reader

    /**
     * Check whether the class supports [modify]
     */
    fun supportsModify(): Boolean

    /**
     * Modify the content.
     *
     * Optional to implement.
     */
    fun modify(): Modifier

    /**
     * Reader reads the spans in a [Spans] object.
     */
    interface Reader {

        /**
         * Get span count on current line
         */
        fun getSpanCount(): Int

        /**
         * Start reading the spans on the given line.
         * You may prepare some data here if the actual spans are not stored by [Span] objects.
         *
         * line may be -1 to release the reader.
         */
        fun moveToLine(line: Int)

        /**
         * Get span at position [index].
         * The result object is read-only. Callers should not modify this object.
         */
        fun getSpanAt(index: Int): Span

        /**
         * Get all spans on the given line. This ignores the line argument set by [moveToLine].
         * The list contains at least 1 span. And the result list is unmodifiable.
         */
        fun getSpansOnLine(line: Int): List<Span>
    }

    /**
     * Modifier updates the spans in a [Spans] object.
     */
    interface Modifier {

        /**
         * Set the line's spans to the new ones. The given [spans] list should not be stored,
         * but the content of it can be copied.
         *
         * If the line index exceeds the current capacity, implementation of this should expand the capacity
         * without throwing an exception. Set spans of the filled lines to color [io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_NORMAL]
         * or extends previous styles.
         */
        fun setSpansOnLine(line: Int, spans: List<Span>)

        /**
         * Add a line at the given position.
         * The given [spans] list should not be stored,
         * but the content of it can be copied.
         */
        fun addLineAt(line: Int, spans: List<Span>)

        /**
         * Remove a line
         */
        fun deleteLineAt(line: Int)
    }
}

/**
 * Idiomatic Kotlin property accessor for line count
 */
inline val Spans.lineCount: Int
    get() = getLineCount()

/**
 * Idiomatic Kotlin property accessor for span count
 */
inline val Spans.Reader.spanCount: Int
    get() = getSpanCount()
