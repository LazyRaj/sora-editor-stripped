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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GraphicsTest {

    @Test
    fun testRectUtils() {
        val rect = RectF(10f, 20f, 50f, 60f)

        // Inside
        assertTrue(RectUtils.contains(rect, 30f, 40f, 0f))
        // Outside bounds but inside extraXSpace
        assertFalse(RectUtils.contains(rect, 5f, 40f, 0f))
        assertTrue(RectUtils.contains(rect, 5f, 40f, 10f))
        // Y outside
        assertFalse(RectUtils.contains(rect, 30f, 15f, 10f))

        // almostContains
        assertTrue(RectUtils.almostContains(rect, 30f, 40f, 0f))
        assertFalse(RectUtils.almostContains(rect, 5f, 15f, 0f))
        assertTrue(RectUtils.almostContains(rect, 5f, 15f, 10f))
        assertFalse(RectUtils.almostContains(rect, 5f, 5f, 10f))
    }

    @Test
    fun testBubbleHelper() {
        val path = Path()
        val bounds = RectF(10f, 10f, 100f, 60f)
        BubbleHelper.buildBubblePath(path, bounds)
        assertFalse(path.isEmpty)
    }

    @Test
    fun testBufferedDrawPoints() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint()

        val buffered = BufferedDrawPoints()
        buffered.setOffsets(5f, 10f)

        // Drawing points triggering resize
        for (i in 0 until 150) {
            buffered.drawPoint(i.toFloat(), i.toFloat())
        }

        buffered.commitPoints(canvas, paint)
        // Calling commit again should do nothing (pointCount is 0)
        buffered.commitPoints(canvas, paint)
    }

    @Test
    fun testGraphicCharacter() {
        // ASCII combining symbols
        assertTrue(GraphicCharacter.isASCIICombiningSymbol('.'.code))
        assertTrue(GraphicCharacter.isASCIICombiningSymbol('/'.code))
        assertTrue(GraphicCharacter.isASCIICombiningSymbol('!'.code))
        assertTrue(GraphicCharacter.isASCIICombiningSymbol('='.code))
        assertTrue(GraphicCharacter.isASCIICombiningSymbol('<'.code))
        assertTrue(GraphicCharacter.isASCIICombiningSymbol('>'.code))
        assertTrue(GraphicCharacter.isASCIICombiningSymbol('-'.code))
        assertFalse(GraphicCharacter.isASCIICombiningSymbol('a'.code))
        assertFalse(GraphicCharacter.isASCIICombiningSymbol('1'.code))

        // Combining characters
        assertTrue(GraphicCharacter.isCombiningCharacter(0x200D)) // ZWJ
        assertTrue(GraphicCharacter.isCombiningCharacter(0x200C)) // ZWNJ
        assertTrue(GraphicCharacter.isCombiningCharacter('.'.code))

        // Emoji parts
        assertTrue(GraphicCharacter.couldBeEmojiPart(0x200D))
        assertFalse(GraphicCharacter.couldBeEmojiPart('A'.code))
    }

    @Test
    fun testGraphemeBoundsBreaker() {
        val advances = floatArrayOf(10f, 0f, 15f, 20f, 0f, 0f, 10f)
        // Total indices 0..6
        // At width 5: cannot take index 0 (width 10)
        val breakAt5 = GraphemeBoundsBreaker.findGraphemeBreakPoint(advances, advances.size, 5, 0)
        assertEquals(0, breakAt5)

        // At width 10: takes index 0 (advance 10) and skips index 1 (advance 0)
        val breakAt10 = GraphemeBoundsBreaker.findGraphemeBreakPoint(advances, advances.size, 10, 0)
        assertEquals(2, breakAt10)

        // At width 25: index 0 (10) + index 1 (0) + index 2 (15) = 25
        val breakAt25 = GraphemeBoundsBreaker.findGraphemeBreakPoint(advances, advances.size, 25, 0)
        assertEquals(3, breakAt25)
    }

    @Test
    fun testSingleCharacterWidths() {
        val widths = SingleCharacterWidths(4)
        val paint = Paint()
        paint.textSize = 20f

        val charWidth = widths.measureChar('A', paint)
        assertTrue(charWidth > 0f)

        val tabWidth = widths.measureChar('\t', paint)
        val spaceWidth = widths.measureChar(' ', paint)
        assertEquals(spaceWidth * 4f, tabWidth, 0.01f)

        val cpWidth = widths.measureCodePoint('B'.code, paint)
        assertTrue(cpWidth > 0f)

        widths.clearCache()
        val textWidth = widths.measureText("Hello", paint)
        assertTrue(textWidth > 0f)
    }

    @Test
    fun testPaint() {
        val paint = Paint()
        paint.textSize = 24f
        assertTrue(paint.spaceWidth > 0f)

        paint.setTextSizeWrapped(30f)
        assertTrue(paint.textSize == 30f)
    }
}
