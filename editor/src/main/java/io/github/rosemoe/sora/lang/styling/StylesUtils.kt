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
package io.github.rosemoe.sora.lang.styling

import android.util.Log
import io.github.rosemoe.sora.text.CharPosition

object StylesUtils {

    private const val LOG_TAG = "StylesUtils"

    /**
     * Check given position's span.
     * If [io.github.rosemoe.sora.lang.styling.TextStyle.NO_COMPLETION_BIT] is set, true is returned.
     */
    @JvmStatic
    fun checkNoCompletion(styles: Styles?, pos: CharPosition): Boolean {
        val span = getSpanForPosition(styles, pos)
        return styles != null && (span == null || TextStyle.isNoCompletion(span.style))
    }

    /**
     * Get [Span] for the given position.
     */
    @JvmStatic
    fun getSpanForPosition(styles: Styles?, pos: CharPosition): Span? {
        return getSpanForPositionImpl(styles, pos, 0)
    }

    /**
     * Get following [Span] for the given position.
     */
    @JvmStatic
    fun getFollowingSpanForPosition(styles: Styles?, pos: CharPosition): Span? {
        return getSpanForPositionImpl(styles, pos, 1)
    }

    private fun getSpanForPositionImpl(
        styles: Styles?,
        pos: CharPosition,
        spanIndexOffset: Int
    ): Span? {
        val line = pos.line
        val column = pos.column
        val spans = styles?.spans ?: return null

        var ex: Exception? = null
        val reader = spans.read()
        return try {
            reader.moveToLine(line)
            var index = reader.spanCount - 1
            if (index == -1) {
                return null
            }
            for (i in 0 until reader.spanCount) {
                if (reader.getSpanAt(i).column > column) {
                    index = i - 1
                    break
                }
            }
            index += spanIndexOffset
            if (index < 0 || index >= reader.spanCount) {
                return null
            }
            reader.getSpanAt(index)
        } catch (e: Exception) {
            // Unexpected exception. Maybe there is something wrong in language implementation
            ex = e
            null
        } finally {
            try {
                reader.moveToLine(-1)
            } catch (e1: Exception) {
                if (ex != null) {
                    ex.addSuppressed(e1)
                } else {
                    Log.e(LOG_TAG, "failed to close $reader", e1)
                }
            }
            if (ex != null) {
                Log.e(LOG_TAG, "failed to get spans from $reader at $pos", ex)
            }
        }
    }
}
