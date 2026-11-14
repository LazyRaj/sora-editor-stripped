/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.lang.styling.span.internal

import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanPool
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor
import io.github.rosemoe.sora.lang.styling.span.SpanExt

/**
 * Span without SpanExt support.
 *
 * @author Rosemoe
 */
open class NoExtSpanImpl internal constructor(
    override var column: Int = 0,
    override var style: Long = 0L
) : Span {

    override var extra: Any? = null

    override var underlineColor: ResolvableColor?
        get() = null
        set(_) {
            throw UnsupportedOperationException()
        }

    override fun setSpanExt(extType: Int, ext: SpanExt?) {
        throw UnsupportedOperationException()
    }

    override fun hasSpanExt(extType: Int): Boolean = false

    override fun <T> getSpanExt(extType: Int): T? = null

    override fun removeAllSpanExt() {
        // no-op
    }

    override fun reset() {
        column = 0
        style = 0L
        extra = null
    }

    override fun copy(): Span {
        val span = NoExtSpanImpl(column, style)
        span.extra = extra
        return span
    }

    override fun recycle(): Boolean {
        reset()
        return pool.offer(this)
    }

    override fun toString(): String {
        return "NoExtSpanImpl(column=$column, style=$style, extra=$extra)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoExtSpanImpl) return false
        return column == other.column && style == other.style && extra == other.extra
    }

    override fun hashCode(): Int {
        var result = column
        result = 31 * result + style.hashCode()
        result = 31 * result + extra.hashCode()
        return result
    }

    companion object {
        private val pool: SpanPool<NoExtSpanImpl> =
            SpanPool { column, style -> NoExtSpanImpl(column, style) }

        fun obtain(column: Int, style: Long): NoExtSpanImpl {
            return pool.obtain(column, style)
        }
    }
}
