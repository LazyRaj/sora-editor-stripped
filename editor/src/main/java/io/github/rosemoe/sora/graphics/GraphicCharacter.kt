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

import io.github.rosemoe.sora.util.MyCharacter

object GraphicCharacter {

    fun isCombiningCharacter(codePoint: Int): Boolean {
        return MyCharacter.isVariationSelector(codePoint) ||
                MyCharacter.isFitzpatrick(codePoint) ||
                MyCharacter.isZWJ(codePoint) ||
                MyCharacter.isZWNJ(codePoint) ||
                MyCharacter.couldBeEmoji(codePoint) ||
                (Character.charCount(codePoint) == 1 && Character.isSurrogate(codePoint.toChar())) ||
                isASCIICombiningSymbol(codePoint)
    }

    fun isASCIICombiningSymbol(codePoint: Int): Boolean {
        return codePoint == '.'.code ||
                codePoint == '/'.code ||
                codePoint == '!'.code ||
                codePoint == '='.code ||
                codePoint == '<'.code ||
                codePoint == '>'.code ||
                codePoint == '-'.code
    }

    fun couldBeEmojiPart(codePoint: Int): Boolean {
        return MyCharacter.isVariationSelector(codePoint) ||
                MyCharacter.isFitzpatrick(codePoint) ||
                MyCharacter.isZWJ(codePoint) ||
                MyCharacter.isZWNJ(codePoint) ||
                MyCharacter.couldBeEmoji(codePoint)
    }
}

/**
 * Check if this codepoint represents a combining character.
 */
inline val Int.isCombiningCharacter: Boolean get() = GraphicCharacter.isCombiningCharacter(this)

/**
 * Check if this codepoint is an ASCII combining symbol.
 */
inline val Int.isAsciiCombiningSymbol: Boolean get() = GraphicCharacter.isASCIICombiningSymbol(this)

/**
 * Check if this codepoint could be part of an emoji sequence.
 */
inline val Int.couldBeEmojiPart: Boolean get() = GraphicCharacter.couldBeEmojiPart(this)

