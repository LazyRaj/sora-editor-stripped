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
package io.github.rosemoe.sora.lang.styling

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

open class EmptyReader : Spans.Reader {

    private val spans: MutableList<Span> = mutableListOf(
        SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
    )

    override fun moveToLine(line: Int) {
        // no-op
    }

    override fun getSpanAt(index: Int): Span {
        return spans[index]
    }

    override fun getSpanCount(): Int = 1

    override fun getSpansOnLine(line: Int): List<Span> {
        return ArrayList(spans)
    }

    companion object {
        private val INSTANCE: EmptyReader = EmptyReader()

        @JvmStatic
        fun getInstance(): EmptyReader = INSTANCE
    }
}
