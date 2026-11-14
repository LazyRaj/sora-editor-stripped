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
/*
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.widget.snippet.variable

open class CommentBasedSnippetVariableResolver(
    /**
     * For example, Java language comment tokens: `arrayOf("//", "/*", "*/")`
     */
    var commentTokens: Array<String>? = null
) : ISnippetVariableResolver {

    override fun getResolvableNames(): Array<String> {
        return arrayOf("LINE_COMMENT", "BLOCK_COMMENT_START", "BLOCK_COMMENT_END")
    }

    override fun resolve(name: String): String {
        val tokens = commentTokens
        if (tokens == null || tokens.size != 3) {
            throw IllegalStateException("language comment style is not configured properly")
        }
        return when (name) {
            "LINE_COMMENT" -> tokens[0]
            "BLOCK_COMMENT_START" -> tokens[1]
            "BLOCK_COMMENT_END" -> tokens[2]
            else -> throw IllegalArgumentException("Unsupported variable name:$name")
        }
    }
}
