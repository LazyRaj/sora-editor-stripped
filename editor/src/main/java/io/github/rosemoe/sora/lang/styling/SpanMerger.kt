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
// Modified by Raj Dave (aka LazyRaj)
package io.github.rosemoe.sora.lang.styling

import io.github.rosemoe.sora.lang.styling.color.ResolvableColor
import io.github.rosemoe.sora.lang.styling.span.SpanColorResolver
import io.github.rosemoe.sora.lang.styling.span.SpanExt
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs
import java.util.AbstractList
import java.util.Collections
import java.util.RandomAccess

/**
 * Composes two read-only span layers without copying unaffected spans.
 *
 * Columns must be non-negative and non-decreasing. Spans at the same column retain their
 * order within each layer; the last one applies to subsequent text. A nonempty lower layer
 * must start at column zero. The upper layer can start later; a zero-style span ends an override.
 * Nonzero upper color IDs replace lower color IDs, style flags are combined, and nonnull
 * upper extensions and extra data take precedence. Color resolvers are merged per channel.
 *
 * The result is an unmodifiable view made of ranges referencing the input lists and a
 * list of generated spans. Neither the inputs nor their spans are modified. The input lists,
 * spans and their extensions must remain unchanged while the result is in use. Spans read
 * from the result are borrowed and must not be modified or recycled.
 */
object SpanMerger {

    @JvmStatic
    fun merge(layerLower: List<Span>, layerUpper: List<Span>): List<Span> {
        checkColumns(layerLower, true)
        checkColumns(layerUpper, false)
        if (layerLower.isEmpty()) {
            return Collections.unmodifiableList(layerUpper)
        }
        if (layerUpper.isEmpty()) {
            return Collections.unmodifiableList(layerLower)
        }
        val builder = Builder()
        var lowerIndex = -1
        var upperIndex = -1
        var wasOverridden = false
        while (lowerIndex + 1 < layerLower.size || upperIndex + 1 < layerUpper.size) {
            val nextLower = if (lowerIndex + 1 < layerLower.size) {
                layerLower[lowerIndex + 1].column.toLong()
            } else {
                Long.MAX_VALUE
            }
            val nextUpper = if (upperIndex + 1 < layerUpper.size) {
                layerUpper[upperIndex + 1].column.toLong()
            } else {
                Long.MAX_VALUE
            }
            val column = Math.min(nextLower, nextUpper).toInt()
            if (nextLower == column.toLong()) {
                lowerIndex++
            }
            if (nextUpper == column.toLong()) {
                upperIndex++
            }
            val lower = layerLower[lowerIndex]
            val upper = if (upperIndex < 0) null else layerUpper[upperIndex]
            val overridden = upper != null && !isEmpty(upper)
            if (!overridden) {
                if (lower.column == column) {
                    builder.append(layerLower, lowerIndex)
                } else if (wasOverridden) {
                    builder.appendGenerated(MergedSpan(column, lower, null))
                }
            } else if (isEmpty(lower) && upper.column == column) {
                builder.append(layerUpper, upperIndex)
            } else {
                builder.appendGenerated(MergedSpan(column, lower, upper))
            }
            wasOverridden = overridden
        }
        return Collections.unmodifiableList(MergedList(builder.regions))
    }

    private fun checkColumns(spans: List<Span>, startsAtZero: Boolean) {
        var previous = -1
        for (i in spans.indices) {
            val column = spans[i].column
            if (column < 0 || column < previous || (i == 0 && startsAtZero && column != 0)) {
                throw IllegalArgumentException("Span columns must be nonnegative and nondecreasing; the lower layer must start at zero")
            }
            previous = column
        }
    }

