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

import java.util.Objects

/**
 * Code block info model
 *
 * @author Rose
 */
open class CodeBlock(
    /**
     * Start line of code block
     */
    var startLine: Int = 0,
    /**
     * Start column of code block
     */
    var startColumn: Int = 0,
    /**
     * End line of code block
     */
    var endLine: Int = 0,
    /**
     * End column of code block
     */
    var endColumn: Int = 0,
    /**
     * Indicates that this BlockLine should be drawn vertically until the bottom of its end line
     */
    var toBottomOfEndLine: Boolean = false
) {

    operator fun component1(): Int = startLine
    operator fun component2(): Int = startColumn
    operator fun component3(): Int = endLine
    operator fun component4(): Int = endColumn
    operator fun component5(): Boolean = toBottomOfEndLine

    fun copy(
        startLine: Int = this.startLine,
        startColumn: Int = this.startColumn,
        endLine: Int = this.endLine,
        endColumn: Int = this.endColumn,
        toBottomOfEndLine: Boolean = this.toBottomOfEndLine
    ): CodeBlock = CodeBlock(startLine, startColumn, endLine, endColumn, toBottomOfEndLine)

    operator fun contains(line: Int): Boolean = line in startLine..endLine

    fun clear() {
        startLine = 0
        startColumn = 0
        endLine = 0
        endColumn = 0
        toBottomOfEndLine = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeBlock) return false
        return startLine == other.startLine &&
                startColumn == other.startColumn &&
                endLine == other.endLine &&
                endColumn == other.endColumn &&
                toBottomOfEndLine == other.toBottomOfEndLine
    }

    override fun hashCode(): Int {
        return Objects.hash(startLine, startColumn, endLine, endColumn, toBottomOfEndLine)
    }

    override fun toString(): String {
        return "BlockLine{startLine=$startLine, startColumn=$startColumn, endLine=$endLine, endColumn=$endColumn, toBottomOfEndLine=$toBottomOfEndLine}"
    }

    companion object {
        val COMPARATOR_END: Comparator<CodeBlock> = Comparator { a, b ->
            val res = a.endLine.compareTo(b.endLine)
            if (res == 0) {
                a.endColumn.compareTo(b.endColumn)
            } else {
                res
            }
        }

        val COMPARATOR_START: Comparator<CodeBlock> = Comparator { a, b ->
            val res = a.startLine.compareTo(b.startLine)
            if (res == 0) {
                a.startColumn.compareTo(b.startColumn)
            } else {
                res
            }
        }

        /**
         * Performs a binary search to find the index of the smallest code block whose end line is
         * greater than or equal to the specified line.
         *
         * This implementation also handles the case where the elements in the list are
         * `null`. If a null element is encountered at `mid`, we look for the
         * first non-null element either before and after the element and set it as `mid`.
         *
         * @param line   The line number to search for.
         * @param blocks The list of code blocks to search within.
         * @return The index of the smallest code block with an end line greater than or equal to the
         * specified line. If no matching code block is found, -1 is returned.
         */
        fun binarySearchEndBlock(line: Int, blocks: List<CodeBlock?>?): Int {
            if (blocks.isNullOrEmpty()) {
                return -1
            }

            var left = 0
            var right = blocks.size - 1
            val max = right

            while (left <= right) {
                var mid = left + (right - left) / 2
                if (mid < 0 || mid > max) {
                    return -1
                }

                var block = blocks[mid]
                if (block == null) {
                    var nonNullLeft = mid - 1
                    var nonNullRight = mid + 1

                    while (true) {
                        if (nonNullLeft < left && nonNullRight > right) {
                            return -1
                        } else if (nonNullLeft >= left && blocks[nonNullLeft] != null) {
                            mid = nonNullLeft
                            break
                        } else if (nonNullRight <= right && blocks[nonNullRight] != null) {
                            mid = nonNullRight
                            break
                        }
                        nonNullLeft--
                        nonNullRight++
                    }

                    block = blocks[mid]
                }

                val row = block!!.endLine
                if (row > line) {
                    right = mid - 1
                } else if (row < line) {
                    left = mid + 1
                } else {
                    left = mid
                    break
                }
            }

            if (left < 0 || left > max) {
                return -1
            }

            return left
        }
    }
}
