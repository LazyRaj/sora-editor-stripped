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

import android.animation.Animator
import android.animation.ValueAnimator
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.style.CursorAnimator

/**
 * Fade-in/Fade-out cursor animation
 *
 * @author Dmitry Rubtsov
 */
open class FadeCursorAnimator(private val editor: CodeEditor) : CursorAnimator, ValueAnimator.AnimatorUpdateListener {

    private val duration: Long = 200

    private var fadeInAnimator: ValueAnimator = ValueAnimator()
    private var fadeOutAnimator: ValueAnimator = ValueAnimator()

    private var phaseEnded: Boolean = false
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
        return fadeInAnimator.isRunning || fadeOutAnimator.isRunning
    }

    override fun cancel() {
        fadeOutAnimator.cancel()
        fadeInAnimator.cancel()
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
        fadeOutAnimator.removeAllUpdateListeners()
        fadeInAnimator.removeAllUpdateListeners()

        val line = editor.cursor.leftLine
        lineHeight = editor.layout.getRowCountForLine(line) * editor.rowHeight.toFloat()
        lineBottom = editor.layout.getCharLayoutOffset(line, editor.text.getColumnCount(line))[0]

        val pos = editor.layout.getCharLayoutOffset(
            editor.cursor.leftLine,
            editor.cursor.leftColumn
        )
        endX = pos[1] + editor.measureTextRegionOffset()
        endY = pos[0]

        fadeOutAnimator = ValueAnimator.ofInt(255, 0).apply {
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
                override fun onAnimationStart(animator: Animator) {
                    phaseEnded = false
                }
                override fun onAnimationEnd(animator: Animator) {
                    phaseEnded = true
                }
            })
            addUpdateListener(this@FadeCursorAnimator)
            setDuration(this@FadeCursorAnimator.duration)
        }

        fadeInAnimator = ValueAnimator.ofInt(0, 255).apply {
            addUpdateListener(this@FadeCursorAnimator)
            startDelay = this@FadeCursorAnimator.duration
            setDuration(this@FadeCursorAnimator.duration)
        }
    }

    override fun start() {
        if (!editor.isCursorAnimationEnabled || System.currentTimeMillis() - lastAnimateTime < 100) {
            lastAnimateTime = System.currentTimeMillis()
            return
        }
        fadeOutAnimator.start()
        fadeInAnimator.start()
        lastAnimateTime = System.currentTimeMillis()
    }

    override fun animatedX(): Float {
        if (phaseEnded || editor.insertHandleDescriptor.position.isEmpty) {
            return endX
        }
        return startX
    }

    override fun animatedY(): Float {
        if (phaseEnded || editor.insertHandleDescriptor.position.isEmpty) {
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
        editor.handleStyle.setAlpha(animation.animatedValue as Int)
        editor.postInvalidateOnAnimation()
    }
}
