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
 * This event happens when [CodeEditor.setText] is called or
 * user edited the displaying content.
 *
 * Note that you should not update the content at this time. Otherwise, there might be some
 * exceptions causing the editor framework to crash. If you do need to update the content, you should
 * post your actions to the main thread so that the user's modification will be successful.
 *
 * @author Rosemoe
 */
class ContentChangeEvent(
    editor: CodeEditor,
    val action: Int,
    changeStart: CharPosition,
    changeEnd: CharPosition,
    val changedText: CharSequence,
    val isCausedByUndoManager: Boolean
) : Event(editor) {

    companion object {
        /**
         * Notify that [CodeEditor.setText] is called
         */
        const val ACTION_SET_NEW_TEXT: Int = 1

        /**
         * Notify that user inserted some texts to the content
         */
        const val ACTION_INSERT: Int = 2

        /**
         * Notify that user deleted some texts in the content
         */
        const val ACTION_DELETE: Int = 3
    }

    val changeStart: CharPosition = changeStart
    val changeEnd: CharPosition = changeEnd
}
