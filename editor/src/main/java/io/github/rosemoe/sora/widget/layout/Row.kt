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
package io.github.rosemoe.sora.widget.layout

import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint

/**
 * This class represents a 'row' in editor.
 * Editor uses this to draw rows
 *
 * @author Rosemoe
 */
class Row {

    /**
     * The index in lines
     * But not row index
     */
    @JvmField
    var lineIndex: Int = 0

    /**
     * Whether this row is the first one of a line.
     * Editor will draw line number to left of this row to indicate this
     */
    @JvmField
    var isLeadingRow: Boolean = false

    /**
     * Whether this row is the last one of a line.
     * Editor will draw soft-wrap or line-break indicator according to this
     */
    @JvmField
    var isTrailingRow: Boolean = false

    /**
     * Start index in target line
     */
    @JvmField
    var startColumn: Int = 0

    /**
     * End index in target line
     */
    @JvmField
    var endColumn: Int = 0

    /**
     * Inlay hints on the row
     */
    @JvmField
    var inlayHints: (@JvmSuppressWildcards List<InlayHint>)? = null

    /**
     * Extra translation when rendering
     */
    @JvmField
    var renderTranslateX: Float = 0f
}
