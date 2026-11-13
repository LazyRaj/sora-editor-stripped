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
/*
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.widget.layout

import android.view.View
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.graphics.SingleCharacterWidths
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.util.MutableInt
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object ViewMeasureHelper {

    /**
     * Get desired view size for the given arguments
     */
    @JvmStatic
    fun getDesiredSize(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
        gutterSize: Float,
        rowHeight: Float,
        wordwrap: Boolean,
        tabSize: Int,
        text: Content,
        paint: Paint
    ): Long {
        var wMeasureSpec = widthMeasureSpec
        var hMeasureSpec = heightMeasureSpec
        val widthMode = View.MeasureSpec.getMode(wMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(hMeasureSpec)
        val maxSize = 0x3FFFFFFF
        val maxWidth = if (widthMode == View.MeasureSpec.UNSPECIFIED) maxSize else View.MeasureSpec.getSize(wMeasureSpec)
        val maxHeight = if (heightMode == View.MeasureSpec.UNSPECIFIED) maxSize else View.MeasureSpec.getSize(hMeasureSpec)
        val measurer = SingleCharacterWidths(tabSize)

        if (wordwrap) {
            if (widthMode != View.MeasureSpec.EXACTLY) {
                val lines = if (heightMode != View.MeasureSpec.EXACTLY) IntArray(text.lineCount) else null
                val lineMaxSize = MutableInt(0)
                text.runReadActionsOnLines(0, text.lineCount - 1, Content.ContentLineConsumer { index, line, _ ->
                    val measured = ceil(measurer.measureText(line, 0, line.length, paint).toDouble()).toInt()
                    if (measured > lineMaxSize.value) {
                        lineMaxSize.value = measured
                    }
                    if (lines != null) {
                        lines[index] = measured
                    }
                })
                val width = min(maxWidth.toDouble(), (lineMaxSize.value + gutterSize).toDouble()).toInt()
                wMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
                if (lines != null) {
                    val rowCount = MutableInt(0)
                    val availableSize = (width - gutterSize).toInt()
                    if (availableSize <= 0) {
                        rowCount.value = text.length
                    } else {
                        for (element in lines) {
                            rowCount.value += max(1.0, ceil(1.0 * element / availableSize)).toInt()
                        }
                    }
                    val height = min((rowHeight * rowCount.value).toInt(), maxHeight)
                    hMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                }
            } else {
                if (heightMode != View.MeasureSpec.EXACTLY) {
                    val rowCount = MutableInt(0)
                    val availableSize = (maxWidth - gutterSize).toInt()
                    if (availableSize <= 0) {
                        rowCount.value = text.length
                    } else {
                        text.runReadActionsOnLines(0, text.lineCount - 1, Content.ContentLineConsumer { _, line, _ ->
                            val measured = ceil(measurer.measureText(line, 0, line.length, paint).toDouble()).toInt()
                            rowCount.value += max(1.0, ceil(1.0 * measured / availableSize)).toInt()
                        })
                    }
                    val height = min((rowHeight * rowCount.value).toInt(), maxHeight)
                    hMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                }
            }
        } else {
            if (widthMode != View.MeasureSpec.EXACTLY) {
                val lineMaxSize = MutableInt(0)
                text.runReadActionsOnLines(0, text.lineCount - 1, Content.ContentLineConsumer { _, line, _ ->
                    val measured = ceil(measurer.measureText(line, 0, line.length, paint).toDouble()).toInt()
                    if (measured > lineMaxSize.value) {
                        lineMaxSize.value = measured
                    }
                })
                val width = min((lineMaxSize.value + gutterSize).toInt(), maxWidth)
                wMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            }
            if (heightMode != View.MeasureSpec.EXACTLY) {
                val height = min(maxHeight, (rowHeight * text.lineCount).toInt())
                hMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            }
        }
        return IntPair.pack(wMeasureSpec, hMeasureSpec)
    }
}
