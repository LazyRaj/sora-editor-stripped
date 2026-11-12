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
 * Keybinding event in editor.
 *
 * This is different from [EditorKeyEvent].
 * An `EditorKeyEvent` is dispatched by the editor whenever there is a key event.
 * However, a `KeyBindingEvent` is dispatched only for keybindings i.e.
 * when multiple keys are pressed at once.
 * For example, Ctrl + X, Ctrl + D, Ctrl + Alt + O, etc.
 *
 * This event is dispatched after the [EditorKeyEvent].
 * So, if any `EditorKeyEvent` consumes the event (sets the [InterceptTarget.TARGET_EDITOR] flag),
 * `KeyBindingEvent` will not be called.
 *
 * @author Akash Yadav
 */
class KeyBindingEvent(
    editor: CodeEditor,
    src: KeyEvent,
    type: Type,
    private val editorAbleToHandle: Boolean
) : EditorKeyEvent(editor, src, type) {

    /**
     * Is the editor capable of handling this key binding event?
     *
     * @return `true` if the editor can handle this event, `false` otherwise.
     */
    fun canEditorHandle(): Boolean = editorAbleToHandle
}
