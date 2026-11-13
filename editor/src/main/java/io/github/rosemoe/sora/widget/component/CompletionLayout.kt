/*
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
 */
/*
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.widget.component

import android.content.Context
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Manages layout of [EditorAutoCompletion]
 */
interface CompletionLayout {

    /**
     * Color scheme changed
     */
    fun onApplyColorScheme(colorScheme: EditorColorScheme)

    /**
     * Attach the [EditorAutoCompletion].
     */
    fun setEditorCompletion(completion: EditorAutoCompletion)

    /**
     * Inflate the layout, return the view root.
     */
    fun inflate(context: Context): View

    /**
     * Get the [AdapterView] to display completion items
     */
    fun getCompletionList(): @JvmSuppressWildcards AdapterView<Adapter>

    /**
     * Set loading state.
     */
    fun setLoading(loading: Boolean)

    /**
     * Make the given position visible
     */
    fun ensureListPositionVisible(position: Int, incrementPixels: Int)

    /**
     * Some layout may support to display more animations
     */
    fun setEnabledAnimation(enabledAnimation: Boolean) {
        // ignore
    }
}
