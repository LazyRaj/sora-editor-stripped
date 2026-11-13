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

import android.os.Message
import android.util.Log
import io.github.rosemoe.sora.annotations.Experimental
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.util.BaseAnalyzeManager
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.util.Collections
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Asynchronous base implementation of [IncrementalAnalyzeManager].
 *
 * @author Rosemoe
 */
abstract class AsyncIncrementalAnalyzeManager<S, T> @JvmOverloads constructor(
    private val useShallowCopy: Boolean = isUseShallowCopyByDefault
) : BaseAnalyzeManager(), IncrementalAnalyzeManager<S, T> {

    private var thread: LooperThread? = null

    /**
     * Not guarded by `this`: subclasses such as TextMateAnalyzer synchronize
     * `tokenizeLine` on the manager, so taking the same monitor here made every edit on the
     * UI thread wait for the analysis thread to finish tokenizing its current line.
     */
    private final val runCount = AtomicLong()

    /**
     * Run the given code block only when the receiver is currently non-null
     */
    protected fun withReceiver(consumer: ReceiverConsumer) {
        val r = receiver
        if (r != null) {
            consumer.accept(r)
        }
    }

    override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {
        val currentThread = thread
        if (currentThread != null) {
            increaseRunCount()
            currentThread.offerMessage(
                MSG_MOD,
                TextModification(IntPair.pack(start.line, start.column), IntPair.pack(end.line, end.column), insertedContent)
            )
        }
    }

    override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {
        val currentThread = thread
        if (currentThread != null) {
            increaseRunCount()
            currentThread.offerMessage(
                MSG_MOD,
                TextModification(IntPair.pack(start.line, start.column), IntPair.pack(end.line, end.column), null)
            )
        }
    }

    override fun rerun() {
        thread?.let { t ->
            if (t.isAlive) {
                t.interrupt()
                t.abort = true
            }
        }
        thread = null

        val ref = contentRef
        if (ref != null) {
            val text = ref.reference.copyText(false, useShallowCopy)
            text.isUndoEnabled = false
            val newThread = LooperThread().apply {
                name = "AsyncAnalyzer-${threadIdCounter.incrementAndGet()}"
                offerMessage(MSG_INIT, text)
            }
            thread = newThread
            increaseRunCount()
            sendNewStyles(null)
            newThread.start()
        }
    }

    override fun getState(line: Int): IncrementalAnalyzeManager.LineTokenizeResult<S, T>? {
        val currentThread = thread
        if (currentThread === Thread.currentThread()) {
            if (line in 0 until currentThread.states.size) {
                return currentThread.states[line]
            }
            return null
        }
        throw SecurityException("Can not get state from non-analytical or abandoned thread")
    }

    override fun onAbandonState(state: S) {
    }

    override fun onAddState(state: S) {
    }

    private fun increaseRunCount() {
        runCount.incrementAndGet()
    }

    override fun destroy() {
        thread?.let { t ->
            if (t.isAlive) {
                t.interrupt()
            }
            t.abort = true
        }
        thread = null
        super.destroy()
    }

    private fun sendNewStyles(styles: Styles?) {
        val r = receiver
        if (r != null) {
            r.setStyles(this, styles)
        }
    }

    private fun sendUpdate(styles: Styles, startLine: Int, endLine: Int) {
        val r = receiver
        if (r != null) {
            r.updateStyles(this, styles, SequenceUpdateRange(startLine, endLine))
        }
    }

    /**
     * Compute code blocks
     *
     * @param text The text. Can be safely accessed.
     */
    abstract fun computeBlocks(text: Content, delegate: CodeBlockAnalyzeDelegate): List<CodeBlock>?

    val managedStyles: Styles?
        get() {
            val currentThread = Thread.currentThread()
            if (currentThread !is AsyncIncrementalAnalyzeManager<*, *>.LooperThread) {
                throw IllegalThreadStateException()
            }
            return currentThread.styles
        }

    internal class LockedSpans : Spans {

        private val lock: Lock = ReentrantLock()
        private val lines: MutableList<Line> = ArrayList(128)

        override fun adjustOnDelete(start: CharPosition, end: CharPosition) {
        }

        override fun adjustOnInsert(start: CharPosition, end: CharPosition) {
        }

        override fun getLineCount(): Int = lines.size

        override fun read(): Spans.Reader = ReaderImpl()

        override fun modify(): Spans.Modifier = ModifierImpl()

        override fun supportsModify(): Boolean = true

        private class Line(var spans: List<Span>) {
            val lock: Lock = ReentrantLock()
        }

        private inner class ReaderImpl : Spans.Reader {

            private var line: Line? = null

            override fun moveToLine(line: Int) {
                if (line < 0 || line >= lines.size) {
                    this.line?.lock?.unlock()
                    this.line = null
                } else {
                    this.line?.lock?.unlock()
                    var locked = false
                    try {
                        locked = lock.tryLock(100, TimeUnit.MICROSECONDS)
                    } catch (e: InterruptedException) {
                        Log.w(LOG_TAG, "failed to acquire the lock", e)
                        Thread.currentThread().interrupt()
                    }
                    if (locked) {
                        try {
                            val obj = lines[line]
                            if (obj.lock.tryLock()) {
                                this.line = obj
                            } else {
                                this.line = null
                            }
                        } finally {
                            lock.unlock()
                        }
                    } else {
                        this.line = null
                    }
                }
            }

            override fun getSpanCount(): Int = line?.spans?.size ?: 1

            override fun getSpanAt(index: Int): Span {
                val currentLine = line
                return if (currentLine == null) {
                    SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
                } else {
                    currentLine.spans[index]
                }
            }

            override fun getSpansOnLine(line: Int): List<Span> {
                val spans = ArrayList<Span>()
                var locked = false
                try {
                    locked = lock.tryLock(1, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    Log.w(LOG_TAG, "failed to acquire the lock", e)
                }
                if (locked) {
                    var obj: Line? = null
                    try {
                        if (line < lines.size) {
                            obj = lines[line]
                        }
                    } finally {
                        lock.unlock()
                    }
                    if (obj != null && obj.lock.tryLock()) {
                        try {
                            return Collections.unmodifiableList(obj.spans)
                        } finally {
                            obj.lock.unlock()
                        }
                    } else {
                        spans.add(getSpanAt(0))
                    }
                } else {
                    spans.add(getSpanAt(0))
                }
                return spans
            }
        }

        private inner class ModifierImpl : Spans.Modifier {

            override fun setSpansOnLine(line: Int, spans: List<Span>) {
                lock.lock()
                try {
                    while (lines.size <= line) {
                        val list = ArrayList<Span>()
                        list.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
                        lines.add(Line(list))
                    }
                    val obj = lines[line]
                    obj.lock.lock()
                    try {
                        obj.spans = spans
                    } finally {
                        obj.lock.unlock()
                    }
                } finally {
                    lock.unlock()
                }
            }

            override fun addLineAt(line: Int, spans: List<Span>) {
                lock.lock()
                try {
                    lines.add(line, Line(spans))
                } finally {
                    lock.unlock()
                }
            }

            override fun deleteLineAt(line: Int) {
                lock.lock()
                try {
                    val obj = lines[line]
                    obj.lock.lock()
                    try {
                        lines.removeAt(line)
                    } finally {
                        obj.lock.unlock()
                    }
                } finally {
                    lock.unlock()
                }
            }
        }

        companion object {
            private const val LOG_TAG = "LockedSpans"
        }
    }

    private class TextModification(
        val start: Long,
        val end: Long,
        val changedText: CharSequence? // null for deletion
    )

    /**
     * Helper class for analyzing code block
     */
    inner class CodeBlockAnalyzeDelegate internal constructor(
        private val thread: LooperThread
    ) {
        var suppressSwitch: Int = Int.MAX_VALUE

        internal fun reset() {
            suppressSwitch = Int.MAX_VALUE
        }

        val isCancelled: Boolean
            get() = thread.myRunCount != runCount.get() || thread.abort || thread.isInterrupted

        val isNotCancelled: Boolean
            get() = !isCancelled
    }

    internal inner class LooperThread : Thread() {

        private val messageQueue: BlockingQueue<Message> = LinkedBlockingQueue()
        @Volatile
        var abort: Boolean = false
        var shadowed: Content? = null
        var myRunCount: Long = 0L

        val states: MutableList<IncrementalAnalyzeManager.LineTokenizeResult<S, T>> = ArrayList()
        var styles: Styles? = null
        var spans: LockedSpans? = null
        val delegate: CodeBlockAnalyzeDelegate = CodeBlockAnalyzeDelegate(this)

        fun offerMessage(what: Int, obj: Any?) {
            val msg = Message.obtain()
            msg.what = what
            msg.obj = obj
            offerMessage(msg)
        }

        fun offerMessage(msg: Message) {
            messageQueue.offer(msg)
        }

        private fun initialize() {
            val currentContent = shadowed ?: return
            val localSpans = LockedSpans().also { spans = it }
            val localStyles = Styles(localSpans).also { styles = it }
            var state: S? = getInitialState()
            val mdf = localSpans.modify()
            for (i in 0 until currentContent.lineCount) {
                if (abort || isInterrupted) break
                val line = currentContent.getLine(i)
                val result = tokenizeLine(line, state, i)
                state = result.state
                val spansList = result.spans ?: generateSpansForLine(result) ?: emptyList()
                states.add(result.clearSpans())
                state?.let { onAddState(it) }
                mdf.addLineAt(i, spansList)
                if (isUpdateStylesDuringAnalysis && i > 0 && i % 1000 == 0 && !abort) {
                    val tmpStyles = Styles().apply {
                        spans = localStyles.spans
                    }
                    sendNewStyles(tmpStyles)
                }
            }
            localStyles.blocks = computeBlocks(currentContent, delegate)
            localStyles.setSuppressSwitch(delegate.suppressSwitch)
            localStyles.finishBuilding()

            if (!abort) {
                sendNewStyles(localStyles)
            }
        }

        fun handleMessage(msg: Message): Boolean {
            try {
                myRunCount = runCount.get()
                delegate.reset()
                when (msg.what) {
                    MSG_INIT -> {
                        shadowed = msg.obj as? Content
                        if (!abort && !isInterrupted) {
                            initialize()
                        }
                    }
                    MSG_MOD -> {
                        var updateStart = 0
                        var updateEnd = 0
                        val currentContent = shadowed
                        val currentStyles = styles
                        val currentSpans = spans
                        if (!abort && !isInterrupted && currentContent != null && currentStyles != null && currentSpans != null) {
                            val mod = msg.obj as TextModification
                            val startLine = IntPair.getFirst(mod.start)
                            val endLine = IntPair.getFirst(mod.end)

                            updateStart = startLine
                            if (mod.changedText == null) {
                                currentContent.delete(
                                    IntPair.getFirst(mod.start), IntPair.getSecond(mod.start),
                                    IntPair.getFirst(mod.end), IntPair.getSecond(mod.end)
                                )
                                var state: S? = if (startLine == 0) getInitialState() else states[startLine - 1].state
                                // Remove states
                                if (endLine >= startLine + 1) {
                                    val subList = states.subList(startLine + 1, endLine + 1)
                                    for (stLineTokenizeResult in subList) {
                                        stLineTokenizeResult.state?.let { onAbandonState(it) }
                                    }
                                    subList.clear()
                                }
                                val mdf = currentSpans.modify()
                                for (i in startLine + 1..endLine) {
                                    mdf.deleteLineAt(startLine + 1)
                                }
                                var line = startLine
                                while (line < currentContent.lineCount) {
                                    val res = tokenizeLine(currentContent.getLine(line), state, line)
                                    mdf.setSpansOnLine(line, res.spans ?: generateSpansForLine(res) ?: emptyList())
                                    val old = states.set(line, res.clearSpans())
                                    old.state?.let { onAbandonState(it) }
                                    res.state?.let { onAddState(it) }
                                    if (stateEquals(old.state, res.state)) {
                                        break
                                    }
                                    state = res.state
                                    line++
                                }
                                updateEnd = line
                            } else {
                                currentContent.insert(IntPair.getFirst(mod.start), IntPair.getSecond(mod.start), mod.changedText)
                                var state: S? = if (startLine == 0) getInitialState() else states[startLine - 1].state
                                var line = startLine
                                val spansModifier = currentStyles.spans!!.modify()
                                // Add Lines
                                while (line <= endLine) {
                                    val res = tokenizeLine(currentContent.getLine(line), state, line)
                                    if (line == startLine) {
                                        spansModifier.setSpansOnLine(line, res.spans ?: generateSpansForLine(res) ?: emptyList())
                                        val old = states.set(line, res.clearSpans())
                                        old.state?.let { onAbandonState(it) }
                                    } else {
                                        spansModifier.addLineAt(line, res.spans ?: generateSpansForLine(res) ?: emptyList())
                                        states.add(line, res.clearSpans())
                                    }
                                    res.state?.let { onAddState(it) }
                                    state = res.state
                                    line++
                                }
                                // line = end.line + 1, check whether the state equals
                                var flag = true
                                while (line < currentContent.lineCount && flag) {
                                    val res = tokenizeLine(currentContent.getLine(line), state, line)
                                    if (stateEquals(res.state, states[line].state)) {
                                        flag = false
                                    }
                                    spansModifier.setSpansOnLine(line, res.spans ?: generateSpansForLine(res) ?: emptyList())
                                    val old = states.set(line, res.clearSpans())
                                    old.state?.let { onAbandonState(it) }
                                    res.state?.let { onAddState(it) }
                                    state = res.state
                                    line++
                                }
                                updateEnd = line
                            }
                        }

                        // Do not update incomplete code blocks
                        if (currentContent != null && currentStyles != null) {
                            val blocks = computeBlocks(currentContent, delegate)
                            if (delegate.isNotCancelled) {
                                currentStyles.blocks = blocks
                                currentStyles.finishBuilding()
                                currentStyles.setSuppressSwitch(delegate.suppressSwitch)
                            }
                            if (!abort) {
                                sendUpdate(currentStyles, updateStart, updateEnd)
                            }
                        }
                    }
                }
                return true
            } catch (e: Exception) {
                Log.w("AsyncAnalysis", "Thread ${Thread.currentThread().name} failed", e)
            }
            return false
        }

        override fun run() {
            try {
                while (!abort && !isInterrupted) {
                    val msg = messageQueue.take()
                    if (!handleMessage(msg)) {
                        break
                    }
                    msg.recycle()
                }
            } catch (_: InterruptedException) {
                // ignored
            } finally {
                if (useShallowCopy) {
                    shadowed?.release()
                }
            }
        }
    }

    fun interface ReceiverConsumer {
        fun accept(receiver: StyleReceiver)
    }

    companion object {
        private const val MSG_BASE = 11451400
        private const val MSG_INIT = MSG_BASE + 1
        private const val MSG_MOD = MSG_BASE + 2
        private val threadIdCounter = AtomicInteger(0)

        @JvmStatic
        @set:Experimental
        var isUseShallowCopyByDefault: Boolean = false

        @JvmStatic
        var isUpdateStylesDuringAnalysis: Boolean = true
    }
}
