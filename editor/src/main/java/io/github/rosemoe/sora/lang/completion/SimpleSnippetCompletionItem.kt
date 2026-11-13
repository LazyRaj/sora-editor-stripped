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
package io.github.rosemoe.sora.lang.completion

import android.graphics.drawable.Drawable
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

open class SimpleSnippetCompletionItem(
    label: CharSequence,
    desc: CharSequence?,
    icon: Drawable?,
    private val snippet: SnippetDescription
) : CompletionItem(label, desc, icon) {

    constructor(label: CharSequence, snippet: SnippetDescription) : this(label, null, null, snippet)

    constructor(label: CharSequence, desc: CharSequence?, snippet: SnippetDescription) : this(
        label,
        desc,
        null,
        snippet
    )

    init {
        kind(CompletionItemKind.Snippet)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        val prefixLength = snippet.selectedLength
        val selectedText =
            text.subSequence(position.index - prefixLength, position.index).toString()
        var actionIndex = position.index
        if (snippet.deleteSelected) {
            text.delete(position.index - prefixLength, position.index)
            actionIndex -= prefixLength
        }
        editor.snippetController.startSnippet(actionIndex, snippet.snippet, selectedText)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        // do nothing
    }
}
