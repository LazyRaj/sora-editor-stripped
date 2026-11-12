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
package io.github.rosemoe.sora.util.regex

import java.util.regex.Matcher

object RegexBackrefHelper {

    @JvmStatic
    fun computeReplacement(
        matcher: Matcher,
        grammar: RegexBackrefGrammar,
        replacementPattern: String
    ): String {
        val parser = RegexBackrefParser(grammar)
        val tokens = parser.parse(replacementPattern, matcher.groupCount())
        return computeReplacement(matcher, tokens)
    }

    @JvmStatic
    fun computeReplacement(
        matcher: Matcher,
        tokens: List<RegexBackrefToken>
    ): String {
        val sb = StringBuilder()
        for (token in tokens) {
            if (token.isReference) {
                val text = matcher.group(token.group)
                sb.append(text ?: "")
            } else {
                sb.append(token.text ?: "")
            }
        }
        return sb.toString()
    }
}
