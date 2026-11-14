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

import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet
import io.github.rosemoe.sora.lang.completion.snippet.ConditionalFormat
import io.github.rosemoe.sora.lang.completion.snippet.FormatString
import io.github.rosemoe.sora.lang.completion.snippet.NextUpperCaseFormat
import io.github.rosemoe.sora.lang.completion.snippet.NoFormat
import io.github.rosemoe.sora.lang.completion.snippet.PlaceHolderElement
import io.github.rosemoe.sora.lang.completion.snippet.PlaceholderDefinition
import io.github.rosemoe.sora.lang.completion.snippet.PlainPlaceholderElement
import io.github.rosemoe.sora.lang.completion.snippet.Transform
import io.github.rosemoe.sora.lang.completion.snippet.VariableItem
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class CodeSnippetParser private constructor(
    private val src: String,
    definitions: List<PlaceholderDefinition>
) {

    private val builder: CodeSnippet.Builder = CodeSnippet.Builder(
        (definitions as? MutableList<PlaceholderDefinition>) ?: ArrayList(definitions)
    )
    private val tokenizer: CodeSnippetTokenizer = CodeSnippetTokenizer(src)
    private var token: Token = Token(0, 0, TokenType.EOF)

    private fun next() {
        if (token.type == TokenType.EOF) {
            return
        }
        token = tokenizer.nextToken()
    }

    private fun accept(type: TokenType): Boolean {
        if (token.type == type) {
            next()
            return true
        }
        return false
    }

    private fun acceptText(type: TokenType): String? {
        if (token.type == type) {
            val text = src.substring(token.index, token.index + token.length)
            next()
            return text
        }
        return null
    }

    private fun acceptAny(vararg types: TokenType): Boolean {
        for (type in types) {
            if (token.type == type) {
                next()
                return true
            }
        }
        return false
    }

    private fun acceptAnyText(vararg types: TokenType): String? {
        for (type in types) {
            if (token.type == type) {
                val text = src.substring(token.index, token.index + token.length)
                next()
                return text
            }
        }
        return null
    }

    private fun consumeText(): String {
        val text = src.substring(token.index, token.index + token.length)
        next()
        return text
    }

    private fun backTo(token: Token) {
        tokenizer.moveTo(token.index + token.length)
        this.token = token
    }

    private fun parse() {
        token = tokenizer.nextToken()
        while (parseInternal()) {
            // empty
        }
    }

    private fun parseInternal(): Boolean {
        return parseEscaped() ||
                parseTabStopOrVariableName() ||
                parseComplexVariable() ||
                parseComplexPlaceholder() ||
                parseInterpolatedShell() ||
                parseAnything()
    }

    private fun parseEscaped(): Boolean {
        if (accept(TokenType.Backslash)) {
            var escaped = acceptAnyText(
                TokenType.CurlyClose,
                TokenType.Dollar,
                TokenType.Backslash,
                TokenType.Backtick
            )
            if (escaped == null) {
                escaped = "\\"
            }
            builder.addPlainText(escaped)
            return true
        }
        return false
    }

    private fun parseInterpolatedShell(): Boolean {
        val backup = token
        if (accept(TokenType.Backtick)) {
            val sb = StringBuilder()
            while (!accept(TokenType.Backtick)) {
                if (accept(TokenType.Backslash)) {
                    if (accept(TokenType.Backtick)) {
                        sb.append('`')
                    } else {
                        sb.append('\\')
                    }
                } else if (token.type == TokenType.EOF) {
                    backTo(backup)
                    return false
                } else {
                    sb.append(consumeText())
                }
            }
            builder.addInterpolatedShell(sb.toString())
            return true
        }
        backTo(token)
        return false
    }

    private fun parseTabStopOrVariableName(): Boolean {
        val backup = token
        if (accept(TokenType.Dollar)) {
            val intText = acceptText(TokenType.Int)
            if (intText != null) {
                builder.addPlaceholder(intText.toInt())
                return true
            }
            val varText = acceptText(TokenType.VariableName)
            if (varText != null) {
                builder.addVariable(varText, null as String?)
                return true
            }
            backTo(backup)
            return false
        }
        return false
    }

    private fun parseComplexVariable(): Boolean {
        val variable = parseComplexVariableInternal()
        if (variable != null) {
            builder.addVariable(variable)
        }
        return variable != null
    }

    private fun parseComplexVariableInternal(): VariableItem? {
        val backup = token
        if (accept(TokenType.Dollar) && accept(TokenType.CurlyOpen)) {
            val variableName = acceptText(TokenType.VariableName)
            if (variableName != null) {
                if (accept(TokenType.Colon)) {
                    // ${name:xxx}
                    val sb = StringBuilder()
                    while (!accept(TokenType.CurlyClose)) {
                        if (accept(TokenType.Backslash)) {
                            val text = acceptAnyText(
                                TokenType.Backslash,
                                TokenType.Dollar,
                                TokenType.CurlyClose
                            )
                            if (text != null) {
                                sb.append(text)
                            } else {
                                sb.append('\\')
                            }
                        } else if (token.type == TokenType.EOF) {
                            backTo(backup)
                            return null
                        } else {
                            sb.append(src, token.index, token.index + token.length)
                            next()
                        }
                    }
                    return VariableItem(-1, variableName, sb.toString())
                } else if (accept(TokenType.Forwardslash)) {
                    // ${name/regexp/format/options}
                    val transform = Transform()
                    if (parseTransform(transform)) {
                        return VariableItem(-1, variableName, null, transform)
                    }
                    backTo(backup)
                    return null
                } else if (accept(TokenType.CurlyClose)) {
                    // ${name}
                    return VariableItem(-1, variableName, "")
                } else {
                    // missing token
                    backTo(backup)
                    return null
                }
            }
        }
        backTo(backup)
        return null
    }

    private fun parseComplexPlaceholder(): Boolean {
        val backup = token
        if (accept(TokenType.Dollar) && accept(TokenType.CurlyOpen)) {
            val idText = acceptText(TokenType.Int)
            if (idText != null) {
                val id = idText.toInt()
                if (accept(TokenType.Colon)) {
                    // ${1:xxx}
                    val elements = ArrayList<PlaceHolderElement>()
                    while (!accept(TokenType.CurlyClose)) {
                        if (accept(TokenType.Backslash)) {
                            val text = acceptAnyText(
                                TokenType.Backslash,
                                TokenType.Dollar,
                                TokenType.CurlyClose
                            )
                            val t = text ?: "\\"
                            appendPlaceholderElement(elements, t)
                        } else if (token.type == TokenType.EOF) {
                            backTo(backup)
                            return false
                        } else {
                            val v = parseSimpleVariableName()
                            if (v != null) {
                                elements.add(VariableItem(token.index, v, ""))
                                continue
                            }

                            val vi = parseComplexVariableInternal()
                            if (vi != null) {
                                vi.setIndex(token.index)
                                elements.add(vi)
                                continue
                            }

                            val t = src.substring(token.index, token.index + token.length)
                            appendPlaceholderElement(elements, t)
                            next()
                        }
                    }
                    builder.addComplexPlaceholder(id, elements)
                } else if (accept(TokenType.Pipe)) {
                    // ${1|one,two,three|}
                    val choices = ArrayList<String>()
                    while (true) {
                        if (parseChoiceElement(choices)) {
                            if (accept(TokenType.Comma)) {
                                continue
                            }
                            if (accept(TokenType.Pipe) && accept(TokenType.CurlyClose)) {
                                builder.addPlaceholder(id, choices)
                                return true
                            }
                        }
                        backTo(backup)
                        return false
                    }
                } else if (accept(TokenType.Forwardslash)) {
                    // ${1/regexp/format/options}
                    val transform = Transform()
                    if (parseTransform(transform)) {
                        builder.addPlaceholder(id, transform)
                        return true
                    }
                    backTo(backup)
                    return false
                } else if (accept(TokenType.CurlyClose)) {
                    // ${1}
                    builder.addPlaceholder(id)
                } else {
                    // missing token
                    backTo(backup)
                    return false
                }
                return true
            }
        }
        backTo(backup)
        return false
    }

    private fun parseSimpleVariableName(): String? {
        val backup = token
        if (accept(TokenType.Dollar)) {
            // Check for : $VARIABLE_NAME
            val v = acceptText(TokenType.VariableName)
            if (v != null) {
                return v
            }
        }
        backTo(backup)
        return null
    }

    private fun parseChoiceElement(choices: MutableList<String>): Boolean {
        val backup = token
        val sb = StringBuilder()
        while (token.type != TokenType.Comma && token.type != TokenType.Pipe) {
            if (accept(TokenType.Backslash)) {
                val text = acceptAnyText(TokenType.Pipe, TokenType.Comma, TokenType.Backslash)
                if (text != null) {
                    sb.append(text)
                } else {
                    sb.append('\\')
                }
            } else if (token.type != TokenType.EOF) {
                sb.append(consumeText())
            } else {
                backTo(backup)
                return false
            }
        }
        if (sb.isEmpty()) {
            backTo(backup)
            return false
        }
        choices.add(sb.toString())
        return true
    }

    private fun parseTransform(transform: Transform): Boolean {
        // ...<regex>/<format>/<options>}
        val backup = token

        // (1) /regex
        val regexValue = StringBuilder()
        while (!accept(TokenType.Forwardslash)) {
            if (accept(TokenType.Backslash)) {
                if (accept(TokenType.Forwardslash)) {
                    regexValue.append('/')
                } else {
                    regexValue.append('\\')
                }
                continue
            }

            if (token.type != TokenType.EOF) {
                regexValue.append(consumeText())
                continue
            }

            return false
        }

        // (2) /format
        val list = ArrayList<FormatString>()
        while (!accept(TokenType.Forwardslash)) {
            if (accept(TokenType.Backslash)) {
                val escaped = acceptAnyText(TokenType.Backslash, TokenType.Forwardslash)
                if (escaped != null) {
                    list.add(NoFormat(escaped))
                } else {
                    val varName = acceptText(TokenType.VariableName)
                    if (varName != null) {
                        if ("u" == varName) {
                            list.add(NextUpperCaseFormat())
                        } else {
                            list.add(NoFormat("\\$varName"))
                        }
                    } else {
                        list.add(NoFormat("\\"))
                    }
                }
                continue
            }

            if (parseFormatString(list) || parseAnything(list)) {
                continue
            }
            return false
        }

        // (3) /option
        val regexOptions = StringBuilder()
        while (!accept(TokenType.CurlyClose)) {
            if (token.type != TokenType.EOF) {
                regexOptions.append(consumeText())
                continue
            }
            return false
        }

        try {
            var options = 0
            if (regexOptions.indexOf("i") != -1) {
                options = options or Pattern.CASE_INSENSITIVE
            }
            if (regexOptions.indexOf("m") != -1) {
                options = options or Pattern.MULTILINE
            }
            transform.globalMode = regexOptions.indexOf("g") != -1
            transform.regexp = Pattern.compile(regexValue.toString(), options)
        } catch (e: PatternSyntaxException) {
            backTo(backup)
            return false
        }
        transform.format = list
        return true
    }

    private fun parseFormatString(formatStrings: MutableList<FormatString>): Boolean {
        val backup = token
        if (!accept(TokenType.Dollar)) {
            return false
        }
        val complex = accept(TokenType.CurlyOpen)
        val text = acceptText(TokenType.Int)
        if (text == null) {
            backTo(backup)
            return false
        }
        val group = text.toInt()
        val format = ConditionalFormat()
        format.group = group
        if (complex) {
            if (accept(TokenType.Colon)) {
                if (accept(TokenType.Forwardslash)) {
                    // ${1:/upcase}
                    val varName = acceptText(TokenType.VariableName)
                    if (varName != null && accept(TokenType.CurlyClose)) {
                        format.shorthand = varName
                        formatStrings.add(format)
                        return true
                    }
                } else if (accept(TokenType.Plus)) {
                    // ${1:+<if>}
                    val ifValue = until(TokenType.CurlyClose)
                    if (ifValue != null) {
                        accept(TokenType.CurlyClose)
                        format.ifValue = ifValue
                        formatStrings.add(format)
                        return true
                    }
                } else if (accept(TokenType.Dash)) {
                    // ${1:-<else>}
                    val elseValue = until(TokenType.CurlyClose)
                    if (elseValue != null) {
                        accept(TokenType.CurlyClose)
                        format.elseValue = elseValue
                        formatStrings.add(format)
                        return true
                    }
                } else if (accept(TokenType.QuestionMark)) {
                    // ${1:?<if>:<else>}
                    val ifValue = until(TokenType.Colon)
                    accept(TokenType.Colon)
                    val elseValue = until(TokenType.CurlyClose)
                    if (ifValue != null && elseValue != null) {
                        accept(TokenType.CurlyClose)
                        format.ifValue = ifValue
                        format.elseValue = elseValue
                        formatStrings.add(format)
                        return true
                    }
                } else {
                    val elseValue = until(TokenType.CurlyClose)
                    if (elseValue != null) {
                        accept(TokenType.CurlyClose)
                        format.elseValue = elseValue
                        formatStrings.add(format)
                        return true
                    }
                }
            }
            backTo(backup)
            return false
        } else {
            // $1
            formatStrings.add(format)
            return true
        }
    }

    private fun parseAnything(formatStrings: MutableList<FormatString>): Boolean {
        if (token.type == TokenType.EOF) {
            return false
        }
        formatStrings.add(NoFormat(consumeText()))
        return true
    }

    private fun parseAnything(): Boolean {
        if (token.type == TokenType.EOF) {
            return false
        }
        builder.addPlainText(consumeText())
        return true
    }

    private fun until(type: TokenType): String? {
        val backup = token
        val sb = StringBuilder()
        while (token.type != type) {
            if (token.type == TokenType.EOF) {
                backTo(backup)
                return null
            }
            if (accept(TokenType.Backslash)) {
                val text =
                    acceptAnyText(TokenType.Backslash, TokenType.Dollar, TokenType.CurlyClose)
                if (text != null) {
                    sb.append(text)
                } else {
                    backTo(backup)
                    return null
                }
            } else {
                sb.append(consumeText())
            }
        }
        return sb.toString()
    }

    companion object {
        private fun appendPlaceholderElement(elements: ArrayList<PlaceHolderElement>, t: String) {
            if (elements.isNotEmpty()) {
                val last = elements[elements.size - 1]
                if (last is PlainPlaceholderElement) {
                    // merge with the last plain placeholder element
                    last.text = last.text + t
                    return
                }
            }
            elements.add(PlainPlaceholderElement(t))
        }

        fun parse(
            snippet: String,
            definitions: List<PlaceholderDefinition> = ArrayList()
        ): CodeSnippet {
            val parser = CodeSnippetParser(snippet, definitions)
            parser.parse()
            return parser.builder.build()
        }
    }
}

fun String.parseAsSnippet(definitions: List<PlaceholderDefinition> = emptyList()): CodeSnippet =
    CodeSnippetParser.parse(this, definitions)

