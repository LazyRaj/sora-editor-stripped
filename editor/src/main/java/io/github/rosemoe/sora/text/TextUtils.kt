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

package io.github.rosemoe.sora.text

import android.icu.lang.UCharacter
import androidx.annotation.IntRange
import io.github.rosemoe.sora.util.IntPair
import kotlin.math.max
import kotlin.math.min

/**
 * Utility class for texts
 */
object TextUtils {

    private const val LINE_FEED = 0x0A
    private const val CARRIAGE_RETURN = 0x0D

    /**
     * Counts the number of whitespaces at the start of the given [CharSequence].
     *
     * @param text The text to count the spaces in.
     * @return A long packed with the number of spaces and tabs at the start of the line.
     * Use [IntPair.getFirst] to get the number of spaces and [IntPair.getSecond]
     * for the number of tabs.
     */
    fun countLeadingSpacesAndTabs(text: CharSequence): Long {
        var p = 0
        var spaces = 0
        var tabs = 0
        while (p < text.length && isWhitespace(text[p])) {
            val c = text[p]
            if (c == '\t') {
                tabs += 1
            } else {
                spaces += 1
            }
            ++p
        }
        return IntPair.pack(spaces, tabs)
    }

    /**
     * Compute leading space count
     *
     * @param tabWidth Tab is considered in `tabWidth` spaces
     */
    fun countLeadingSpaceCount(text: CharSequence, tabWidth: Int): Int {
        val result = countLeadingSpacesAndTabs(text)
        return IntPair.getFirst(result) + (tabWidth * IntPair.getSecond(result))
    }

    /**
     * Create indent space
     *
     * @return Generated space string
     */
    fun createIndent(indentSize: Int, tabWidth: Int, useTab: Boolean): String {
        val size = max(0, indentSize)
        val tab: Int
        val space: Int
        if (useTab) {
            tab = size / tabWidth
            space = size % tabWidth
        } else {
            tab = 0
            space = size
        }
        val s = StringBuilder(tab + space)
        for (i in 0 until tab) {
            s.append('\t')
        }
        for (i in 0 until space) {
            s.append(' ')
        }
        return s.toString()
    }

    fun indexOf(
        text: CharSequence,
        pattern: CharSequence,
        ignoreCase: Boolean,
        fromIndex: Int
    ): Int {
        val max = text.length - pattern.length
        val len = pattern.length
        for (i in fromIndex..max) {
            var match = true
            for (j in 0 until len) {
                val s = text[i + j]
                val p = pattern[j]
                if (!(s == p || (ignoreCase && s.lowercaseChar() == p.lowercaseChar()))) {
                    match = false
                    break
                }
            }
            if (match) {
                return i
            }
        }
        return -1
    }

    fun lastIndexOf(
        text: CharSequence,
        pattern: CharSequence,
        ignoreCase: Boolean,
        fromIndex: Int
    ): Int {
        val len = pattern.length
        val actualFromIndex = min(fromIndex, text.length - len)
        for (i in actualFromIndex downTo 0) {
            var match = true
            for (j in 0 until len) {
                val s = text[i + j]
                val p = pattern[j]
                if (!(s == p || (ignoreCase && s.lowercaseChar() == p.lowercaseChar()))) {
                    match = false
                    break
                }
            }
            if (match) {
                return i
            }
        }
        return -1
    }

    fun startsWith(text: CharSequence, pattern: CharSequence, ignoreCase: Boolean): Boolean {
        if (text.length < pattern.length) {
            return false
        }
        val len = pattern.length
        for (i in 0 until len) {
            val s = text[i]
            val p = pattern[i]
            if (!(s == p || (ignoreCase && s.lowercaseChar() == p.lowercaseChar()))) {
                return false
            }
        }
        return true
    }

    fun isWhitespace(ch: Char): Boolean {
        return ch == '\t' || ch == ' '
    }

    fun padStart(src: String, padChar: Char, length: Int): String {
        if (src.length >= length) {
            return src
        }
        val sb = StringBuilder(length)
        for (i in 0 until length - src.length) {
            sb.append(padChar)
        }
        sb.append(src)
        return sb.toString()
    }

    /**
     * Find where leading spaces end and trailing spaces start
     *
     * @param line The line to search
     */
    fun findLeadingAndTrailingWhitespacePos(line: CharSequence): Long {
        return findLeadingAndTrailingWhitespacePos(line, 0, line.length)
    }

    /**
     * Find where leading spaces end and trailing spaces start
     *
     * @param line Range text
     * @param start Range start (inclusive)
     * @param end Range end (exclusive)
     */
    fun findLeadingAndTrailingWhitespacePos(line: CharSequence, start: Int, end: Int): Long {
        var leading = start
        var trailing = end
        while (leading < end && isWhitespace(line[leading])) {
            leading++
        }
        if (leading != end) {
            while (trailing > 0 && isWhitespace(line[trailing - 1])) {
                trailing--
            }
        }
        return IntPair.pack(leading, trailing)
    }

    fun trimToSize(text: CharSequence?, @IntRange(from = 1) size: Int): CharSequence? {
        require(size > 0) { "size must be bigger than 0" }
        if (text == null || text.length <= size) return text
        var targetSize = size
        if (Character.isHighSurrogate(text[targetSize - 1]) && Character.isLowSurrogate(text[targetSize])) {
            targetSize -= 1
        }
        return text.subSequence(0, targetSize)
    }

    // From AOSP, BaseKeyListener.java

