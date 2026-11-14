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

import android.graphics.Canvas
import android.graphics.RectF

/**
 * Class for custom handle style
 *
 * @author Rosemoe
 */
interface SelectionHandleStyle {

    /**
     * Draw a handle to the given canvas and return descriptor of handle.
     *
     * @param canvas     Canvas to draw
     * @param handleType Type of handle being drawn at this position.
     * @param x          The x of text position on canvas
     * @param y          The y of row bottom position on canvas
     * @param rowHeight  The height of a single row
     * @param color      The color of handle configured in [io.github.rosemoe.sora.widget.schemes.EditorColorScheme]
     * @param descriptor The descriptor that should be adjusted
     */
    fun draw(
        canvas: Canvas,
        handleType: Int,
        x: Float,
        y: Float,
        rowHeight: Int,
        color: Int,
        descriptor: HandleDescriptor
    )

    fun setAlpha(alpha: Int)

    fun setScale(factor: Float)

    /**
     * The descriptor of a drawn handle on canvas
     */
    class HandleDescriptor(
        /**
         * The position of handle
         */
        val position: RectF = RectF(),

        /**
         * The alignment of the handle (of the x coordinate)
         */
        var alignment: Int = ALIGN_CENTER
    ) {
        operator fun component1(): RectF = position
        operator fun component2(): Int = alignment

        fun set(left: Float, top: Float, right: Float, bottom: Float, alignment: Int) {
            this.alignment = alignment
            position.set(left, top, right, bottom)
        }

        fun setEmpty() {
            position.setEmpty()
            alignment = ALIGN_CENTER
        }
    }

    companion object {
        const val HANDLE_TYPE_UNDEFINED = -1
        const val HANDLE_TYPE_INSERT = 0
        const val HANDLE_TYPE_LEFT = 1
        const val HANDLE_TYPE_RIGHT = 2

        const val ALIGN_CENTER = 0
        const val ALIGN_LEFT = 1
        const val ALIGN_RIGHT = 2
    }
}
