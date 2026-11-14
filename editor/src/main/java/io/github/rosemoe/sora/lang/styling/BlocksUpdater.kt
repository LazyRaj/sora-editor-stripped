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

/**
 * Update block line positions on edit
 */
object BlocksUpdater {

    /**
     * Update blocks
     *
     * @param blocks   Block lines to update
     * @param restrict Min line to update
     * @param delta    Delta for line index
     */
    fun update(blocks: MutableList<CodeBlock>, restrict: Int, delta: Int) {
        if (delta == 0) {
            return
        }
        val itr = blocks.iterator()
        while (itr.hasNext()) {
            val block = itr.next()
            if (block.startLine >= restrict) {
                block.startLine += delta
            }
            if (block.endLine >= restrict) {
                block.endLine += delta
            }
            if (block.startLine >= block.endLine) {
                itr.remove()
            }
        }
    }
}

fun MutableList<CodeBlock>.updateOffsets(restrict: Int, delta: Int) {
    BlocksUpdater.update(this, restrict, delta)
}
