/*******************************************************************************
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
 *
 *     Modified by Raj Dave (aka LazyRaj)
 ******************************************************************************/

package io.github.rosemoe.sora.lsp.editor.text

import android.graphics.Typeface
import android.util.Log

class MarkdownCodeHighlighterRegistry {
    private val highlighters = mutableMapOf<String, MarkdownCodeHighlighter>()
    private val aliases = mutableMapOf<String, String>()
    private val providers = mutableListOf<CodeHighlighterProvider>()

    fun withProvider(provider: CodeHighlighterProvider): Disposable {
        providers.add(provider)
        return Disposable { providers.remove(provider) }
    }

    fun register(language: LanguageName, highlighter: MarkdownCodeHighlighter): Disposable {
        val key = language.lowercase()
        highlighters[key] = highlighter
        return Disposable { highlighters.remove(key) }
    }

    fun registerWithAlias(alias: LanguageAlias, highlighter: MarkdownCodeHighlighter): Disposable {
        val target = alias.target.lowercase()
        highlighters[target] = highlighter
        alias.aliases.forEach { a ->
            aliases[a.lowercase()] = target
        }
        return Disposable {
            highlighters.remove(target)
            alias.aliases.forEach { a ->
                aliases.remove(a.lowercase())
            }
        }
    }

    fun unregister(language: String) {
        val key = language.lowercase()
        highlighters.remove(key)
        aliases.entries.removeIf { it.value == key }
    }

    fun addAlias(from: LanguageName, to: LanguageName) {
        aliases[from.lowercase()] = to.lowercase()
    }

    fun highlight(code: String, language: LanguageName?, codeTypeface: Typeface): HighlightResult {
        if (language == null) return HighlightResult(code, false)
        val resolved = aliases[language.lowercase()] ?: language.lowercase()
        val highlighter = highlighters[resolved] ?: providers.firstNotNullOfOrNull { it.provide(resolved) }
            ?.also { highlighters[resolved] = it }
        if (highlighter == null) return HighlightResult(code, false)
        if (highlighter.isAsync) {
            Log.w("CodeHighlighterRegistry", "Async highlighter for '$resolved' called in sync context")
            return HighlightResult(code, true)
        }
        return HighlightResult(highlighter.highlight(code, language, codeTypeface), false)
    }

    suspend fun highlightAsync(code: String, language: LanguageName?, codeTypeface: Typeface): CharSequence {
        if (language == null) return code
        val resolved = aliases[language.lowercase()] ?: language.lowercase()
        val highlighter = highlighters[resolved] ?: providers.firstNotNullOfOrNull { it.provide(resolved) }
            ?.also { highlighters[resolved] = it }
        return highlighter?.highlightAsync(code, language, codeTypeface) ?: code
    }

    data class HighlightResult(
        val content: CharSequence,
        val needsAsync: Boolean
    )

    data class LanguageAlias(
        val target: LanguageName,
        val aliases: List<LanguageName> = emptyList()
    ) {
        companion object {
            val Kotlin = LanguageAlias("kotlin", listOf("kt"))
            val Java = LanguageAlias("java")
            val JavaScript = LanguageAlias("javascript", listOf("js", "jsx"))
            val TypeScript = LanguageAlias("typescript", listOf("ts", "tsx"))
            val Python = LanguageAlias("python", listOf("py"))
            val Cpp = LanguageAlias("cpp", listOf("c++"))
            val CSharp = LanguageAlias("csharp", listOf("c#", "cs"))
            val Ruby = LanguageAlias("ruby", listOf("rb"))
            val Shell = LanguageAlias("shell", listOf("sh", "bash", "zsh"))
            val Yaml = LanguageAlias("yaml", listOf("yml"))
            val Markdown = LanguageAlias("markdown", listOf("md"))
        }
    }

    companion object {
        val global = MarkdownCodeHighlighterRegistry()
    }

    fun interface Disposable {
        fun dispose()
    }

    fun interface CodeHighlighterProvider {
        fun provide(language: LanguageName): MarkdownCodeHighlighter?
    }
}

typealias LanguageName = String
