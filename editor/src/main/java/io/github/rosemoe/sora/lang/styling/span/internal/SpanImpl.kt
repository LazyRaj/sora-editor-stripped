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
package io.github.rosemoe.sora.lang.styling.span.internal

import androidx.collection.MutableIntObjectMap
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanPool
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor
import io.github.rosemoe.sora.lang.styling.span.SpanExt
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs

open class SpanImpl internal constructor(
    override var column: Int = 0,
    override var style: Long = 0L
) : Span {

    override var extra: Any? = null
    private var extMap: MutableIntObjectMap<SpanExt>? = null

    override var underlineColor: ResolvableColor?
        get() = getSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR)
        set(value) {
            setSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR, value)
        }

    override fun setSpanExt(extType: Int, ext: SpanExt?) {
        require(SpanExtAttrs.checkType(extType, ext)) {
            "type mismatch: extType $extType and extObj $ext"
        }
        if (ext == null) {
            extMap?.remove(extType)
            return
        }
        val map = extMap ?: MutableIntObjectMap<SpanExt>().also { extMap = it }
        map.set(extType, ext)
    }

    override fun hasSpanExt(extType: Int): Boolean {
        return getSpanExt<SpanExt>(extType) != null
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getSpanExt(extType: Int): T? {
        return extMap?.get(extType) as? T
    }

    override fun removeAllSpanExt() {
        extMap?.clear()
    }

    override fun reset() {
        column = 0
        style = 0L
        extra = null
        removeAllSpanExt()
    }

    override fun copy(): Span {
        val span = SpanImpl(column, style)
        val currentMap = extMap
        if (currentMap != null) {
            val newMap = MutableIntObjectMap<SpanExt>()
            newMap.putAll(currentMap)
            span.extMap = newMap
        }
        return span
    }

    override fun recycle(): Boolean {
        reset()
        return pool.offer(this)
    }

    override fun toString(): String {
        return "SpanImpl(column=$column, style=$style, extra=$extra, extMap=$extMap)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpanImpl) return false
        return column == other.column && style == other.style && extMap == other.extMap
    }

    override fun hashCode(): Int {
        var result = column
        result = 31 * result + style.hashCode()
        result = 31 * result + extMap.hashCode()
        return result
    }

    companion object {
        private val pool: SpanPool<SpanImpl> = SpanPool { column, style -> SpanImpl(column, style) }

        fun obtain(column: Int, style: Long): SpanImpl {
            return pool.obtain(column, style)
        }
    }
}
