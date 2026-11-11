/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
package io.github.rosemoe.sora.langs.textmate.utils

import android.graphics.Color

object ColorUtils {

    /**
     * Converts common web-standard color formats, primarily RGBA (including RGB), into the ARGB integer format
     * used on the Android platform.
     *
     * @param colorString The color string to be parsed (#RRGGBB, #RRGGBBAA, or named color).
     * @return The 32-bit integer color in ARGB format.
     */
    @JvmStatic
    fun parseRGBAToARGB(colorString: String): Int {
        if (!colorString.startsWith('#')) {
            return Color.parseColor(colorString)
        }
        val color = colorString.substring(1).toLong(16)
        return when (colorString.length) {
            7 -> {
                (color or 0x00000000ff000000L).toInt()
            }
            9 -> {
                val r = ((color shr 24) and 0xFF).toInt()
                val g = ((color shr 16) and 0xFF).toInt()
                val b = ((color shr 8) and 0xFF).toInt()
                val a = (color and 0xFF).toInt()
                (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            else -> throw IllegalArgumentException("Unknown color: $colorString")
        }
    }
}
