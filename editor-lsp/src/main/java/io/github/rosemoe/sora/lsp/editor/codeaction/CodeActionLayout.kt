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

package io.github.rosemoe.sora.lsp.editor.codeaction

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

interface CodeActionLayout {
    /** Bind this layout to its hosting window. */
    fun attach(window: CodeActionWindow)
    /** Create the root view that will be rendered in the popup window. */
    fun createView(inflater: LayoutInflater): View
    /** Apply colors and typeface from the editor. */
    fun applyColorScheme(colorScheme: EditorColorScheme, typeface: Typeface)
    /** Render the provided list of code actions. */
    fun renderActions(actions: List<CodeActionItem>)
    /** Notify layout that editor text size changed. */
    fun onTextSizeChanged(oldSize: Float, newSize: Float)
}
