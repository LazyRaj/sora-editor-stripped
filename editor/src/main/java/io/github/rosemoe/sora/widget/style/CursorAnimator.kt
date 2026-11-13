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
package io.github.rosemoe.sora.widget.style

/**
 * Interface for provide various cursor animations
 *
 * @author Rosemoe, Dmitry Rubtsov
 */
interface CursorAnimator {

    /**
     * Mark the current cursor position as animation start position
     */
    fun markStartPos()

    /**
     * Mark the current cursor position as animation end position
     */
    fun markEndPos()

    /**
     * Start animation
     */
    fun start()

    /**
     * Cancel animation
     */
    fun cancel()

    /**
     * Check whether animation is in process
     */
    fun isRunning(): Boolean

    /**
     * The current x position of cursor in view offset
     */
    fun animatedX(): Float

    /**
     * The current y position of cursor in view offset
     */
    fun animatedY(): Float

    /**
     * Height of current line background
     */
    fun animatedLineHeight(): Float

    /**
     * Bottom Y position in view offset of current line background
     */
    fun animatedLineBottom(): Float
}
