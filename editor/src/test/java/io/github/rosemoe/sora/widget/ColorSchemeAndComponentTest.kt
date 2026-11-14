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
package io.github.rosemoe.sora.widget

import android.graphics.RectF
import io.github.rosemoe.sora.lang.completion.snippet.NoFormat
import io.github.rosemoe.sora.lang.completion.snippet.Transform
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.snippet.applyTransform
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.regex.Pattern

@RunWith(RobolectricTestRunner::class)
class ColorSchemeAndComponentTest {

    @Test
    fun testColorSchemeIndexingAndDefaults() {
        val scheme = EditorColorScheme(false)
        assertFalse(scheme.isDark())

        scheme[EditorColorScheme.WHOLE_BACKGROUND] = 0xff123456.toInt()
        assertEquals(0xff123456.toInt(), scheme[EditorColorScheme.WHOLE_BACKGROUND])
        assertEquals(0xff123456.toInt(), scheme.getColor(EditorColorScheme.WHOLE_BACKGROUND))

        val defaultScheme = EditorColorScheme.getDefault()
        assertNotNull(defaultScheme)
    }

    @Test
    fun testSelectionHandleDescriptorDestructuring() {
        val rect = RectF(10f, 20f, 30f, 40f)
        val descriptor = SelectionHandleStyle.HandleDescriptor(rect, SelectionHandleStyle.ALIGN_LEFT)
        val (pos, align) = descriptor
        assertEquals(rect, pos)
        assertEquals(SelectionHandleStyle.ALIGN_LEFT, align)
    }

    @Test
    fun testTransformExtension() {
        val transform = Transform(
            regexp = Pattern.compile("(\\w+)"),
            globalMode = false,
            format = listOf(NoFormat("world"))
        )
        val transformed = "hello world".applyTransform(transform)
        assertEquals("world world", transformed)
    }

    @Test
    fun testDirectAccessProps() {
        val props = DirectAccessProps()
        assertTrue(props.deleteEmptyLineFast)
        props.deleteEmptyLineFast = false
        assertFalse(props.deleteEmptyLineFast)
        assertEquals(1, props.deleteMultiSpaces)
    }
}
