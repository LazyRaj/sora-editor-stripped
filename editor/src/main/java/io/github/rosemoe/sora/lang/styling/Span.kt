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

import io.github.rosemoe.sora.lang.styling.color.ConstColor
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor
import io.github.rosemoe.sora.lang.styling.span.SpanExt
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs

/**
 * Span describes the appearance and other attributes of text segment
 *
 * @author Rosemoe
 */
interface Span {

    /**
     * Column of this span
     */
    var column: Int

    /**
     * Shift column by given delta
     */
    fun shiftColumnBy(deltaColumn: Int) {
        column += deltaColumn
    }

    /**
     * Style of the span
     *
     * @see TextStyle
     */
    var style: Long

    /**
     * Foreground color ID from style
     */
    val foregroundColorId: Int
        get() = TextStyle.getForegroundColorId(style)

    /**
     * Background color ID from style
     */
    val backgroundColorId: Int
        get() = TextStyle.getBackgroundColorId(style)

    /**
     * Bits of other text styles that affects measuring
     */
    val styleBits: Long
        get() = TextStyle.getStyleBits(style)

    /**
     * The [ResolvableColor] instance for resolving underline color of this span
     */
    var underlineColor: ResolvableColor?

    /**
     * Set underline color of span. `0` for no underline.
     * **This is not color ID**
     */
    fun setUnderlineColor(color: Int) {
        if (color == 0) {
            underlineColor = null
            return
        }
        underlineColor = ConstColor(color)
    }

    /**
     * Extra data for language internal use
     */
    var extra: Any?

    /**
     * Set extended attribute of this span. The type of [ext] is checked whether it is compatible
     * with the given [extType].
     *
     * @param extType Type of extension, from [SpanExtAttrs]
     * @param ext     The data to set. Use null to unset.
     */
    fun setSpanExt(extType: Int, ext: SpanExt?)

    /**
     * Check if certain extended attribute is set
     */
    fun hasSpanExt(extType: Int): Boolean

    /**
     * Get extended attribute of given type. If it is unset, null is returned.
     */
    fun <T> getSpanExt(extType: Int): T?

    /**
     * Remove all [SpanExt]s
     */
    fun removeAllSpanExt()

    /**
     * Reset all properties of this span, including column, style and ext.
     */
    fun reset()

    /**
     * Create a new span with the same attributes. The new span can be safely modified with affecting
     * the original span.
     *
     * Note that [SpanExt] objects are **shared** by the old span and new span instance.
     *
     * @return new span with the same attribute
     */
    fun copy(): Span

    /**
     * Recycle this span to pool for later use. After calling this method, you should not
     * make any access to this [Span] instance. And all attributes of this span are reset.
     *
     * Note that no matter whether the span is added to pool, it will be reset.
     *
     * @return if the span is actually added to the pool.
     */
    fun recycle(): Boolean

    companion object {
        /**
         * Get an available [Span] object from either cache or new instance.
         * The result object will be initialized with the given arguments.
         */
        @JvmStatic
        fun obtain(column: Int, style: Long): Span {
            return SpanFactory.obtain(column, style)
        }

        /**
         * Recycle all spans in the given collection
         */
        @JvmStatic
        fun recycleAll(spans: Collection<Span>) {
            SpanFactory.recycleAll(spans)
        }
    }
}
