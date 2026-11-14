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
package io.github.rosemoe.sora.lang.styling.span

import io.github.rosemoe.sora.lang.styling.color.ResolvableColor

object SpanExtAttrs {
    /**
     * @see SpanColorResolver
     */
    const val EXT_COLOR_RESOLVER: Int = 0

    /**
     * @see SpanExternalRenderer
     */
    const val EXT_EXTERNAL_RENDERER: Int = 1

    /**
     * @see SpanInteractionInfo
     */
    const val EXT_INTERACTION_INFO: Int = 2

    /**
     * Set a [ResolvableColor] object for underline color resolving
     */
    const val EXT_UNDERLINE_COLOR: Int = 3

    fun checkType(extType: Int, ext: SpanExt?): Boolean {
        return ext == null || when (extType) {
            EXT_COLOR_RESOLVER -> ext is SpanColorResolver
            EXT_EXTERNAL_RENDERER -> ext is SpanExternalRenderer
            EXT_INTERACTION_INFO -> ext is SpanInteractionInfo
            EXT_UNDERLINE_COLOR -> ext is ResolvableColor
            else -> true
        }
    }
}
