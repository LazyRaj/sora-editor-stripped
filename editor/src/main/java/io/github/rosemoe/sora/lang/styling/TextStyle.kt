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

/**
 * Utility class for text style related operations
 *
 * @author Rosemoe
 */
object TextStyle {

    const val COLOR_ID_BIT_COUNT: Int = 19
    const val FOREGROUND_BITS: Long = (1L shl COLOR_ID_BIT_COUNT) - 1L
    const val BACKGROUND_BITS: Long = FOREGROUND_BITS shl COLOR_ID_BIT_COUNT

    /**
     * Bold text style
     */
    const val BOLD_BIT: Long = 1L shl (COLOR_ID_BIT_COUNT * 2)

    /**
     * Italic text style
     */
    const val ITALICS_BIT: Long = BOLD_BIT shl 1

    /**
     * Show a strikethrough
     */
    const val STRIKETHROUGH_BIT: Long = ITALICS_BIT shl 1

    /**
     * Edit texts in the region will not cause auto-completion to work
     */
    const val NO_COMPLETION_BIT: Long = STRIKETHROUGH_BIT shl 1

    /**
     * Make a TextStyle with the given style arguments
     *
     * Note: colorId must be less than 20 bits
     *
     * @see BOLD_BIT
     * @see ITALICS_BIT
     * @see STRIKETHROUGH_BIT
     * @see NO_COMPLETION_BIT
     */
    fun makeStyle(
        foregroundColorId: Int,
        backgroundColorId: Int = 0,
        bold: Boolean = false,
        italic: Boolean = false,
        strikeThrough: Boolean = false,
        noCompletion: Boolean = false
    ): Long {
        checkColorId(foregroundColorId)
        if (backgroundColorId != 0) {
            checkColorId(backgroundColorId)
        }
        return foregroundColorId.toLong() +
                (backgroundColorId.toLong() shl COLOR_ID_BIT_COUNT) or
                (if (bold) BOLD_BIT else 0L) or
                (if (italic) ITALICS_BIT else 0L) or
                (if (strikeThrough) STRIKETHROUGH_BIT else 0L) or
                (if (noCompletion) NO_COMPLETION_BIT else 0L)
    }

    /**
     * Convenient method
     *
     * @see makeStyle
     */
    fun makeStyle(foregroundColorId: Int, noCompletion: Boolean): Long {
        return makeStyle(
            foregroundColorId = foregroundColorId,
            backgroundColorId = 0,
            bold = false,
            italic = false,
            strikeThrough = false,
            noCompletion = noCompletion
        )
    }

    fun getForegroundColorId(style: Long): Int {
        return (style and FOREGROUND_BITS).toInt()
    }

    fun getBackgroundColorId(style: Long): Int {
        return ((style and BACKGROUND_BITS) shr COLOR_ID_BIT_COUNT).toInt()
    }

    fun isBold(style: Long): Boolean {
        return (style and BOLD_BIT) != 0L
    }

    fun isItalics(style: Long): Boolean {
        return (style and ITALICS_BIT) != 0L
    }

    fun isStrikeThrough(style: Long): Boolean {
        return (style and STRIKETHROUGH_BIT) != 0L
    }

    fun isNoCompletion(style: Long): Boolean {
        return (style and NO_COMPLETION_BIT) != 0L
    }

    fun getStyleBits(style: Long): Long {
        return style and (BOLD_BIT + ITALICS_BIT + STRIKETHROUGH_BIT)
    }

    fun checkColorId(colorId: Int) {
        if (colorId > (1 shl COLOR_ID_BIT_COUNT) - 1 || colorId < 0) {
            throw IllegalArgumentException("color id must be positive and bit count is less than $COLOR_ID_BIT_COUNT")
        }
    }
}

inline val Long.foregroundColorId: Int get() = TextStyle.getForegroundColorId(this)
inline val Long.backgroundColorId: Int get() = TextStyle.getBackgroundColorId(this)
inline val Long.isBold: Boolean get() = TextStyle.isBold(this)
inline val Long.isItalics: Boolean get() = TextStyle.isItalics(this)
inline val Long.isStrikeThrough: Boolean get() = TextStyle.isStrikeThrough(this)
inline val Long.isNoCompletion: Boolean get() = TextStyle.isNoCompletion(this)
inline val Long.styleBits: Long get() = TextStyle.getStyleBits(this)

