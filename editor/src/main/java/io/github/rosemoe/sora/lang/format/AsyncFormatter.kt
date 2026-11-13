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
package io.github.rosemoe.sora.lang.format

import android.util.Log
import androidx.annotation.WorkerThread
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextRange
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Base class for formatting code in another thread.
 */
abstract class AsyncFormatter : Formatter {

    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()
    private var receiver: WeakReference<Formatter.FormatResultReceiver>? = null

    @Volatile
    private var text: Content? = null

    @Volatile
    private var range: TextRange? = null

    @Volatile
    private var cursorRange: TextRange? = null

    private var thread: FormattingThread? = null

    override fun setReceiver(receiver: Formatter.FormatResultReceiver?) {
        this.receiver = receiver?.let { WeakReference(it) }
    }

    private fun run() {
        val currentThread = thread
        if (currentThread == null || !currentThread.isAlive) {
            // Create new thread
            Log.v(LOG_TAG, "Starting a new thread for formatting")
            val newThread = FormattingThread().apply {
                isDaemon = true
                name = "AsyncFormatter-${threadIdCounter.incrementAndGet()}"
            }
            thread = newThread
            newThread.start()
        } else {
            // Wake up thread
            Log.v(LOG_TAG, "Waking up thread for formatting")
            lock.withLock {
                condition.signal()
            }
        }
    }

    override fun format(text: Content, cursorRange: TextRange) {
        this.text = text
        this.range = null
        this.cursorRange = cursorRange
        run()
    }

    override fun isRunning(): Boolean {
        val currentThread = thread
        return currentThread != null && currentThread.isAlive && lock.isLocked
    }

    override fun formatRegion(text: Content, rangeToFormat: TextRange, cursorRange: TextRange) {
        this.text = text
        this.range = rangeToFormat
        this.cursorRange = cursorRange
        run()
    }

    /**
     * like [Formatter.format], but run in background thread.
     *
     * Implementation of this method can edit text directly to generate formatted code.
     *
     * @return the new cursor range to be applied to the text
     */
    @WorkerThread
    abstract fun formatAsync(text: Content, cursorRange: TextRange): TextRange?

    /**
     * like [Formatter.formatRegion], but run in background thread
     *
     * Implementation of this method can edit text directly to generate formatted code.
     *
     * @return the new cursor range to be applied to the text
     */
    @WorkerThread
    abstract fun formatRegionAsync(text: Content, rangeToFormat: TextRange, cursorRange: TextRange): TextRange?

    private fun sendUpdate(text: Content, cursorRange: TextRange?) {
        val r = receiver?.get()
        if (!Thread.currentThread().isInterrupted && r != null) {
            r.onFormatSucceed(text, cursorRange)
        }
    }

    private fun sendFailure(throwable: Throwable) {
        val r = receiver?.get()
        if (!Thread.currentThread().isInterrupted && r != null) {
            r.onFormatFail(throwable)
        }
    }

    override fun cancel() {
        thread?.let { t ->
            if (t.isAlive) {
                t.interrupt()
            }
            thread = null
        }
    }

    override fun destroy() {
        thread?.let { t ->
            if (t.isAlive) {
                t.interrupt()
            }
        }
        thread = null
        receiver = null
        text = null
        range = null
        cursorRange = null
    }

    private inner class FormattingThread : Thread() {

        override fun run() {
            Log.v(LOG_TAG, "AsyncFormatter thread started")
            try {
                lock.lock()
                try {
                    while (!isInterrupted) {
                        val currentText = text
                        val currentCursorRange = cursorRange
                        if (currentText == null || currentCursorRange == null) {
                            condition.await()
                            continue
                        }
                        val currentRange = range
                        val newRange = if (currentRange == null) {
                            formatAsync(currentText, currentCursorRange)
                        } else {
                            formatRegionAsync(currentText, currentRange, currentCursorRange)
                        }
                        sendUpdate(currentText, newRange)
                        text = null
                        range = null
                        condition.await()
                    }
                } finally {
                    lock.unlock()
                }
            } catch (e: InterruptedException) {
                Log.v(LOG_TAG, "Thread is interrupted.")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Unexpected exception is thrown in the thread.", e)
                sendFailure(e)
            }
        }
    }

    companion object {
        private const val LOG_TAG = "AsyncFormatter"
        private val threadIdCounter = AtomicInteger(0)
    }
}
