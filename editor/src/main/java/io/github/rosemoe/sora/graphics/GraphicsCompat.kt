/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.graphics

import android.graphics.Canvas
import android.text.GetChars
import android.text.TextUtils
import io.github.rosemoe.sora.util.TemporaryCharBuffer
import io.github.rosemoe.sora.util.use

object GraphicsCompat {

    fun drawTextRun(
        canvas: Canvas,
        text: GetChars,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        x: Float,
        y: Float,
        isRtl: Boolean,
        paint: android.graphics.Paint
    ) {
        TemporaryCharBuffer.use(contextCount) { buffer ->
            TextUtils.getChars(text, contextIndex, contextIndex + contextCount, buffer, 0)
            canvas.drawTextRun(buffer, index - contextIndex, count, 0, contextCount, x, y, isRtl, paint)
        }
    }

    fun getRunAdvance(
        paint: Paint,
        text: GetChars,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        offset: Int
    ): Float {
        return paint.getRunAdvance(text, start, end, contextStart, contextEnd, isRtl, offset)
    }
}

/**
 * Extension on [Canvas] to draw a text run with [GetChars] source text.
 */
fun Canvas.drawTextRun(
    text: GetChars,
    index: Int,
    count: Int,
    contextIndex: Int,
    contextCount: Int,
    x: Float,
    y: Float,
    isRtl: Boolean,
    paint: android.graphics.Paint
) = GraphicsCompat.drawTextRun(this, text, index, count, contextIndex, contextCount, x, y, isRtl, paint)

/**
 * Extension on [Paint] to get run advance with [GetChars] source text.
 */
fun Paint.getRunAdvanceCompat(
    text: GetChars,
    start: Int,
    end: Int,
    contextStart: Int,
    contextEnd: Int,
    isRtl: Boolean,
    offset: Int
): Float = GraphicsCompat.getRunAdvance(this, text, start, end, contextStart, contextEnd, isRtl, offset)

