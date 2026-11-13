/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.lang.analysis

import android.os.Bundle
import android.util.Log
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Built-in implementation of [AnalyzeManager].
 *
 * This is a simple version without any incremental actions.
 *
 * The analysis will always re-run when the text changes. Hopefully, it will stop previous outdated
 * runs by providing a [Delegate] object.
 *
 * @param <V> The shared object type that we get for auto-completion.
 */
abstract class SimpleAnalyzeManager<V> : AnalyzeManager {

    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()
    private var receiver: StyleReceiver? = null

    @Volatile
    private var ref: ContentReference? = null
    private var extraArguments: Bundle? = null

    @Volatile
    private var newestRequestId: Long = 0L
    private var thread: AnalyzeThread? = null

    @get:JvmName("getData")
    var data: V? = null
        private set

    override fun setReceiver(receiver: StyleReceiver?) {
        this.receiver = receiver
    }

    override fun reset(content: ContentReference, extraArguments: Bundle) {
        this.ref = content
        this.extraArguments = extraArguments
        rerun()
    }

    override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {
        rerun()
    }

    override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {
        rerun()
    }

    @Synchronized
    override fun rerun() {
        newestRequestId++
        val currentThread = thread
        if (currentThread == null || !currentThread.isAlive) {
            Log.v(LOG_TAG, "Starting a new thread for analysis")
            val newThread = AnalyzeThread().apply {
                isDaemon = true
                name = "SplAnalyzer-${threadIdCounter.incrementAndGet()}"
            }
            thread = newThread
            newThread.start()
        }
        lock.withLock {
            condition.signal()
        }
    }

    override fun destroy() {
        ref = null
        extraArguments = null
        newestRequestId = 0L
        data = null
        thread?.let { t ->
            if (t.isAlive) {
                t.interrupt()
            }
        }
        thread = null
        receiver = null
    }

    /**
     * Get extra arguments set by CodeEditor.setText(CharSequence, Bundle)
     */
    fun getExtraArguments(): Bundle? = extraArguments

    /**
     * Analyze the given input.
     *
     * @param text A [StringBuilder] instance containing the text in editor. DO NOT SAVE THE INSTANCE OR
     *             UPDATE IT. It is continuously used by this analyzer.
     * @param delegate A delegate used to check whether this invocation is outdated. You should stop your logic
     *                 if [Delegate.isCancelled] returns true.
     * @return Styles created according to the text.
     */
    protected abstract fun analyze(text: StringBuilder, delegate: Delegate<V>): Styles?

    /**
     * Analyze thread.
     *
     * The thread will keep alive unless there is any exception or [AnalyzeManager.destroy] is called.
     */
    private inner class AnalyzeThread : Thread() {

        /**
         * Single instance for text storing
         */
        private val textContainer = StringBuilder()

        override fun run() {
            Log.v(LOG_TAG, "Analyze thread started")
            try {
                while (!isInterrupted) {
                    var text = ref
                    if (text != null) {
                        var requestId: Long = 0L
                        var result: Styles? = null
                        var newData: V? = null
                        // Do the analysis, until the requestId matches
                        do {
                            text = ref
                            if (text == null) {
                                break
                            }
                            requestId = newestRequestId
                            val delegate = Delegate<V>(requestId)

                            // Collect line contents
                            textContainer.setLength(0)
                            textContainer.ensureCapacity(text.length)
                            for (i in 0 until text.lineCount) {
                                if (requestId != newestRequestId) break
                                if (i != 0) {
                                    textContainer.append(text.getLineSeparator(i - 1))
                                }
                                text.appendLineTo(textContainer, i)
                            }

                            // Invoke the implementation
                            result = analyze(textContainer, delegate)
                            newData = delegate.data
                        } while (requestId != newestRequestId)

                        // Send result
                        val currentReceiver = this@SimpleAnalyzeManager.receiver
                        if (currentReceiver != null && result != null) {
                            currentReceiver.setStyles(this@SimpleAnalyzeManager, result)
                        }
                        this@SimpleAnalyzeManager.data = newData
                    }

                    // Wait for next time
                    lock.withLock {
                        condition.await()
                    }
                }
            } catch (_: InterruptedException) {
                Log.v(LOG_TAG, "Thread is interrupted.")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Unexpected exception is thrown in the thread.", e)
            }
        }
    }

    /**
     * Delegate between manager and analysis implementation
     */
    inner class Delegate<T>(private val myRequestId: Long) {

        var data: T? = null

        /**
         * Check whether the operation is cancelled
         */
        fun isCancelled(): Boolean {
            return myRequestId != newestRequestId
        }
    }

    companion object {
        private const val LOG_TAG = "SimpleAnalyzeManager"
        private val threadIdCounter = AtomicInteger(0)
    }
}
