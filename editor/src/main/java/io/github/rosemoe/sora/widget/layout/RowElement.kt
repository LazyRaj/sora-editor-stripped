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
/*
 * Modified by Raj Dave (aka LazyRaj)
 */
package io.github.rosemoe.sora.widget.layout

import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint

/**
 * Element on a row
 *
 * @author Rosemoe
 */
class RowElement {

    /**
     * Type of element.
     *
     * @see RowElementTypes
     */
    @JvmField
    var type: Int = 0

    /* Fields for type TEXT */

    /**
     * Start column of text
     */
    @JvmField
    var startColumn: Int = 0

    /**
     * End column of text
     */
    @JvmField
    var endColumn: Int = 0

    /**
     * Direction of the text run
     */
    @JvmField
    var isRtlText: Boolean = false

    /* Fields for type INLAY_HINT */

    /**
     * The inlay hint
     */
    @JvmField
    var inlayHint: InlayHint? = null

    /**
     * The expected column position to display after
     */
    @JvmField
    var displayColumnPosition: Int = 0
}
