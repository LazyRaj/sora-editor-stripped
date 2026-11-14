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
package io.github.rosemoe.sora.lang.completion

import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion

/**
 * Helper class for completion
 *
 * @author Rosemoe
 */
object CompletionHelper {

    /**
     * Searches backward on the line, with the given checker to check chars.
     * Returns the longest text that matches the requirement
     */
    fun computePrefix(ref: ContentReference, pos: CharPosition, checker: PrefixChecker): String {
        var begin = pos.column
        val line = ref.getLine(pos.line)
        while (begin > 0) {
            if (!checker.check(line[begin - 1])) {
                break
            }
            begin--
        }
        return line.substring(begin, pos.column)
    }

    /**
     * Check whether the thread is abandoned by editor.
     * Return true if it is cancelled by editor.
     */
    fun checkCancelled(): Boolean {
        val thread = Thread.currentThread()
        return thread is EditorAutoCompletion.CompletionThread && thread.isCancelled
    }

    fun interface PrefixChecker {
        fun check(ch: Char): Boolean
    }
}

fun ContentReference.computePrefix(pos: CharPosition, checker: CompletionHelper.PrefixChecker): String =
    CompletionHelper.computePrefix(this, pos, checker)

