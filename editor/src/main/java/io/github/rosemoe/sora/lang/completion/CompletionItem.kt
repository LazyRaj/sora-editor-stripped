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

/**
 * The class used to save auto complete result items.
 * For functionality, this class only manages the information to be displayed in list view.
 * You can implement [CompletionItem.performCompletion] or
 * [CompletionItem.performCompletion] to customize
 * your own completion method so that you can develop complex actions.
 *
 * For the simplest usage, see [SimpleCompletionItem]
 *
 * @author Rosemoe
 * @see SimpleCompletionItem
 */
abstract class CompletionItem @JvmOverloads constructor(
    /**
     * Text to display as title in adapter
     */
    @JvmField var label: CharSequence,

    /**
     * Text to display as description in adapter
     */
    @JvmField var desc: CharSequence? = null,

    /**
     * Icon for displaying in adapter
     */
    @JvmField var icon: Drawable? = null
) {

    /**
     * Text to display less prominently directly after label, without any spacing.
     */
    @JvmField
    var detail: CharSequence? = null

    /**
     * Indicates whether this completion item is deprecated.
     * When true, the item is typically rendered using strike-through text.
     */
    @JvmField
    var deprecated: Boolean = false

    /**
     * The kind of this completion item. Based on the kind
     * an icon is chosen by the editor.
     */
    @JvmField
    var kind: CompletionItemKind? = null

    /**
     * Use for default sort
     */
    @JvmField
    var prefixLength: Int = 0

    /**
     * A string that should be used when comparing this item
     * with other items. When null the [label]
     * is used.
     */
    @JvmField
    var sortText: String? = null

    /**
     * A string that should be used when comparing this item
     * with other items. When null the [sortText]
     * is used.
     */
    @JvmField
    var filterText: String? = null

    @JvmField
    var extra: Any? = null

    open fun label(label: CharSequence): CompletionItem {
        this.label = label
        return this
    }

    open fun desc(desc: CharSequence?): CompletionItem {
        this.desc = desc
        return this
    }

    open fun kind(kind: CompletionItemKind?): CompletionItem {
        this.kind = kind
        return this
    }

    open fun icon(icon: Drawable?): CompletionItem {
        this.icon = icon
        return this
    }

    /**
     * Perform this completion.
     * You can implement custom logic to make your completion better (by updating selection and text
     * from here).
     * To make it considered as a single action, the editor will enter batch edit state before invoking
     * this method. Feel free to update the text by multiple calls to `text`.
     *
     * @param editor   The editor. You can set cursor position with that.
     * @param text     The text in editor. You can make modifications to it.
     * @param position The requested completion position (the one passed to completion thread)
     */
    open fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        performCompletion(editor, text, position.line, position.column)
    }

    /**
     * Perform this completion.
     * You can implement custom logic to make your completion better (by updating selection and text
     * from here).
     * To make it considered as a single action, the editor will enter batch edit state before invoking
     * this method. Feel free to update the text by multiple calls to `text`.
     *
     * @param editor The editor. You can set cursor position with that.
     * @param text   The text in editor. You can make modifications to it.
     * @param line   The auto-completion line
     * @param column The auto-completion column
     * @see performCompletion Editor calls this method to do completion
     */
    abstract fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int)
}
