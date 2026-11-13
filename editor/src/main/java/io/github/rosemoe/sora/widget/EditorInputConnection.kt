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
package io.github.rosemoe.sora.widget

import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.text.TextUtils
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.SurroundingText
import android.view.inputmethod.TextAttribute
import android.view.inputmethod.TextSnapshot
import androidx.annotation.RequiresApi
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.ImePrivateCommandEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.analysis.SequenceUpdateRange
import io.github.rosemoe.sora.text.ComposingText
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.Cursor
import io.github.rosemoe.sora.util.Logger
import kotlin.math.max
import kotlin.math.min

/**
 * Connection between input method and editor
 *
 * @author Rosemoe
 */
open class EditorInputConnection(
    private val editor: CodeEditor
) : BaseInputConnection(editor, true) {

    companion object {
        private val logger = Logger.instance("EditorInputConnection")
        private const val MEMORY_EFFICIENT_TEXT_LENGTH = 2048
        @JvmField
        var DEBUG = false
    }

    @JvmField
    var composingText: ComposingText = ComposingText()

    @JvmField
    var imeConsumingInput: Boolean = false

    private var connectionInvalid: Boolean = false

    init {
        editor.subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
            if (event.action == ContentChangeEvent.ACTION_INSERT) {
                composingText.shiftOnInsert(event.changeStart.index, event.changeEnd.index)
            } else if (event.action == ContentChangeEvent.ACTION_DELETE) {
                composingText.shiftOnDelete(event.changeStart.index, event.changeEnd.index)
            }
        }
    }

    open fun markInvalid() {
        connectionInvalid = true
        resetComposingText()
        resetBatchEdit()
        editor.invalidate()
    }

    /**
     * Reset the state of this connection
     */
    open fun reset() {
        resetBatchEdit()
        resetComposingText()
        connectionInvalid = false
        imeConsumingInput = false
    }

    private fun resetBatchEdit() {
        val content = editor.text
        while (content.isInBatchEdit) {
            content.endBatchEdit()
        }
    }

    private val cursor: Cursor
        get() = editor.cursor

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (DEBUG) {
            logger.d("commitText text = $text, pos = $newCursorPosition")
        }

        if (!editor.isEditable || connectionInvalid || text == null) {
            return false
        }

        if ("\n" == text.toString()) {
            sendKeyClick(KeyEvent.KEYCODE_ENTER)
        } else {
            commitTextInternal(text, true)
        }
        return true
    }

    override fun closeConnection() {
        super.closeConnection()
        resetBatchEdit()
        resetComposingText()
        editor.onCloseConnection()
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        return TextUtils.getCapsMode(editor.text, cursor.left, reqModes)
    }

    private fun getTextRegionInternal(
        start: Int,
        end: Int,
        flags: Int,
        ignoreIPCLimit: Boolean
    ): CharSequence {
        val origin = editor.text
        var s = start
        var e = end
        if (s > e) {
            val tmp = s
            s = e
            e = tmp
        }
        if (s < 0) {
            s = 0
        }
        if (e > origin.length) {
            e = origin.length
        }
        if (e < s) {
            s = 0
            e = 0
        }
        if (!ignoreIPCLimit && e - s > editor.props.maxIPCTextLength) {
            e = s + max(0, editor.props.maxIPCTextLength)
        }
        val sub = origin.subSequence(s, e).toString()
        if (flags == GET_TEXT_WITH_STYLES) {
            val text = SpannableStringBuilder(sub)
            if (composingText.isComposing) {
                try {
                    val originalComposingStart = composingText.startIndex
                    val originalComposingEnd = composingText.endIndex
                    var transferredStart = originalComposingStart - s
                    if (transferredStart >= text.length) {
                        return text
                    }
                    if (transferredStart < 0) {
                        transferredStart = 0
                    }
                    var transferredEnd = originalComposingEnd - s
                    if (transferredEnd <= 0) {
                        return text
                    }
                    if (transferredEnd >= text.length) {
                        transferredEnd = text.length
                    }
                    text.setSpan(
                        Spanned.SPAN_COMPOSING,
                        transferredStart,
                        transferredEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } catch (ignored: IndexOutOfBoundsException) {
                }
            }
            return text
        }
        return sub
    }

    open fun getTextRegion(start: Int, end: Int, flags: Int): CharSequence {
        return try {
            val res = getTextRegionInternal(start, end, flags, false)
            if (DEBUG) {
                logger.d("getTextRegion result:$res")
            }
            res
        } catch (e: IndexOutOfBoundsException) {
            logger.w("Failed to get text region for IME", e)
            ""
        }
    }

    open fun getTextRegionUnlimited(start: Int, end: Int, flags: Int): CharSequence {
        return try {
            val res = getTextRegionInternal(start, end, flags, true)
            if (DEBUG) {
                logger.d("getTextRegion result:$res")
            }
            res
        } catch (e: IndexOutOfBoundsException) {
            logger.w("Failed to get text region for IME", e)
            ""
        }
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        if (editor.props.disallowSuggestions) {
            return null
        }
        val left = cursor.left
        val right = cursor.right
        return if (left == right) null else getTextRegion(left, right, flags)
    }

    override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence {
        if (editor.props.disallowSuggestions) {
            return ""
        }
        val end = cursor.left
        val start = max(end - length, end - editor.props.maxIPCTextLength)
        return getTextRegion(start, end, flags)
    }

    override fun getTextAfterCursor(length: Int, flags: Int): CharSequence {
        if (editor.props.disallowSuggestions) {
            return ""
        }
        val end = cursor.right
        return getTextRegion(end, end + length, flags)
    }

    private fun sendKeyClick(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        sendKeyEvent(
            KeyEvent(
                eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
        sendKeyEvent(
            KeyEvent(
                SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    open fun commitTextInternal(text: CharSequence, applyAutoIndent: Boolean) {
        var mutableText = text
        val composingStateBefore = composingText.isComposing
        if (editor.props.trackComposingTextOnCommit) {
            if (composingText.isComposing) {
                val currentComp = editor.text.subSequence(composingText.startIndex, composingText.endIndex).toString()
                val commitStr = mutableText.toString()
                if (composingText.endIndex == cursor.left && !cursor.isSelected && commitStr.startsWith(currentComp) && commitStr.length > currentComp.length) {
                    mutableText = commitStr.substring(currentComp.length)
                    resetComposingText()
                } else {
                    deleteComposingText()
                }
            }
        } else if (composingText.isComposing) {
            deleteComposingText()
        }

        editor.commitText(mutableText, applyAutoIndent)
        if (composingStateBefore) {
            endBatchEdit()
        }
    }

    private fun deleteComposingText() {
        if (!composingText.isComposing) {
            return
        }
        try {
            editor.text.delete(composingText.startIndex, composingText.endIndex)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
        resetComposingText()
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (DEBUG) {
            logger.d("deleteSurroundingText, before = $beforeLength, after = $afterLength")
        }
        if (!editor.isEditable || connectionInvalid) {
            return false
        }
        if (beforeLength < 0 || afterLength < 0) {
            return false
        }

        if (beforeLength == 1 && afterLength == 0 && !composingText.isComposing) {
            editor.deleteText()
            return true
        }

        if (beforeLength > 0 && afterLength > 0) {
            beginBatchEdit()
        }

        val composing = composingText.isComposing
        var composingStart = if (composing) composingText.startIndex else 0
        var composingEnd = if (composing) composingText.endIndex else 0

        var rangeEnd = cursor.left
        var rangeStart = rangeEnd - beforeLength
        if (rangeStart < 0) {
            rangeStart = 0
        }
        editor.text.delete(rangeStart, rangeEnd)

        if (composing) {
            val crossStart = max(rangeStart, composingStart)
            val crossEnd = min(rangeEnd, composingEnd)
            composingEnd -= max(0, crossEnd - crossStart)
            val delta = max(0, crossStart - rangeStart)
            composingEnd -= delta
            composingStart -= delta
        }

        rangeStart = cursor.right
        rangeEnd = rangeStart + afterLength
        if (rangeEnd > editor.text.length) {
            rangeEnd = editor.text.length
        }
        editor.text.delete(rangeStart, rangeEnd)

        if (composing) {
            val crossStart = max(rangeStart, composingStart)
            val crossEnd = min(rangeEnd, composingEnd)
            composingEnd -= max(0, crossEnd - crossStart)
            val delta = max(0, crossStart - rangeStart)
            composingEnd -= delta
            composingStart -= delta
        }

        if (beforeLength > 0 && afterLength > 0) {
            endBatchEdit()
        }

        return true
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        return false
    }

    @Synchronized
    override fun beginBatchEdit(): Boolean {
        if (DEBUG) {
            logger.d("beginBatchEdit")
        }
        if (editor.props.disallowSuggestions) {
            return editor.text.isInBatchEdit
        }
        return editor.text.beginBatchEdit()
    }

    @Synchronized
    override fun endBatchEdit(): Boolean {
        if (DEBUG) {
            logger.d("endBatchEdit")
        }
        val inBatch = editor.text.endBatchEdit()
        if (!inBatch) {
            editor.updateSelection()
        }
        return inBatch
    }

    private fun deleteSelected() {
        if (cursor.isSelected) {
            editor.deleteText()
        }
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (DEBUG) {
            logger.d("setComposingText, text = $text, pos = $newCursorPosition")
        }
        if (!editor.isEditable || connectionInvalid || !editor.acceptsComposingText() || text == null) {
            return false
        }
        if (editor.props.disallowSuggestions) {
            resetComposingText()
            commitText(text, 0)
            return false
        }
        if (TextUtils.indexOf(text, '\n') != -1) {
            return false
        }
        invalidateComposingBackground()
        if (!composingText.isComposing) {
            composingText.preSetComposing = true
            deleteSelected()
            beginBatchEdit()
            editor.commitText(text)
            composingText.preSetComposing = false
            composingText.set(cursor.left - text.length, cursor.left)
            editor.updateCursor()
        } else {
            if (composingText.isComposing) {
                if (editor.props.minimizeComposingTextUpdate) {
                    setComposingTextCompat(text.toString())
                } else {
                    editor.text.replace(composingText.startIndex, composingText.endIndex, text)
                }
                composingText.adjustLength(text.length)
            }
        }
        if (text.isEmpty()) {
            finishComposingText()
            return true
        }
        composingText.text = if (text is Spanned) SpannedString.valueOf(text) else text.toString()
        invalidateComposingBackground()
        return true
    }

    private fun invalidateComposingBackground() {
        if (composingText.text is Spanned && composingText.startIndex >= 0
            && composingText.startIndex <= editor.text.length
        ) {
            val position = editor.text.indexer.getCharPosition(composingText.startIndex)
            editor.renderContext.updateForRange(SequenceUpdateRange(position.line, position.line))
            editor.invalidate()
        }
    }

    private fun resetComposingText() {
        invalidateComposingBackground()
        composingText.reset()
    }

    private fun setComposingTextCompat(text: String) {
        val content = editor.text
        val current = content.substring(composingText.startIndex, composingText.endIndex)
        if (current == text) {
            return
        }
        if (current.length < text.length && text.startsWith(current)) {
            val pos = content.indexer.getCharPosition(composingText.endIndex)
            content.insert(pos.line, pos.column, text.substring(current.length))
        } else if (current.length > text.length && current.startsWith(text)) {
            content.delete(composingText.endIndex - (current.length - text.length), composingText.endIndex)
        } else {
            content.replace(composingText.startIndex, composingText.endIndex, text)
        }
    }

    override fun finishComposingText(): Boolean {
        if (DEBUG) {
            logger.d("finishComposingText")
        }
        if (!editor.isEditable || connectionInvalid) {
            return false
        }
        if (editor.props.disallowSuggestions) {
            return false
        }
        resetComposingText()
        endBatchEdit()
        editor.updateCursor()
        editor.invalidate()
        return true
    }

    private fun getWrappedIndex(index: Int): Int {
        if (index < 0) {
            return 0
        }
        if (index > editor.text.length) {
            return editor.text.length
        }
        return index
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        if (DEBUG) {
            logger.d("setSelection, s = $start, e = $end")
        }
        if (!editor.isEditable || connectionInvalid || editor.props.disallowSuggestions) {
            return false
        }
        var s = getWrappedIndex(start)
        var e = getWrappedIndex(end)
        if (s > e) {
            val tmp = s
            s = e
            e = tmp
        }
        if (s == cursor.left && e == cursor.right) {
            return true
        }
        val content = editor.text
        val startPos = content.indexer.getCharPosition(s)
        val endPos = content.indexer.getCharPosition(e)
        editor.setSelectionRegion(startPos.line, startPos.column, endPos.line, endPos.column, false, SelectionChangeEvent.CAUSE_IME)
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        if (DEBUG) {
            logger.d("setComposingRegion, s = $start, e = $end")
        }
        if (!editor.isEditable || connectionInvalid || !editor.acceptsComposingText() || editor.props.disallowSuggestions) {
            return false
        }
        if (start == end) {
            finishComposingText()
            return true
        }
        try {
            var s = start
            var e = end
            if (s > e) {
                val tmp = s
                s = e
                e = tmp
            }
            if (s < 0) {
                s = 0
            }
            val content = editor.text
            if (e > content.length) {
                e = content.length
            }
            if (s >= e) {
                return false
            }
            invalidateComposingBackground()
            composingText.set(s, e)
            editor.invalidate()
        } catch (e: IndexOutOfBoundsException) {
            logger.w("set composing region for IME failed", e)
            return false
        }
        beginBatchEdit()
        return true
    }

    override fun performContextMenuAction(id: Int): Boolean {
        when (id) {
            android.R.id.selectAll -> {
                editor.selectAll()
                return true
            }
            android.R.id.cut -> {
                editor.copyText()
                if (cursor.isSelected) {
                    editor.deleteText()
                }
                return true
            }
            android.R.id.paste,
            android.R.id.pasteAsPlainText -> {
                editor.pasteText()
                return true
            }
            android.R.id.copy -> {
                editor.copyText()
                return true
            }
            android.R.id.undo -> {
                editor.undo()
                return true
            }
            android.R.id.redo -> {
                editor.redo()
                return true
            }
        }
        return false
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        editor.updateCursorAnchor()
        return true
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        if (DEBUG) {
            logger.d("getExtractedText, flags = $flags")
        }
        if (editor.props.disallowSuggestions || editor.props.disableTextExtracting || request == null) {
            return null
        }
        if ((flags and GET_EXTRACTED_TEXT_MONITOR) != 0) {
            editor.setExtracting(request)
        } else {
            editor.setExtracting(null)
        }

        return editor.extractText(request)
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        editor.keyMetaStates.clearMetaStates(states)
        return true
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        return false
    }

    override fun getHandler(): Handler {
        return editor.handler
    }

    @RequiresApi(31)
    override fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): SurroundingText? {
        if (DEBUG) {
            logger.d("getSurroundingText, beforeLen = $beforeLength, afterLen = $afterLength")
        }
        if (editor.props.disallowSuggestions) {
            return SurroundingText("", 0, 0, -1)
        }
        if ((beforeLength or afterLength) < 0) {
            throw IllegalArgumentException("length < 0")
        }
        var startOffset = max(0, cursor.left - beforeLength)
        val selStart = cursor.left
        startOffset = min(startOffset, selStart)
        val text = getTextRegionUnlimited(
            startOffset,
            min(editor.text.length, cursor.right + afterLength),
            flags
        )
        return SurroundingText(
            text,
            cursor.left - startOffset,
            cursor.right - startOffset,
            startOffset
        )
    }

    override fun setImeConsumesInput(imeConsumesInput: Boolean): Boolean {
        if (connectionInvalid) {
            return false
        }
        imeConsumingInput = imeConsumesInput
        editor.invalidate()
        return true
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        if (connectionInvalid || action == null) {
            return false
        }
        editor.dispatchEvent(ImePrivateCommandEvent(editor, action, data))
        return true
    }

    override fun replaceText(
        start: Int,
        end: Int,
        text: CharSequence,
        newCursorPosition: Int,
        textAttribute: TextAttribute?
    ): Boolean {
        if (DEBUG) {
            logger.d("replaceText, st = $start, ed = $end, text = $text, nCurPos = $newCursorPosition")
        }
        val length = editor.text.length
        if (start < 0 || end < 0 || start > end || start > length || end > length) {
            return false
        }
        beginBatchEdit()
        finishComposingText()
        setSelection(start, end)
        commitText(text, newCursorPosition)
        endBatchEdit()
        return true
    }

    @RequiresApi(33)
    override fun takeSnapshot(): TextSnapshot? {
        var composingStart = -1
        var composingEnd = -1
        if (composingText.isComposing) {
            composingStart = composingText.startIndex
            composingEnd = composingText.endIndex
        }

        val surroundingText = getSurroundingText(
            MEMORY_EFFICIENT_TEXT_LENGTH / 2,
            MEMORY_EFFICIENT_TEXT_LENGTH / 2,
            GET_TEXT_WITH_STYLES
        ) ?: return null

        val cursorCapsMode = getCursorCapsMode(
            TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES
        )

        return TextSnapshot(surroundingText, composingStart, composingEnd, cursorCapsMode)
    }
}
