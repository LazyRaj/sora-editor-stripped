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
package io.github.rosemoe.sora.langs.textmate.registry.reader

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition
import org.eclipse.tm4e.core.registry.IGrammarSource
import java.io.BufferedReader
import java.io.InputStreamReader

object LanguageDefinitionReader {

    @JvmStatic
    fun read(path: String): List<GrammarDefinition> {
        val stream = FileProviderRegistry.getInstance().tryGetInputStream(path) ?: return emptyList()
        return read(BufferedReader(InputStreamReader(stream, Charsets.UTF_8)))
    }

    private fun read(bufferedReader: BufferedReader): List<GrammarDefinition> {
        val result = GsonBuilder().registerTypeAdapter(
            GrammarDefinition::class.java,
            JsonDeserializer<GrammarDefinition> { json, _, _ ->
                val `object` = json.asJsonObject
                val grammarPath = `object`.get("grammar").asString
                val name = `object`.get("name").asString
                val scopeName = `object`.get("scopeName").asString

                val embeddedLanguagesElement = `object`.get("embeddedLanguages")
                var embeddedLanguages: JsonObject? = null
                if (embeddedLanguagesElement != null && embeddedLanguagesElement.isJsonObject) {
                    embeddedLanguages = embeddedLanguagesElement.asJsonObject
                }

                val languageConfigurationElement = `object`.get("languageConfiguration")
                var languageConfiguration: String? = null
                if (languageConfigurationElement != null && !languageConfigurationElement.isJsonNull) {
                    languageConfiguration = languageConfigurationElement.asString
                }

                val grammarInput = FileProviderRegistry.getInstance().tryGetInputStream(grammarPath)
                    ?: throw IllegalArgumentException("grammar file can not be opened: $grammarPath")

                val grammarSource = IGrammarSource.fromInputStream(grammarInput, grammarPath, Charsets.UTF_8)
                val grammarDefinition = DefaultGrammarDefinition.withLanguageConfiguration(
                    grammarSource,
                    languageConfiguration,
                    name,
                    scopeName
                )

                if (embeddedLanguages != null) {
                    val embeddedLanguagesMap = HashMap<String, String>()
                    for (entry in embeddedLanguages.entrySet()) {
                        val value = entry.value
                        if (!value.isJsonNull) {
                            embeddedLanguagesMap[entry.key] = value.asString
                        }
                    }
                    grammarDefinition.withEmbeddedLanguages(embeddedLanguagesMap)
                } else {
                    grammarDefinition
                }
            }
        )
            .create()
            .fromJson(bufferedReader, LanguageDefinitionList::class.java)

        return result?.grammarDefinition ?: emptyList()
    }

    private class LanguageDefinitionList(
        @SerializedName("languages")
        var grammarDefinition: List<GrammarDefinition>? = null
    )
}
