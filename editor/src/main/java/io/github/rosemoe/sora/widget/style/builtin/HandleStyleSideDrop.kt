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
package io.github.rosemoe.sora.widget.style.builtin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle

open class HandleStyleSideDrop(context: Context) : HandleStyleDrop(context) {

    private val size: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22f, context.resources.displayMetrics).toInt()
    private val paint: Paint = Paint().apply {
        isAntiAlias = true
    }

    override fun draw(
        canvas: Canvas,
        handleType: Int,
        x: Float,
        y: Float,
        rowHeight: Int,
        color: Int,
        descriptor: SelectionHandleStyle.HandleDescriptor
    ) {
        val radius = size / 2f
        paint.color = color
        if (handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT || handleType == SelectionHandleStyle.HANDLE_TYPE_UNDEFINED) {
            super.draw(canvas, handleType, x, y, rowHeight, color, descriptor)
        } else {
            val isLeft = handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT
            val cx = if (isLeft) x - radius else x + radius
            canvas.drawCircle(cx, y + radius, radius, paint)
            canvas.drawRect(if (isLeft) cx else cx - radius, y, if (isLeft) cx + radius else cx, y + radius, paint)
            descriptor.set(
                cx - radius,
                y,
                cx + radius,
                y + 2 * radius,
                if (isLeft) SelectionHandleStyle.ALIGN_LEFT else SelectionHandleStyle.ALIGN_RIGHT
            )
        }
    }
}
