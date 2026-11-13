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

    @Test
    fun testMappedSpansAndBuilder() {
        val emptyBuilder = MappedSpans.Builder()
        emptyBuilder.addNormalIfNull()
        val emptySpans = emptyBuilder.build()
        assertEquals(1, emptySpans.lineCount)

        val builder = MappedSpans.Builder(10)
        builder.add(0, SpanFactory.obtainNoExt(0, 100L))
        builder.addIfNeeded(0, 5, 200L)
        builder.addIfNeeded(0, 8, 200L) // Redundant style, should be skipped
        builder.determine(2)

        val mappedSpans = builder.build()
        assertTrue(mappedSpans.supportsModify())
        assertEquals(3, mappedSpans.lineCount)

        val reader = mappedSpans.read()
        reader.moveToLine(0)
        assertEquals(2, reader.spanCount)
        assertEquals(0, reader.getSpanAt(0).column)
        assertEquals(5, reader.getSpanAt(1).column)
        assertEquals(2, reader.getSpansOnLine(0).size)

        reader.moveToLine(-1)

        val modifier = mappedSpans.modify()
        modifier.addLineAt(1, listOf(SpanFactory.obtainNoExt(0, 300L)))
        assertEquals(4, mappedSpans.lineCount)

        modifier.deleteLineAt(1)
        assertEquals(3, mappedSpans.lineCount)

        modifier.setSpansOnLine(5, listOf(SpanFactory.obtainNoExt(0, 400L)))
        assertTrue(mappedSpans.lineCount >= 6)

        // Test adjust on single/multi-line insert and delete
        mappedSpans.adjustOnInsert(
            io.github.rosemoe.sora.text.CharPosition(0, 2, 2),
            io.github.rosemoe.sora.text.CharPosition(0, 4, 4)
        )
        mappedSpans.adjustOnDelete(
            io.github.rosemoe.sora.text.CharPosition(0, 2, 2),
            io.github.rosemoe.sora.text.CharPosition(0, 4, 4)
        )
        mappedSpans.adjustOnInsert(
            io.github.rosemoe.sora.text.CharPosition(0, 2, 2),
            io.github.rosemoe.sora.text.CharPosition(1, 4, 10)
        )
        mappedSpans.adjustOnDelete(
            io.github.rosemoe.sora.text.CharPosition(0, 2, 2),
            io.github.rosemoe.sora.text.CharPosition(1, 4, 10)
        )
    }

    @Test
    fun testCodeBlockAndBinarySearch() {
        val block1 = CodeBlock(startLine = 1, startColumn = 0, endLine = 5, endColumn = 4, toBottomOfEndLine = false)
        val block2 = CodeBlock(startLine = 6, startColumn = 2, endLine = 10, endColumn = 0, toBottomOfEndLine = true)

        assertEquals(1, block1.startLine)
        assertEquals(5, block1.endLine)
        assertFalse(block1.toBottomOfEndLine)
        assertTrue(block2.toBottomOfEndLine)

        val blocks = listOf(block1, block2)
        assertEquals(0, CodeBlock.binarySearchEndBlock(3, blocks))
        assertEquals(0, CodeBlock.binarySearchEndBlock(5, blocks))
        assertEquals(1, CodeBlock.binarySearchEndBlock(6, blocks))
        assertEquals(1, CodeBlock.binarySearchEndBlock(10, blocks))
        assertEquals(-1, CodeBlock.binarySearchEndBlock(15, blocks))

        val copy1 = CodeBlock(startLine = 1, startColumn = 0, endLine = 5, endColumn = 4, toBottomOfEndLine = false)
        assertEquals(block1, copy1)
        assertEquals(block1.hashCode(), copy1.hashCode())
        assertTrue(block1.toString().contains("startLine=1"))

        assertTrue(CodeBlock.COMPARATOR_START.compare(block1, block2) < 0)
        assertTrue(CodeBlock.COMPARATOR_END.compare(block1, block2) < 0)

        block1.clear()
        assertEquals(0, block1.startLine)
        assertEquals(0, block1.endLine)
    }

    @Test
    fun testBlocksUpdater() {
        val blocks = mutableListOf(
            CodeBlock(1, 0, 5, 0),
            CodeBlock(6, 0, 10, 0)
        )

        BlocksUpdater.update(blocks, restrict = 3, delta = 2)
        assertEquals(1, blocks[0].startLine)
        assertEquals(7, blocks[0].endLine)
        assertEquals(8, blocks[1].startLine)
        assertEquals(12, blocks[1].endLine)

        // Test deletion causing invalid block
        val blocks2 = mutableListOf(CodeBlock(5, 0, 6, 0))
        BlocksUpdater.update(blocks2, restrict = 0, delta = 0) // No-op
        assertEquals(1, blocks2.size)

        BlocksUpdater.update(blocks2, restrict = 6, delta = -2)
        // startLine=5, endLine=4 -> removed
        assertEquals(0, blocks2.size)
    }

    @Test
    fun testStylesAndStylesUtils() {
        val styles = Styles()
        assertNotNull(styles.blocks)
        styles.addCodeBlock(CodeBlock(0, 0, 5, 0))
        assertEquals(1, styles.blocks?.size)

        styles.suppressSwitch = 50
        assertEquals(50, styles.getSuppressSwitch())
        styles.setSuppressSwitch(100)
        assertEquals(100, styles.suppressSwitch)

        styles.indentCountMode = true
        assertTrue(styles.isIndentCountMode())
        styles.setIndentCountMode(false)
        assertFalse(styles.indentCountMode)

        val anchorStyle = object : LineAnchorStyle(2) {}
        styles.addLineStyle(anchorStyle)
        assertNotNull(styles.lineStyles)
        assertEquals(1, styles.lineStyles?.size)

        styles.eraseLineStyle(2, anchorStyle.javaClass)
        styles.eraseAllLineStyles()
        assertEquals(0, styles.lineStyles?.size)

        val builder = MappedSpans.Builder()
        builder.add(0, SpanFactory.obtainNoExt(0, TextStyle.makeStyle(1, noCompletion = true)))
        styles.spans = builder.build()
        assertEquals(styles.spans, styles.getSpans())

        styles.finishBuilding()

        val pos0 = io.github.rosemoe.sora.text.CharPosition(0, 0, 0)
        assertTrue(StylesUtils.checkNoCompletion(styles, pos0))
        val span = StylesUtils.getSpanForPosition(styles, pos0)
        assertNotNull(span)
        assertEquals(0, span?.column)
        assertNull(StylesUtils.getFollowingSpanForPosition(styles, pos0))
    }

    @Test
    fun testSpanMerger() {
        val lower = listOf(
            SpanFactory.obtainNoExt(0, TextStyle.makeStyle(1)),
            SpanFactory.obtainNoExt(5, TextStyle.makeStyle(2))
        )
        val upper = listOf(
            SpanFactory.obtainNoExt(3, TextStyle.makeStyle(10))
        )

        val merged = SpanMerger.merge(lower, upper)
        assertEquals(3, merged.size)
        assertEquals(0, merged[0].column)
        assertEquals(3, merged[1].column)
        assertEquals(10, merged[1].foregroundColorId)
        assertEquals(5, merged[2].column)
        assertEquals(10, merged[2].foregroundColorId)

        val emptyUpper = emptyList<Span>()
        val mergedEmpty = SpanMerger.merge(lower, emptyUpper)
        assertEquals(2, mergedEmpty.size)
    }
}
