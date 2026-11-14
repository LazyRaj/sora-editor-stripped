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

object TemporaryFloatBuffer {

    val sCache: FloatArrayCache = FloatArrayCache()

    fun obtain(len: Int): FloatArray {
        return sCache.obtain(len)
    }

    fun recycle(temp: FloatArray) {
        sCache.recycle(temp)
    }

    class FloatArrayCache {

        private var temp: FloatArray? = null

        fun obtain(len: Int): FloatArray {
            val buf: FloatArray?
            synchronized(this) {
                buf = temp
                temp = null
            }
            return if (buf == null || buf.size < len) {
                FloatArray(len)
            } else {
                buf
            }
        }

        fun recycle(temp: FloatArray) {
            if (temp.size > 1000) return
            synchronized(this) {
                this.temp = temp
            }
        }
    }
}

inline fun <R> TemporaryFloatBuffer.use(len: Int, block: (FloatArray) -> R): R {
    val buf = obtain(len)
    return try {
        block(buf)
    } finally {
        recycle(buf)
    }
}

