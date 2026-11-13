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
     * Convenient method
     *
     * @see makeStyle
     */
    @JvmStatic
    fun makeStyle(foregroundColorId: Int): Long {
        checkColorId(foregroundColorId)
        return foregroundColorId.toLong()
    }

    /**
     * Convenient method
     *
     * @see makeStyle
     */
    @JvmStatic
    fun makeStyle(foregroundColorId: Int, noCompletion: Boolean): Long {
        checkColorId(foregroundColorId)
        return foregroundColorId.toLong() or (if (noCompletion) NO_COMPLETION_BIT else 0L)
    }

    /**
     * Convenient method
     *
     * @see makeStyle
     */
    @JvmStatic
    fun makeStyle(
        foregroundColorId: Int,
        backgroundColorId: Int,
        bold: Boolean,
        italic: Boolean,
        strikeThrough: Boolean
    ): Long {
        return makeStyle(foregroundColorId, backgroundColorId, bold, italic, strikeThrough, false)
    }

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
    @JvmStatic
    fun makeStyle(
        foregroundColorId: Int,
        backgroundColorId: Int,
        bold: Boolean,
        italic: Boolean,
        strikeThrough: Boolean,
        noCompletion: Boolean
    ): Long {
        checkColorId(foregroundColorId)
        checkColorId(backgroundColorId)
        return foregroundColorId.toLong() +
                (backgroundColorId.toLong() shl COLOR_ID_BIT_COUNT) or
                (if (bold) BOLD_BIT else 0L) or
                (if (italic) ITALICS_BIT else 0L) or
                (if (strikeThrough) STRIKETHROUGH_BIT else 0L) or
                (if (noCompletion) NO_COMPLETION_BIT else 0L)
    }

    @JvmStatic
    fun getForegroundColorId(style: Long): Int {
        return (style and FOREGROUND_BITS).toInt()
    }

    @JvmStatic
    fun getBackgroundColorId(style: Long): Int {
        return ((style and BACKGROUND_BITS) shr COLOR_ID_BIT_COUNT).toInt()
    }

    @JvmStatic
    fun isBold(style: Long): Boolean {
        return (style and BOLD_BIT) != 0L
    }

    @JvmStatic
    fun isItalics(style: Long): Boolean {
        return (style and ITALICS_BIT) != 0L
    }

    @JvmStatic
    fun isStrikeThrough(style: Long): Boolean {
        return (style and STRIKETHROUGH_BIT) != 0L
    }

    @JvmStatic
    fun isNoCompletion(style: Long): Boolean {
        return (style and NO_COMPLETION_BIT) != 0L
    }

    @JvmStatic
    fun getStyleBits(style: Long): Long {
        return style and (BOLD_BIT + ITALICS_BIT + STRIKETHROUGH_BIT)
    }

    @JvmStatic
    fun checkColorId(colorId: Int) {
        if (colorId > (1 shl COLOR_ID_BIT_COUNT) - 1 || colorId < 0) {
            throw IllegalArgumentException("color id must be positive and bit count is less than $COLOR_ID_BIT_COUNT")
        }
    }
}
