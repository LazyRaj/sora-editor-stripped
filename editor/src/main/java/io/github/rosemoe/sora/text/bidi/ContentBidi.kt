/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *    Modified by Raj Dave (aka LazyRaj)
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

package io.github.rosemoe.sora.text.bidi

import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.util.IntPair

/**
 * Companion for [Content] to manage line directions
 *
 * @author Rosemoe
 */
open class ContentBidi(content: Content) : ContentListener {

    private val entries = arrayOfNulls<DirectionsEntry>(MAX_BIDI_CACHE_ENTRY_COUNT)
    private val text: Content = content

    var isEnabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                entries.fill(null)
            }
        }

    init {
        text.addContentListener(this)
    }

    open fun getLineDirections(lineText: ContentLine, line: Int): Directions {
        if (!isEnabled) {
            return Directions(longArrayOf(IntPair.pack(0, 0)), lineText.length)
        }
        synchronized(this) {
            for (entry in entries) {
                if (entry != null && entry.line == line) {
                    return entry.dir
                }
            }
        }
        val dir = TextBidi.getDirections(lineText)
        synchronized(this) {
            System.arraycopy(entries, 0, entries, 1, entries.size - 1)
            entries[0] = DirectionsEntry(dir, line)
        }
        return dir
    }

    @Synchronized
    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        val delta = endLine - startLine
        for (i in entries.indices) {
            val entry = entries[i] ?: continue
            if (entry.line >= startLine) {
                if (entry.line > endLine) {
                    entry.line -= delta
                } else {
                    entries[i] = null
                }
            }
        }
    }

    @Synchronized
    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        val delta = endLine - startLine
        for (i in entries.indices) {
            val entry = entries[i] ?: continue
            if (entry.line > startLine) {
                entry.line += delta
            } else if (entry.line == startLine) {
                entries[i] = null
            }
        }
    }

    override fun beforeReplace(content: Content) {
    }

    open fun destroy() {
        text.removeContentListener(this)
        entries.fill(null)
    }

    private class DirectionsEntry(val dir: Directions, var line: Int)

    companion object {
        const val MAX_BIDI_CACHE_ENTRY_COUNT: Int = 64
    }
}
