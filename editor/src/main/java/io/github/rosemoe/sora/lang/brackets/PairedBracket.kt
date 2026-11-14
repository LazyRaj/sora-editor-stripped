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
package io.github.rosemoe.sora.lang.brackets

/**
 * Describes paired brackets.
 *
 * @param leftIndex Index of left bracket in text
 * @param leftLength Text length of left bracket
 * @param rightIndex Index of right bracket in text
 * @param rightLength Text length of right bracket
 * @author Rosemoe
 */
data class PairedBracket(
    val leftIndex: Int,
    val leftLength: Int,
    val rightIndex: Int,
    val rightLength: Int
) {
    /**
     * Currently length is always 1.
     *
     * @see PairedBracket
     */
    constructor(leftIndex: Int, rightIndex: Int) : this(leftIndex, 1, rightIndex, 1)
}
