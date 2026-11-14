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

import android.app.ProgressDialog
import android.widget.Toast
import androidx.annotation.IntRange
import io.github.rosemoe.sora.I18nConfig
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.PreserveCaseReplace
import io.github.rosemoe.sora.text.TextUtils
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.util.LongArrayList
import io.github.rosemoe.sora.util.regex.RegexBackrefGrammar
import io.github.rosemoe.sora.util.regex.RegexBackrefHelper
import io.github.rosemoe.sora.util.regex.RegexBackrefParser
import io.github.rosemoe.sora.util.regex.RegexBackrefToken
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Search text in editor.
 *
 * Note that editor searches text in another thread, so results may not be available immediately. Also,
 * the searcher does not match empty text. For example, you will never match a single empty
 * line by regex '^.*$'. What's more, zero-length pattern is not permitted.
 * The searcher updates its search results automatically when editor text is changed, even after [CodeEditor.setText]
 * is invoked. So be careful that the search result is changing and [PublishSearchResultEvent] is
 * re-triggered when search result is available for changed text.
 *
 * @author Rosemoe
 * @see PublishSearchResultEvent
 * @see SearchOptions
 */
open class EditorSearcher(
    private val editor: CodeEditor
) {

    var currentPattern: String? = null

    var searchOptions: SearchOptions? = null

    var replaceOptions: ReplaceOptions = ReplaceOptions.DEFAULT

    var currentThread: Thread? = null

    /**
     * Search results. Note that it is naturally sorted by start index (and also end index).
     * No overlapping region is permitted.
     */
    var lastResults: LongArrayList? = null

    var isCyclicJumping: Boolean = true
    var isEnsureOccurrenceVisible: Boolean = false

    init {
        editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            if (hasQuery()) {
                executeMatch()
            }
        }
    }

    /**
     * Search text with the given pattern and options. If you use [SearchOptions.TYPE_REGULAR_EXPRESSION],
     * the pattern will be your regular expression.
     *
     * [stopSearch] should be called if you want to stop, instead of invoking this method with nulls.
     *
     * @throws IllegalArgumentException if pattern length is zero
     * @throws java.util.regex.PatternSyntaxException if pattern is invalid when regex is enabled.
     */
    open fun search(pattern: String, options: SearchOptions) {
        require(pattern.isNotEmpty()) { "pattern length must be > 0" }
        if (options.type == SearchOptions.TYPE_REGULAR_EXPRESSION) {
            Pattern.compile(pattern)
        }
        currentPattern = pattern
        searchOptions = options
        executeMatch()
        editor.postInvalidate()
    }

    private fun executeMatch() {
        val thread = currentThread
        if (thread != null && thread.isAlive) {
            thread.interrupt()
        }
        val runnable = SearchRunnable(editor.text, searchOptions!!, currentPattern!!)
        val newThread = Thread(runnable)
        currentThread = newThread
        newThread.start()
    }

    open fun stopSearch() {
        val thread = currentThread
        if (thread != null && thread.isAlive) {
            thread.interrupt()
        }
        currentThread = null
        lastResults = null
        currentPattern = null
        searchOptions = null
        editor.dispatchEvent(PublishSearchResultEvent(editor))
    }

    open fun hasQuery(): Boolean = currentPattern != null

    private fun checkState() {
        check(hasQuery()) { "pattern not set" }
    }

    open fun getCurrentMatchedPositionIndex(): Int {
        checkState()
        val cur = editor.cursor
        if (!cur.isSelected) {
            return -1
        }
        val left = cur.left
        val right = cur.right

        if (isResultValid()) {
            val res = lastResults ?: return -1
            val packed = IntPair.pack(left, right)
            val index = res.lowerBound(packed)
            if (index < res.size() && res.get(index) == packed) {
                return index
            }
        }
        return -1
    }

    open fun getMatchedPositionCount(): Int {
        checkState()
        if (!isResultValid()) {
            return 0
        }
        val result = lastResults
        return result?.size() ?: 0
    }

    open fun gotoNext(): Boolean {
        checkState()
        if (isResultValid()) {
            val res = lastResults ?: return false
            val right = editor.cursor.right
            var index = res.lowerBoundByFirst(right)
            if (index == res.size() && isCyclicJumping) {
                index = 0
            }
            if (index < res.size()) {
                val data = res.get(index)
                val start = IntPair.getFirst(data)
                val pos1 = editor.text.indexer.getCharPosition(start)
                val pos2 = editor.text.indexer.getCharPosition(IntPair.getSecond(data))
                editor.setSelectionRegion(
                    pos1.line,
                    pos1.column,
                    pos2.line,
                    pos2.column,
                    SelectionChangeEvent.CAUSE_SEARCH
                )
                return true
            }
        }
        return false
    }

    open fun gotoPrevious(): Boolean {
        checkState()
        if (isResultValid()) {
            val res = lastResults
            if (res == null || res.size() == 0) {
                return false
            }
            val left = editor.cursor.left
            var index = res.lowerBoundByFirst(left)
            if (index == res.size() || IntPair.getFirst(res.get(index)) >= left) {
                index--
            }
            if (index < 0 && isCyclicJumping) {
                index = res.size() - 1
            }
            if (index in 0 until res.size()) {
                val data = res.get(index)
                val end = IntPair.getSecond(data)
                val pos1 = editor.text.indexer.getCharPosition(IntPair.getFirst(data))
                val pos2 = editor.text.indexer.getCharPosition(end)
                editor.setSelectionRegion(
                    pos1.line,
                    pos1.column,
                    pos2.line,
                    pos2.column,
                    SelectionChangeEvent.CAUSE_SEARCH
                )
                return true
            }
        }
        return false
    }

    open fun isMatchedPositionSelected(): Boolean {
        return getCurrentMatchedPositionIndex() > -1
    }

    open fun replaceCurrentMatch(replacement: String) {
        var rep = replacement
        if (!editor.isEditable) {
            return
        }
        if (isMatchedPositionSelected()) {
            if (rep.isEmpty()) {
                editor.deleteText()
            } else {
                val opts = searchOptions
                if (opts != null && opts.type == SearchOptions.TYPE_REGULAR_EXPRESSION && opts.regexBackrefGrammar != null) {
                    val cursor = editor.cursor
                    val currentText = editor.text.substring(cursor.left, cursor.right)
                    val pattern = Pattern.compile(
                        currentPattern!!,
                        (if (opts.caseInsensitive) Pattern.CASE_INSENSITIVE else 0) or Pattern.MULTILINE
                    )
                    val matcher = pattern.matcher(currentText)
                    if (!matcher.find()) {
                        return
                    }
                    rep = RegexBackrefHelper.computeReplacement(
                        matcher,
                        opts.regexBackrefGrammar,
                        rep
                    )
                }
                if (replaceOptions.preserveCase) {
                    val cursor = editor.cursor
                    val currentText = editor.text.substring(cursor.left, cursor.right)
                    rep = PreserveCaseReplace.getReplacementSimple(currentText, rep)
                }
                editor.commitText(rep, false, false)
            }
        } else {
            gotoNext()
        }
    }

    @Suppress("DEPRECATION")
    open fun replaceAll(replacement: String, whenSucceeded: Runnable? = null) {
        if (!editor.isEditable) {
            return
        }
        checkState()
        if (!isResultValid()) {
            Toast.makeText(
                editor.context,
                I18nConfig.getResourceId(R.string.sora_editor_editor_search_busy),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val context = editor.context
        val dialog = ProgressDialog.show(
            context,
            I18nConfig.getString(context, R.string.sora_editor_replaceAll),
            I18nConfig.getString(context, R.string.sora_editor_editor_search_replacing),
            true,
            false
        )
        val res = lastResults ?: return
        val options = searchOptions ?: return
        val pattern = currentPattern ?: return
        Thread {
            try {
                val sb = editor.text.toStringBuilder()
                if (options.type == SearchOptions.TYPE_REGULAR_EXPRESSION && options.regexBackrefGrammar != null) {
                    val regex = Pattern.compile(
                        pattern,
                        (if (options.caseInsensitive) Pattern.CASE_INSENSITIVE else 0) or Pattern.MULTILINE
                    )
                    var matcher: Matcher? = null
                    var tokens: List<RegexBackrefToken>? = null
                    var delta = 0
                    val text = sb.toString()
                    for (i in 0 until res.size()) {
                        val region = res.get(i)
                        val start = IntPair.getFirst(region)
                        val end = IntPair.getSecond(region)
                        val regionText = text.substring(start, end)
                        if (matcher == null) {
                            matcher = regex.matcher(regionText)
                        } else {
                            matcher.reset(regionText)
                        }
                        if (!matcher.find()) {
                            continue
                        }
                        if (tokens == null) {
                            tokens = RegexBackrefParser(options.regexBackrefGrammar).parse(
                                replacement,
                                matcher.groupCount()
                            )
                        }
                        var computedReplacement =
                            RegexBackrefHelper.computeReplacement(matcher, tokens)
                        val newLength = computedReplacement.length
                        val oldLength = end - start
                        if (replaceOptions.preserveCase) {
                            computedReplacement = PreserveCaseReplace.getReplacementSimple(
                                sb.substring(start + delta, end + delta),
                                computedReplacement
                            )
                        }
                        sb.replace(start + delta, end + delta, computedReplacement)
                        delta += newLength - oldLength
                    }
                } else {
                    val newLength = replacement.length
                    var delta = 0
                    for (i in 0 until res.size()) {
                        val region = res.get(i)
                        val start = IntPair.getFirst(region)
                        val end = IntPair.getSecond(region)
                        val oldLength = end - start
                        var replaceText = replacement
                        if (replaceOptions.preserveCase) {
                            replaceText = PreserveCaseReplace.getReplacementSimple(
                                sb.substring(start + delta, end + delta),
                                replacement
                            )
                        }
                        sb.replace(start + delta, end + delta, replaceText)
                        delta += newLength - oldLength
                    }
                }
                editor.postInLifecycle {
                    val pos = editor.cursor.left()
                    editor.text.replace(
                        0,
                        0,
                        editor.lineCount - 1,
                        editor.text.getColumnCount(editor.lineCount - 1),
                        sb
                    )
                    editor.setSelectionAround(pos.line, pos.column)
                    dialog.dismiss()
                    whenSucceeded?.run()
                }
            } catch (e: Exception) {
                editor.postInLifecycle {
                    Toast.makeText(editor.context, "Replace failed:$e", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }.start()
    }

    open fun isResultValid(): Boolean {
        val thread = currentThread
        return thread == null || !thread.isAlive
    }

    /**
     * Search options for [EditorSearcher.search]
     */
    open class SearchOptions(
        @IntRange(from = 1, to = 3)
        val type: Int,
        val caseInsensitive: Boolean,
        val regexBackrefGrammar: RegexBackrefGrammar? = null
    ) {
        operator fun component1(): Int = type
        operator fun component2(): Boolean = caseInsensitive
        operator fun component3(): RegexBackrefGrammar? = regexBackrefGrammar

        companion object {
            const val TYPE_NORMAL = 1
            const val TYPE_WHOLE_WORD = 2
            const val TYPE_REGULAR_EXPRESSION = 3
        }

        init {
            require(type in 1..3) { "invalid type" }
        }

        constructor(caseInsensitive: Boolean, useRegex: Boolean) : this(
            if (useRegex) TYPE_REGULAR_EXPRESSION else TYPE_NORMAL,
            caseInsensitive,
            null
        )
    }

    /**
     * Replace options for [EditorSearcher.replaceCurrentMatch] and [EditorSearcher.replaceAll]
     */
    open class ReplaceOptions(
        val preserveCase: Boolean
    ) {
        operator fun component1(): Boolean = preserveCase

        companion object {
            val DEFAULT: ReplaceOptions = ReplaceOptions(false)
        }
    }

    private inner class SearchRunnable(
        content: Content,
        private val options: SearchOptions,
        private var pattern: String
    ) : Runnable {

        private val text: StringBuilder = content.toStringBuilder()
        private var localThread: Thread? = null

        private fun checkNotCancelled(): Boolean {
            return currentThread === localThread && !Thread.interrupted()
        }

        override fun run() {
            localThread = Thread.currentThread()
            val results = LongArrayList()
            val textLength = text.length
            val ignoreCase = options.caseInsensitive
            var pat = this.pattern
            when (options.type) {
                SearchOptions.TYPE_NORMAL -> {
                    var nextStart = 0
                    val patternLength = pat.length
                    while (nextStart != -1 && nextStart < textLength && checkNotCancelled()) {
                        nextStart = TextUtils.indexOf(text, pat, ignoreCase, nextStart)
                        if (nextStart != -1) {
                            results.add(IntPair.pack(nextStart, nextStart + patternLength))
                            nextStart += patternLength
                        }
                    }
                }

                SearchOptions.TYPE_WHOLE_WORD,
                SearchOptions.TYPE_REGULAR_EXPRESSION -> {
                    if (options.type == SearchOptions.TYPE_WHOLE_WORD) {
                        pat = "\\b" + Pattern.quote(pat) + "\\b"
                    }
                    val regex = Pattern.compile(
                        pat,
                        (if (ignoreCase) Pattern.CASE_INSENSITIVE else 0) or Pattern.MULTILINE
                    )
                    val string = text.toString()
                    val matcher = regex.matcher(string)
                    while (matcher.find() && checkNotCancelled()) {
                        results.add(IntPair.pack(matcher.start(), matcher.end()))
                        if (matcher.end() == string.length) {
                            break
                        }
                    }
                }
            }
            if (checkNotCancelled()) {
                editor.postInLifecycle {
                    if (currentThread === localThread) {
                        lastResults = results
                        editor.invalidate()
                        editor.dispatchEvent(PublishSearchResultEvent(editor))
                        currentThread = null
                        if (isEnsureOccurrenceVisible && results.size() > 0) {
                            val right = editor.cursor.right
                            var index = results.lowerBoundByFirst(right)

                            if (index >= results.size()) {
                                index = results.size() - 1
                            }

                            val match = results.get(index)
                            val start = IntPair.getFirst(match)
                            val startPos = editor.text.indexer.getCharPosition(start)
                            editor.ensurePositionVisible(startPos.line, startPos.column)
                        }
                    }
                }
            }
        }
    }
}
