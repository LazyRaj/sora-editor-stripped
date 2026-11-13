/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.lang.format

import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Provider for custom formatters.
 *
 * @author KonerDev
 */
fun interface FormatterProvider {

    /**
     * Get a formatter for the specified editor.
     *
     * If no custom formatter is available, this method should return
     * `null` to allow the editor to fall back to the language formatter.
     *
     * @param editor The current code editor.
     * @return A custom formatter, or null to use the default formatter
     */
    fun getFormatter(editor: CodeEditor): Formatter?
}
