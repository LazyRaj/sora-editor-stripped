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
/**
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.text

import io.github.rosemoe.sora.annotations.UnsupportedUserUsage
import io.github.rosemoe.sora.util.IntPair

/**
 * The cursor position will update automatically when the content has been changed by other ways.
 *
 * @author Rosemoe
 */
class Cursor(private val content: Content) {

    companion object {
        const val DIRECTION_NONE: Int = 0
        const val DIRECTION_LTR: Int = 1
        const val DIRECTION_RTL: Int = 2
    }

    val indexer: CachedIndexer = CachedIndexer(content)
    private var leftSel: CharPosition = CharPosition().toBOF()
    private var rightSel: CharPosition = CharPosition().toBOF()
    private var cache0: CharPosition? = null
    private var cache1: CharPosition? = null
    private var cache2: CharPosition? = null
    var selectionDirection: Int = DIRECTION_NONE

    /**
     * Make left and right cursor on the given position
     *
     * @param line   The line position
     * @param column The column position
     */
    fun set(line: Int, column: Int) {
        setLeft(line, column)
        setRight(line, column)
    }

    /**
     * Make left cursor on the given position
     *
     * @param line   The line position
     * @param column The column position
     */
    fun setLeft(line: Int, column: Int) {
        leftSel = indexer.getCharPosition(line, column).fromThis()
    }

    /**
     * Make right cursor on the given position
     *
     * @param line   The line position
     * @param column The column position
     */
    fun setRight(line: Int, column: Int) {
        rightSel = indexer.getCharPosition(line, column).fromThis()
    }

    /**
     * Get the left cursor line
     *
     * @return line of left cursor
     */
    val leftLine: Int
        get() = leftSel.line

    /**
     * Get the left cursor column
     *
     * @return column of left cursor
     */
    val leftColumn: Int
        get() = leftSel.column

    /**
     * Get the right cursor line
     *
     * @return line of right cursor
     */
    val rightLine: Int
        get() = rightSel.line

    /**
     * Get the right cursor column
     *
     * @return column of right cursor
     */
    val rightColumn: Int
        get() = rightSel.column

    /**
     * Whether the given position is in selected region
     *
     * @param line   The line to query
     * @param column The column to query
     * @return Whether is in selected region
     */
    fun isInSelectedRegion(line: Int, column: Int): Boolean {
        if (line in leftLine..rightLine) {
            var yes = true
            if (line == leftLine) {
                yes = column >= leftColumn
            }
            if (line == rightLine) {
                yes = yes && column < rightColumn
            }
            return yes
        }
        return false
    }

    /**
     * Get the left cursor index
     *
     * @return index of left cursor
     */
    val left: Int
        get() = leftSel.index

    /**
     * Get the right cursor index
     *
     * @return index of right cursor
     */
    val right: Int
        get() = rightSel.index

    /**
     * Notify the Indexer to update its cache for current display position
     *
     * This will make querying actions quicker
     *
     * Especially when the editor user want to set a new cursor position after scrolling long time
     *
     * @param line First visible line
     */
    fun updateCache(line: Int) {
        indexer.getCharIndex(line, 0)
    }

    /**
     * Get whether text is selected
     *
     * @return Whether selected
     */
    val isSelected: Boolean
        get() = leftSel.index != rightSel.index

    /**
     * Get position after moving left once
     *
     * @param position A packed pair (line, column) describing the original position
     * @return A packed pair (line, column) describing the result position
     */
    fun getLeftOf(position: Long): Long {
        val line = IntPair.getFirst(position)
        val column = IntPair.getSecond(position)
        val nColumn = TextLayoutHelper.get().getCurPosLeft(column, content.getLine(line))
        return if (nColumn == column && column == 0) {
            if (line == 0) {
                0L
            } else {
                val cColumn = content.getColumnCount(line - 1)
                IntPair.pack(line - 1, cColumn)
            }
        } else {
            IntPair.pack(line, nColumn)
        }
    }

    /**
     * Get position after moving right once
     *
     * @param position A packed pair (line, column) describing the original position
     * @return A packed pair (line, column) describing the result position
     */
    fun getRightOf(position: Long): Long {
        val line = IntPair.getFirst(position)
        val column = IntPair.getSecond(position)
        val cColumn = content.getColumnCount(line)
        val nColumn = TextLayoutHelper.get().getCurPosRight(column, content.getLine(line))
        return if (nColumn == cColumn && column == nColumn) {
            if (line + 1 == content.lineCount) {
                IntPair.pack(line, cColumn)
            } else {
                IntPair.pack(line + 1, 0)
            }
        } else {
            IntPair.pack(line, nColumn)
        }
    }

    /**
     * Get copy of left cursor
     */
    fun left(): CharPosition = leftSel.fromThis()

    /**
     * Get copy of right cursor
     */
    fun right(): CharPosition = rightSel.fromThis()

    /**
     * Get current range of cursor. Modifications to the returned object does not affect cursor positions.
     *
     * @return [TextRange] object describing cursor positions
     */
    val range: TextRange
        get() = TextRange(left(), right())

    /**
     * Internal call back before insertion
     *
     * @param startLine   Start line
     * @param startColumn Start column
     */
    @UnsupportedUserUsage
    fun beforeInsert(startLine: Int, startColumn: Int) {
        cache0 = indexer.getCharPosition(startLine, startColumn).fromThis()
    }

    /**
     * Internal call back before deletion
     *
     * @param startLine   Start line
     * @param startColumn Start column
     * @param endLine     End line
     * @param endColumn   End column
     */
    @UnsupportedUserUsage
    fun beforeDelete(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        cache1 = indexer.getCharPosition(startLine, startColumn).fromThis()
        cache2 = indexer.getCharPosition(endLine, endColumn).fromThis()
    }

    /**
     * Internal call back before replace
     */
    @UnsupportedUserUsage
    fun beforeReplace() {
        indexer.beforeReplace(content)
    }

    /**
     * Internal call back after insertion
     *
     * @param startLine       Start line
     * @param startColumn     Start column
     * @param endLine         End line
     * @param endColumn       End column
     * @param insertedContent Inserted content
     */
    @UnsupportedUserUsage
    fun afterInsert(
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        indexer.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        val beginIdx = cache0?.index ?: 0
        if (left >= beginIdx) {
            leftSel = indexer.getCharPosition(left + insertedContent.length).fromThis()
        }
        if (right >= beginIdx) {
            rightSel = indexer.getCharPosition(right + insertedContent.length).fromThis()
        }
    }

    /**
     * Internal call back
     *
     * @param startLine      Start line
     * @param startColumn    Start column
     * @param endLine        End line
     * @param endColumn      End column
     * @param deletedContent Deleted content
     */
    @UnsupportedUserUsage
    fun afterDelete(
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        indexer.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        val beginIdx = cache1?.index ?: 0
        val endIdx = cache2?.index ?: 0
        var l = left
        var r = right
        if (beginIdx > r) {
            return
        }
        l -= maxOf(0, minOf(l - beginIdx, endIdx - beginIdx))
        r -= maxOf(0, minOf(r - beginIdx, endIdx - beginIdx))
        leftSel = indexer.getCharPosition(l).fromThis()
        rightSel = indexer.getCharPosition(r).fromThis()
    }
}
