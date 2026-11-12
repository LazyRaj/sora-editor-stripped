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
 * Reports a scroll in editor.
 * The scrolling action can either have run or be running when this event is generated and sent.
 *
 * The returned x,y positions are usually positive when over-scrolling is disabled. They represent
 * the left-top position's pixel in editor.
 */
class ScrollEvent @JvmOverloads constructor(
    editor: CodeEditor,
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
    val cause: Int,
    var flingVelocityX: Float = 0f,
    var flingVelocityY: Float = 0f
) : Event(editor) {

    companion object {
        /**
         * Caused by thumb's exact movements
         */
        const val CAUSE_USER_DRAG: Int = 1

        /**
         * Caused by fling after user's movements
         */
        const val CAUSE_USER_FLING: Int = 2

        /**
         * Caused by calling [CodeEditor.ensurePositionVisible].
         * This can happen when this method is manually called or either the user edits the text
         */
        const val CAUSE_MAKE_POSITION_VISIBLE: Int = 3

        /**
         * Caused by the user's thumb reaching the edge of editor viewport, which causes the editor to
         * scroll to move the selection to text currently outside the viewport.
         */
        const val CAUSE_TEXT_SELECTING: Int = 4

        const val CAUSE_SCALE_TEXT: Int = 5
    }
}
