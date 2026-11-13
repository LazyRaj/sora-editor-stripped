/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.lang.styling

import io.github.rosemoe.sora.lang.styling.color.ConstColor
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor
import io.github.rosemoe.sora.lang.styling.line.LineAnchorStyle
import io.github.rosemoe.sora.lang.styling.span.SpanColorResolver
import io.github.rosemoe.sora.lang.styling.span.SpanExt
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs
import io.github.rosemoe.sora.lang.styling.span.SpanExternalRenderer
import io.github.rosemoe.sora.lang.styling.span.SpanInteractionInfo
import io.github.rosemoe.sora.lang.styling.span.internal.NoExtSpanImpl
import io.github.rosemoe.sora.lang.styling.span.internal.SpanImpl
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StylingTest {

    @Test
    fun testTextStyleOperations() {
        val style1 = TextStyle.makeStyle(10)
        assertEquals(10, TextStyle.getForegroundColorId(style1))
        assertEquals(0, TextStyle.getBackgroundColorId(style1))
        assertFalse(TextStyle.isBold(style1))
        assertFalse(TextStyle.isItalics(style1))
        assertFalse(TextStyle.isStrikeThrough(style1))
        assertFalse(TextStyle.isNoCompletion(style1))

        val style2 = TextStyle.makeStyle(5, true)
        assertEquals(5, TextStyle.getForegroundColorId(style2))
        assertTrue(TextStyle.isNoCompletion(style2))

        val style3 = TextStyle.makeStyle(1, 2, bold = true, italic = true, strikeThrough = true, noCompletion = true)
        assertEquals(1, TextStyle.getForegroundColorId(style3))
        assertEquals(2, TextStyle.getBackgroundColorId(style3))
        assertTrue(TextStyle.isBold(style3))
        assertTrue(TextStyle.isItalics(style3))
        assertTrue(TextStyle.isStrikeThrough(style3))
        assertTrue(TextStyle.isNoCompletion(style3))

        val styleBits = TextStyle.getStyleBits(style3)
        assertEquals(TextStyle.BOLD_BIT or TextStyle.ITALICS_BIT or TextStyle.STRIKETHROUGH_BIT, styleBits)

        try {
            TextStyle.checkColorId(-1)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // Success
        }

        try {
            TextStyle.checkColorId(1 shl TextStyle.COLOR_ID_BIT_COUNT)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // Success
        }
    }

    @Test
    fun testSpanExtAttrsCheckType() {
        assertTrue(SpanExtAttrs.checkType(SpanExtAttrs.EXT_COLOR_RESOLVER, null))
        val resolver = object : SpanColorResolver {
            override fun getForegroundColor(span: Span): ResolvableColor? = ConstColor(0)
            override fun getBackgroundColor(span: Span): ResolvableColor? = null
        }
        assertTrue(SpanExtAttrs.checkType(SpanExtAttrs.EXT_COLOR_RESOLVER, resolver))
        assertFalse(SpanExtAttrs.checkType(SpanExtAttrs.EXT_EXTERNAL_RENDERER, resolver))

        val color = ConstColor(0xff00ff)
        assertTrue(SpanExtAttrs.checkType(SpanExtAttrs.EXT_UNDERLINE_COLOR, color))
    }

    @Test
    fun testSpanImplAndSpanFactory() {
        val span = SpanFactory.obtain(5, TextStyle.makeStyle(1, 2, bold = true, italic = false, strikeThrough = false))
        assertTrue(span is SpanImpl)
        assertEquals(5, span.column)
        assertEquals(1, span.foregroundColorId)
        assertEquals(2, span.backgroundColorId)
        assertTrue(TextStyle.isBold(span.style))

        span.shiftColumnBy(3)
        assertEquals(8, span.column)

        span.setUnderlineColor(0xff0000)
        assertNotNull(span.underlineColor)
        span.setUnderlineColor(0)
        assertNull(span.underlineColor)

        span.extra = "testExtra"
        assertEquals("testExtra", span.extra)

        val resolver = object : SpanColorResolver {
            override fun getForegroundColor(span: Span): ResolvableColor? = ConstColor(42)
            override fun getBackgroundColor(span: Span): ResolvableColor? = null
        }
        span.setSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER, resolver)
        assertTrue(span.hasSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER))
        assertEquals(resolver, span.getSpanExt<SpanColorResolver>(SpanExtAttrs.EXT_COLOR_RESOLVER))

        val copy = span.copy()
        assertEquals(span.column, copy.column)
        assertEquals(span.style, copy.style)
        assertTrue(copy.hasSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER))

        span.removeAllSpanExt()
        assertFalse(span.hasSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER))

        span.reset()
        assertEquals(0, span.column)
        assertEquals(0L, span.style)
        assertNull(span.extra)

        assertTrue(span.recycle())
    }

    @Test
    fun testNoExtSpanImpl() {
        val span = SpanFactory.obtainNoExt(10, 42L)
        assertTrue(span is NoExtSpanImpl)
        assertEquals(10, span.column)
        assertEquals(42L, span.style)
        assertFalse(span.hasSpanExt(0))
        assertNull(span.getSpanExt<SpanExt>(0))
        assertNull(span.underlineColor)

        try {
            span.setUnderlineColor(0xff0000)
            fail("Expected UnsupportedOperationException")
        } catch (expected: UnsupportedOperationException) {
            // Success
        }

        try {
            span.setSpanExt(0, ConstColor(0))
            fail("Expected UnsupportedOperationException")
        } catch (expected: UnsupportedOperationException) {
            // Success
        }

        span.extra = "extra"
        assertEquals("extra", span.extra)

        val copy = span.copy()
        assertEquals(span, copy)
        assertTrue(span.recycle())
    }

    @Test
    fun testEmptyReader() {
        val reader = EmptyReader.getInstance()
        assertEquals(1, reader.spanCount)
        val span = reader.getSpanAt(0)
        assertEquals(0, span.column)
        assertEquals(EditorColorScheme.TEXT_NORMAL.toLong(), span.style)

        val list = reader.getSpansOnLine(0)
        assertEquals(1, list.size)
        assertEquals(span, list[0])
    }

    @Test
    fun testExtraStylesProvider() {
        val provider = ExtraStylesProvider { line, styles ->
            styles.add(object : LineAnchorStyle(line) {})
        }
        val styles = mutableListOf<LineAnchorStyle>()
        provider.getExtraStyles(3, styles)
        assertEquals(1, styles.size)
        assertEquals(3, styles[0].line)
    }

    @Test
    fun testSpanFactoryRecycleAll() {
        val spans = listOf(
            SpanFactory.obtain(0, 1L),
            SpanFactory.obtainNoExt(5, 2L)
        )
        SpanFactory.recycleAll(spans)
        // Spans recycled
    }
}
