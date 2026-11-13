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

import android.graphics.Paint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Default adapter to display results
 *
 * @author Rose
 */
class DefaultCompletionItemAdapter : EditorCompletionAdapter() {

    override fun getItemHeight(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            45f,
            getContext().resources.displayMetrics
        ).toInt()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?,
        isCurrentCursorPosition: Boolean
    ): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(getContext())
                .inflate(R.layout.default_completion_result_item, parent, false)
        }
        val item = getItem(position)

        var tv: TextView = view!!.findViewById(R.id.result_item_label)
        tv.text = item.label
        tv.setTextColor(getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY))
        tv.isSelected = isCurrentCursorPosition
        if (item.deprecated) {
            tv.paintFlags = tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            tv.paintFlags = tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        tv = view.findViewById(R.id.result_item_detail)
        tv.text = item.detail
        tv.setTextColor(getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY))
        tv.isSelected = isCurrentCursorPosition
        if (item.deprecated) {
            tv.paintFlags = tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            tv.paintFlags = tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        tv = view.findViewById(R.id.result_item_desc)
        tv.text = item.desc
        tv.setTextColor(getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY))
        tv.isSelected = isCurrentCursorPosition

        view.tag = position
        if (isCurrentCursorPosition) {
            view.setBackgroundColor(getThemeColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT))
        } else {
            view.setBackgroundColor(0)
        }
        val iv: ImageView = view.findViewById(R.id.result_item_image)
        iv.setImageDrawable(item.icon)
        return view
    }
}
