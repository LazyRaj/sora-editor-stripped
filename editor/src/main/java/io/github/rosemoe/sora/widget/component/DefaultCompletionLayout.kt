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

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

open class DefaultCompletionLayout : CompletionLayout {

    companion object {
        private const val TAG = "DefaultCompletionLayout"
        private const val MAX_SCROLL_ITERATIONS = 100
    }

    private var listView: ListView? = null
    private var progressBar: ProgressBar? = null
    private var rootView: LinearLayout? = null
    private var editorAutoCompletion: EditorAutoCompletion? = null

    override fun setEditorCompletion(completion: EditorAutoCompletion) {
        editorAutoCompletion = completion
    }

    override fun setEnabledAnimation(enabledAnimation: Boolean) {
        val root = rootView
        val list = listView
        if (enabledAnimation) {
            val transition = LayoutTransition().apply {
                enableTransitionType(LayoutTransition.CHANGING)
                enableTransitionType(LayoutTransition.APPEARING)
                enableTransitionType(LayoutTransition.DISAPPEARING)
                enableTransitionType(LayoutTransition.CHANGE_APPEARING)
                enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
                addTransitionListener(object : LayoutTransition.TransitionListener {
                    override fun startTransition(
                        transition: LayoutTransition,
                        container: ViewGroup,
                        view: View,
                        transitionType: Int
                    ) {
                    }

                    override fun endTransition(
                        transition: LayoutTransition,
                        container: ViewGroup,
                        view: View,
                        transitionType: Int
                    ) {
                        if (view === list) {
                            view.requestLayout()
                        }
                    }
                })
            }
            root?.layoutTransition = transition
            list?.layoutTransition = transition
        } else {
            root?.layoutTransition = null
            list?.layoutTransition = null
        }
    }

    override fun inflate(context: Context): View {
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootView = rootLayout
        val lv = ListView(context)
        listView = lv
        val pb = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        progressBar = pb

        setEnabledAnimation(false)

        rootLayout.addView(
            pb,
            LinearLayout.LayoutParams(
                -1,
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    20f,
                    context.resources.displayMetrics
                ).toInt()
            )
        )
        rootLayout.addView(lv, LinearLayout.LayoutParams(-1, -1))

        pb.isIndeterminate = true
        val progressBarLayoutParams = pb.layoutParams as LinearLayout.LayoutParams
        val dm = context.resources.displayMetrics
        progressBarLayoutParams.topMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -8f, dm).toInt()
        progressBarLayoutParams.bottomMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -8f, dm).toInt()
        progressBarLayoutParams.leftMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, dm).toInt()
        progressBarLayoutParams.rightMargin =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, dm).toInt()

        val gd = GradientDrawable().apply {
            cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, dm)
        }
        rootLayout.background = gd

        setRootViewOutlineProvider(rootLayout)

        lv.dividerHeight = 0
        setLoading(true)

        lv.setOnItemClickListener { _, _, position, _ ->
            try {
                editorAutoCompletion?.select(position)
            } catch (e: Exception) {
                e.printStackTrace(System.err)
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        return rootLayout
    }

    override fun onApplyColorScheme(colorScheme: EditorColorScheme) {
        val root = rootView ?: return
        val completion = editorAutoCompletion ?: return
        val dm = completion.context.resources.displayMetrics
        val gd = GradientDrawable().apply {
            cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, dm)
            setStroke(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm).toInt(),
                colorScheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER)
            )
            setColor(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND))
        }
        root.background = gd
        setRootViewOutlineProvider(root)
    }

    override fun setLoading(loading: Boolean) {
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }

    @Suppress("UNCHECKED_CAST")
    override fun getCompletionList(): AdapterView<Adapter> {
        return (listView
            ?: throw IllegalStateException("Layout not inflated")) as AdapterView<Adapter>
    }

    private fun performScrollList(offset: Int) {
        val adpView = getCompletionList()
        val down = SystemClock.uptimeMillis()
        var ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        adpView.onTouchEvent(ev)
        ev.recycle()

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_MOVE, 0f, offset.toFloat(), 0)
        adpView.onTouchEvent(ev)
        ev.recycle()

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_CANCEL, 0f, offset.toFloat(), 0)
        adpView.onTouchEvent(ev)
        ev.recycle()
    }

    private fun setRootViewOutlineProvider(root: View) {
        root.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0,
                    0,
                    view.width,
                    view.height,
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        8f,
                        view.context.resources.displayMetrics
                    )
                )
            }
        }
        root.clipToOutline = true
    }

    override fun ensureListPositionVisible(position: Int, incrementPixels: Int) {
        val lv = listView ?: return
        lv.post {
            if (position == 0 && incrementPixels == 0) {
                lv.setSelectionFromTop(0, 0)
                return@post
            }

            var upScrollIterations = 0
            while (lv.firstVisiblePosition + 1 > position &&
                lv.canScrollList(-1) &&
                upScrollIterations < MAX_SCROLL_ITERATIONS
            ) {
                performScrollList(incrementPixels / 2)
                upScrollIterations++
            }

            var downScrollIterations = 0
            while (lv.lastVisiblePosition - 1 < position &&
                lv.canScrollList(1) &&
                downScrollIterations < MAX_SCROLL_ITERATIONS
            ) {
                performScrollList(-incrementPixels / 2)
                downScrollIterations++
            }

            if (upScrollIterations >= MAX_SCROLL_ITERATIONS || downScrollIterations >= MAX_SCROLL_ITERATIONS) {
                Log.w(
                    TAG,
                    "ensureListPositionVisible hit iteration limit: position=$position, upScrolls=$upScrollIterations, downScrolls=$downScrollIterations"
                )
            }
        }
    }
}
