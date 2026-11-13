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
/*
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.widget.snippet

import io.github.rosemoe.sora.lang.completion.snippet.ConditionalFormat
import io.github.rosemoe.sora.lang.completion.snippet.FormatString
import io.github.rosemoe.sora.lang.completion.snippet.NextUpperCaseFormat
import io.github.rosemoe.sora.lang.completion.snippet.NoFormat
import io.github.rosemoe.sora.lang.completion.snippet.Transform
import io.github.rosemoe.sora.util.MyCharacter.isAlpha
import java.util.Locale
import java.util.regex.Matcher

/**
 * Utility class for applying [Transform] objects
 *
 * @author Rosemoe
 */
object TransformApplier {

    /**
     * Apply the given [Transform] to the text and return transform result
     *
     * @param text the text to be transformed. must not be null
     * @param transform the [Transform] object describing how to transform the text, maybe null
     * @return the transformed text
     */
    @JvmStatic
    fun doTransform(text: String, transform: Transform?): String {
        val regexp = transform?.regexp ?: return text
        val format = transform.format ?: return text
        val sb = StringBuilder()
        val matcher = regexp.matcher(text)
        var loopCount = 0
        val limit = if (transform.globalMode) Int.MAX_VALUE else 1
        var nextIndex = 0
        while (loopCount < limit && nextIndex < text.length) {
            if (matcher.find(nextIndex)) {
                val start = matcher.start()
                val end = matcher.end()
                sb.append(text, nextIndex, start)
                sb.append(applySingle(matcher, format))
                nextIndex = end
            } else {
                break
            }
            loopCount++
        }
        if (nextIndex < text.length) {
            sb.append(text, nextIndex, text.length)
        }
        return sb.toString()
    }

    /**
     * Generate text for the given region in Matcher.
     *
     * @param matcher the Matcher at the requested region
     * @param formatStringList the format descriptors
     * @return generated(transform) text
     */
    private fun applySingle(matcher: Matcher, formatStringList: List<FormatString>): CharSequence {
        val sb = StringBuilder()
        var nextUpperCase = false
        for (formatString in formatStringList) {
            if (formatString is NoFormat) {
                sb.append(applyFirstUpperCase(formatString.text, nextUpperCase))
            } else if (formatString is ConditionalFormat) {
                val group = matcher.group(formatString.group)
                val shorthand = formatString.shorthand
                if (shorthand != null) {
                    if (group != null) {
                        when (shorthand) {
                            "upcase" -> sb.append(
                                applyFirstUpperCase(
                                    group.uppercase(Locale.ROOT),
                                    nextUpperCase
                                )
                            )

                            "lowcase" -> sb.append(
                                applyFirstUpperCase(
                                    group.lowercase(Locale.ROOT),
                                    nextUpperCase
                                )
                            )

                            else -> sb.append(applyFirstUpperCase(group, nextUpperCase))
                        }
                    }
                } else {
                    val ifValue = formatString.ifValue ?: group
                    val elseValue = formatString.elseValue ?: ""
                    sb.append(
                        applyFirstUpperCase(
                            if (group != null) ifValue else elseValue,
                            nextUpperCase
                        )
                    )
                }
            }
            nextUpperCase = formatString is NextUpperCaseFormat
        }
        return sb
    }

    /**
     * Convenient method for applying upper case of first character only
     */
    private fun applyFirstUpperCase(text: String?, apply: Boolean): String? {
        if (apply && !text.isNullOrEmpty() && isAlpha(text[0])) {
            return Character.toUpperCase(text[0]).toString() + text.substring(1)
        }
        return text
    }
}
