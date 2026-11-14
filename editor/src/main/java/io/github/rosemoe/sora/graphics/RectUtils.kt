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

package io.github.rosemoe.sora.graphics

import android.graphics.RectF

object RectUtils {

    fun contains(rect: RectF, x: Float, y: Float, extraXSpace: Float = 0f): Boolean {
        return x >= rect.left - extraXSpace && x <= rect.right + extraXSpace && y >= rect.top && y <= rect.bottom
    }

    fun almostContains(rect: RectF, x: Float, y: Float, extraSpace: Float): Boolean {
        return x >= rect.left - extraSpace && x <= rect.right + extraSpace && y >= rect.top - extraSpace && y <= rect.bottom + extraSpace
    }
}

/**
 * Extension function to check if [RectF] contains point (x, y) with optional [extraXSpace].
 */
fun RectF.contains(x: Float, y: Float, extraXSpace: Float = 0f): Boolean =
    RectUtils.contains(this, x, y, extraXSpace)

/**
 * Extension function to check if [RectF] almost contains point (x, y) with [extraSpace].
 */
fun RectF.almostContains(x: Float, y: Float, extraSpace: Float): Boolean =
    RectUtils.almostContains(this, x, y, extraSpace)

