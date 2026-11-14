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
package io.github.rosemoe.sora.lang.completion

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import io.github.rosemoe.sora.text.TextUtils

/**
 * Utility class to provide some useful matching functions in generating completion.
 *
 * @author Rosemoe
 */
open class MatchHelper {

    /**
     * Color for matched text highlighting
     */
    var highlightColor: Int = -0xc0ae4b // 0xff3f51b5.toInt()

    /**
     * Case in-sensitive
     */
    var ignoreCase: Boolean = false

    /**
     * Match case of first letter if ignoreCase=true
     *
     * for [startsWith] only
     */
    var matchFirstCase: Boolean = false

    open fun startsWith(name: CharSequence, pattern: CharSequence): Spanned? {
        return startsWith(name, pattern, matchFirstCase, ignoreCase)
    }

    open fun startsWith(
        name: CharSequence,
        pattern: CharSequence,
        matchFirstCase: Boolean,
        ignoreCase: Boolean
    ): Spanned? {
        if (name.length >= pattern.length) {
            val len = pattern.length
            var matches = true
            for (i in 0 until len) {
                val a = name[i]
                val b = pattern[i]
                if (!(a == b || ((ignoreCase && (i != 0 || !matchFirstCase)) && a.lowercaseChar() == b.lowercaseChar()))) {
                    matches = false
                    break
                }
            }
            if (matches) {
                val spanned = SpannableString(name)
                spanned.setSpan(
                    ForegroundColorSpan(highlightColor),
                    0,
                    len,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return spanned
            }
        }
        return null
    }

    open fun contains(name: CharSequence, pattern: CharSequence): Spanned? {
        return contains(name, pattern, ignoreCase)
    }

    open fun contains(name: CharSequence, pattern: CharSequence, ignoreCase: Boolean): Spanned? {
        val index = TextUtils.indexOf(name, pattern, ignoreCase, 0)
        if (index != -1) {
            val spanned = SpannableString(name)
            spanned.setSpan(
                ForegroundColorSpan(highlightColor),
                index,
                index + pattern.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spanned
        }
        return null
    }

    /**
     * Common sub-sequence
     */
    open fun commonSub(name: CharSequence, pattern: CharSequence): Spanned? {
        return commonSub(name, pattern, ignoreCase)
    }

    /**
     * Common sub-sequence
     */
    open fun commonSub(name: CharSequence, pattern: CharSequence, ignoreCase: Boolean): Spanned? {
        if (name.length >= pattern.length) {
            var spanned: SpannableString? = null
            val len = pattern.length
            var j = 0
            for (i in 0 until len) {
                val p = pattern[i]
                var matched = false
                while (j < name.length && !matched) {
                    val s = name[j]
                    if (s == p || (ignoreCase && s.lowercaseChar() == p.lowercaseChar())) {
                        matched = true
                        if (spanned == null) {
                            spanned = SpannableString(name)
                        }
                        spanned.setSpan(
                            ForegroundColorSpan(highlightColor),
                            j,
                            j + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    j++
                }
                if (!matched) {
                    return null
                }
            }
            return spanned
        }
        return null
    }
}
