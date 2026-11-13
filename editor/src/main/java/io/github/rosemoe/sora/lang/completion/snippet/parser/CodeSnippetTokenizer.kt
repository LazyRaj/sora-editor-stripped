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
package io.github.rosemoe.sora.lang.completion.snippet.parser

class CodeSnippetTokenizer(input: String) {

    private var index: Int = 0
    private var length: Int = 0
    private val value: String = input
    var token: TokenType = TokenType.EOF
        private set

    fun nextToken(): Token {
        token = nextTokenInternal()
        return Token(index, length, token)
    }

    private fun nextTokenInternal(): TokenType {
        index += length
        length = 0
        if (index >= value.length) {
            return TokenType.EOF
        }
        var ch = value[index]

        val code = ch.code
        val staticType = if (code < 128) staticTypes[code] else null
        if (staticType != null) {
            length = 1
            return staticType
        }

        if (isDigitChar(ch)) {
            length = 1
            while (index + length < value.length && isDigitChar(value[index + length])) {
                length++
            }
            return TokenType.Int
        }

        if (isVariableChar(ch)) {
            length = 1
            while (index + length < value.length) {
                ch = value[index + length]
                if (isVariableChar(ch) || isDigitChar(ch)) {
                    length++
                } else {
                    break
                }
            }
            return TokenType.VariableName
        }

        while (index + length < value.length) {
            ch = value[index + length]
            if (isDigitChar(ch) || isVariableChar(ch) || (ch.code < 128 && staticTypes[ch.code] != null)) {
                break
            }
            length++
        }
        return TokenType.Format
    }

    fun moveTo(index: Int) {
        this.index = index
        length = 0
    }

    val tokenText: String
        get() = value.substring(index, index + length)

    val tokenLength: Int
        get() = length

    val tokenStartIndex: Int
        get() = index

    val tokenEndIndex: Int
        get() = index + length

    companion object {
        private val staticTypes = Array<TokenType?>(128) { null }.apply {
            for (value in TokenType.entries) {
                val ch = value.targetCharacter
                if (ch != '\u0000' && ch.code < 128) {
                    this[ch.code] = value
                }
            }
        }

        private fun isDigitChar(ch: Char): Boolean = ch.isDigit()

        private fun isVariableChar(ch: Char): Boolean {
            return (ch in 'a'..'z') || (ch in 'A'..'Z') || ch == '_'
        }
    }
}
