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

abstract class ObjectPool<T : Any>(size: Int = 16) {

    private val pool: Array<Any?> = arrayOfNulls(size)

    open fun recycle(obj: T?) {
        if (obj == null) {
            return
        }
        onRecycleObject(obj)
        synchronized(this) {
            for (i in pool.indices) {
                if (pool[i] == null) {
                    pool[i] = obj
                    break
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    open fun obtain(): T {
        var result: T? = null
        synchronized(this) {
            for (i in pool.size - 1 downTo 0) {
                if (pool[i] != null) {
                    result = pool[i] as T?
                    pool[i] = null
                    break
                }
            }
        }
        return result ?: allocateNew()
    }

    protected open fun onRecycleObject(recycledObj: T) {
    }

    protected abstract fun allocateNew(): T
}

inline fun <T : Any, R> ObjectPool<T>.use(block: (T) -> R): R {
    val obj = obtain()
    return try {
        block(obj)
    } finally {
        recycle(obj)
    }
}

