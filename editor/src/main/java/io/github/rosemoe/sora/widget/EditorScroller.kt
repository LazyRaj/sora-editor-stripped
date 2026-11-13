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
package io.github.rosemoe.sora.widget

import android.widget.OverScroller

open class EditorScroller(
    private val editor: CodeEditor
) {

    private val scroller: OverScroller = OverScroller(editor.context)

    open val isFinished: Boolean
        get() = scroller.isFinished

    open val currX: Int
        get() = scroller.currX

    open val currY: Int
        get() = scroller.currY

    open val finalX: Int
        get() = scroller.finalX

    open val finalY: Int
        get() = scroller.finalY

    open val startX: Int
        get() = scroller.startX

    open val startY: Int
        get() = scroller.startY

    open val currVelocity: Float
        get() = scroller.currVelocity

    open val isOverScrolled: Boolean
        get() = scroller.isOverScrolled

    open val implScroller: OverScroller
        get() = scroller

    open fun setEditorOffsets() {
        editor.scrollX = scroller.currX
        editor.scrollY = scroller.currY
    }

    open fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        startScroll(startX, startY, dx, dy, editor.props.scrollAnimationDurationMs)
    }

    open fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        scroller.startScroll(startX, startY, dx, dy, duration)
        setEditorOffsets()
    }

    open fun forceFinished(finished: Boolean) {
        scroller.forceFinished(finished)
        setEditorOffsets()
    }

    open fun abortAnimation() {
        scroller.abortAnimation()
        setEditorOffsets()
    }

    open fun computeScrollOffset(): Boolean {
        val computed = scroller.computeScrollOffset()
        if (computed) {
            setEditorOffsets()
        }
        return computed
    }

    open fun fling(
        startX: Int,
        startY: Int,
        velocityX: Int,
        velocityY: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        overX: Int,
        overY: Int
    ) {
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, overX, overY)
        setEditorOffsets()
    }
}
