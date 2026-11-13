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
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * SimpleCompletionItem represents a simple replace action for auto-completion.
 * [prefixLength] is the length of prefix (text length you want to replace before the
 * auto-completion position).
 * [commitText] is the text you want to replace the original text.
 *
 * Note that you must make sure the start position of replacement is on the same line as auto-completion's
 * required position.
 *
 * @see CompletionItem
 */
open class SimpleCompletionItem(
    label: CharSequence,
    desc: CharSequence?,
    icon: Drawable?,
    prefixLength: Int,
    @JvmField var commitText: String?
) : CompletionItem(label, desc, icon) {

    init {
        this.prefixLength = prefixLength
    }

    constructor(prefixLength: Int, commitText: String) : this(
        commitText,
        null,
        null,
        prefixLength,
        commitText
    )

    constructor(label: CharSequence, prefixLength: Int, commitText: String) : this(
        label,
        null,
        null,
        prefixLength,
        commitText
    )

    constructor(
        label: CharSequence,
        desc: CharSequence?,
        prefixLength: Int,
        commitText: String
    ) : this(label, desc, null, prefixLength, commitText)

    override fun desc(desc: CharSequence?): SimpleCompletionItem {
        super.desc(desc)
        return this
    }

    override fun icon(icon: Drawable?): SimpleCompletionItem {
        super.icon(icon)
        return this
    }

    override fun label(label: CharSequence): SimpleCompletionItem {
        super.label(label)
        return this
    }

    override fun kind(kind: CompletionItemKind?): SimpleCompletionItem {
        super.kind(kind)
        if (this.icon == null && kind != null) {
            icon = SimpleCompletionIconDrawer.draw(kind)
        }
        return this
    }

    open fun commit(prefixLength: Int, commitText: String?): SimpleCompletionItem {
        this.prefixLength = prefixLength
        this.commitText = commitText
        return this
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        val commit = commitText ?: return
        if (prefixLength == 0) {
            text.insert(line, column, commit)
            return
        }
        text.replace(line, column - prefixLength, line, column, commit)
    }
}
