/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
/* Modified by Raj Dave (aka LazyRaj) */
package io.github.rosemoe.sora.langs.textmate

import android.graphics.Color
import android.os.Bundle
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.rosemoe.sora.lang.analysis.IncrementalAnalyzeManager.LineTokenizeResult
import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.OnlineBracketsMatcher
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.langs.textmate.folding.FoldingHelper
import io.github.rosemoe.sora.langs.textmate.folding.IndentRange
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.utils.StringUtils
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.EncodedTokenAttributes
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import org.eclipse.tm4e.core.internal.oniguruma.OnigRegExp
import org.eclipse.tm4e.core.internal.oniguruma.OnigResult
import org.eclipse.tm4e.core.internal.oniguruma.OnigString
import org.eclipse.tm4e.core.internal.oniguruma.Oniguruma
import org.eclipse.tm4e.core.internal.theme.FontStyle
import org.eclipse.tm4e.core.internal.theme.Theme
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import java.time.Duration
import java.util.Objects

class TextMateAnalyzer(
    private val language: TextMateLanguage,
    private val grammar: IGrammar,
    languageConfiguration: LanguageConfiguration?,
    private val themeRegistry: ThemeRegistry
) : AsyncIncrementalAnalyzeManager<MyState, Span>(), FoldingHelper, ThemeRegistry.ThemeChangeListener {

    private var theme: Theme? = themeRegistry.currentThemeModel.theme
    private val configuration: LanguageConfiguration? = languageConfiguration
    private var cachedRegExp: OnigRegExp? = null
    private var foldingOffside: Boolean = false
    private var bracketsProvider: BracketsProvider? = null
    val syncIdentifiers: IdentifierAutoComplete.SyncIdentifiers = IdentifierAutoComplete.SyncIdentifiers()

    init {
        if (!themeRegistry.hasListener(this)) {
            themeRegistry.addListener(this)
        }

        if (languageConfiguration != null) {
            val pairs = languageConfiguration.brackets
            if (!pairs.isNullOrEmpty()) {
                var size = pairs.size
                for (pair in pairs) {
                    if (pair.open.length != 1 || pair.close.length != 1) {
                        size--
                    }
                }
                val pairArr = CharArray(size * 2)
                var i = 0
                for (pair in pairs) {
                    if (pair.open.length != 1 || pair.close.length != 1) {
                        continue
                    }
                    pairArr[i * 2] = pair.open[0]
                    pairArr[i * 2 + 1] = pair.close[0]
                    i++
                }
                bracketsProvider = OnlineBracketsMatcher(pairArr, 100000)
            }
        }

        createFoldingExp()
    }

    private fun createFoldingExp() {
        val markers = configuration?.folding ?: return
        foldingOffside = markers.offSide
        cachedRegExp = Oniguruma.newRegex("(${markers.markersStart})|(?:${markers.markersEnd})")
    }

    override fun getInitialState(): MyState? = null

    override fun stateEquals(state: MyState?, another: MyState?): Boolean {
        if (state == null && another == null) {
            return true
        }
        if (state != null && another != null) {
            return Objects.equals(state.tokenizeState, another.tokenizeState)
        }
        return false
    }

    override fun getIndentFor(line: Int): Int {
        return getState(line)?.state?.indent ?: 0
    }

    override fun getResultFor(line: Int): OnigResult? {
        return getState(line)?.state?.foldingCache
    }

    override fun computeBlocks(text: Content, delegate: CodeBlockAnalyzeDelegate): List<CodeBlock> {
        val list = ArrayList<CodeBlock>()
        analyzeCodeBlocks(text, list, delegate)
        if (delegate.isNotCancelled) {
            withReceiver { r -> r.updateBracketProvider(this, bracketsProvider) }
        }
        return list
    }

    fun analyzeCodeBlocks(model: Content, blocks: ArrayList<CodeBlock>, delegate: CodeBlockAnalyzeDelegate) {
        val regExp = cachedRegExp ?: return
        try {
            val foldingRegions = IndentRange.computeRanges(model, language.tabSize, foldingOffside, this, regExp, delegate)
            blocks.ensureCapacity(foldingRegions.size)
            for (i in 0 until foldingRegions.size) {
                if (!delegate.isNotCancelled) break
                val startLine = foldingRegions.getStartLineNumber(i)
                val endLine = foldingRegions.getEndLineNumber(i)
                if (startLine != endLine) {
                    val codeBlock = CodeBlock().apply {
                        toBottomOfEndLine = true
                        this.startLine = startLine
                        this.endLine = endLine

                        val length = model.getColumnCount(startLine)
                        val chars = model.getLine(startLine)

                        startColumn = IndentRange.computeStartColumn(chars, length, language.tabSize)
                        endColumn = startColumn
                    }
                    blocks.add(codeBlock)
                }
            }
            blocks.sortWith(CodeBlock.COMPARATOR_END)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        managedStyles?.indentCountMode = true
    }

    @Synchronized
    override fun tokenizeLine(line: CharSequence, state: MyState?, lineIndex: Int): LineTokenizeResult<MyState, Span> {
        val lineStr = if (line is ContentLine) line.toStringWithNewline() else line.toString()
        val tokens = ArrayList<Span>()
        val surrogate = StringUtils.checkSurrogate(lineStr)
        val lineTokens = grammar.tokenizeLine2(lineStr, state?.tokenizeState, Duration.ofSeconds(2))
        val tokensLength = lineTokens.tokens.size / 2
        val identifiers = if (language.collectIdentifiers) ArrayList<String>() else null

        for (i in 0 until tokensLength) {
            val startIndex = StringUtils.convertUnicodeOffsetToUtf16(lineStr, lineTokens.tokens[2 * i], surrogate)
            if (i == 0 && startIndex != 0) {
                tokens.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
            }
            val metadata = lineTokens.tokens[2 * i + 1]
            val foreground = EncodedTokenAttributes.getForeground(metadata)
            val background = EncodedTokenAttributes.getBackground(metadata)
            val fontStyle = EncodedTokenAttributes.getFontStyle(metadata)
            val tokenType = EncodedTokenAttributes.getTokenType(metadata)

            if (language.collectIdentifiers && identifiers != null) {
                if (tokenType == StandardTokenType.Other) {
                    val end = if (i + 1 == tokensLength) {
                        line.length
                    } else {
                        StringUtils.convertUnicodeOffsetToUtf16(lineStr, lineTokens.tokens[2 * (i + 1)], surrogate)
                    }
                    if (end > startIndex && MyCharacter.isJavaIdentifierStart(lineStr[startIndex])) {
                        var flag = true
                        for (j in startIndex + 1 until end) {
                            if (!MyCharacter.isJavaIdentifierPart(lineStr[j])) {
                                flag = false
                                break
                            }
                        }
                        if (flag) {
                            identifiers.add(lineStr.substring(startIndex, end))
                        }
                    }
                }
            }

            val backgroundColorId = if (theme?.defaults?.backgroundId == background) 0 else background + 255
            val needUnderline = (fontStyle and FontStyle.Underline) != 0
            val style = TextStyle.makeStyle(
                foreground + 255,
                backgroundColorId,
                (fontStyle and FontStyle.Bold) != 0,
                (fontStyle and FontStyle.Italic) != 0,
                false
            )
            val span = if (needUnderline) {
                SpanFactory.obtain(startIndex, style)
            } else {
                SpanFactory.obtainNoExt(startIndex, style)
            }

            span.extra = tokenType

            if (needUnderline) {
                val color = theme?.getColor(foreground)
                if (color != null && color != "@default") {
                    span.setUnderlineColor(Color.parseColor(color))
                }
            }

            tokens.add(span)
        }

        return LineTokenizeResult(
            MyState(
                lineTokens.ruleStack,
                cachedRegExp?.search(OnigString.of(lineStr), 0),
                IndentRange.computeIndentLevel(line, line.length, language.tabSize),
                identifiers
            ),
            null,
            tokens
        )
    }

    override fun onAddState(state: MyState) {
        super.onAddState(state)
        if (language.collectIdentifiers) {
            state.identifiers?.forEach { identifier ->
                syncIdentifiers.identifierIncrease(identifier)
            }
        }
    }

    override fun onAbandonState(state: MyState) {
        super.onAbandonState(state)
        if (language.collectIdentifiers) {
            state.identifiers?.forEach { identifier ->
                syncIdentifiers.identifierDecrease(identifier)
            }
        }
    }

    override fun reset(content: ContentReference, extraArguments: Bundle) {
        super.reset(content, extraArguments)
        syncIdentifiers.clear()
    }

    override fun destroy() {
        super.destroy()
        themeRegistry.removeListener(this)
    }

    override fun generateSpansForLine(tokens: LineTokenizeResult<MyState, Span>): List<Span>? = null

    override fun onChangeTheme(newTheme: ThemeModel) {
        this.theme = newTheme.theme
    }
}
