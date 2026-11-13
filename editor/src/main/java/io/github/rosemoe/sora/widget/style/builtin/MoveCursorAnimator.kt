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
 * Default cursor animation implementation
 *
 * @author Rosemoe
 */
open class MoveCursorAnimator(private val editor: CodeEditor) : CursorAnimator, ValueAnimator.AnimatorUpdateListener {

    private val duration: Long = 120
    private var animatorX: ValueAnimator = ValueAnimator()
    private var animatorY: ValueAnimator = ValueAnimator()
    private var animatorBgBottom: ValueAnimator = ValueAnimator()
    private var animatorBackground: ValueAnimator = ValueAnimator()
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var startSize: Float = 0f
    private var startBottom: Float = 0f
    private var lastAnimateTime: Long = 0

    private fun getHeightOfRows(rowCount: Int): Float {
        return (editor.rowHeight * rowCount).toFloat()
    }

    override fun markStartPos() {
        val line = editor.cursor.leftLine
        val pos = editor.layout.getCharLayoutOffset(line, editor.cursor.leftColumn)
        startX = editor.measureTextRegionOffset() + pos[1]
        startY = pos[0] - minusHeight()
        startSize = getHeightOfRows(editor.layout.getRowCountForLine(line))
        startBottom = editor.layout.getCharLayoutOffset(line, editor.text.getColumnCount(line))[0]
    }

    override fun isRunning(): Boolean {
        return animatorX.isRunning || animatorY.isRunning || animatorBackground.isRunning || animatorBgBottom.isRunning
    }

    override fun cancel() {
        animatorX.cancel()
        animatorY.cancel()
        animatorBackground.cancel()
        animatorBgBottom.cancel()
    }

    private fun minusHeight(): Float {
        return if (editor.props.textBackgroundWrapTextOnly) editor.lineSpacingPixels / 2f else 0f
    }

    override fun markEndPos() {
        if (!editor.isCursorAnimationEnabled) {
            return
        }
        if (isRunning()) {
            startX = animatedX()
            startY = animatedY()
            startSize = animatorBackground.animatedValue as Float
            startBottom = animatorBgBottom.animatedValue as Float
            cancel()
        }
        if (System.currentTimeMillis() - lastAnimateTime < 100) {
            return
        }
        val line = editor.cursor.leftLine
        animatorX.removeAllUpdateListeners()
        val pos = editor.layout.getCharLayoutOffset(editor.cursor.leftLine, editor.cursor.leftColumn)

        animatorX = ValueAnimator.ofFloat(startX, pos[1] + editor.measureTextRegionOffset())
        animatorY = ValueAnimator.ofFloat(startY, pos[0] - minusHeight())

        animatorBackground = ValueAnimator.ofFloat(
            startSize,
            getHeightOfRows(editor.layout.getRowCountForLine(editor.cursor.leftLine))
        )
        animatorBgBottom = ValueAnimator.ofFloat(
            startBottom,
            editor.layout.getCharLayoutOffset(line, editor.text.getColumnCount(line))[0]
        )

        animatorX.addUpdateListener(this)

        animatorX.duration = duration
        animatorY.duration = duration
        animatorBackground.duration = duration
        animatorBgBottom.duration = duration
    }

    override fun start() {
        if (!editor.isCursorAnimationEnabled || System.currentTimeMillis() - lastAnimateTime < 100) {
            lastAnimateTime = System.currentTimeMillis()
            return
        }
        animatorX.start()
        animatorY.start()
        animatorBackground.start()
        animatorBgBottom.start()

        lastAnimateTime = System.currentTimeMillis()
    }

    override fun animatedX(): Float {
        return (animatorX.animatedValue as? Float) ?: 0f
    }

    override fun animatedY(): Float {
        return (animatorY.animatedValue as? Float) ?: 0f
    }

    override fun animatedLineHeight(): Float {
        return (animatorBackground.animatedValue as? Float) ?: 0f
    }

    override fun animatedLineBottom(): Float {
        return (animatorBgBottom.animatedValue as? Float) ?: 0f
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        editor.postInvalidateOnAnimation()
    }
}
