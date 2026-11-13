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
 * picked from Visual Studio 2019
 * Thanks to liyujiang-gzu (GitHub @liyujiang-gzu)
 */
open class SchemeVS2019 : EditorColorScheme(true) {

    override fun applyDefault() {
        super.applyDefault()
        setColor(ANNOTATION, 0xff4ec9b0.toInt())
        setColor(FUNCTION_NAME, 0xffdcdcdc.toInt())
        setColor(IDENTIFIER_NAME, 0xff4ec9b0.toInt())
        setColor(IDENTIFIER_VAR, 0xffdcdcaa.toInt())
        setColor(LITERAL, 0xffd69d85.toInt())
        setColor(OPERATOR, 0xffdcdcdc.toInt())
        setColor(COMMENT, 0xff57a64a.toInt())
        setColor(KEYWORD, 0xff569cd6.toInt())
        setColor(WHOLE_BACKGROUND, 0xff1e1e1e.toInt())
        setColor(COMPLETION_WND_BACKGROUND, 0xff1e1e1e.toInt())
        setColor(COMPLETION_WND_CORNER, 0xff999999.toInt())
        setColor(TEXT_NORMAL, 0xffdcdcdc.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xff1e1e1e.toInt())
        setColor(LINE_NUMBER, 0xff2b9eaf.toInt())
        setColor(LINE_NUMBER_CURRENT, 0xff2b9eaf.toInt())
        setColor(LINE_DIVIDER, 0xff2b9eaf.toInt())
        setColor(SCROLL_BAR_THUMB, 0xff3e3e42.toInt())
        setColor(SCROLL_BAR_THUMB_PRESSED, 0xff9e9e9e.toInt())
        setColor(SELECTED_TEXT_BACKGROUND, 0xff3676b8.toInt())
        setColor(MATCHED_TEXT_BACKGROUND, 0xff653306.toInt())
        setColor(CURRENT_LINE, 0xff464646.toInt())
        setColor(SELECTION_INSERT, 0xffffffff.toInt())
        setColor(SELECTION_HANDLE, 0xffffffff.toInt())
        setColor(BLOCK_LINE, 0xff717171.toInt())
        setColor(BLOCK_LINE_CURRENT, 0)
        setColor(NON_PRINTABLE_CHAR, 0xffdddddd.toInt())
        setColor(TEXT_SELECTED, 0xffffffff.toInt())
    }
}
