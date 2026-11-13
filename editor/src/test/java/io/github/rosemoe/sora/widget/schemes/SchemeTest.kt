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
package io.github.rosemoe.sora.widget.schemes

import io.github.rosemoe.sora.annotations.Experimental
import io.github.rosemoe.sora.annotations.InvalidateRequired
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SchemeTest {

    @Test
    fun testDefaultColorScheme() {
        val scheme = EditorColorScheme()
        assertFalse(scheme.isDark())
        assertNotNull(EditorColorScheme.getDefault())

        assertEquals(0xFF333333.toInt(), scheme.getColor(EditorColorScheme.TEXT_NORMAL))
        assertEquals(scheme.getColor(EditorColorScheme.TEXT_NORMAL), scheme[EditorColorScheme.TEXT_NORMAL])

        scheme.setColor(EditorColorScheme.TEXT_NORMAL, 0xFF123456.toInt())
        assertEquals(0xFF123456.toInt(), scheme.getColor(EditorColorScheme.TEXT_NORMAL))

        scheme[EditorColorScheme.TEXT_NORMAL] = 0xFF654321.toInt()
        assertEquals(0xFF654321.toInt(), scheme[EditorColorScheme.TEXT_NORMAL])

        val darkScheme = EditorColorScheme(true)
        assertTrue(darkScheme.isDark())
    }

    @Test
    fun testSubSchemes() {
        val darcula = SchemeDarcula()
        assertTrue(darcula.isDark())
        assertEquals(0xff2b2b2b.toInt(), darcula[EditorColorScheme.WHOLE_BACKGROUND])
        assertEquals(0xffcc7832.toInt(), darcula[EditorColorScheme.KEYWORD])

        val eclipse = SchemeEclipse()
        assertFalse(eclipse.isDark())
        assertEquals(0xffffffff.toInt(), eclipse[EditorColorScheme.WHOLE_BACKGROUND])
        assertEquals(0xff7f0074.toInt(), eclipse[EditorColorScheme.KEYWORD])

        val github = SchemeGitHub()
        assertFalse(github.isDark())
        assertEquals(0xffffffff.toInt(), github[EditorColorScheme.WHOLE_BACKGROUND])
        assertEquals(0xffde3a49.toInt(), github[EditorColorScheme.KEYWORD])

        val notepad = SchemeNotepadXX()
        assertFalse(notepad.isDark())
        assertEquals(0xffffffff.toInt(), notepad[EditorColorScheme.WHOLE_BACKGROUND])
        assertEquals(0xff8000ff.toInt(), notepad[EditorColorScheme.KEYWORD])

        val vs2019 = SchemeVS2019()
        assertTrue(vs2019.isDark())
        assertEquals(0xff1e1e1e.toInt(), vs2019[EditorColorScheme.WHOLE_BACKGROUND])
        assertEquals(0xff569cd6.toInt(), vs2019[EditorColorScheme.KEYWORD])
    }

    @Test
    fun testAnnotationsPresence() {
        @Experimental
        @UnsupportedUserUsage
        class AnnotatedSample {
            @InvalidateRequired
            var field: Int = 0
        }

        val sample = AnnotatedSample()
        sample.field = 42
        assertEquals(42, sample.field)
    }
}
