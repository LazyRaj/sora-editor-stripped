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

import android.os.Bundle
import android.util.Log
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration

open class TextMateLanguage protected constructor(
    grammar: IGrammar,
    languageConfiguration: LanguageConfiguration?,
    var grammarRegistry: GrammarRegistry,
    var themeRegistry: ThemeRegistry,
    val collectIdentifiers: Boolean
) : EmptyLanguage() {

    var tabSize: Int = 4
    private var isUseTab: Boolean = false

    private val autoComplete: IdentifierAutoComplete = IdentifierAutoComplete()
    var isAutoCompleteEnabled: Boolean = true

    var textMateAnalyzer: TextMateAnalyzer? = null
        private set

    var languageConfiguration: LanguageConfiguration? = languageConfiguration
        private set

    private var _newlineHandlers: Array<NewlineHandler>? = null
    var newlineHandler: TextMateNewlineHandler? = null
        private set

    var symbolPairMatch: TextMateSymbolPairMatch = TextMateSymbolPairMatch(this)
        private set

    init {
        createAnalyzerAndNewlineHandler(grammar, languageConfiguration)
    }

    companion object {
        private const val LOG_TAG = "TextMateLanguage"

        /**
         * Create a new TextMate language object. Lookup language grammars from default grammar registry.
         *
         * @param languageScopeName  Root language scope name
         * @param collectIdentifiers Collect identifiers for auto-completion
         */
        fun create(
            languageScopeName: String,
            collectIdentifiers: Boolean = false
        ): TextMateLanguage =
            create(languageScopeName, GrammarRegistry.getInstance(), collectIdentifiers)

        /**
         * Create a new TextMate language object. Lookup language grammars from given grammar registry.
         *
         * @param languageScopeName  Root language scope name
         * @param grammarRegistry    Grammar registry to lookup language grammars
         * @param collectIdentifiers Collect identifiers for auto-completion
         */
        fun create(
            languageScopeName: String,
            grammarRegistry: GrammarRegistry,
            collectIdentifiers: Boolean = false
        ): TextMateLanguage =
            create(languageScopeName, grammarRegistry, ThemeRegistry.getInstance(), collectIdentifiers)

        /**
         * Create a new TextMate language object. Lookup language grammars from given grammar registry. Use theme from given theme registry.
         *
         * @param languageScopeName  Root language scope name
         * @param grammarRegistry    Grammar registry to lookup language grammars
         * @param themeRegistry      Theme registry for theme management
         * @param collectIdentifiers Collect identifiers for auto-completion
         */
        fun create(
            languageScopeName: String,
            grammarRegistry: GrammarRegistry,
            themeRegistry: ThemeRegistry,
            collectIdentifiers: Boolean
        ): TextMateLanguage {
            val grammar = grammarRegistry.findGrammar(languageScopeName)
                ?: throw IllegalArgumentException("Language with $grammarRegistry scope name $languageScopeName not found")
            val languageConfiguration = grammarRegistry.findLanguageConfiguration(grammar.scopeName)
            return TextMateLanguage(grammar, languageConfiguration, grammarRegistry, themeRegistry, collectIdentifiers)
        }

        /**
         * Create a new TextMate language object. Lookup language grammars from default grammar registry.
         *
         * @param grammarDefinition  Root language grammar definition
         * @param collectIdentifiers Collect identifiers for auto-completion
         */
        fun create(
            grammarDefinition: GrammarDefinition,
            collectIdentifiers: Boolean = false
        ): TextMateLanguage =
            create(grammarDefinition, GrammarRegistry.getInstance(), collectIdentifiers)

        /**
         * Create a new TextMate language object. Lookup language grammars from given grammar registry.
         *
         * @param grammarDefinition  Root language grammar definition
         * @param grammarRegistry    Grammar registry to lookup language grammars
         * @param collectIdentifiers Collect identifiers for auto-completion
         */
        fun create(
            grammarDefinition: GrammarDefinition,
            grammarRegistry: GrammarRegistry,
            collectIdentifiers: Boolean = false
        ): TextMateLanguage =
            create(grammarDefinition, grammarRegistry, ThemeRegistry.getInstance(), collectIdentifiers)

        /**
         * Create a new TextMate language object. Lookup language grammars from given grammar registry. Use theme from given theme registry.
         *
         * @param grammarDefinition  Root language grammar definition
         * @param grammarRegistry    Grammar registry to lookup language grammars
         * @param themeRegistry      Theme registry for theme management
         * @param collectIdentifiers Collect identifiers for auto-completion
         */
        fun create(
            grammarDefinition: GrammarDefinition,
            grammarRegistry: GrammarRegistry,
            themeRegistry: ThemeRegistry,
            collectIdentifiers: Boolean
        ): TextMateLanguage {
            val grammar = grammarRegistry.loadGrammar(grammarDefinition)
            val languageConfiguration = grammarRegistry.findLanguageConfiguration(grammar.scopeName)
            return TextMateLanguage(grammar, languageConfiguration, grammarRegistry, themeRegistry, collectIdentifiers)
        }
    }

    private fun createAnalyzerAndNewlineHandler(grammar: IGrammar, languageConfiguration: LanguageConfiguration?) {
        val old = textMateAnalyzer
        if (old != null) {
            old.setReceiver(null)
            old.destroy()
        }
        try {
            textMateAnalyzer = TextMateAnalyzer(this, grammar, languageConfiguration, themeRegistry)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to create analyzer for TextMate", e)
        }
        this.languageConfiguration = languageConfiguration
        val handler = TextMateNewlineHandler(this)
        this.newlineHandler = handler
        this._newlineHandlers = arrayOf(handler)
        if (languageConfiguration != null) {
            symbolPairMatch.updatePair()
        }
    }

    /**
     * Update the root language to the given scope name.
     *
     * **This should be called when the language is detached from editor.**
     *
     * @param scopeName New language scope name
     */
    fun updateLanguage(scopeName: String) {
        val grammar = grammarRegistry.findGrammar(scopeName)
            ?: throw IllegalArgumentException("Language with $grammarRegistry scope name $scopeName not found")
        val config = grammarRegistry.findLanguageConfiguration(grammar.scopeName)
        createAnalyzerAndNewlineHandler(grammar, config)
    }

    /**
     * Update the root language to the given grammar definition.
     *
     * **This should be called when the language is detached from editor.**
     *
     * @param grammarDefinition New language grammar definition
     */
    fun updateLanguage(grammarDefinition: GrammarDefinition) {
        val grammar = grammarRegistry.loadGrammar(grammarDefinition)
        val config = grammarRegistry.findLanguageConfiguration(grammar.scopeName)
        createAnalyzerAndNewlineHandler(grammar, config)
    }

    override val analyzeManager: AnalyzeManager
        get() = textMateAnalyzer ?: EmptyLanguage.EmptyAnalyzeManager.INSTANCE

    override fun useTab(): Boolean = isUseTab

    fun useTab(useTab: Boolean) {
        this.isUseTab = useTab
    }

    override val symbolPairs: TextMateSymbolPairMatch
        get() = symbolPairMatch

    override val newlineHandlers: Array<NewlineHandler>?
        get() = _newlineHandlers

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        if (!isAutoCompleteEnabled) {
            return
        }
        val prefix = CompletionHelper.computePrefix(content, position) { MyCharacter.isJavaIdentifierPart(it) }
        val idt = textMateAnalyzer?.syncIdentifiers
        autoComplete.requireAutoComplete(content, position, prefix, publisher, idt)
    }

    val autoCompleter: IdentifierAutoComplete
        get() = autoComplete

    /**
     * Set keywords for auto-completion
     */
    fun setCompleterKeywords(keywords: Array<String>) {
        autoComplete.setKeywords(keywords, false)
    }
}
