/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

package io.github.rosemoe.sora.text.emoji

import android.icu.lang.UCharacter
import android.icu.lang.UProperty

internal class PlatformEmojiPropertyProvider private constructor() : EmojiPropertyProvider {

    override fun isVariationSelector(codePoint: Int): Boolean {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.VARIATION_SELECTOR)
    }

    override fun isEmoji(codePoint: Int): Boolean {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI)
    }

    override fun isEmojiModifier(codePoint: Int): Boolean {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER)
    }

    override fun isEmojiModifierBase(codePoint: Int): Boolean {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER_BASE)
    }

    companion object {
        @JvmField
        val INSTANCE: PlatformEmojiPropertyProvider = PlatformEmojiPropertyProvider()
    }
}
