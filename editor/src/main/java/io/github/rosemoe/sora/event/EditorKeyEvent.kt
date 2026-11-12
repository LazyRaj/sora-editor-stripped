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

import android.view.KeyEvent
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Receives key related events in editor.
 *
 * You may set a boolean for editor to return to the Android KeyEvent framework.
 *
 * @author Rosemoe
 * @see ResultedEvent.result
 *
 * This class mirrors methods of [KeyEvent], but some methods are changed:
 * @see isAltPressed
 * @see isShiftPressed
 */
open class EditorKeyEvent(
    editor: CodeEditor,
    private val src: KeyEvent,
    val eventType: Type
) : ResultedEvent<Boolean>(editor) {

    val isShiftPressed: Boolean = editor.keyMetaStates.isShiftPressed
    val isAltPressed: Boolean = editor.keyMetaStates.isAltPressed

    override fun canIntercept(): Boolean = true

    val action: Int
        get() = src.action

    val keyCode: Int
        get() = src.keyCode

    val repeatCount: Int
        get() = src.repeatCount

    val metaState: Int
        get() = src.metaState

    val modifiers: Int
        get() = src.modifiers

    val downTime: Long
        get() = src.downTime

    override val eventTime: Long
        get() = src.eventTime

    val isCtrlPressed: Boolean
        get() = (src.metaState and KeyEvent.META_CTRL_ON) != 0

    fun markAsConsumed() {
        interceptAndSetResult(true)
    }

    fun result(editorResult: Boolean): Boolean {
        val res = result
        val userResult = res != null && res
        return if (isIntercepted) {
            userResult
        } else {
            userResult || editorResult
        }
    }

    /**
     * The type of [EditorKeyEvent].
     */
    enum class Type {
        /**
         * Used for [CodeEditor.onKeyUp].
         */
        UP,

        /**
         * Used for [CodeEditor.onKeyDown].
         */
        DOWN,

        /**
         * Used for [CodeEditor.onKeyMultiple].
         */
        MULTIPLE
    }
}
