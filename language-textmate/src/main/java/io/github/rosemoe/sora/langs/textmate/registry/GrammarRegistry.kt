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
package io.github.rosemoe.sora.langs.textmate.registry

import io.github.rosemoe.sora.langs.textmate.registry.dsl.LanguageDefinitionListBuilder
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.reader.LanguageDefinitionReader
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.core.registry.Registry
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import java.io.InputStreamReader
import java.util.LinkedHashMap

class GrammarRegistry(private var parent: GrammarRegistry? = null) {

    private var registry: Registry? = Registry()

    private val languageConfigurationMap = LinkedHashMap<String, LanguageConfiguration>()
    private val scopeName2GrammarId = LinkedHashMap<String, Int>()
    private val grammarFileName2ScopeName = LinkedHashMap<String, String>()
    private val scopeName2GrammarDefinition = LinkedHashMap<String, GrammarDefinition>()

    private fun initThemeListener() {
        val themeRegistry = ThemeRegistry.getInstance()
        val themeChangeListener = ThemeRegistry.ThemeChangeListener { newTheme ->
            try {
                setTheme(newTheme)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        if (!themeRegistry.hasListener(themeChangeListener)) {
            themeRegistry.addListener(themeChangeListener)
        }
    }

    fun findGrammar(scopeName: String, findInParent: Boolean = true): IGrammar? {
        val grammar = registry?.grammarForScopeName(scopeName)
        if (grammar != null) {
            return grammar
        }
        if (!findInParent) {
            return null
        }
        return parent?.findGrammar(scopeName, true)
    }

    fun findLanguageConfiguration(scopeName: String, findInParent: Boolean = true): LanguageConfiguration? {
        val languageConfiguration = languageConfigurationMap[scopeName]
        if (languageConfiguration != null) {
            return languageConfiguration
        }
        if (!findInParent) {
            return null
        }
        return parent?.findLanguageConfiguration(scopeName, true)
    }

    fun loadLanguageAndLanguageConfiguration(grammarDefinition: GrammarDefinition): Pair<IGrammar, LanguageConfiguration?> {
        val grammar = loadGrammar(grammarDefinition)
        val languageConfiguration = findLanguageConfiguration(grammar.scopeName, false)
        return Pair(grammar, languageConfiguration)
    }

    fun loadGrammars(builder: LanguageDefinitionListBuilder): List<IGrammar> {
        return loadGrammars(builder.build())
    }

    fun loadGrammars(list: List<GrammarDefinition>): List<IGrammar> {
        prepareLoadGrammars(list)
        return list.map { loadGrammar(it) }
    }

    fun loadGrammars(jsonPath: String): List<IGrammar> {
        return loadGrammars(LanguageDefinitionReader.read(jsonPath))
    }

    @Synchronized
    fun loadGrammar(grammarDefinition: GrammarDefinition): IGrammar {
        val languageName = grammarDefinition.name
        val scope = grammarDefinition.scopeName

        if (grammarFileName2ScopeName.containsKey(languageName) && scope != null) {
            return registry?.grammarForScopeName(scope)
                ?: throw IllegalStateException("Registry disposed or grammar not found for $scope")
        }

        val grammar = doLoadGrammar(grammarDefinition)
        if (scope != null) {
            grammarFileName2ScopeName[languageName] = scope
            scopeName2GrammarDefinition[grammar.scopeName] = grammarDefinition
        }
        return grammar
    }

    @Synchronized
    private fun doLoadGrammar(grammarDefinition: GrammarDefinition): IGrammar {
        val reg = registry ?: throw IllegalStateException("GrammarRegistry has been disposed")

        val languageConfigurationPath = grammarDefinition.languageConfiguration
        if (languageConfigurationPath != null) {
            val languageConfigurationStream = FileProviderRegistry.getInstance()
                .tryGetInputStream(languageConfigurationPath)
            if (languageConfigurationStream != null) {
                val languageConfiguration = LanguageConfiguration.load(
                    InputStreamReader(languageConfigurationStream, Charsets.UTF_8)
                )
                val scope = grammarDefinition.scopeName
                if (scope != null && languageConfiguration != null) {
                    languageConfigurationMap[scope] = languageConfiguration
                }
            }
        }

        val grammar = if (grammarDefinition.embeddedLanguages.isNotEmpty()) {
            reg.addGrammar(grammarDefinition.grammar)
        } else {
            val scope = grammarDefinition.scopeName
            reg.addGrammar(
                grammarDefinition.grammar,
                null,
                if (scope != null) getOrPullGrammarId(scope) else 0,
                findGrammarIds(grammarDefinition.embeddedLanguages)
            )
        }

        if (grammarDefinition.scopeName != null && grammar.scopeName != grammarDefinition.scopeName) {
            throw IllegalStateException(
                "The scope name loaded by the grammar file does not match the declared scope name, it should be ${grammar.scopeName} instead of ${grammarDefinition.scopeName}"
            )
        }

        return grammar
    }

    private fun prepareLoadGrammars(grammarDefinitions: List<GrammarDefinition>) {
        for (grammar in grammarDefinitions) {
            val scope = grammar.scopeName
            if (scope != null) {
                getOrPullGrammarId(scope)
            }
        }
    }

    @Synchronized
    fun setTheme(themeModel: ThemeModel) {
        val reg = registry ?: return
        if (!themeModel.isLoaded) {
            themeModel.load(reg.colorMap)
        }
        reg.setTheme(themeModel.theme)
    }

    @Synchronized
    private fun getOrPullGrammarId(scopeName: String): Int {
        var id = scopeName2GrammarId[scopeName]
        if (id == null) {
            id = scopeName2GrammarId.size + 2
            scopeName2GrammarId[scopeName] = id
        }
        return id
    }

    @Synchronized
    private fun findGrammarIds(scopeName2LanguageName: Map<String, String>): Map<String, Int> {
        val result = HashMap<String, Int>()
        for ((key, value) in scopeName2LanguageName) {
            result[key] = getOrPullGrammarId(getGrammarScopeName(value))
        }
        return result
    }

    private fun getGrammarScopeName(name: String): String {
        if (scopeName2GrammarDefinition.containsKey(name)) {
            return name
        }
        return grammarFileName2ScopeName[name] ?: name
    }

    @Synchronized
    fun dispose(closeParent: Boolean = false) {
        registry = null
        grammarFileName2ScopeName.clear()
        languageConfigurationMap.clear()
        scopeName2GrammarId.clear()
        scopeName2GrammarDefinition.clear()

        if (parent != null && closeParent) {
            parent?.dispose(true)
        }
    }

    companion object {
        @Volatile
        private var instance: GrammarRegistry? = null

        @Synchronized
        fun getInstance(): GrammarRegistry {
            return instance ?: GrammarRegistry().also {
                instance = it
                it.initThemeListener()
            }
        }
    }
}
