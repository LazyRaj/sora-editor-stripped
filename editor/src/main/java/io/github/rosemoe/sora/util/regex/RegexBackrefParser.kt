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

class RegexBackrefParser(val grammar: RegexBackrefGrammar) {

    fun parse(pattern: String, groupCount: Int): List<RegexBackrefToken> {
        val workingPattern = pattern + '\u0000' // add an extra char to truncate trailing backref
        val result = mutableListOf<RegexBackrefToken>()
        val escapeChar = grammar.escapeChar
        val backrefChar = grammar.backrefStartChar
        var index = 0
        val len = workingPattern.length
        // State 0: Text
        // State 1: Right after escape character
        // State 2: Require first digit for backref
        // State 3: Scan reset digits for backref
        var state = 0
        var textStart = 0
        var currentGroup = 0L

        while (index < len) {
            val ch = workingPattern[index]
            when (state) {
                0 -> {
                    if (ch == escapeChar) {
                        result.add(RegexBackrefToken(false, workingPattern.substring(textStart, index), -1))
                        state = 1
                    } else if (ch == backrefChar) {
                        result.add(RegexBackrefToken(false, workingPattern.substring(textStart, index), -1))
                        state = 2
                    }
                }
                1 -> {
                    if (ch == escapeChar || ch == backrefChar) {
                        result.add(RegexBackrefToken(false, ch.toString(), -1))
                    } else {
                        result.add(RegexBackrefToken(false, workingPattern.substring(index - 1, index + 1), -1))
                    }
                    state = 0
                    textStart = index + 1
                }
                2 -> {
                    if (ch in '0'..'9') {
                        currentGroup = (ch - '0').toLong()
                        if (currentGroup <= groupCount) {
                            state = 3 // scan rest digits
                        } else {
                            // not backref, fallback to plain text
                            textStart = index - 1
                            index--
                            state = 0
                        }
                    } else {
                        // not backref, fallback to plain text
                        textStart = index - 1
                        index--
                        state = 0
                    }
                }
                3 -> {
                    if (ch in '0'..'9') {
                        val newGroup = currentGroup * 10 + (ch - '0')
                        if (newGroup <= groupCount) {
                            currentGroup = newGroup
                        } else {
                            result.add(RegexBackrefToken(true, null, currentGroup.toInt()))
                            textStart = index
                            state = 0
                        }
                    } else {
                        result.add(RegexBackrefToken(true, null, currentGroup.toInt()))
                        textStart = index
                        state = 0
                    }
                }
            }
            index++
        }

        if (state != 0) {
            throw IllegalArgumentException("illegal backref expression")
        } else {
            result.add(RegexBackrefToken(false, workingPattern.substring(textStart, len - 1), -1))
        }

        return result
    }
}