    /**
     * Returns the start offset to be deleted by a backspace key from the given offset.
     */
    fun getOffsetForBackspaceKey(text: CharSequence, offset: Int): Int {
        if (offset <= 1) {
            return 0
        }

        // Initial state
        val stateStart = 0
        val stateLf = 1
        val stateBeforeKeycap = 2
        val stateBeforeVsAndKeycap = 3
        val stateBeforeEmojiModifier = 4
        val stateBeforeVsAndEmojiModifier = 5
        val stateBeforeVs = 6
        val stateBeforeEmoji = 7
        val stateBeforeZwj = 8
        val stateBeforeVsAndZwj = 9
        val stateOddNumberedRis = 10
        val stateEvenNumberedRis = 11
        val stateInTagSequence = 12
        val stateFinished = 13

        var deleteCharCount = 0
        var lastSeenVSCharCount = 0
        var state = stateStart
        var tmpOffset = offset

        do {
            val codePoint = Character.codePointBefore(text, tmpOffset)
            tmpOffset -= Character.charCount(codePoint)

            when (state) {
                stateStart -> {
                    deleteCharCount = Character.charCount(codePoint)
                    state = when {
                        codePoint == LINE_FEED -> stateLf
                        AndroidEmoji.isVariationSelector(codePoint) -> stateBeforeVs
                        AndroidEmoji.isRegionalIndicatorSymbol(codePoint) -> stateOddNumberedRis
                        AndroidEmoji.isEmojiModifier(codePoint) -> stateBeforeEmojiModifier
                        codePoint == AndroidEmoji.COMBINING_ENCLOSING_KEYCAP -> stateBeforeKeycap
                        AndroidEmoji.isEmoji(codePoint) -> stateBeforeEmoji
                        codePoint == AndroidEmoji.CANCEL_TAG -> stateInTagSequence
                        else -> stateFinished
                    }
                }

                stateLf -> {
                    if (codePoint == CARRIAGE_RETURN) {
                        ++deleteCharCount
                    }
                    state = stateFinished
                }

                stateOddNumberedRis -> {
                    if (AndroidEmoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount += 2
                        state = stateEvenNumberedRis
                    } else {
                        state = stateFinished
                    }
                }

                stateEvenNumberedRis -> {
                    if (AndroidEmoji.isRegionalIndicatorSymbol(codePoint)) {
                        deleteCharCount -= 2
                        state = stateOddNumberedRis
                    } else {
                        state = stateFinished
                    }
                }

                stateBeforeKeycap -> {
                    if (AndroidEmoji.isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint)
                        state = stateBeforeVsAndKeycap
                    } else {
                        if (AndroidEmoji.isKeycapBase(codePoint)) {
                            deleteCharCount += Character.charCount(codePoint)
                        }
                        state = stateFinished
                    }
                }

                stateBeforeVsAndKeycap -> {
                    if (AndroidEmoji.isKeycapBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint)
                    }
                    state = stateFinished
                }

                stateBeforeEmojiModifier -> {
                    if (AndroidEmoji.isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint)
                        state = stateBeforeVsAndEmojiModifier
                    } else if (AndroidEmoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint)
                        state = stateBeforeEmoji
                    } else {
                        state = stateFinished
                    }
                }

                stateBeforeVsAndEmojiModifier -> {
                    if (AndroidEmoji.isEmojiModifierBase(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + Character.charCount(codePoint)
                    }
                    state = stateFinished
                }

                stateBeforeVs -> {
                    if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint)
                        state = stateBeforeEmoji
                    } else {
                        if (!AndroidEmoji.isVariationSelector(codePoint) && UCharacter.getCombiningClass(
                                codePoint
                            ) == 0
                        ) {
                            deleteCharCount += Character.charCount(codePoint)
                        }
                        state = stateFinished
                    }
                }

                stateBeforeEmoji -> {
                    state = if (codePoint == AndroidEmoji.ZERO_WIDTH_JOINER) {
                        stateBeforeZwj
                    } else {
                        stateFinished
                    }
                }

                stateBeforeZwj -> {
                    if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint) + 1
                        state = if (AndroidEmoji.isEmojiModifier(codePoint)) {
                            stateBeforeEmojiModifier
                        } else {
                            stateBeforeEmoji
                        }
                    } else if (AndroidEmoji.isVariationSelector(codePoint)) {
                        lastSeenVSCharCount = Character.charCount(codePoint)
                        state = stateBeforeVsAndZwj
                    } else {
                        state = stateFinished
                    }
                }

                stateBeforeVsAndZwj -> {
                    if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += lastSeenVSCharCount + 1 + Character.charCount(codePoint)
                        lastSeenVSCharCount = 0
                        state = stateBeforeEmoji
                    } else {
                        state = stateFinished
                    }
                }

                stateInTagSequence -> {
                    if (AndroidEmoji.isTagSpecChar(codePoint)) {
                        deleteCharCount += 2
                    } else if (AndroidEmoji.isEmoji(codePoint)) {
                        deleteCharCount += Character.charCount(codePoint)
                        state = stateFinished
                    } else {
                        deleteCharCount = 2
                        state = stateFinished
                    }
                }

                else -> throw IllegalArgumentException("state $state is unknown")
            }
        } while (tmpOffset > 0 && state != stateFinished)

        return offset - deleteCharCount
    }
}

/**
 * Check if character is a whitespace character (' ' or '\t').
 */
inline val Char.isWhitespaceChar: Boolean
    get() = TextUtils.isWhitespace(this)

/**
 * Extension on [CharSequence] to count leading spaces and tabs.
 */
fun CharSequence.countLeadingSpacesAndTabs(): Long = TextUtils.countLeadingSpacesAndTabs(this)

/**
 * Extension on [CharSequence] to count leading spaces with given [tabWidth].
 */
fun CharSequence.countLeadingSpaceCount(tabWidth: Int): Int = TextUtils.countLeadingSpaceCount(this, tabWidth)

