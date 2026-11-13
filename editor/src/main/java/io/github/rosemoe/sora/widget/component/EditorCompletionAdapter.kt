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
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.BaseAdapter
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * A class to make custom adapter for auto-completion window
 */
abstract class EditorCompletionAdapter : BaseAdapter(), Adapter {

    private var window: EditorAutoCompletion? = null
    private var items: List<CompletionItem>? = null

    /**
     * Called by [EditorAutoCompletion] to attach some arguments
     */
    open fun attachValues(window: EditorAutoCompletion, items: List<CompletionItem>) {
        this.window = window
        this.items = items
    }

    override fun getItem(position: Int): CompletionItem {
        return items?.get(position) ?: throw IndexOutOfBoundsException("Position: $position")
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    override fun getCount(): Int {
        return items?.size ?: 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return getView(position, convertView, parent, position == (window?.currentPosition ?: -1))
    }

    /**
     * Get color scheme in editor
     */
    protected open fun getColorScheme(): EditorColorScheme {
        return window?.editor?.colorScheme ?: throw IllegalStateException("Window not attached")
    }

    /**
     * Get theme color from current color scheme
     */
    protected open fun getThemeColor(type: Int): Int {
        return getColorScheme().getColor(type)
    }

    /**
     * Get context from editor
     */
    protected open fun getContext(): Context {
        return window?.context ?: throw IllegalStateException("Window not attached")
    }

    /**
     * Implementation of this class should provide exact height of its item
     */
    abstract fun getItemHeight(): Int

    /**
     * @param isCurrentCursorPosition Is the position currently selected
     */
    protected abstract fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?,
        isCurrentCursorPosition: Boolean
    ): View
}
