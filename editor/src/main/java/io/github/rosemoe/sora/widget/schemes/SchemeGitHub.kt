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
 * picked from GitHub site
 * Thanks to liyujiang-gzu (GitHub @liyujiang-gzu)
 */
open class SchemeGitHub : EditorColorScheme(false) {

    override fun applyDefault() {
        super.applyDefault()
        setColor(ANNOTATION, 0xff6f42c1.toInt())
        setColor(FUNCTION_NAME, 0xff24292e.toInt())
        setColor(IDENTIFIER_NAME, 0xff24292e.toInt())
        setColor(IDENTIFIER_VAR, 0xff24292e.toInt())
        setColor(LITERAL, 0xff032f62.toInt())
        setColor(OPERATOR, 0xff005cc5.toInt())
        setColor(COMMENT, 0xff6a737d.toInt())
        setColor(KEYWORD, 0xffde3a49.toInt())
        setColor(WHOLE_BACKGROUND, 0xffffffff.toInt())
        setColor(TEXT_NORMAL, 0xff24292e.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xffffffff.toInt())
        setColor(LINE_NUMBER, 0xffbec0c1.toInt())
        setColor(LINE_NUMBER_CURRENT, 0xffbec0c1.toInt())
        setColor(SELECTION_INSERT, 0xffc7edcc.toInt())
        setColor(SELECTION_HANDLE, 0xffc7edcc.toInt())
    }
}
