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

import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.Unsubscribe

/**
 * This class is used to control cursor visibility
 *
 * @author Rose
 */
class CursorBlink(
    @JvmField val editor: CodeEditor,
    period: Int
) : Runnable, EventReceiver<SelectionChangeEvent> {

    @JvmField
    var visibility: Boolean = true

    @JvmField
    var valid: Boolean = false

    @JvmField
    var lastSelectionModificationTime: Long = 0L

    @JvmField
    var period: Int = period

    private var buffer: FloatArray? = null

    init {
        editor.subscribeEvent(SelectionChangeEvent::class.java, this)
    }

    override fun onReceive(event: SelectionChangeEvent, unsubscribe: Unsubscribe) {
        onSelectionChanged()
    }

    fun setPeriod(period: Int) {
        this.period = period
        if (period <= 0) {
            visibility = true
            valid = false
        } else {
            valid = true
        }
    }

    fun onSelectionChanged() {
        lastSelectionModificationTime = System.currentTimeMillis()
        visibility = true
    }

    fun isSelectionVisible(): Boolean {
        val buf = buffer ?: return false
        val offsetY = editor.offsetY
        val offsetX = editor.offsetX
        return (buf[0] >= offsetY && buf[0] - editor.rowHeight <= offsetY + editor.height
                && buf[1] >= offsetX && buf[1] - 100f <= offsetX + editor.width)
    }

    override fun run() {
        if (valid && period > 0) {
            if (System.currentTimeMillis() - lastSelectionModificationTime >= period * 2L) {
                visibility = !visibility
                val left = editor.cursor.left()
                buffer = editor.layout.getCharLayoutOffset(left.line, left.column, buffer)
                if (!editor.cursor.isSelected && isSelectionVisible()) {
                    editor.postInvalidate()
                }
            } else {
                visibility = true
            }
            editor.postDelayedInLifecycle(this, period.toLong())
        } else {
            visibility = true
        }
    }
}