    private fun isEmpty(span: Span): Boolean {
        return span.style == 0L && span.extra == null &&
                !span.hasSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER) &&
                !span.hasSpanExt(SpanExtAttrs.EXT_EXTERNAL_RENDERER) &&
                !span.hasSpanExt(SpanExtAttrs.EXT_INTERACTION_INFO) &&
                span.underlineColor == null
    }

    private class Region(
        val source: List<Span>,
        val sourceStart: Int,
        val resultStart: Int
    ) {
        var size: Int = 1
    }

    private class Builder {
        val regions: MutableList<Region> = ArrayList()
        val generated: MutableList<Span> = ArrayList()
        var size: Int = 0

        fun append(source: List<Span>, index: Int) {
            val last = if (regions.isEmpty()) null else regions[regions.size - 1]
            if (last != null && last.source === source && last.sourceStart + last.size == index) {
                last.size++
            } else {
                regions.add(Region(source, index, size))
            }
            size++
        }

        fun appendGenerated(span: Span) {
            generated.add(span)
            append(generated, generated.size - 1)
        }
    }

    private class MergedList(regionsList: List<Region>) : AbstractList<Span>(), RandomAccess {
        private val regions: Array<Region> = regionsList.toTypedArray()
        override val size: Int

        init {
            val last = regions[regions.size - 1]
            size = last.resultStart + last.size
        }

        override fun get(index: Int): Span {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException("index=$index, size=$size")
            }
            var low = 0
            var high = regions.size - 1
            while (low <= high) {
                val mid = (low + high) ushr 1
                val region = regions[mid]
                if (index < region.resultStart) {
                    high = mid - 1
                } else if (index >= region.resultStart + region.size) {
                    low = mid + 1
                } else {
                    return region.source[region.sourceStart + index - region.resultStart]
                }
            }
            throw AssertionError("Missing span region")
        }
    }

    private class MergedSpan(
        override var column: Int,
        private val lower: Span,
        private val upper: Span?
    ) : Span, SpanColorResolver {

        override var style: Long
            get() {
                var s = lower.style
                if (upper != null) {
                    val overlay = upper.style
                    if ((overlay and TextStyle.FOREGROUND_BITS) != 0L) {
                        s = s and TextStyle.FOREGROUND_BITS.inv()
                    }
                    if ((overlay and TextStyle.BACKGROUND_BITS) != 0L) {
                        s = s and TextStyle.BACKGROUND_BITS.inv()
                    }
                    s = s or overlay
                }
                return s
            }
            set(_) {
                throw UnsupportedOperationException()
            }

        override var extra: Any?
            get() = if (upper?.extra != null) upper.extra else lower.extra
            set(_) {
                throw UnsupportedOperationException()
            }

        override var underlineColor: ResolvableColor?
            get() = getSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR)
            set(_) {
                throw UnsupportedOperationException()
            }

        override fun shiftColumnBy(deltaColumn: Int) {
            throw UnsupportedOperationException()
        }

        override fun hasSpanExt(extType: Int): Boolean {
            return getSpanExt<SpanExt>(extType) != null
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> getSpanExt(extType: Int): T? {
            if (extType == SpanExtAttrs.EXT_COLOR_RESOLVER) {
                return if (lower.hasSpanExt(extType) || (upper != null && upper.hasSpanExt(extType))) this as T else null
            }
            if (extType == SpanExtAttrs.EXT_UNDERLINE_COLOR) {
                val overlay = upper?.underlineColor
                return (overlay ?: lower.underlineColor) as T?
            }
            val overlay = upper?.getSpanExt<SpanExt>(extType)
            return (overlay ?: lower.getSpanExt(extType)) as T?
        }

        override fun getForegroundColor(span: Span): ResolvableColor? {
            return resolveColor(true)
        }

        override fun getBackgroundColor(span: Span): ResolvableColor? {
            return resolveColor(false)
        }

        private fun resolveColor(foreground: Boolean): ResolvableColor? {
            if (upper != null) {
                val resolver = upper.getSpanExt<SpanColorResolver>(SpanExtAttrs.EXT_COLOR_RESOLVER)
                if (resolver != null) {
                    val color = if (foreground) resolver.getForegroundColor(upper) else resolver.getBackgroundColor(upper)
                    if (color != null) {
                        return color
                    }
                }
                if ((if (foreground) upper.foregroundColorId else upper.backgroundColorId) != 0) {
                    return null
                }
            }
            val resolver = lower.getSpanExt<SpanColorResolver>(SpanExtAttrs.EXT_COLOR_RESOLVER)
            return resolver?.let { if (foreground) it.getForegroundColor(lower) else it.getBackgroundColor(lower) }
        }

        override fun copy(): Span {
            val copy = SpanFactory.obtain(column, style)
            copy.extra = extra
            for (type in SpanExtAttrs.EXT_COLOR_RESOLVER..SpanExtAttrs.EXT_UNDERLINE_COLOR) {
                copy.setSpanExt(type, getSpanExt(type))
            }
            return copy
        }

        override fun setSpanExt(extType: Int, ext: SpanExt?) {
            throw UnsupportedOperationException()
        }

        override fun removeAllSpanExt() {
            throw UnsupportedOperationException()
        }

        override fun reset() {
            throw UnsupportedOperationException()
        }

        override fun recycle(): Boolean {
            throw UnsupportedOperationException()
        }
    }
}
