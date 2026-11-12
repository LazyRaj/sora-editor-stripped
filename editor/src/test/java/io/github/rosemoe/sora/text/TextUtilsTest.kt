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

package io.github.rosemoe.sora.text

import io.github.rosemoe.sora.I18nConfig
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.QuickQuoteHandler
import io.github.rosemoe.sora.util.IntPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TextUtilsTest {

    @Test
    fun testLeadingSpacesAndTabs() {
        val packed = TextUtils.countLeadingSpacesAndTabs("   \t  code")
        assertEquals(5, IntPair.getFirst(packed))
        assertEquals(1, IntPair.getSecond(packed))

        val spaceCount = TextUtils.countLeadingSpaceCount("   \t  code", 4)
        assertEquals(9, spaceCount)
    }

    @Test
    fun testCreateIndent() {
        assertEquals("    ", TextUtils.createIndent(4, 4, false))
        assertEquals("\t", TextUtils.createIndent(4, 4, true))
        assertEquals("\t  ", TextUtils.createIndent(6, 4, true))
    }

    @Test
    fun testIndexOfAndStartsWith() {
        val text = "Hello World"
        assertEquals(6, TextUtils.indexOf(text, "world", true, 0))
        assertEquals(-1, TextUtils.indexOf(text, "world", false, 0))
        assertEquals(0, TextUtils.indexOf(text, "Hello", false, 0))

        assertEquals(6, TextUtils.lastIndexOf(text, "World", false, text.length))
        assertTrue(TextUtils.startsWith(text, "hello", true))
        assertFalse(TextUtils.startsWith(text, "hello", false))
    }

    @Test
    fun testWhitespaceAndPad() {
        assertTrue(TextUtils.isWhitespace(' '))
        assertTrue(TextUtils.isWhitespace('\t'))
        assertFalse(TextUtils.isWhitespace('a'))

        assertEquals("0042", TextUtils.padStart("42", '0', 4))
        assertEquals("hello", TextUtils.padStart("hello", '0', 3))
    }

    @Test
    fun testFindWhitespacePositions() {
        val line = "   code block   "
        val pos = TextUtils.findLeadingAndTrailingWhitespacePos(line)
        assertEquals(3, IntPair.getFirst(pos))
        assertEquals(13, IntPair.getSecond(pos))
    }

    @Test
    fun testTrimToSize() {
        assertEquals("Hello", TextUtils.trimToSize("Hello World", 5).toString())
        assertEquals(null, TextUtils.trimToSize(null, 5))
    }

    @Test
    fun testBackspaceOffset() {
        val text = "Hello\r\n"
        val offset = TextUtils.getOffsetForBackspaceKey(text, text.length)
        assertEquals(5, offset)
    }

    @Test
    fun testI18nConfig() {
        assertEquals(1234, I18nConfig.getResourceId(1234))
        I18nConfig.mapTo(1234, 5678)
        assertEquals(5678, I18nConfig.getResourceId(1234))
    }

    @Test
    fun testEmptyLanguage() {
        val lang = EmptyLanguage()
        assertEquals(Language.INTERRUPTION_LEVEL_STRONG, lang.interruptionLevel)
        assertNotNull(lang.formatter)
        assertNotNull(lang.symbolPairs)
        assertNotNull(lang.analyzeManager)
        assertNotNull(lang.newlineHandlers)
        assertEquals(0, lang.getIndentAdvance(ContentReference(Content()), 0, 0))
        assertFalse(lang.useTab())
    }

    @Test
    fun testQuickQuoteHandlerResult() {
        val result = QuickQuoteHandler.HandleResult.NOT_CONSUMED
        assertFalse(result.isConsumed)
        assertEquals(null, result.newCursorRange)
    }
}
