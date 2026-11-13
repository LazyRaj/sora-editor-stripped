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
package io.github.rosemoe.sora.widget.layout

import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Base layout implementation of [Layout].
 * It provides some convenient methods to editor instance and text measuring.
 *
 * @author Rosemoe
 */
abstract class AbstractLayout(
    @JvmField protected var editor: CodeEditor?,
    @JvmField protected var text: Content?
) : Layout {

    companion object {
        const val SUBTASK_COUNT = 8
        const val MIN_LINE_COUNT_FOR_SUBTASK = 3000
        private val executor: ThreadPoolExecutor

        init {
            val maximumPoolSize = max(2, Runtime.getRuntime().availableProcessors())
            val corePoolSize = 2
            executor = ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                1L,
                TimeUnit.MINUTES,
                LinkedBlockingQueue(128)
            )
        }
    }

    open fun getSpans(line: Int): List<Span> {
        val ed = editor ?: return emptyList()
        return ed.getSpansForLine(line)
    }

    open fun getInlayHints(line: Int): List<InlayHint> {
        val hints = editor?.inlayHints
        return hints?.getForLine(line) ?: emptyList()
    }

    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
    }

    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
    }

    override fun destroyLayout() {
        editor = null
        text = null
    }

    protected open fun submitTask(task: LayoutTask<*>) {
        executor.submit(task)
    }

    protected open class TaskMonitor(
        private val taskCount: Int,
        private val callback: Callback
    ) {
        private val results: Array<Any?> = arrayOfNulls(taskCount)
        private var completedCount = 0
        private var cancelledCount = 0

        @Synchronized
        fun reportCompleted(result: Any?) {
            results[completedCount++] = result
            if (completedCount == taskCount) {
                callback.onCompleted(results, cancelledCount)
            }
        }

        @Synchronized
        fun reportCancelled() {
            cancelledCount++
            reportCompleted(null)
        }

        fun interface Callback {
            fun onCompleted(results: Array<Any?>, cancelledCount: Int)
        }
    }

    protected abstract inner class LayoutTask<T>(
        private val monitor: TaskMonitor
    ) : Runnable {

        protected open fun shouldRun(): Boolean {
            return editor != null
        }

        override fun run() {
            if (shouldRun()) {
                val result = compute()
                monitor.reportCompleted(result)
            } else {
                monitor.reportCancelled()
            }
        }

        protected abstract fun compute(): T
    }
}
