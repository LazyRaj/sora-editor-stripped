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

package io.github.rosemoe.sora.text

import android.text.DynamicLayout
import android.text.Editable
import android.text.Layout
import android.text.Selection
import android.text.TextDirectionHeuristics
import android.text.TextPaint

/**
 * Helper class for indirectly calling Paint#getTextRunCursor(), which is
 * responsible for cursor controlling. Modernized for API 30+ baseline.
 *
 * @author Rosemoe
 */
open class TextLayoutHelper private constructor() {

    private val text: Editable = Editable.Factory.getInstance().newEditable("")
    private val layout: DynamicLayout =
        DynamicLayout.Builder.obtain(text, TextPaint(), Int.MAX_VALUE / 2)
            .setIncludePad(true)
            .setLineSpacing(0f, 0f)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

    /**
     * Get cursor position after moving left
     */
    open fun getCurPosLeft(offset: Int, s: CharSequence): Int {
        val left = maxOf(0, offset - CHAR_FACTOR)
        var index = offset - left
        text.append(s, left, minOf(s.length, offset + CHAR_FACTOR + 1))
        index = minOf(index, text.length)
        Selection.setSelection(text, index)
        Selection.moveLeft(text, layout)
        index = Selection.getSelectionStart(text)
        text.clear()
        Selection.removeSelection(text)
        return left + index
    }

    /**
     * Get cursor position after moving right
     */
    open fun getCurPosRight(offset: Int, s: CharSequence): Int {
        val left = maxOf(0, offset - CHAR_FACTOR)
        var index = offset - left
        text.append(s, left, minOf(s.length, offset + CHAR_FACTOR + 1))
        index = minOf(index, text.length)
        Selection.setSelection(text, index)
        Selection.moveRight(text, layout)
        index = Selection.getSelectionStart(text)
        text.clear()
        Selection.removeSelection(text)
        return left + index
    }

    companion object {
        private const val CHAR_FACTOR: Int = 64
        private val sLocal: ThreadLocal<TextLayoutHelper> =
            ThreadLocal.withInitial { TextLayoutHelper() }

        /**
         * Get TextLayoutHelper for current thread
         */
        fun get(): TextLayoutHelper = sLocal.get()!!
    }
}
