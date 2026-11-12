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

import android.graphics.Path
import android.graphics.RectF
import kotlin.math.max

/**
 * Helper class for building a bubble rect
 *
 * @author Rosemoe
 */
object BubbleHelper {

    private const val SQRT2 = 1.41421356f

    /**
     * Build a bubble into the given Path object. Old content in given Path is cleared.
     * @param path target Path object
     * @param bounds The bounds for the bubble
     */
    @JvmStatic
    fun buildBubblePath(path: Path, bounds: RectF) {
        path.reset()

        val height = bounds.height()
        val r = height / 2f
        // Ensure we are convex.
        val width = max(r + SQRT2 * r, bounds.width())
        pathArcTo(path, r, r, r, 90f, 180f)
        val o1X = width - SQRT2 * r
        pathArcTo(path, o1X, r, r, -90f, 45f)
        val r2 = r / 5f
        val o2X = width - SQRT2 * r2
        pathArcTo(path, o2X, r, r2, -45f, 90f)
        pathArcTo(path, o1X, r, r, 45f, 45f)
        path.close()

        path.offset(bounds.left, bounds.top)
    }

    private fun pathArcTo(
        path: Path,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float
    ) {
        path.arcTo(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            startAngle,
            sweepAngle,
            false
        )
    }
}
