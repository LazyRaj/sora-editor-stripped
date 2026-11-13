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
package io.github.rosemoe.sora.widget.component

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.View
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.annotation.FloatRange
import androidx.annotation.Px
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.EventManager
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Magnifier specially designed for CodeEditor
 *
 * @author Rosemoe
 */
open class Magnifier(
    private val view: CodeEditor
) : EditorBuiltinComponent {

    private val eventManager: EventManager = view.createSubEventManager()
    private val popup: PopupWindow = PopupWindow()
    private val image: ImageView
    private val paint: Paint = Paint()

    @Px
    var maxTextSize: Float
    private var x: Int = 0
    private var y: Int = 0
    private var _enabled: Boolean = true
    var isWithinEditorForcibly: Boolean = false
    var parentView: View = view

    @FloatRange(from = 1.0, fromInclusive = false)
    var scaleFactor: Float = 1.25f
        set(value) {
            require(value > 1.0f) { "factor can not be under 1.0" }
            field = value
        }

    init {
        popup.elevation = view.dpUnit * 4
        @SuppressLint("InflateParams")
        val popupView = LayoutInflater.from(view.context).inflate(R.layout.magnifier_popup, null)
        image = popupView.findViewById(R.id.magnifier_image_view)
        popup.height = (view.dpUnit * 70).toInt()
        popup.width = (view.dpUnit * 100).toInt()
        popup.contentView = popupView
        maxTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            28f,
            popupView.resources.displayMetrics
        )
        applyBackgroundTint()
        eventManager.subscribeAlways(ColorSchemeUpdateEvent::class.java) {
            applyBackgroundTint()
        }
    }

    private fun applyBackgroundTint() {
        val background = popup.contentView?.background
        background?.setTint(view.colorScheme.getColor(EditorColorScheme.WHOLE_BACKGROUND))
    }

    override fun isEnabled(): Boolean = _enabled

    override fun setEnabled(enabled: Boolean) {
        _enabled = enabled
        if (!enabled) {
            dismiss()
        }
    }

    open fun show(x: Int, y: Int) {
        if (!_enabled) {
            return
        }
        if (abs(x - this.x) < 2 && abs(y - this.y) < 2) {
            return
        }
        if (view.textSizePx > maxTextSize) {
            if (isShowing) {
                dismiss()
            }
            return
        }
        popup.width = min(view.width * 3 / 5, (view.dpUnit * 250).toInt())
        this.x = x
        this.y = y
        val pos = IntArray(2)
        view.getLocationInWindow(pos)
        var left = max(pos[0] + x - popup.width / 2, 0)
        var right = left + popup.width
        if (right > view.width + pos[0]) {
            right = view.width + pos[0]
            left = max(0, right - popup.width)
        }
        val top = max(pos[1] + y - popup.height - view.rowHeight, 0)
        if (popup.isShowing) {
            popup.update(left, top, popup.width, popup.height)
        } else {
            popup.showAtLocation(parentView, Gravity.START or Gravity.TOP, left, top)
        }
        updateDisplay()
    }

    open val isShowing: Boolean
        get() = popup.isShowing

    open fun dismiss() {
        popup.dismiss()
    }

    open fun updateDisplay() {
        if (!isShowing) {
            return
        }
        if (!isWithinEditorForcibly && isPixelCopyApplicable()) {
            updateDisplayPixelCopy(view.context as Activity)
        } else {
            updateDisplayWithinEditor()
        }
    }

    private fun isPixelCopyApplicable(): Boolean {
        val ctx = view.context
        if (ctx !is Activity) {
            return false
        }
        val localWndId = view.windowId
        val activityWnd = ctx.window ?: return false
        val activityWndId = activityWnd.decorView.windowId
        return localWndId != null && localWndId == activityWndId
    }

    private fun updateDisplayPixelCopy(activity: Activity) {
        val requiredWidth = (popup.width / scaleFactor).toInt()
        val requiredHeight = (popup.height / scaleFactor).toInt()

        var left = max(x - requiredWidth / 2, 0)
        var top = max(y - requiredHeight / 2, 0)
        var right = min(left + requiredWidth, view.width)
        var bottom = min(top + requiredHeight, view.height)
        if (right - left < requiredWidth) {
            left = max(0, right - requiredWidth)
        }
        if (bottom - top < requiredHeight) {
            top = max(0, bottom - requiredHeight)
        }
        if (right - left <= 0 || bottom - top <= 0) {
            dismiss()
            return
        }
        val pos = IntArray(2)
        view.getLocationInWindow(pos)
        val clip = Bitmap.createBitmap(right - left, bottom - top, Bitmap.Config.ARGB_8888)
        try {
            PixelCopy.request(
                activity.window,
                Rect(pos[0] + left, pos[1] + top, pos[0] + right, pos[1] + bottom),
                clip,
                { statusCode ->
                    if (statusCode == PixelCopy.SUCCESS) {
                        val dest =
                            Bitmap.createBitmap(popup.width, popup.height, Bitmap.Config.ARGB_8888)
                        val scaled =
                            Bitmap.createScaledBitmap(clip, popup.width, popup.height, true)
                        clip.recycle()

                        val canvas = Canvas(dest)
                        paint.reset()
                        paint.isAntiAlias = true
                        canvas.drawARGB(0, 0, 0, 0)
                        val roundFactor = 6
                        canvas.drawRoundRect(
                            0f,
                            0f,
                            popup.width.toFloat(),
                            popup.height.toFloat(),
                            view.dpUnit * roundFactor,
                            view.dpUnit * roundFactor,
                            paint
                        )
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                        canvas.drawBitmap(scaled, 0f, 0f, paint)
                        scaled.recycle()

                        image.setImageBitmap(dest)
                    } else {
                        Log.w("Magnifier", "Failed to copy pixels, error = $statusCode")
                    }
                },
                view.handler
            )
        } catch (e: IllegalArgumentException) {
            dismiss()
            if (!clip.isRecycled) {
                clip.recycle()
            }
        }
    }

    private fun updateDisplayWithinEditor() {
        if (popup.width <= 0 || popup.height <= 0) {
            dismiss()
            return
        }
        val dest = Bitmap.createBitmap(popup.width, popup.height, Bitmap.Config.ARGB_8888)
        val requiredWidth = (popup.width / scaleFactor).toInt()
        val requiredHeight = (popup.height / scaleFactor).toInt()

        var left = max(x - requiredWidth / 2, 0)
        var top = max(y - requiredHeight / 2, 0)
        val right = min(left + requiredWidth, view.width)
        val bottom = min(top + requiredHeight, view.height)
        if (right - left < requiredWidth) {
            left = max(0, right - requiredWidth)
        }
        if (bottom - top < requiredHeight) {
            top = max(0, bottom - requiredHeight)
        }
        if (right - left <= 0 || bottom - top <= 0) {
            dismiss()
            dest.recycle()
            return
        }
        val clip = Bitmap.createBitmap(requiredWidth, requiredHeight, Bitmap.Config.ARGB_8888)
        val viewCanvas = Canvas(clip)
        viewCanvas.translate((-left - view.offsetX).toFloat(), (-top - view.offsetY).toFloat())
        view.draw(viewCanvas)
        val scaled = Bitmap.createScaledBitmap(clip, popup.width, popup.height, true)
        clip.recycle()

        val canvas = Canvas(dest)
        paint.reset()
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        val roundFactor = 6
        canvas.drawRoundRect(
            0f,
            0f,
            popup.width.toFloat(),
            popup.height.toFloat(),
            view.dpUnit * roundFactor,
            view.dpUnit * roundFactor,
            paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        scaled.recycle()

        image.setImageBitmap(dest)
    }
}
