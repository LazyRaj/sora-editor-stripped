/*******************************************************************************
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
 *
 *     Modified by Raj Dave (aka LazyRaj)
 ******************************************************************************/

package io.github.rosemoe.sora.lsp.editor.hover

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.lsp4j.Hover

interface HoverLayout {
    /** Bind the host window to this layout. */
    fun attach(window: HoverWindow)
    /** Build the root view that will be shown by the window. */
    fun createView(inflater: LayoutInflater): View
    /** Apply editor colors and fonts to the layout. */
    fun applyColorScheme(colorScheme: EditorColorScheme, typeface: Typeface)
    /** Render the provided hover content. */
    fun renderHover(hover: Hover)
    /** Notify the layout that the editor text size changed. */
    fun onTextSizeChanged(oldSize: Float, newSize: Float)
}
