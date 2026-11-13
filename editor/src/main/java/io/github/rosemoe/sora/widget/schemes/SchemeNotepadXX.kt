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
// Modified by Raj Dave (aka LazyRaj)
package io.github.rosemoe.sora.widget.schemes

/**
 * ColorScheme for editor
 * picked from Notepad++ v7.8.1
 * Thanks to liyujiang-gzu (GitHub @liyujiang-gzu)
 */
open class SchemeNotepadXX : EditorColorScheme(false) {

    override fun applyDefault() {
        super.applyDefault()
        setColor(ANNOTATION, 0xff0000ff.toInt())
        setColor(FUNCTION_NAME, 0xff000000.toInt())
        setColor(IDENTIFIER_NAME, 0xff000000.toInt())
        setColor(IDENTIFIER_VAR, 0xff000000.toInt())
        setColor(LITERAL, 0xff808080.toInt())
        setColor(OPERATOR, 0xff0000ff.toInt())
        setColor(COMMENT, 0xff008000.toInt())
        setColor(KEYWORD, 0xff8000ff.toInt())
        setColor(WHOLE_BACKGROUND, 0xffffffff.toInt())
        setColor(TEXT_NORMAL, 0xff000000.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xffe4e4e4.toInt())
        setColor(LINE_NUMBER, 0xff808080.toInt())
        setColor(LINE_NUMBER_CURRENT, 0xff808080.toInt())
        setColor(SELECTED_TEXT_BACKGROUND, 0xff75d975.toInt())
        setColor(MATCHED_TEXT_BACKGROUND, 0xffc0c0c0.toInt())
        setColor(CURRENT_LINE, 0xffe8e8ff.toInt())
        setColor(SELECTION_INSERT, 0xff8000ff.toInt())
        setColor(SELECTION_HANDLE, 0xff8000ff.toInt())
        setColor(BLOCK_LINE, 0xffc0c0c0.toInt())
        setColor(BLOCK_LINE_CURRENT, 0)
        setColor(TEXT_SELECTED, 0xffffffff.toInt())
    }
}
