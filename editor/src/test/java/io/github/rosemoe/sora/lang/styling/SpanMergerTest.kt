/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

import io.github.rosemoe.sora.lang.styling.span.SpanColorResolver
import io.github.rosemoe.sora.lang.styling.span.SpanConstColorResolver
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs
import io.github.rosemoe.sora.util.RendererUtils
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SpanMergerTest {

    @Test
    fun mergesColorsAndFlagsWithoutChangingInputs() {
        val base = SpanFactory.obtainNoExt(0, TextStyle.makeStyle(5, 6, true, false, false))
        val overlay = SpanFactory.obtainNoExt(2, TextStyle.makeStyle(0, 7, false, true, false))
        val lower = listOf(base, SpanFactory.obtainNoExt(8, 9))
        val upper = listOf(overlay, SpanFactory.obtainNoExt(5, 0))
        val merged = SpanMerger.merge(lower, upper)

        assertColumns(merged, 0, 2, 5, 8)
        assertSame(base, merged[0])
        assertSame(lower[1], merged[3])
        assertEquals(5, merged[1].foregroundColorId)
        assertEquals(7, merged[1].backgroundColorId)
        assertTrue(TextStyle.isBold(merged[1].style))
        assertTrue(TextStyle.isItalics(merged[1].style))
        assertEquals(base.style, merged[2].style)
        assertEquals(0, base.column)
        assertEquals(6, base.backgroundColorId)
        assertEquals(2, overlay.column)
        assertEquals(0, overlay.foregroundColorId)
        assertThrows(UnsupportedOperationException::class.java) { (merged as MutableList).clear() }
        assertThrows(UnsupportedOperationException::class.java) { (merged as MutableList)[0] = overlay }
        assertThrows(UnsupportedOperationException::class.java) { merged[1].style = 0 }
        assertThrows(UnsupportedOperationException::class.java) { merged[1].recycle() }
    }

    @Test
    fun resolvesEachColorChannelWithUpperPrecedence() {
        val lower = SpanFactory.obtain(0, TextStyle.makeStyle(5, 6, true, false, false))
        lower.setSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER, SpanConstColorResolver(0xff112233.toInt(), 0xff445566.toInt()))
        lower.setUnderlineColor(0xff010203.toInt())
        val extra = Any()
        lower.extra = extra
        val upper = SpanFactory.obtain(1, 0)
        upper.setSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER, SpanConstColorResolver(0, 0x66ef3566))
        val merged = SpanMerger.merge(listOf(lower), listOf(upper))[1]
        val scheme = EditorColorScheme()

        assertEquals(0xff112233.toInt(), RendererUtils.getForegroundColor(merged, scheme))
        assertEquals(0x66ef3566, RendererUtils.getBackgroundColor(merged, scheme))
        assertSame(lower.underlineColor, merged.underlineColor)
        assertSame(extra, merged.extra)
        assertSame(extra, merged.copy().extra)

        val numeric = SpanFactory.obtainNoExt(0, TextStyle.makeStyle(7, 8, false, false, false))
        scheme.setColor(7, 0xff778899.toInt())
        scheme.setColor(8, 0xffaabbcc.toInt())
        val replaced = SpanMerger.merge(listOf(lower), listOf(numeric))[0]
        assertEquals(0xff778899.toInt(), RendererUtils.getForegroundColor(replaced, scheme))
        assertEquals(0xffaabbcc.toInt(), RendererUtils.getBackgroundColor(replaced, scheme))
        assertNull(
            replaced.getSpanExt<SpanColorResolver>(SpanExtAttrs.EXT_COLOR_RESOLVER)?.getForegroundColor(replaced)
        )
    }

    @Test
    fun reusesLargeUnaffectedRegionsAndSupportsIterationAndSubLists() {
        val lower = ArrayList<Span>()
        for (i in 0 until 1000) {
            lower.add(SpanFactory.obtainNoExt(i * 2, 1))
        }
        val merged = SpanMerger.merge(
            java.util.List.copyOf(lower),
            listOf(
                SpanFactory.obtainNoExt(501, TextStyle.BACKGROUND_BITS),
                SpanFactory.obtainNoExt(503, 0)
            )
        )
        assertEquals(1002, merged.size)
        assertSame(lower[0], merged[0])
        assertSame(lower[250], merged[250])
        assertSame(lower[252], merged[254])
        assertSame(lower[999], merged[1001])
        var previous = -1
        for (span in merged) {
            assertTrue(span.column > previous)
            previous = span.column
        }
        assertEquals(3, merged.subList(250, 253).size)
        assertThrows(IndexOutOfBoundsException::class.java) { merged[-1] }
        assertThrows(IndexOutOfBoundsException::class.java) { merged[merged.size] }
        assertSame(lower[0], SpanMerger.merge(lower, emptyList())[0])
    }

    @Test
    fun acceptsRepeatedColumnsAndUsesLastSpanInEachLayer() {
        val lower = listOf(
            SpanFactory.obtainNoExt(0, TextStyle.makeStyle(5, 0, true, false, false)),
            SpanFactory.obtainNoExt(0, TextStyle.makeStyle(6, 0, false, true, false)),
            SpanFactory.obtainNoExt(2, TextStyle.makeStyle(7, 0, false, false, true)),
            SpanFactory.obtainNoExt(2, 8),
            SpanFactory.obtainNoExt(6, 9),
            SpanFactory.obtainNoExt(6, 10)
        )
        val upper = listOf(
            SpanFactory.obtainNoExt(0, TextStyle.makeStyle(0, 1, true, false, false)),
            SpanFactory.obtainNoExt(0, 0),
            SpanFactory.obtainNoExt(2, TextStyle.makeStyle(11, 2, true, false, false)),
            SpanFactory.obtainNoExt(2, 0),
            SpanFactory.obtainNoExt(2, TextStyle.makeStyle(0, 3, false, true, false)),
            SpanFactory.obtainNoExt(6, TextStyle.makeStyle(0, 4, false, false, true)),
            SpanFactory.obtainNoExt(6, 0)
        )
        val merged = SpanMerger.merge(lower, upper)

        for (column in 0 until 8) {
            val expected = if (column < 2) lower[1].style
            else if (column < 6) TextStyle.makeStyle(8, 3, false, true, false) else 10L
            assertEquals(expected, styleAt(merged, column))
        }
        var previous = -1
        for (span in merged) {
            assertTrue(span.column >= previous)
            previous = span.column
        }
        assertSame(lower[5], merged[merged.size - 1])
        val lowerOnly = SpanMerger.merge(lower, emptyList())
        val upperOnly = SpanMerger.merge(emptyList(), upper)
        for (i in lower.indices) {
            assertSame(lower[i], lowerOnly[i])
        }
        for (i in upper.indices) {
            assertSame(upper[i], upperOnly[i])
        }
    }

    @Test
    fun rejectsNegativeAndDescendingColumns() {
        val valid = listOf(SpanFactory.obtainNoExt(0, 1))
        val negative = listOf(SpanFactory.obtainNoExt(-1, 1))
        val descending = listOf(
            SpanFactory.obtainNoExt(0, 1),
            SpanFactory.obtainNoExt(2, 1),
            SpanFactory.obtainNoExt(1, 1)
        )
        assertThrows(IllegalArgumentException::class.java) { SpanMerger.merge(negative, valid) }
        assertThrows(IllegalArgumentException::class.java) { SpanMerger.merge(valid, negative) }
        assertThrows(IllegalArgumentException::class.java) { SpanMerger.merge(descending, valid) }
        assertThrows(IllegalArgumentException::class.java) { SpanMerger.merge(valid, descending) }
    }

    @Test
    fun matchesReferenceCompositionForRandomBoundaries() {
        val random = Random(883)
        for (trial in 0 until 100) {
            val lower = ArrayList<Span>()
            val upper = ArrayList<Span>()
            lower.add(SpanFactory.obtainNoExt(0, randomStyle(random)))
            for (column in 0 until 100) {
                if (random.nextInt(5) == 0) {
                    val count = 1 + random.nextInt(3)
                    for (i in 0 until count) {
                        lower.add(SpanFactory.obtainNoExt(column, randomStyle(random)))
                    }
                }
                if (random.nextInt(5) == 0) {
                    val count = 1 + random.nextInt(3)
                    for (i in 0 until count) {
                        upper.add(SpanFactory.obtainNoExt(column, randomStyle(random)))
                    }
                }
            }
            val merged = SpanMerger.merge(lower, upper)
            for (column in 0 until 110) {
                val a = styleAt(lower, column)
                val b = styleAt(upper, column)
                val mask = (if (TextStyle.getForegroundColorId(b) != 0) TextStyle.FOREGROUND_BITS else 0L) or
                        (if (TextStyle.getBackgroundColorId(b) != 0) TextStyle.BACKGROUND_BITS else 0L)
                assertEquals((a and mask.inv()) or b, styleAt(merged, column))
            }
        }
    }

    private fun randomStyle(random: Random): Long {
        return TextStyle.makeStyle(
            random.nextInt(3),
            random.nextInt(3),
            random.nextBoolean(),
            random.nextBoolean(),
            random.nextBoolean(),
            random.nextBoolean()
        )
    }

    private fun styleAt(spans: List<Span>, column: Int): Long {
        var style = 0L
        for (span in spans) {
            if (span.column > column) break
            style = span.style
        }
        return style
    }

    private fun assertColumns(spans: List<Span>, vararg columns: Int) {
        assertEquals(columns.size, spans.size)
        for (i in columns.indices) {
            assertEquals(columns[i], spans[i].column)
        }
    }
}
