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
package io.github.rosemoe.sora.widget.style.builtin

import android.animation.ValueAnimator
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.style.CursorAnimator

/**
 * Scale-Up/Scale-Down cursor animation
 *
 * @author Dmitry Rubtsov
 */
open class ScaleCursorAnimator(private val editor: CodeEditor) : CursorAnimator,
    ValueAnimator.AnimatorUpdateListener {

    private val duration: Long = 180

    private var scaleAnimator: ValueAnimator = ValueAnimator()
    private var lastAnimateTime: Long = 0
    private var lineHeight: Float = 0f
    private var lineBottom: Float = 0f
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var endX: Float = 0f
    private var endY: Float = 0f

    override fun markStartPos() {
        val line = editor.cursor.leftLine
        lineHeight = editor.layout.getRowCountForLine(line) * editor.rowHeight.toFloat()
        lineBottom = editor.layout.getCharLayoutOffset(line, editor.text.getColumnCount(line))[0]

        val pos = editor.layout.getCharLayoutOffset(
            editor.cursor.leftLine,
            editor.cursor.leftColumn
        )
        startX = pos[1] + editor.measureTextRegionOffset()
        startY = pos[0]
    }

    override fun isRunning(): Boolean {
        return scaleAnimator.isRunning
    }

    override fun cancel() {
        scaleAnimator.cancel()
    }

    override fun markEndPos() {
        if (!editor.isCursorAnimationEnabled) {
            return
        }
        if (isRunning()) {
            cancel()
        }
        if (System.currentTimeMillis() - lastAnimateTime < 100) {
            return
        }
        scaleAnimator.removeAllUpdateListeners()

        val line = editor.cursor.leftLine
        lineHeight = editor.layout.getRowCountForLine(line) * editor.rowHeight.toFloat()
        lineBottom = editor.layout.getCharLayoutOffset(line, editor.text.getColumnCount(line))[0]

        val pos = editor.layout.getCharLayoutOffset(
            editor.cursor.leftLine,
            editor.cursor.leftColumn
        )
        endX = pos[1] + editor.measureTextRegionOffset()
        endY = pos[0]

        scaleAnimator = if (editor.insertHandleDescriptor.position.isEmpty) {
            ValueAnimator.ofFloat(0f, 1.0f).apply {
                duration = this@ScaleCursorAnimator.duration
            }
        } else {
            ValueAnimator.ofFloat(1.0f, 0f, 1.0f).apply {
                duration = this@ScaleCursorAnimator.duration * 2
            }
        }
        scaleAnimator.addUpdateListener(this)
    }

    override fun start() {
        if (!editor.isCursorAnimationEnabled || System.currentTimeMillis() - lastAnimateTime < 100 || editor.insertHandleDescriptor.position.isEmpty) {
            lastAnimateTime = System.currentTimeMillis()
            return
        }
        if (startX == endX && startY == endY && !editor.insertHandleDescriptor.position.isEmpty) {
            return
        }
        scaleAnimator.start()
        lastAnimateTime = System.currentTimeMillis()
    }

    private fun shouldReturnEndValue(): Boolean {
        return !scaleAnimator.isRunning || editor.insertHandleDescriptor.position.isEmpty || scaleAnimator.duration == duration || scaleAnimator.currentPlayTime > duration
    }

    override fun animatedX(): Float {
        if (shouldReturnEndValue()) {
            return endX
        }
        return startX
    }

    override fun animatedY(): Float {
        if (shouldReturnEndValue()) {
            return endY
        }
        return startY
    }

    override fun animatedLineHeight(): Float {
        return lineHeight
    }

    override fun animatedLineBottom(): Float {
        return lineBottom
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        editor.handleStyle.setScale(animation.animatedValue as Float)
        editor.postInvalidateOnAnimation()
    }
}
