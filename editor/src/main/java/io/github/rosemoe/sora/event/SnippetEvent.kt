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
package io.github.rosemoe.sora.event

import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Notify that snippet controller state is changed.
 *
 * If action is [ACTION_START] and any event receiver intercepts editor, the snippet edit will
 * stop before moving to any tab stop. And consequently, a [SnippetEvent] with action [ACTION_STOP]
 * will be broadcast immediately.
 *
 * There is at least one tab stop in the list when action is [ACTION_START] or [ACTION_SHIFT].
 * But no tab stop is left there when action is [ACTION_STOP]. The last tab stop is where the selection
 * will be placed when the snippet is finished normally.
 *
 * @author Rosemoe
 */
class SnippetEvent(
    editor: CodeEditor,
    val action: Int,
    val currentTabStop: Int,
    val totalTabStop: Int
) : Event(editor) {

    companion object {
        /**
         * Called before controller shifts to any tab stop
         */
        const val ACTION_START: Int = 1

        /**
         * Called when controller shifted to a tab stop
         */
        const val ACTION_SHIFT: Int = 2

        /**
         * Called when controller has exited a snippet
         */
        const val ACTION_STOP: Int = 3
    }
}
