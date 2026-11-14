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

package io.github.rosemoe.sora.lsp.editor.codeaction

import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.jsonrpc.messages.Either

/**
 * Presentation model for an LSP code action entry that the popup can display.
 */
data class CodeActionItem(
    val title: String,
    val action: Either<Command, CodeAction>,
) {
    companion object {
        fun from(source: Either<Command, CodeAction>): CodeActionItem {
            val title = if (source.isLeft) {
                source.left?.title ?: ""
            } else {
                source.right?.title ?: ""
            }
            return CodeActionItem(title = title, action = source)
        }
    }
}
