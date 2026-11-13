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
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.TypedValue
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle

open class HandleStyleDrop(context: Context) : SelectionHandleStyle {

    private val drawable: Drawable = context.getDrawable(R.drawable.ic_sora_handle_drop)!!.mutate()
    private val width: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, context.resources.displayMetrics).toInt()
    private val height: Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, context.resources.displayMetrics).toInt()
    private var lastColor = 0

    private var alpha = 255
    private var scaleFactor = 1.0f

    override fun draw(
        canvas: Canvas,
        handleType: Int,
        x: Float,
        y: Float,
        rowHeight: Int,
        color: Int,
        descriptor: SelectionHandleStyle.HandleDescriptor
    ) {
        if (lastColor != color) {
            lastColor = color
            drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        val left = (x - (width * scaleFactor) / 2).toInt()
        val top = y.toInt()
        val right = (x + (width * scaleFactor) / 2).toInt()
        val bottom = (y + height * scaleFactor).toInt()
        drawable.setBounds(left, top, right, bottom)
        drawable.alpha = alpha
        drawable.draw(canvas)
        descriptor.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), SelectionHandleStyle.ALIGN_CENTER)
    }

    override fun setAlpha(alpha: Int) {
        this.alpha = alpha
    }

    override fun setScale(factor: Float) {
        this.scaleFactor = factor
    }
}
