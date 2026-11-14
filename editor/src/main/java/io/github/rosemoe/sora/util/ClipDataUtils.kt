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
package io.github.rosemoe.sora.util

import android.content.ClipData
import android.content.Intent

object ClipDataUtils {

    fun clipDataToString(clipData: ClipData?): String {
        if (clipData == null || clipData.itemCount == 0) {
            return ""
        }
        val sb = StringBuilder()
        for (i in 0 until clipData.itemCount) {
            if (i > 0) {
                sb.append('\n')
            }
            val item = clipData.getItemAt(i)
            val text = item.text
            if (text != null) {
                sb.append(text)
            } else {
                val uri = item.uri
                if (uri != null) {
                    sb.append(uri.toString())
                } else {
                    val intent = item.intent
                    if (intent != null) {
                        sb.append(intent.toUri(Intent.URI_INTENT_SCHEME))
                    }
                }
            }
        }
        return sb.toString()
    }
}

fun ClipData?.toPlainText(): String = ClipDataUtils.clipDataToString(this)

