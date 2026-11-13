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
/**
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.text

import io.github.rosemoe.sora.text.bidi.ContentBidi
import io.github.rosemoe.sora.text.bidi.Directions
import java.util.LinkedList
import java.util.Objects
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * This class saves the text content for editor and maintains line widths.
 * It is thread-safe by default. Use [Content] constructor with `threadSafe = false` to
 * create a non thread-safe one.
 *
 * @author Rosemoe
 */
open class Content @JvmOverloads constructor(
    src: CharSequence? = null,
    threadSafe: Boolean = true
) : CharSequence {

    internal val lines: MutableList<ContentLine>
    private val contentListeners: MutableList<ContentListener>
    private val lock: ReadWriteLock?
    private var textLength: Int
    var nestedBatchEdit: Int
        private set
    private val documentVersionAtomic: AtomicLong
    private val indexerInternal: Indexer
    private val bidi: ContentBidi
    var undoManager: UndoManager
    private var _cursor: Cursor? = null

    init {
        val nonNullSrc = src ?: ""
        lock = if (threadSafe) ReentrantReadWriteLock() else null
        textLength = 0
        nestedBatchEdit = 0
        documentVersionAtomic = AtomicLong(1L)
        lines = ArrayList(initialLineCapacity)
        lines.add(ContentLine())
        contentListeners = ArrayList()
        bidi = ContentBidi(this)
        undoManager = UndoManager()
        undoManager.maxUndoStackSize = DEFAULT_MAX_UNDO_STACK_SIZE
        indexerInternal = CachedIndexer(this)
        if (nonNullSrc.isEmpty()) {
            undoManager.isUndoEnabled = true
        } else {
            undoManager.isUndoEnabled = false
            insert(0, 0, nonNullSrc)
            undoManager.isUndoEnabled = true
        }
    }

    val isThreadSafe: Boolean
        get() = lock != null

    open fun lock(write: Boolean) {
        if (lock == null) {
            return
        }
        (if (write) lock.writeLock() else lock.readLock()).lock()
    }

    open fun unlock(write: Boolean) {
        if (lock == null) {
            return
        }
        (if (write) lock.writeLock() else lock.readLock()).unlock()
    }

    private inline fun <T> withReadLock(action: () -> T): T {
        lock(false)
        try {
            return action()
        } finally {
            unlock(false)
        }
    }

    private inline fun <T> withWriteLock(action: () -> T): T {
        lock(true)
        try {
            return action()
        } finally {
            unlock(true)
        }
    }

    override operator fun get(index: Int): Char {
        checkIndex(index, CHECK_TYPE_READ)
        return withReadLock {
            val p = indexer.getCharPosition(index)
            lines[p.line].get(p.column)
        }
    }

    /**
     * Get the character at the given position
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The character at the given position
     */
    open fun charAt(line: Int, column: Int): Char {
        return withReadLock {
            checkLineAndColumn(line, column, CHECK_TYPE_READ)
            lines[line].get(column)
        }
    }

    override val length: Int
        get() = textLength

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex > endIndex) {
            throw StringIndexOutOfBoundsException("start > end")
        }
        return withReadLock {
            val s = indexer.getCharPosition(startIndex)
            val e = indexer.getCharPosition(endIndex)
            subContentInternal(s.line, s.column, e.line, e.column, true)
        }
    }

    open fun substring(start: Int, end: Int): String {
        if (start > end) {
            throw StringIndexOutOfBoundsException("start > end")
        }
        return withReadLock {
            val s = indexer.getCharPosition(start)
            val e = indexer.getCharPosition(end)
            subStringBuilder(s.line, s.column, e.line, e.column, end - start + 1).toString()
        }
    }

    /**
     * Get raw data of line.
     * The result should not be modified by code out of editor framework.
     *
     * @param line Line
     * @return Raw ContentLine used by Content
     */
    open fun getLine(line: Int): ContentLine {
        return withReadLock {
            lines[line]
        }
    }

    internal fun getLineUnsafe(line: Int): ContentLine {
        return lines[line]
    }

    /**
     * Get how many lines there are
     *
     * @return Line count
     */
    open val lineCount: Int
        get() = lines.size

    /**
     * Get how many characters is on the given line
     * If (line < 0 or line >= lineCount), it will throw a IndexOutOfBoundsException
     *
     * @param line The line to get
     * @return Character count on line
     */
    open fun getColumnCount(line: Int): Int {
        return getLine(line).length
    }

    /**
     * Get the given line text without '\n' character
     *
     * @param line The line to get
     * @return New String object of this line
     */
    open fun getLineString(line: Int): String {
        return withReadLock {
            checkLine(line)
            lines[line].toString()
        }
    }

    /**
     * Get region of the given line
     *
     * @param dest   Destination of characters
     * @param offset Offset in dest to store the chars
     */
    @Suppress("DEPRECATION")
    open fun getRegionOnLine(line: Int, start: Int, end: Int, dest: CharArray, offset: Int) {
        withReadLock {
            lines[line].getChars(start, end, dest, offset)
        }
    }

    /**
     * Get characters of line
     */
    open fun getLineChars(line: Int, dest: CharArray) {
        getRegionOnLine(line, 0, getColumnCount(line), dest, 0)
    }

    /**
     * Transform the (line, column) position to index
     * This task will usually be completed by [Indexer]
     *
     * @param line   Line of index
     * @param column Column on line of index
     * @return Transformed index for the given arguments
     */
    open fun getCharIndex(line: Int, column: Int): Int {
        return withReadLock {
            indexer.getCharIndex(line, column)
        }
    }

    /**
     * Check if the given [CharPosition] is valid in this text. Checks include line, column and index.
     *
     * @param position the position to check, maybe null
     * @return if the position is valid in this text. null position is always invalid.
     */
    open fun isValidPosition(position: CharPosition?): Boolean {
        if (position == null) {
            return false
        }
        val line = position.line
        val column = position.column
        val index = position.index
        return withReadLock {
            if (line < 0 || line >= lineCount) {
                false
            } else {
                val text = getLine(line)
                !(column > text.length + text.lineSeparator.length || column < 0) && indexer.getCharIndex(
                    line,
                    column
                ) == index
            }
        }
    }

    /**
     * Insert content to this object
     *
     * @param line   The insertion's line position
     * @param column The insertion's column position
     * @param text   The text you want to insert at the position
     */
    open fun insert(line: Int, column: Int, text: CharSequence) {
        lock(true)
        documentVersionAtomic.getAndIncrement()
        try {
            insertInternal(line, column, text)
        } finally {
            unlock(true)
        }
    }

    private fun insertInternal(line: Int, column: Int, text: CharSequence) {
        checkLineAndColumn(line, column, CHECK_TYPE_CURSOR)
        var col = column
        if (col > lines[line].length) {
            // Never insert texts between line separator characters
            col = lines[line].length
        }

        // Notify listeners and cursor manager
        _cursor?.beforeInsert(line, col)
        dispatchBeforeModification()

        var workLine = line
        var workIndex = col
        var currLine = makeLineMutable(workLine)
        val helper = InsertTextHelper.forInsertion(text)
        var type: Int
        var peekType = InsertTextHelper.TYPE_EOF
        var fromPeek = false
        val newLines = LinkedList<ContentLine>()
        val startSeparator = currLine.lineSeparator
        while (true) {
            type = if (fromPeek) peekType else helper.forward()
            fromPeek = false
            if (type == InsertTextHelper.TYPE_EOF) {
                break
            }
            if (type == InsertTextHelper.TYPE_LINE_CONTENT) {
                currLine.insert(workIndex, text, helper.index, helper.indexNext)
                workIndex += helper.indexNext - helper.index
            } else {
                val separator =
                    LineSeparator.fromSeparatorString(text, helper.index, helper.indexNext)
                currLine.lineSeparator = separator

                // Peek!
                peekType = helper.forward()
                fromPeek = true

                val newLine =
                    ContentLine(currLine.length - workIndex + helper.indexNext - helper.index + 10)
                newLine.insert(0, currLine, workIndex, currLine.length)
                currLine.delete(workIndex, currLine.length)
                workIndex = 0
                // Newly created lines are always mutable
                currLine = newLine
                newLines.add(newLine)
                workLine++
            }
        }
        currLine.lineSeparator = startSeparator
        lines.addAll(line + 1, newLines)
        helper.recycle()
        textLength += text.length
        this.dispatchAfterInsert(line, col, workLine, workIndex, text)
    }

    /**
     * Delete character in [start, end)
     *
     * @param start Start position in content
     * @param end   End position in content
     */
    open fun delete(start: Int, end: Int) {
        lock(true)
        checkIndex(start, CHECK_TYPE_CURSOR)
        checkIndex(end, CHECK_TYPE_CURSOR)
        documentVersionAtomic.getAndIncrement()
        try {
            val startPos = indexer.getCharPosition(start)
            val endPos = indexer.getCharPosition(end)
            if (start != end) {
                deleteInternal(startPos.line, startPos.column, endPos.line, endPos.column)
            }
        } finally {
            unlock(true)
        }
    }

    /**
     * Delete text in the given region
     *
     * @param startLine         The start line position
     * @param columnOnStartLine The start column position
     * @param endLine           The end line position
     * @param columnOnEndLine   The end column position
     */
    open fun delete(startLine: Int, columnOnStartLine: Int, endLine: Int, columnOnEndLine: Int) {
        lock(true)
        documentVersionAtomic.getAndIncrement()
        try {
            deleteInternal(startLine, columnOnStartLine, endLine, columnOnEndLine)
        } finally {
            unlock(true)
        }
    }

    private fun deleteInternal(
        startLine: Int,
        columnOnStartLine: Int,
        endLine: Int,
        columnOnEndLine: Int
    ) {
        checkLineAndColumn(endLine, columnOnEndLine, CHECK_TYPE_CURSOR)
        checkLineAndColumn(startLine, columnOnStartLine, CHECK_TYPE_CURSOR)
        if (startLine == endLine && columnOnStartLine == columnOnEndLine) {
            return
        }
        val endLineObj = lines[endLine]
        if (columnOnEndLine > endLineObj.length && endLine + 1 < lineCount) {
            // Expected to delete the whole newline
            deleteInternal(startLine, columnOnStartLine, endLine + 1, 0)
            return
        }
        val startLineObj = lines[startLine]
        if (columnOnStartLine > startLineObj.length) {
            // Expected to delete the whole newline
            deleteInternal(startLine, startLineObj.length, endLine, columnOnEndLine)
            return
        }
        val changedContent = java.lang.StringBuilder()
        if (startLine == endLine) {
            val curr = makeLineMutable(startLine)
            val len = curr.length
            if (columnOnStartLine < 0 || columnOnEndLine > len || columnOnStartLine > columnOnEndLine) {
                throw StringIndexOutOfBoundsException("invalid bounds")
            }

            // Notify listeners and cursor manager
            _cursor?.beforeDelete(startLine, columnOnStartLine, endLine, columnOnEndLine)
            dispatchBeforeModification()

            changedContent.append(curr, columnOnStartLine, columnOnEndLine)
            curr.delete(columnOnStartLine, columnOnEndLine)
            textLength -= columnOnEndLine - columnOnStartLine
        } else if (startLine < endLine) {
            // Notify listeners and cursor manager
            _cursor?.beforeDelete(startLine, columnOnStartLine, endLine, columnOnEndLine)
            dispatchBeforeModification()

            for (i in startLine + 1 until endLine) {
                val line = lines[i]
                val separator = line.lineSeparator
                textLength -= line.length + separator.length
                line.appendTo(changedContent)
                changedContent.append(separator.content)
                line.release()
            }
            if (endLine > startLine + 1) {
                lines.subList(startLine + 1, endLine).clear()
            }

            val currEnd = startLine + 1
            val start = makeLineMutable(startLine)
            val end = lines[currEnd]
            textLength -= start.length - columnOnStartLine
            changedContent.insert(0, start, columnOnStartLine, start.length)
                .insert(start.length - columnOnStartLine, start.lineSeparator.content)
            start.delete(columnOnStartLine, start.length)
            textLength -= columnOnEndLine
            changedContent.append(end, 0, columnOnEndLine)
            textLength -= start.lineSeparator.length
            lines.removeAt(currEnd)
            start.append(TextReference(end, columnOnEndLine, end.length))
            start.lineSeparator = end.lineSeparator
            end.release()
        } else {
            throw IllegalArgumentException("start line > end line")
        }
        this.dispatchAfterDelete(
            startLine,
            columnOnStartLine,
            endLine,
            columnOnEndLine,
            changedContent
        )
    }

    /**
     * Make the given line mutable
     */
    private fun makeLineMutable(line: Int): ContentLine {
        val data = lines[line]
        val mut = data.toMutable()
        if (mut !== data) {
            lines[line] = mut
            data.release()
        }
        return mut
    }

    /**
     * Replace the text in the given region
     * This action will be completed by calling [delete] and [insert]
     *
     * @param startLine         The start line position
     * @param columnOnStartLine The start column position
     * @param endLine           The end line position
     * @param columnOnEndLine   The end column position
     * @param text              The text to replace old text
     */
    open fun replace(
        startLine: Int,
        columnOnStartLine: Int,
        endLine: Int,
        columnOnEndLine: Int,
        text: CharSequence
    ) {
        lock(true)
        documentVersionAtomic.getAndIncrement()
        try {
            this.dispatchBeforeReplace()
            deleteInternal(startLine, columnOnStartLine, endLine, columnOnEndLine)
            insertInternal(startLine, columnOnStartLine, text)
        } finally {
            unlock(true)
        }
    }

    /**
     * Replace text in the given region with the text
     */
    open fun replace(startIndex: Int, endIndex: Int, text: CharSequence) {
        val start = indexer.getCharPosition(startIndex)
        val end = indexer.getCharPosition(endIndex)
        replace(start.line, start.column, end.line, end.column, text)
    }

    /**
     * Get current document version. The returned value is increasing (if the modification count is
     * smaller than Long.MAX_VALUE).
     */
    val documentVersion: Long
        get() = documentVersionAtomic.get()

    /**
     * Undo the last modification.
     *
     * NOTE: When there are too much modification, old modification will be deleted from UndoManager
     */
    open fun undo(): TextRange? {
        return undoManager.undo(this)
    }

    /**
     * Redo the last modification
     */
    open fun redo() {
        undoManager.redo(this)
    }

    /**
     * Check whether the [UndoManager] is working to undo/redo
     */
    val isUndoManagerWorking: Boolean
        get() = undoManager.isModifyingContent

    /**
     * Whether we can undo
     *
     * @return Whether we can undo
     */
    open fun canUndo(): Boolean {
        return undoManager.canUndo()
    }

    /**
     * Whether we can redo
     *
     * @return Whether we can redo
     */
    open fun canRedo(): Boolean {
        return undoManager.canRedo()
    }

    /**
     * Get whether UndoManager is enabled
     *
     * @return Whether UndoManager is enabled
     */
    var isUndoEnabled: Boolean
        get() = undoManager.isUndoEnabled
        set(enabled) {
            undoManager.isUndoEnabled = enabled
        }

    /**
     * Get current max stack size of UndoManager
     *
     * @return current max stack size
     */
    var maxUndoStackSize: Int
        get() = undoManager.maxUndoStackSize
        set(maxSize) {
            undoManager.maxUndoStackSize = maxSize
        }

    /**
     * A delegate method.
     * Notify the UndoManager to begin batch edit (enter a new layer).
     * NOTE: batch edit in Android can be nested.
     *
     * @return Whether in batch edit
     */
    open fun beginBatchEdit(): Boolean {
        nestedBatchEdit++
        return isInBatchEdit
    }

    /**
     * A delegate method.
     * Notify the UndoManager to end batch edit (exit current layer).
     *
     * @return Whether in batch edit
     */
    open fun endBatchEdit(): Boolean {
        nestedBatchEdit--
        if (nestedBatchEdit == 0) {
            undoManager.onExitBatchEdit()
        }
        if (nestedBatchEdit < 0) {
            nestedBatchEdit = 0
        }
        return isInBatchEdit
    }

    open fun resetBatchEdit() {
        nestedBatchEdit = 0
    }

    /**
     * Returns whether we are in batch edit
     *
     * @return Whether in batch edit
     */
    val isInBatchEdit: Boolean
        get() = nestedBatchEdit > 0

    /**
     * Add a new [ContentListener] to the Content
     *
     * @param listener The listener to add
     */
    open fun addContentListener(listener: ContentListener) {
        require(listener !is Indexer) { "Permission denied" }
        if (!contentListeners.contains(listener)) {
            contentListeners.add(listener)
        }
    }

    /**
     * Remove the given listener of this Content
     *
     * @param listener The listener to remove
     */
    open fun removeContentListener(listener: ContentListener) {
        require(listener !is Indexer) { "Permission denied" }
        contentListeners.remove(listener)
    }

    /**
     * Get the using [Indexer] object
     *
     * @return Indexer for this object
     */
    val indexer: Indexer
        get() {
            val c = _cursor
            if (c != null) {
                return c.indexer
            }
            return indexerInternal
        }

    /**
     * Quick method to get sub string of this object
     *
     * @param startLine            The start line position
     * @param startColumn          The start column position
     * @param endLine              The end line position
     * @param endColumn            The end column position
     * @param newContentThreadSafe Should the returned instance be thread-safe
     * @return sub-sequence of this Content
     */
    @JvmOverloads
    open fun subContent(
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        newContentThreadSafe: Boolean = true
    ): Content {
        return withReadLock {
            subContentInternal(startLine, startColumn, endLine, endColumn, newContentThreadSafe)
        }
    }

    private fun subContentInternal(
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        threadSafe: Boolean
    ): Content {
        val c = Content(null, threadSafe)
        c.isUndoEnabled = false
        if (startLine == endLine) {
            val line = lines[startLine]
            if (endColumn == line.length + 1 && line.lineSeparator == LineSeparator.CRLF) {
                if (startColumn < endColumn) {
                    c.insert(0, 0, line.subSequence(startColumn, line.length))
                    c.lines[0].lineSeparator = LineSeparator.CR
                    c.textLength++
                    c.lines.add(ContentLine())
                }
            } else {
                c.insert(0, 0, line.subSequence(startColumn, endColumn))
            }
        } else if (startLine < endLine) {
            val firstLine = lines[startLine]
            if (firstLine.lineSeparator == LineSeparator.CRLF) {
                if (startColumn <= firstLine.length) {
                    c.insert(0, 0, firstLine.subSequence(startColumn, firstLine.length))
                    c.lines[0].lineSeparator = firstLine.lineSeparator
                    c.textLength += firstLine.lineSeparator.length
                } else if (startColumn == firstLine.length + 1) {
                    c.lines[0].lineSeparator = LineSeparator.LF
                    c.textLength += LineSeparator.LF.length
                } else {
                    throw IndexOutOfBoundsException()
                }
            } else {
                c.insert(0, 0, firstLine.subSequence(startColumn, firstLine.length))
                c.lines[0].lineSeparator = firstLine.lineSeparator
                c.textLength += firstLine.lineSeparator.length
            }
            for (i in startLine + 1 until endLine) {
                val line = lines[i]
                c.lines.add(ContentLine(line))
                c.textLength += line.length + line.lineSeparator.length
            }
            val end = lines[endLine]
            if (endColumn == end.length + 1 && end.lineSeparator == LineSeparator.CRLF) {
                val newLine = ContentLine().insert(0, end, 0, endColumn - 1)
                c.lines.add(newLine)
                newLine.lineSeparator = LineSeparator.CR
                c.textLength += endColumn + 1
            } else {
                c.lines.add(ContentLine().insert(0, end, 0, endColumn))
                c.textLength += endColumn
            }
        } else {
            throw StringIndexOutOfBoundsException("start > end")
        }
        c.isUndoEnabled = true
        return c
    }

    private fun subStringBuilder(
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        length: Int
    ): java.lang.StringBuilder {
        val sb = java.lang.StringBuilder(length)
        if (startLine == endLine) {
            val line = lines[startLine]
            if (endColumn == line.length + 1 && line.lineSeparator == LineSeparator.CRLF) {
                if (startColumn < endColumn) {
                    sb.append(lines[startLine], startColumn, line.length)
                        .append(LineSeparator.CR.content)
                }
            } else {
                sb.append(lines[startLine], startColumn, endColumn)
            }
        } else if (startLine < endLine) {
            val firstLine = lines[startLine]
            if (firstLine.lineSeparator == LineSeparator.CRLF) {
                if (startColumn <= firstLine.length) {
                    sb.append(firstLine, startColumn, firstLine.length)
                    sb.append(firstLine.lineSeparator.content)
                } else if (startColumn == firstLine.length + 1) {
                    sb.append(LineSeparator.LF.content)
                } else {
                    throw IndexOutOfBoundsException()
                }
            } else {
                sb.append(firstLine, startColumn, firstLine.length)
                sb.append(firstLine.lineSeparator.content)
            }
            for (i in startLine + 1 until endLine) {
                val line = lines[i]
                sb.append(line)
                    .append(line.lineSeparator.content)
            }
            val end = lines[endLine]
            if (endColumn == end.length + 1 && end.lineSeparator == LineSeparator.CRLF) {
                sb.append(end, 0, endColumn)
                    .append(LineSeparator.CR.content)
            } else {
                sb.append(end, 0, endColumn)
            }
        } else {
            throw StringIndexOutOfBoundsException("start > end")
        }
        return sb
    }

    open fun getLineDirections(line: Int): Directions {
        return withReadLock {
            bidi.getLineDirections(lines[line], line)
        }
    }

    var isBidiEnabled: Boolean
        get() = bidi.isEnabled
        set(enabled) {
            bidi.isEnabled = enabled
        }

    open fun isRtlAt(line: Int, column: Int): Boolean {
        val dirs = getLineDirections(line)
        for (i in 0 until dirs.runCount) {
            if (column >= dirs.getRunStart(i) && column < dirs.getRunEnd(i)) {
                return dirs.isRunRtl(i)
            }
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Content) {
            return false
        }
        if (other.length != this.length) {
            return false
        }
        if (this.lineCount != other.lineCount) {
            return false
        }
        for (i in 0 until this.lineCount) {
            if (!textEquals(lines[i], other.lines[i])) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(lines, textLength)
    }

    override fun toString(): String {
        return toStringBuilder().toString()
    }

    /**
     * Get the text in StringBuilder form
     *
     * This can improve the speed in char reading for tokenizing
     *
     * @return StringBuilder form of Content
     */
    open fun toStringBuilder(): java.lang.StringBuilder {
        val sb = java.lang.StringBuilder()
        appendToStringBuilder(sb)
        return sb
    }

    /**
     * Append the content to the given [StringBuilder]
     */
    open fun appendToStringBuilder(sb: java.lang.StringBuilder) {
        sb.ensureCapacity(sb.length + length)
        withReadLock {
            val count = lineCount
            for (i in 0 until count) {
                val line = this.lines[i]
                line.appendTo(sb)
                sb.append(line.lineSeparator.content)
            }
        }
    }

    /**
     * Get Cursor for editor (Create if there is not)
     *
     * @return Cursor
     */
    val cursor: Cursor
        get() {
            var c = _cursor
            if (c == null) {
                c = Cursor(this)
                _cursor = c
            }
            return c
        }

    /**
     * Check if there is a cursor created for this Content object
     */
    val isCursorCreated: Boolean
        get() = _cursor != null

    /**
     * Dispatch events to listener before replacement
     */
    private fun dispatchBeforeReplace() {
        undoManager.beforeReplace(this)
        _cursor?.beforeReplace()
        (indexerInternal as? ContentListener)?.beforeReplace(this)
        for (lis in contentListeners) {
            lis.beforeReplace(this)
        }
    }

    /**
     * Dispatch events to listener after deletion
     *
     * @param a Start line
     * @param b Start Column
     * @param c End line
     * @param d End column
     * @param e Text deleted
     */
    private fun dispatchAfterDelete(a: Int, b: Int, c: Int, d: Int, e: CharSequence) {
        undoManager.afterDelete(this, a, b, c, d, e)
        _cursor?.afterDelete(a, b, c, d, e)
        (indexerInternal as? ContentListener)?.afterDelete(this, a, b, c, d, e)
        for (lis in contentListeners) {
            lis.afterDelete(this, a, b, c, d, e)
        }
    }

    private fun dispatchBeforeModification() {
        undoManager.beforeModification(this)
        for (lis in contentListeners) {
            lis.beforeModification(this)
        }
    }

    /**
     * Dispatch events to listener after insertion
     *
     * @param a Start line
     * @param b Start Column
     * @param c End line
     * @param d End column
     * @param e Text deleted
     */
    private fun dispatchAfterInsert(a: Int, b: Int, c: Int, d: Int, e: CharSequence) {
        undoManager.afterInsert(this, a, b, c, d, e)
        _cursor?.afterInsert(a, b, c, d, e)
        (indexerInternal as? ContentListener)?.afterInsert(this, a, b, c, d, e)
        for (lis in contentListeners) {
            lis.afterInsert(this, a, b, c, d, e)
        }
    }

    /**
     * Check whether the index is valid
     *
     * @param index Index to check
     */
    open fun checkIndex(index: Int, checkType: Int) {
        if ((if (checkType == CHECK_TYPE_READ) index >= length else index > length) || index < 0) {
            throw StringIndexOutOfBoundsException("Index $index out of bounds. length:$length")
        }
    }

    /**
     * Check whether the line is valid
     *
     * @param line Line to check
     */
    open fun checkLine(line: Int) {
        if (line >= lineCount || line < 0) {
            throw StringIndexOutOfBoundsException("Line $line out of bounds. line count:$lineCount")
        }
    }

    /**
     * Check whether the line and column is valid
     *
     * @param line   The line to check
     * @param column The column to check
     */
    open fun checkLineAndColumn(line: Int, column: Int, checkType: Int) {
        checkLine(line)
        val text = lines[line]
        // Clarify valid line positions here
        // Check Type   Valid Range
        // READ         [0, columnCount+lineSepLength)
        // INSERT       [0, columnCount]
        // INDEX        See CachedIndexer
        when (checkType) {
            CHECK_TYPE_READ -> {
                val len = text.length + text.lineSeparator.length
                if (column >= len || column < 0) {
                    throw StringIndexOutOfBoundsException(
                        "Column $column out of bounds for READ. line: $line, valid range: [0, $len)"
                    )
                }
            }

            CHECK_TYPE_CURSOR -> {
                val len = text.length
                if (column > len || column < 0) {
                    throw StringIndexOutOfBoundsException(
                        "Column $column out of bounds for CURSOR. line: $line, valid range: [0, $len]"
                    )
                }
            }

            CHECK_TYPE_INDEX -> {
                val len = text.length + text.lineSeparator.length
                if (line == lineCount - 1) {
                    if (column > len || column < 0) {
                        throw StringIndexOutOfBoundsException(
                            "Column $column out of bounds for INDEX. line: $line, valid range: [0, $len]"
                        )
                    }
                } else {
                    if (column >= len || column < 0) {
                        throw StringIndexOutOfBoundsException(
                            "Column $column out of bounds for INDEX. line: $line, valid range: [0, $len)"
                        )
                    }
                }
            }
        }
    }

    /**
     * Copy text in this Content object.
     * Returns a new thread-safe Content object with the same text as this object. By default, the object is
     * thread-safe and access operations are locked when accessed by multiple threads.
     */
    @JvmOverloads
    open fun copyText(newContentThreadSafe: Boolean = true, shallow: Boolean = false): Content {
        return withReadLock {
            val n = Content(null, newContentThreadSafe)
            n.lines.removeAt(0)
            (n.lines as? ArrayList<ContentLine>)?.ensureCapacity(lineCount)
            if (shallow) {
                for (line in lines) {
                    line.retain()
                }
                n.lines.addAll(lines)
            } else {
                for (line in lines) {
                    n.lines.add(ContentLine(line))
                }
            }
            n.textLength = textLength
            n
        }
    }

    /**
     * Shallow copy text in this Content object.
     * Returns a new Content object with the same text as this object. By default, the object is not
     * thread-safe and should be accessed by a single thread.
     */
    @JvmOverloads
    open fun copyTextShallow(newContentThreadSafe: Boolean = false): Content {
        return copyText(newContentThreadSafe, true)
    }

    /**
     * Release this text object.
     * Release any shareable instance currently held. It's recommended to call this after a shallow copied
     * instance is no longer used.
     */
    open fun release() {
        withWriteLock {
            for (line in lines) {
                line.release()
            }
            lines.clear()
            textLength = 0
            _cursor = null
            bidi.destroy()
        }
    }

    open fun getColumnCountUnsafe(line: Int): Int {
        return lines[line].length
    }

    open fun getLineSeparatorUnsafe(line: Int): LineSeparator {
        return lines[line].lineSeparator
    }

    /**
     * Read the lines (ordered).
     * This is for optimizing frequent lock acquiring.
     *
     * @param startLine inclusive
     * @param endLine   inclusive
     */
    open fun runReadActionsOnLines(startLine: Int, endLine: Int, consumer: ContentLineConsumer) {
        withReadLock {
            for (i in startLine..endLine) {
                val t = lines[i]
                consumer.accept(i, t, bidi.getLineDirections(t, i))
            }
        }
    }

    /**
     * Read the lines (ordered).
     * This is for optimizing frequent lock acquiring.
     *
     * @param startLine inclusive
     * @param endLine   inclusive
     */
    open fun runReadActionsOnLines(startLine: Int, endLine: Int, consumer: ContentLineConsumer2) {
        withReadLock {
            val flag = ContentLineConsumer2.AbortFlag()
            var i = startLine
            while (i <= endLine && !flag.set) {
                consumer.accept(i, lines[i], flag)
                i++
            }
        }
    }

    fun interface ContentLineConsumer {
        fun accept(lineIndex: Int, line: ContentLine, dirs: Directions)
    }

    fun interface ContentLineConsumer2 {
        fun accept(lineIndex: Int, line: ContentLine, flag: AbortFlag)

        class AbortFlag {
            @JvmField
            var set: Boolean = false
        }
    }

    companion object {
        const val DEFAULT_MAX_UNDO_STACK_SIZE: Int = 500
        const val DEFAULT_LIST_CAPACITY: Int = 1000

        const val CHECK_TYPE_READ: Int = 0
        const val CHECK_TYPE_CURSOR: Int = 1
        const val CHECK_TYPE_INDEX: Int = 2

        @JvmStatic
        var initialLineCapacity: Int = DEFAULT_LIST_CAPACITY
            set(capacity) {
                require(capacity > 0) { "capacity can not be negative or zero" }
                field = capacity
            }

        /**
         * Test whether the two ContentLine have the same text
         *
         * @param a ContentLine
         * @param b another ContentLine
         * @return Whether the text in the given two lines equal
         */
        @JvmStatic
        private fun textEquals(a: ContentLine, b: ContentLine): Boolean {
            if (a.length != b.length) {
                return false
            }
            if (a === b) {
                return true
            }
            for (i in 0 until a.length) {
                if (a[i] != b[i]) {
                    return false
                }
            }
            return true
        }
    }
}

/**
 * A delegate method.
 * Notify the UndoManager to batch edit.
 * @see Content.beginBatchEdit
 * @see Content.endBatchEdit
 */
fun Content.batchEdit(block: (Content) -> Unit): Content {
    this.beginBatchEdit()
    block(this)
    this.endBatchEdit()
    return this
}
