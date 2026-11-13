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

abstract class SnippetItem(
    start: Int = 0,
    end: Int = start
) : Cloneable {

    var startIndex: Int = start
        private set

    var endIndex: Int = end
        private set

    constructor() : this(0, 0)

    constructor(index: Int) : this(index, index)

    open fun setIndex(index: Int) {
        setIndex(index, index)
    }

    open fun setIndex(start: Int, end: Int) {
        this.startIndex = start
        this.endIndex = end
    }

    open fun shiftIndex(deltaIndex: Int) {
        startIndex += deltaIndex
        endIndex += deltaIndex
    }

    public abstract override fun clone(): SnippetItem
}
