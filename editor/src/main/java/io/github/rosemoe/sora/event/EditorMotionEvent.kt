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
package io.github.rosemoe.sora.event

import android.view.InputDevice
import android.view.MotionEvent
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Base class for click events
 *
 * @author Rosemoe
 * @see ClickEvent
 * @see DoubleClickEvent
 * @see LongPressEvent
 * @see ContextClickEvent
 * @see HoverEvent
 */
abstract class EditorMotionEvent(
    editor: CodeEditor,
    private val pos: CharPosition,
    private val event: MotionEvent,
    val span: Span?,
    val spanRange: TextRange?,
    val motionRegion: Int,
    val motionBound: Int
) : Event(editor) {

    companion object {
        const val REGION_OUTBOUND: Int = io.github.rosemoe.sora.widget.REGION_OUTBOUND
        const val REGION_LINE_NUMBER: Int = io.github.rosemoe.sora.widget.REGION_LINE_NUMBER
        const val REGION_SIDE_ICON: Int = io.github.rosemoe.sora.widget.REGION_SIDE_ICON
        const val REGION_DIVIDER_MARGIN: Int = io.github.rosemoe.sora.widget.REGION_DIVIDER_MARGIN
        const val REGION_DIVIDER: Int = io.github.rosemoe.sora.widget.REGION_DIVIDER
        const val REGION_TEXT: Int = io.github.rosemoe.sora.widget.REGION_TEXT
        const val IN_BOUND: Int = io.github.rosemoe.sora.widget.IN_BOUND
        const val OUT_BOUND: Int = io.github.rosemoe.sora.widget.OUT_BOUND
    }

    override fun canIntercept(): Boolean = true

    val isFromMouse: Boolean
        get() = event.isFromSource(InputDevice.SOURCE_MOUSE)

    val line: Int
        get() = pos.line

    val column: Int
        get() = pos.column

    val index: Int
        get() = pos.index

    fun getCharPosition(): CharPosition = pos.fromThis()

    val x: Float
        get() = event.x

    val y: Float
        get() = event.y

    val causingEvent: MotionEvent
        get() = event
}
