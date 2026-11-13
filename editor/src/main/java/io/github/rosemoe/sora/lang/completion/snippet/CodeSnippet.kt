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
package io.github.rosemoe.sora.lang.completion.snippet

import androidx.collection.MutableIntObjectMap
import java.util.TreeSet

open class CodeSnippet(
    val items: MutableList<SnippetItem>,
    placeholders: List<PlaceholderDefinition>
) : Cloneable {

    private val placeholders: List<PlaceholderDefinition> = placeholders

    val placeholderDefinitions: List<PlaceholderDefinition>
        get() = placeholders

    fun checkContent(): Boolean {
        var index = 0
        for (item in items) {
            if (item.startIndex != index) {
                return false
            }
            if (item is PlaceholderItem) {
                if (!placeholders.contains(item.definition)) {
                    return false
                }
            }
            index = item.endIndex
        }
        val set = TreeSet<Int>()
        for (placeholder in placeholders) {
            if (!set.contains(placeholder.id)) {
                set.add(placeholder.id)
            } else {
                return false
            }
        }
        return true
    }

    public override fun clone(): CodeSnippet {
        val definitions = ArrayList<PlaceholderDefinition>(placeholders.size)
        val map = HashMap<PlaceholderDefinition, PlaceholderDefinition>()
        for (placeholder in placeholders) {
            val n = PlaceholderDefinition(
                placeholder.id,
                placeholder.choices,
                ArrayList(placeholder.elements),
                placeholder.transform
            )
            definitions.add(n)
            map[placeholder] = n
        }
        val itemsClone = ArrayList<SnippetItem>(items.size)
        for (item in items) {
            val n = item.clone()
            itemsClone.add(n)
            if (n is PlaceholderItem) {
                val clonedDef = map[n.definition]
                if (clonedDef != null) {
                    n.definition = clonedDef
                }
            }
        }
        return CodeSnippet(itemsClone, definitions)
    }

    /**
     * Specific method for LSP code snippets to generate default insert text when snippet edit
     * is not available in editor.
     */
    fun toInsertTextForLsp(): String {
        val sb = StringBuilder()
        val defaultValues = MutableIntObjectMap<String>()
        for (item in items) {
            when (item) {
                is PlainTextItem -> sb.append(item.text)
                is PlaceholderItem -> {
                    val id = item.definition.id
                    if (!defaultValues.contains(id)) {
                        val value = StringBuilder()
                        for (element in item.definition.elements) {
                            if (element is PlainPlaceholderElement) {
                                value.append(element.text)
                            }
                        }
                        defaultValues.put(id, value.toString())
                    }
                    sb.append(defaultValues.get(id))
                }
            }
        }
        return sb.toString()
    }

    class Builder(
        private val definitions: MutableList<PlaceholderDefinition> = ArrayList()
    ) {
        private val items: MutableList<SnippetItem> = ArrayList()
        private var index: Int = 0

        fun addPlainText(text: String): Builder {
            if (items.isNotEmpty()) {
                val last = items.last()
                if (last is PlainTextItem) {
                    // Merge plain texts
                    last.text = last.text + text
                    last.setIndex(last.startIndex, last.endIndex + text.length)
                    index += text.length
                    return this
                }
            }
            items.add(PlainTextItem(text, index))
            index += text.length
            return this
        }

        fun addInterpolatedShell(shell: String): Builder {
            items.add(InterpolatedShellItem(shell, index))
            return this
        }

        fun addPlaceholder(id: Int): Builder {
            return addPlaceholder(id, null as String?)
        }

        fun addPlaceholder(id: Int, choices: List<String>): Builder {
            if (choices.isEmpty()) {
                return addPlaceholder(id)
            } else if (choices.size == 1) {
                return addPlaceholder(id, choices[0])
            }
            addPlaceholder(id, choices[0])
            val def = definitions.firstOrNull { it.id == id }
            requireNotNull(def)
            def.choices = choices
            return this
        }

        fun addPlaceholder(id: Int, transform: Transform?): Builder {
            if (transform == null) {
                return addPlaceholder(id)
            }
            addPlaceholder(id)
            val def = definitions.firstOrNull { it.id == id }
            requireNotNull(def)
            def.transform = transform
            return this
        }

        fun addPlaceholder(id: Int, defaultValue: String?): Builder {
            val elements = ArrayList<PlaceHolderElement>()
            if (!defaultValue.isNullOrEmpty()) {
                elements.add(PlainPlaceholderElement(defaultValue))
            }
            return addComplexPlaceholder(id, elements)
        }

        fun addComplexPlaceholder(id: Int, elements: List<PlaceHolderElement>): Builder {
            var def = definitions.firstOrNull { it.id == id }
            if (def == null) {
                def = PlaceholderDefinition(id)
                definitions.add(def)
            }
            def.elements.addAll(elements)

            val item = PlaceholderItem(def, index)
            items.add(item)
            return this
        }

        fun addVariable(name: String, defaultValue: String?): Builder {
            items.add(VariableItem(index, name, defaultValue))
            return this
        }

        fun addVariable(name: String, transform: Transform?): Builder {
            items.add(VariableItem(index, name, null, transform))
            return this
        }

        fun addVariable(item: VariableItem): Builder {
            item.setIndex(index)
            items.add(item)
            return this
        }

        fun build(): CodeSnippet {
            return CodeSnippet(items, definitions)
        }
    }
}
