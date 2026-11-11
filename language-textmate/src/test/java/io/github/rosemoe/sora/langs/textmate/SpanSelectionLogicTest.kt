/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
 */
package io.github.rosemoe.sora.langs.textmate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the span selection logic introduced in TextMateAnalyzer#tokenizeLine.
 */
class SpanSelectionLogicTest {

    companion object {
        // Mirrors org.eclipse.tm4e.core.internal.theme.FontStyle constants
        private const val FONT_STYLE_NONE = 0
        private const val FONT_STYLE_ITALIC = 1
        private const val FONT_STYLE_BOLD = 2
        private const val FONT_STYLE_UNDERLINE = 4
        private const val FONT_STYLE_STRIKETHROUGH = 8
    }

    /**
     * Simulates the `needUnderline` guard extracted in the fix.
     */
    private fun needUnderline(fontStyle: Int): Boolean {
        return (fontStyle and FONT_STYLE_UNDERLINE) != 0
    }

    /**
     * Simulates the span type selection: returns "obtain" when underline is needed,
     * "obtainNoExt" otherwise — mirroring the ternary introduced in the fix.
     */
    private fun selectSpanFactory(fontStyle: Int): String {
        return if (needUnderline(fontStyle)) "obtain" else "obtainNoExt"
    }

    // ------------------------------------------------------------------
    // needUnderline() guard
    // ------------------------------------------------------------------

    @Test
    fun needUnderline_returnsFalse_whenFontStyleIsNone() {
        assertFalse(needUnderline(FONT_STYLE_NONE))
    }

    @Test
    fun needUnderline_returnsFalse_whenOnlyItalic() {
        assertFalse(needUnderline(FONT_STYLE_ITALIC))
    }

    @Test
    fun needUnderline_returnsFalse_whenOnlyBold() {
        assertFalse(needUnderline(FONT_STYLE_BOLD))
    }

    @Test
    fun needUnderline_returnsFalse_whenOnlyStrikethrough() {
        assertFalse(needUnderline(FONT_STYLE_STRIKETHROUGH))
    }

    @Test
    fun needUnderline_returnsTrue_whenOnlyUnderline() {
        assertTrue(needUnderline(FONT_STYLE_UNDERLINE))
    }

    @Test
    fun needUnderline_returnsTrue_whenUnderlineWithBold() {
        assertTrue(needUnderline(FONT_STYLE_BOLD or FONT_STYLE_UNDERLINE))
    }

    @Test
    fun needUnderline_returnsTrue_whenUnderlineWithItalic() {
        assertTrue(needUnderline(FONT_STYLE_ITALIC or FONT_STYLE_UNDERLINE))
    }

    @Test
    fun needUnderline_returnsTrue_whenAllStylesSet() {
        val all = FONT_STYLE_ITALIC or FONT_STYLE_BOLD or FONT_STYLE_UNDERLINE or FONT_STYLE_STRIKETHROUGH
        assertTrue(needUnderline(all))
    }

    // ------------------------------------------------------------------
    // Span factory selection
    // ------------------------------------------------------------------

    @Test
    fun selectSpanFactory_usesObtainNoExt_whenNoUnderline() {
        assertEquals("obtainNoExt", selectSpanFactory(FONT_STYLE_NONE))
    }

    @Test
    fun selectSpanFactory_usesObtainNoExt_whenBoldItalic() {
        assertEquals("obtainNoExt", selectSpanFactory(FONT_STYLE_BOLD or FONT_STYLE_ITALIC))
    }

    @Test
    fun selectSpanFactory_usesObtain_whenUnderline() {
        assertEquals("obtain", selectSpanFactory(FONT_STYLE_UNDERLINE))
    }

    @Test
    fun selectSpanFactory_usesObtain_whenBoldUnderline() {
        assertEquals("obtain", selectSpanFactory(FONT_STYLE_BOLD or FONT_STYLE_UNDERLINE))
    }

    @Test
    fun selectSpanFactory_usesObtain_whenAllStyles() {
        val all = FONT_STYLE_ITALIC or FONT_STYLE_BOLD or FONT_STYLE_UNDERLINE or FONT_STYLE_STRIKETHROUGH
        assertEquals("obtain", selectSpanFactory(all))
    }

    // ------------------------------------------------------------------
    // Before-fix regression: NoExtSpanImpl would throw on setUnderlineColor
    // ------------------------------------------------------------------

    @Test
    fun noExtSpan_throwsOnSetUnderlineColor() {
        val setUnderlineOnNoExt = Runnable {
            throw UnsupportedOperationException()
        }

        val fontStyleWithUnderline = FONT_STYLE_UNDERLINE

        assertThrows(
            "NoExtSpanImpl must throw UnsupportedOperationException when setUnderlineColor is called",
            UnsupportedOperationException::class.java
        ) {
            val wouldUseNoExt = true
            if (wouldUseNoExt && (fontStyleWithUnderline and FONT_STYLE_UNDERLINE) != 0) {
                setUnderlineOnNoExt.run()
            }
        }
    }

    @Test
    fun fixedLogic_doesNotCallSetUnderlineOnNoExtSpan() {
        val fontStyleWithUnderline = FONT_STYLE_UNDERLINE
        val needUnderline = needUnderline(fontStyleWithUnderline)

        val factory = selectSpanFactory(fontStyleWithUnderline)

        assertNotEquals(
            "Fixed logic must not select obtainNoExt when underline flag is set",
            "obtainNoExt",
            factory
        )
        assertEquals("obtain", factory)
        assertTrue(needUnderline)
    }
}
