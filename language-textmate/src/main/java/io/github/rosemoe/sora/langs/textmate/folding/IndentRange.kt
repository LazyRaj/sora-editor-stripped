/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
package io.github.rosemoe.sora.langs.textmate.folding

import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.rosemoe.sora.text.Content
import org.eclipse.tm4e.core.internal.oniguruma.OnigRegExp
import org.eclipse.tm4e.core.internal.oniguruma.OnigResult

object IndentRange {
    const val MAX_LINE_NUMBER = 0xFFFFFF
    const val MAX_FOLDING_REGIONS = 0xFFFF
    const val MASK_INDENT = -0x1000000 // 0xFF000000.toInt()

    @JvmStatic
    fun computeStartColumn(line: CharSequence, len: Int, tabSize: Int): Int {
        var column = 0
        var i = 0

        while (i < len) {
            val chCode = line[i]
            if (chCode == ' ') {
                column++
            } else if (chCode == '\t') {
                column += tabSize
            } else {
                break
            }
            i++
        }

        if (i == len) {
            return -1
        }
        return column
    }

    @JvmStatic
    fun computeIndentLevel(line: CharSequence, len: Int, tabSize: Int): Int {
        var indent = 0
        var i = 0

        while (i < len) {
            val chCode = line[i]
            if (chCode == ' ') {
                indent++
            } else if (chCode == '\t') {
                indent = indent - indent % tabSize + tabSize
            } else {
                break
            }
            i++
        }

        if (i == len) {
            return -1
        }
        return indent
    }

    @Throws(Exception::class)
    @JvmStatic
    fun computeRanges(
        model: Content,
        tabSize: Int,
        offSide: Boolean,
        helper: FoldingHelper,
        pattern: OnigRegExp?,
        delegate: AsyncIncrementalAnalyzeManager<*, *>.CodeBlockAnalyzeDelegate
    ): FoldingRegions {
        val result = RangesCollector()
        val previousRegions = ArrayList<PreviousRegion>()
        var line = model.lineCount + 1
        previousRegions.add(PreviousRegion(-1, line, line))

        line = model.lineCount - 1
        while (line >= 0 && delegate.isNotCancelled) {
            val indent = helper.getIndentFor(line)
            var previous = previousRegions[previousRegions.size - 1]
            if (indent == -1) {
                if (offSide) {
                    previous.endAbove = line
                }
                line--
                continue
            }
            var m: OnigResult? = null
            if (pattern != null && (helper.getResultFor(line).also { m = it }) != null) {
                val match = m!!
                if (match.count() >= 2) {
                    var i = previousRegions.size - 1
                    while (i > 0 && previousRegions[i].indent != -2) {
                        i--
                    }
                    if (i > 0) {
                        previous = previousRegions[i]
                        result.insertFirst(line, previous.line, indent)
                        previous.line = line
                        previous.indent = indent
                        previous.endAbove = line
                        line--
                        continue
                    }
                } else {
                    previousRegions.add(PreviousRegion(-2, line, line))
                    line--
                    continue
                }
            }
            if (previous.indent > indent) {
                do {
                    previousRegions.removeAt(previousRegions.size - 1)
                    previous = previousRegions[previousRegions.size - 1]
                } while (previous.indent > indent)

                val endLineNumber = previous.endAbove - 1
                if (endLineNumber - line >= 1) {
                    result.insertFirst(line, endLineNumber, indent)
                }
            }
            if (previous.indent == indent) {
                previous.endAbove = line
            } else {
                previousRegions.add(PreviousRegion(indent, line, line))
            }
            line--
        }
        return result.toIndentRanges(model)
    }
}
