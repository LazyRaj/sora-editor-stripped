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

package io.github.rosemoe.sora.text.method

import android.text.Editable
import android.text.method.MetaKeyKeyListener
import android.view.KeyEvent
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Handles key events such as SHIFT
 *
 * @author Rosemoe
 */
open class KeyMetaStates(private val editor: CodeEditor) : MetaKeyKeyListener() {

    /**
     * Dummy text used for Android original APIs
     */
    private val dest: Editable = Editable.Factory.getInstance().newEditable("")

    var isCtrlPressed: Boolean = false
        private set

    open fun onKeyDown(event: KeyEvent) {
        onKeyDown(editor, dest, event.keyCode, event)
        isCtrlPressed = event.isCtrlPressed
    }

    open fun onKeyUp(event: KeyEvent) {
        onKeyUp(editor, dest, event.keyCode, event)
        isCtrlPressed = event.isCtrlPressed
    }

    open fun getMetaState(event: KeyEvent): Int {
        return getMetaState(dest, event)
    }

    val isShiftPressed: Boolean
        get() = getMetaState(dest, META_SHIFT_ON) == 1

    val isAltPressed: Boolean
        get() = getMetaState(dest, META_ALT_ON) == 1

    val isSymPressed: Boolean
        get() = getMetaState(dest, META_SYM_ON) == 1

    val isSelecting: Boolean
        get() = isShiftPressed && !isAltPressed

    open fun adjustAfterKeyPress() {
        adjustMetaAfterKeypress(dest)
    }

    open fun clearMetaStates(states: Int) {
        clearMetaKeyState(editor, dest, states)
    }
}
