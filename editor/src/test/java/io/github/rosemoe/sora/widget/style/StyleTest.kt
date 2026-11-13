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
package io.github.rosemoe.sora.widget.style

import android.graphics.Canvas
import io.github.rosemoe.sora.widget.style.builtin.HandleStyleDrop
import io.github.rosemoe.sora.widget.style.builtin.HandleStyleSideDrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StyleTest {

    @Test
    fun testDiagnosticIndicatorStyle() {
        assertEquals(4, DiagnosticIndicatorStyle.entries.size)
        assertEquals(DiagnosticIndicatorStyle.NONE, DiagnosticIndicatorStyle.valueOf("NONE"))
        assertEquals(DiagnosticIndicatorStyle.WAVY_LINE, DiagnosticIndicatorStyle.valueOf("WAVY_LINE"))
    }

    @Test
    fun testLineInfoPanelPositionConstants() {
        assertEquals(1, LineInfoPanelPosition.LEFT)
        assertEquals(2, LineInfoPanelPosition.TOP)
        assertEquals(4, LineInfoPanelPosition.RIGHT)
        assertEquals(8, LineInfoPanelPosition.BOTTOM)
        assertEquals(15, LineInfoPanelPosition.CENTER)

        assertEquals(0, LineInfoPanelPositionMode.FIXED)
        assertEquals(1, LineInfoPanelPositionMode.FOLLOW)
    }

    @Test
    fun testHandleDescriptor() {
        val descriptor = SelectionHandleStyle.HandleDescriptor()
        assertTrue(descriptor.position.isEmpty)
        assertEquals(SelectionHandleStyle.ALIGN_CENTER, descriptor.alignment)

        descriptor.set(10f, 20f, 30f, 40f, SelectionHandleStyle.ALIGN_LEFT)
        assertFalse(descriptor.position.isEmpty)
        assertEquals(10f, descriptor.position.left)
        assertEquals(SelectionHandleStyle.ALIGN_LEFT, descriptor.alignment)

        descriptor.setEmpty()
        assertTrue(descriptor.position.isEmpty)
        assertEquals(SelectionHandleStyle.ALIGN_CENTER, descriptor.alignment)
    }

    @Test
    fun testHandleStyleDropAndSideDrop() {
        val context = RuntimeEnvironment.getApplication()
        val drop = HandleStyleDrop(context)
        drop.setAlpha(128)
        drop.setScale(1.5f)

        val canvas = Canvas()
        val desc = SelectionHandleStyle.HandleDescriptor()
        drop.draw(canvas, SelectionHandleStyle.HANDLE_TYPE_INSERT, 50f, 100f, 20, 0xff0000, desc)
        assertFalse(desc.position.isEmpty)

        val sideDrop = HandleStyleSideDrop(context)
        sideDrop.draw(canvas, SelectionHandleStyle.HANDLE_TYPE_LEFT, 50f, 100f, 20, 0xff0000, desc)
        assertEquals(SelectionHandleStyle.ALIGN_LEFT, desc.alignment)
        sideDrop.draw(canvas, SelectionHandleStyle.HANDLE_TYPE_RIGHT, 50f, 100f, 20, 0xff0000, desc)
        assertEquals(SelectionHandleStyle.ALIGN_RIGHT, desc.alignment)
    }
}
