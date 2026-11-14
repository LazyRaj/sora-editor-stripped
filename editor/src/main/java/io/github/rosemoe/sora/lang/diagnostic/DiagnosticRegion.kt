/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
 *    Modified by Raj Dave (aka LazyRaj)
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
package io.github.rosemoe.sora.lang.diagnostic

/**
 * Class for describing a diagnostic region.
 *
 * @author Rosemoe
 */
class DiagnosticRegion(
    var startIndex: Int,
    var endIndex: Int,
    var severity: Short,
    var id: Long = 0L,
    var detail: DiagnosticDetail? = null
) : Comparable<DiagnosticRegion> {

    operator fun component1(): Int = startIndex
    operator fun component2(): Int = endIndex
    operator fun component3(): Short = severity
    operator fun component4(): Long = id
    operator fun component5(): DiagnosticDetail? = detail

    operator fun contains(index: Int): Boolean = index in startIndex until endIndex

    fun copy(
        startIndex: Int = this.startIndex,
        endIndex: Int = this.endIndex,
        severity: Short = this.severity,
        id: Long = this.id,
        detail: DiagnosticDetail? = this.detail
    ): DiagnosticRegion = DiagnosticRegion(startIndex, endIndex, severity, id, detail)

    companion object {
        const val SEVERITY_NONE: Short = 0
        const val SEVERITY_TYPO: Short = 1
        const val SEVERITY_WARNING: Short = 2
        const val SEVERITY_ERROR: Short = 3
    }

    override fun compareTo(other: DiagnosticRegion): Int {
        var cmp = startIndex.compareTo(other.startIndex)
        if (cmp == 0) {
            cmp = endIndex.compareTo(other.endIndex)
        }
        if (cmp == 0) {
            cmp = severity.compareTo(other.severity)
        }
        if (cmp == 0) {
            cmp = id.compareTo(other.id)
        }
        return cmp
    }
}
