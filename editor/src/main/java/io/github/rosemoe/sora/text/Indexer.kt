/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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

package io.github.rosemoe.sora.text

/**
 * A helper class for ITextContent to transform (line,column) and index
 *
 * @author Rose
 */
interface Indexer {

    /**
     * Get the index of (line,column)
     *
     * @param line The line position of index
     * @param column The column position of index
     * @return Calculated index
     */
    fun getCharIndex(line: Int, column: Int): Int

    /**
     * Get the line position of index
     *
     * @param index The index you want to know its line
     * @return Line position of index
     */
    fun getCharLine(index: Int): Int

    /**
     * Get the column position of index
     *
     * @param index The index you want to know its column
     * @return Column position of index
     */
    fun getCharColumn(index: Int): Int

    /**
     * Get the CharPosition for the given index
     *
     * @param index The index you want to get
     * @return The CharPosition object.
     */
    fun getCharPosition(index: Int): CharPosition

    /**
     * Get the CharPosition for the given (line,column)
     *
     * @param line The line position you want to get
     * @param column The column position you want to get
     * @return The CharPosition object.
     */
    fun getCharPosition(line: Int, column: Int): CharPosition

    /**
     * @param dest Destination of result
     * @see getCharPosition
     */
    fun getCharPosition(index: Int, dest: CharPosition)

    /**
     * @param dest Destination of result
     * @see getCharPosition
     */
    fun getCharPosition(line: Int, column: Int, dest: CharPosition)
}
