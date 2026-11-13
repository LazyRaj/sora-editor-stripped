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
// Modified by Raj Dave (aka LazyRaj)
package io.github.rosemoe.sora.lang.styling

import io.github.rosemoe.sora.lang.styling.line.LineAnchorStyle
import io.github.rosemoe.sora.lang.styling.line.LineStyles
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.util.MutableInt
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * This class stores styles of text and other decorations in editor related to code.
 *
 * Note that this does not save any information related to languages. No extra space is provided
 * for communication between analyzers and auto-completion. You should manage it by yourself.
 *
 * If you are going to extend this class, please read the source code carefully in advance
 */
@JvmSuppressWildcards
open class Styles @JvmOverloads constructor(
    @JvmField var spans: Spans? = null,
    initCodeBlocks: Boolean = true
) {

    /**
     * **Sorted** list of LineStyles
     */
    @JvmField
    var lineStyles: MutableList<LineStyles>? = null

    @JvmField
    var styleTypeCount: MutableMap<Class<*>, MutableInt>? = null

    @JvmField
    var blocks: (@JvmSuppressWildcards List<CodeBlock>)? = if (initCodeBlocks) ArrayList(128) else null

    @JvmField
    var suppressSwitch: Int = Int.MAX_VALUE

    @JvmField
    var indentCountMode: Boolean = false

    /**
     * Get analyzed spans
     */
    fun getSpans(): Spans? = spans

    /**
     * Add a new code block info
     *
     * @param block Info of code block
     */
    fun addCodeBlock(block: CodeBlock) {
        val list = (blocks as? MutableList<CodeBlock>) ?: ArrayList<CodeBlock>(128).also { blocks = it }
        list.add(block)
    }

    /**
     * Returns suppress switch
     */
    fun getSuppressSwitch(): Int = suppressSwitch

    /**
     * Set suppress switch for editor
     */
    fun setSuppressSwitch(suppressSwitch: Int) {
        this.suppressSwitch = suppressSwitch
    }

    /**
     * Set indent count mode
     */
    fun setIndentCountMode(indentCountMode: Boolean) {
        this.indentCountMode = indentCountMode
    }

    /**
     * Check if indent count mode is enabled
     */
    fun isIndentCountMode(): Boolean = indentCountMode

    /**
     * Adjust styles on insert.
     */
    fun adjustOnInsert(start: CharPosition, end: CharPosition) {
        spans?.adjustOnInsert(start, end)
        val delta = end.line - start.line
        if (delta == 0) {
            return
        }
        blocks?.let {
            if (it is MutableList<CodeBlock>) {
                BlocksUpdater.update(it, start.line, delta)
            }
        }
        lineStyles?.let { stylesList ->
            for (styles in stylesList) {
                if (styles.line > start.line) {
                    styles.line += delta
                    styles.updateElements()
                }
            }
        }
    }

    /**
     * Adjust styles on delete.
     */
    fun adjustOnDelete(start: CharPosition, end: CharPosition) {
        spans?.adjustOnDelete(start, end)
        val delta = start.line - end.line
        if (delta == 0) {
            return
        }
        blocks?.let {
            if (it is MutableList<CodeBlock>) {
                BlocksUpdater.update(it, start.line, delta)
            }
        }
        lineStyles?.let { stylesList ->
            val itr = stylesList.iterator()
            while (itr.hasNext()) {
                val styles = itr.next()
                val line = styles.line
                if (line > end.line) {
                    styles.line += delta
                    styles.updateElements()
                } else if (line > start.line /* line <= end.line */) {
                    itr.remove()
                }
            }
        }
    }

    fun addLineStyle(style: LineAnchorStyle) {
        val list = lineStyles ?: ArrayList<LineStyles>().also {
            lineStyles = it
            styleTypeCount = ConcurrentHashMap()
        }
        val type = style.javaClass
        for (lineStyle in list) {
            if (lineStyle.line == style.line) {
                styleCountUpdate(type, lineStyle.addStyle(style))
                return
            }
        }
        val lineStyle = LineStyles(style.line)
        list.add(lineStyle)
        styleCountUpdate(type, lineStyle.addStyle(style))
    }

    private fun styleCountUpdate(type: Class<*>, delta: Int) {
        val counts = styleTypeCount ?: ConcurrentHashMap<Class<*>, MutableInt>().also { styleTypeCount = it }
        var res = counts[type]
        if (res == null) {
            res = MutableInt(0)
            counts[type] = res
        }
        res.value += delta
    }

    /**
     * Remove the style of given kind from line
     */
    fun eraseLineStyle(line: Int, type: Class<out LineAnchorStyle>) {
        val list = lineStyles ?: return
        for (lineStyle in list) {
            if (lineStyle.line == line) {
                styleCountUpdate(type, -lineStyle.eraseStyle(type))
                break
            }
        }
    }

    /**
     * Remove all line styles
     */
    fun eraseAllLineStyles() {
        lineStyles?.clear()
        styleTypeCount?.clear()
    }

    /**
     * Do some extra work before finally sending the result to editor.
     */
    fun finishBuilding() {
        val blockList = blocks
        if (blockList != null) {
            var pre = -1
            var sort = false
            for (i in 0 until blockList.size - 1) {
                val cur = blockList[i + 1].endLine
                if (pre > cur) {
                    sort = true
                    break
                }
                pre = cur
            }
            if (sort && blockList is MutableList<CodeBlock>) {
                Collections.sort(blockList, CodeBlock.COMPARATOR_END)
            }
        }
        lineStyles?.let { Collections.sort(it) }
    }
}
