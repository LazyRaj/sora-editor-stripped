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

import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * This event happens when text is edited by the user, or the user click the view to change the
 * position of selection. Even when the actual values of CharPosition are not changed, you may receive the event.
 *
 * Note that you should not change returned CharPosition objects because they are shared in an event
 * dispatch.
 */
class SelectionChangeEvent(
    editor: CodeEditor,
    val oldLeft: CharPosition?,
    val oldRight: CharPosition?,
    val cause: Int
) : Event(editor) {

    companion object {
        /**
         * Unknown cause
         */
        const val CAUSE_UNKNOWN: Int = 0

        /**
         * Selection change caused by text modifications
         */
        const val CAUSE_TEXT_MODIFICATION: Int = 1

        /**
         * Set selection by handle
         */
        const val CAUSE_SELECTION_HANDLE: Int = 2

        /**
         * Set selection by single tap
         */
        const val CAUSE_TAP: Int = 3

        /**
         * Set selection because of [android.view.inputmethod.InputConnection.setSelection]
         */
        const val CAUSE_IME: Int = 4

        /**
         * Long press
         */
        const val CAUSE_LONG_PRESS: Int = 5

        /**
         * Search text by [io.github.rosemoe.sora.widget.EditorSearcher]
         */
        const val CAUSE_SEARCH: Int = 6

        /**
         * From keyboard or direct method invocation to change selection
         */
        const val CAUSE_KEYBOARD_OR_CODE: Int = 7

        /**
         * From mouse
         */
        const val CAUSE_MOUSE_INPUT: Int = 8

        /**
         * Caused by a dead key press
         */
        const val CAUSE_DEAD_KEYS: Int = 9
    }

    val left: CharPosition = editor.text.cursor.left()
    val right: CharPosition = editor.text.cursor.right()

    /**
     * Checks whether text is selected
     */
    val isSelected: Boolean
        get() = left.index != right.index
}
