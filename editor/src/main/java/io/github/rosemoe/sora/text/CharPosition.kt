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

import io.github.rosemoe.sora.util.IntPair
import java.util.Objects

/**
 * This a data class of a character position in [Content]
 *
 * @author Rosemoe
 */
class CharPosition {

    @JvmField
    var index: Int

    @JvmField
    var line: Int

    @JvmField
    var column: Int

    constructor() : this(0, 0, 0)

    constructor(line: Int, column: Int) : this(line, column, -1)

    constructor(line: Int, column: Int, index: Int) {
        this.index = index
        this.line = line
        this.column = column
    }

    /**
     * Get the index
     *
     * @return index
     */
    fun getIndex(): Int = index

    /**
     * Get column
     *
     * @return column
     */
    fun getColumn(): Int = column

    /**
     * Get line
     *
     * @return line
     */
    fun getLine(): Int = line

    /**
     * Make this CharPosition zero and return self
     *
     * @return self
     */
    fun toBOF(): CharPosition {
        index = 0
        line = 0
        column = 0
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CharPosition) return false
        return column == other.column && line == other.line && index == other.index
    }

    override fun hashCode(): Int {
        return Objects.hash(index, line, column)
    }

    /**
     * Convert [line] and [column] to a Long number
     *
     * First integer is line and second integer is column
     *
     * @return A Long integer describing the position
     */
    fun toIntPair(): Long = IntPair.pack(line, column)

    /**
     * Make a copy of this CharPosition and return the copy
     *
     * @return New CharPosition including info of this CharPosition
     */
    fun fromThis(): CharPosition {
        val pos = CharPosition()
        pos.set(this)
        return pos
    }

    /**
     * Set this [CharPosition] object's data the same as [another]
     */
    fun set(another: CharPosition) {
        index = another.index
        line = another.line
        column = another.column
    }

    override fun toString(): String = "CharPosition(line = $line,column = $column,index = $index)"
}
