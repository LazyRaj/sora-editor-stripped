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

package io.github.rosemoe.sora.text

import android.util.Log
import io.github.rosemoe.sora.lang.styling.Span
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

open class SpanRecycler private constructor() {

    private val taskQueue: BlockingQueue<MutableList<Span>> = ArrayBlockingQueue(8)
    private var recycleThread: Thread? = null

    open fun recycle(spans: MutableList<Span>?) {
        if (spans == null) {
            return
        }
        val t = recycleThread
        if (t == null || !t.isAlive) {
            val newThread = RecycleThread()
            recycleThread = newThread
            newThread.start()
        }
        taskQueue.offer(spans)
    }

    private inner class RecycleThread : Thread() {

        init {
            isDaemon = true
            name = "SpanRecycleDaemon"
        }

        override fun run() {
            try {
                while (!isInterrupted) {
                    try {
                        val spans = taskQueue.take()
                        var count = 0
                        val size = spans.size
                        for (i in 0 until size) {
                            val recycled = spans.removeAt(size - 1 - i).recycle()
                            if (!recycled) {
                                break
                            }
                            count++
                        }
                        Log.i(LOG_TAG, "Called recycle() on $count spans")
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(LOG_TAG, e)
            }
            Log.i(LOG_TAG, "Recycler exited")
        }
    }

    companion object {
        private const val LOG_TAG = "SpanRecycler"

        @Volatile
        private var instance: SpanRecycler? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): SpanRecycler {
            return instance ?: SpanRecycler().also { instance = it }
        }
    }
}
