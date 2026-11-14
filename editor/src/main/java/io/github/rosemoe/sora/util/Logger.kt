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

import android.util.Log
import java.util.WeakHashMap

class Logger private constructor(val name: String) {

    companion object {
        private val map = WeakHashMap<String, Logger>()

        @Synchronized
        fun instance(name: String): Logger {
            var logger = map[name]
            if (logger == null) {
                logger = Logger(name)
                map[name] = logger
            }
            return logger
        }
    }

    fun d(msg: String) {
        Log.d(name, msg)
    }

    fun d(msg: String, vararg format: Any?) {
        Log.d(name, java.lang.String.format(msg, *format))
    }

    fun i(msg: String) {
        Log.i(name, msg)
    }

    fun i(msg: String, vararg format: Any?) {
        Log.i(name, java.lang.String.format(msg, *format))
    }

    fun v(msg: String) {
        Log.v(name, msg)
    }

    fun v(msg: String, vararg format: Any?) {
        Log.v(name, java.lang.String.format(msg, *format))
    }

    fun w(msg: String) {
        Log.w(name, msg)
    }

    fun w(msg: String, vararg format: Any?) {
        Log.w(name, java.lang.String.format(msg, *format))
    }

    fun w(msg: String, e: Throwable) {
        Log.w(name, msg, e)
    }

    fun w(msg: String, e: Throwable, vararg format: Any?) {
        Log.w(name, java.lang.String.format(msg, *format), e)
    }

    fun e(msg: String) {
        Log.e(name, msg)
    }

    fun e(msg: String, vararg format: Any?) {
        Log.e(name, java.lang.String.format(msg, *format))
    }

    fun e(msg: String, e: Throwable) {
        Log.e(name, msg, e)
    }

    fun e(msg: String, e: Throwable, vararg format: Any?) {
        Log.e(name, java.lang.String.format(msg, *format), e)
    }
}
