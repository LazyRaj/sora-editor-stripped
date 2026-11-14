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

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import io.github.rosemoe.sora.R

/**
 * A simple symbol input view implementation for editor.
 *
 * First, add your symbols by [addSymbols].
 * Then, bind a certain editor by [bindEditor] so that it works.
 *
 * @author Rosemoe
 */
open class SymbolInputView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    private var _textColor: Int = 0

    var textColor: Int
        get() = _textColor
        set(value) {
            for (i in 0 until childCount) {
                (getChildAt(i) as? Button)?.setTextColor(value)
            }
            _textColor = value
        }

    var editor: CodeEditor? = null

    init {
        if (background == null) {
            setBackgroundColor(context.getColor(R.color.defaultSymbolInputBackgroundColor))
        }
        orientation = HORIZONTAL
        textColor = context.getColor(R.color.defaultSymbolInputTextColor)
    }

    /**
     * Bind editor for the view
     */
    open fun bindEditor(editor: CodeEditor?) {
        this.editor = editor
    }

    /**
     * Remove all added symbols
     */
    open fun removeSymbols() {
        removeAllViews()
    }

    /**
     * Add symbols to the view.
     *
     * @param display    The texts displayed in button
     * @param insertText The actual text to be inserted to editor when the button is clicked
     */
    open fun addSymbols(display: Array<String>, insertText: Array<String>) {
        val count = maxOf(display.size, insertText.size)
        for (i in 0 until count) {
            val btn = Button(context, null, android.R.attr.buttonStyleSmall).apply {
                text = display[i]
                background = ColorDrawable(Color.TRANSPARENT)
                setTextColor(_textColor)
                setOnClickListener {
                    val currentEditor = editor
                    if (currentEditor == null || !currentEditor.isEditable) {
                        return@setOnClickListener
                    }

                    if ("\t" == insertText[i]) {
                        if (currentEditor.snippetController.isInSnippet()) {
                            currentEditor.snippetController.shiftToNextTabStop()
                        } else {
                            currentEditor.indentOrCommitTab()
                        }
                    } else {
                        currentEditor.insertText(insertText[i], 1)
                    }
                }
            }
            addView(btn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
    }

    open fun forEachButton(consumer: ButtonConsumer) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is Button) {
                consumer.accept(child)
            }
        }
    }

    fun interface ButtonConsumer {
        fun accept(btn: Button)
    }
}
