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

import android.graphics.Typeface
import android.text.GetChars
import android.text.TextUtils
import io.github.rosemoe.sora.text.FunctionCharacters
import io.github.rosemoe.sora.util.TemporaryCharBuffer
import io.github.rosemoe.sora.util.use

open class Paint(
    var isRenderFunctionCharacters: Boolean = false
) : android.graphics.Paint() {

    var spaceWidth: Float = 0f
        private set

    init {
        spaceWidth = measureText(" ")
    }

    fun onAttributeUpdate() {
        spaceWidth = measureText(" ")
    }

    fun setTypefaceWrapped(typeface: Typeface?) {
        super.setTypeface(typeface)
        onAttributeUpdate()
    }

    fun setTextSizeWrapped(textSize: Float) {
        super.setTextSize(textSize)
        onAttributeUpdate()
    }

    fun setFontFeatureSettingsWrapped(settings: String?) {
        super.setFontFeatureSettings(settings)
        onAttributeUpdate()
    }

    override fun setLetterSpacing(letterSpacing: Float) {
        super.setLetterSpacing(letterSpacing)
        onAttributeUpdate()
    }

    fun myGetTextRunAdvances(
        chars: GetChars,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        isRtl: Boolean,
        advances: FloatArray?,
        advancesIndex: Int
    ): Float {
        return TemporaryCharBuffer.use(contextCount) { buffer ->
            TextUtils.getChars(chars, contextIndex, contextIndex + contextCount, buffer, 0)
            var advance = getTextRunAdvances(
                buffer,
                index - contextIndex,
                count,
                0,
                contextCount,
                isRtl,
                advances,
                advancesIndex
            )
            if (isRenderFunctionCharacters) {
                for (i in 0 until count) {
                    val ch = chars[index + i]
                    if (FunctionCharacters.isEditorFunctionChar(ch)) {
                        val width = measureText(FunctionCharacters.getNameForFunctionCharacter(ch))
                        if (advances != null) {
                            advance -= advances[advancesIndex + i]
                            advances[advancesIndex + i] = width
                        } else {
                            advance -= measureText(ch.toString())
                        }
                        advance += width
                    }
                }
            }
            advance
        }
    }

    /**
     * Get the advance of text with the context positions related to shaping the characters
     */
    fun measureTextRunAdvance(
        text: GetChars,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean
    ): Float {
        return myGetTextRunAdvances(
            text,
            start,
            end - start,
            contextStart,
            contextEnd - contextStart,
            isRtl,
            null,
            0
        )
    }

    /**
     * Find offset for a certain advance returned by [measureTextRunAdvance]
     */
    fun findOffsetByRunAdvance(
        text: GetChars,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        advance: Float
    ): Int {
        if (isRenderFunctionCharacters) {
            var lastEnd = start
            var current = 0f
            for (i in start until end) {
                val ch = text[i]
                if (FunctionCharacters.isEditorFunctionChar(ch)) {
                    val result = if (lastEnd == i) {
                        i
                    } else {
                        breakTextImpl(
                            text,
                            lastEnd,
                            i,
                            contextStart,
                            contextEnd,
                            isRtl,
                            advance - current
                        )
                    }
                    if (result < i) {
                        return result
                    }
                    current += measureTextRunAdvance(
                        text,
                        lastEnd,
                        i,
                        contextStart,
                        contextEnd,
                        isRtl
                    )
                    current += measureText(FunctionCharacters.getNameForFunctionCharacter(ch))
                    if (current >= advance) {
                        return i
                    }
                    lastEnd = i + 1
                }
            }
            if (lastEnd < end) {
                return breakTextImpl(
                    text,
                    lastEnd,
                    end,
                    contextStart,
                    contextEnd,
                    isRtl,
                    advance - current
                )
            }
            return end
        } else {
            return breakTextImpl(text, start, end, contextStart, contextEnd, isRtl, advance)
        }
    }

    private fun breakTextImpl(
        text: GetChars,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        advance: Float
    ): Int {
        val count = contextEnd - contextStart
        return TemporaryCharBuffer.use(count) { buffer ->
            TextUtils.getChars(text, contextStart, contextEnd, buffer, 0)
            contextStart + getOffsetForAdvance(
                buffer,
                start - contextStart,
                end - contextStart,
                0,
                count,
                isRtl,
                advance
            )
        }
    }
}
