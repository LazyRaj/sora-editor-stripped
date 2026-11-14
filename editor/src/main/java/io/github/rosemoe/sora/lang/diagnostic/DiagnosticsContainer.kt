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

import io.github.rosemoe.sora.annotations.UnsupportedUserUsage
import io.github.rosemoe.sora.event.PublishDiagnosticsEvent
import io.github.rosemoe.sora.widget.CodeEditor
import java.lang.ref.WeakReference

/**
 * A thread-safe class for containing diagnostics
 *
 * @author Rosemoe
 */
open class DiagnosticsContainer(
    private val shiftEnabled: Boolean = true
) {

    private val regions: MutableList<DiagnosticRegion> = ArrayList()
    private var editorRef: WeakReference<CodeEditor?> = WeakReference(null)

    @Synchronized
    private fun modifyAndDispatch(modification: Runnable) {
        val editor = editorRef.get()
        val oldRegions = ArrayList(regions)
        modification.run()
        val newRegions = ArrayList(regions)
        if (editor != null) {
            editor.dispatchEvent(PublishDiagnosticsEvent(editor, oldRegions, newRegions))
        }
    }

    /**
     * Set multiple diagnostics and clear old ones. Only one event will be dispatched.
     */
    open fun setDiagnostics(regions: Collection<DiagnosticRegion>) {
        modifyAndDispatch {
            this.regions.clear()
            this.regions.addAll(regions)
        }
    }

    /**
     * Add multiple diagnostics
     */
    open fun addDiagnostics(regions: Collection<DiagnosticRegion>) {
        modifyAndDispatch {
            this.regions.addAll(regions)
        }
    }

    /**
     * Add single diagnostic item
     */
    open fun addDiagnostic(diagnostic: DiagnosticRegion) {
        modifyAndDispatch {
            this.regions.add(diagnostic)
        }
    }

    /**
     * Query diagnostics that can be displayed either partly or fully in the given region
     *
     * @param result Destination of result
     * @param startIndex Start index of query
     * @param endIndex End index of query
     */
    @Synchronized
    open fun queryInRegion(result: MutableList<DiagnosticRegion>, startIndex: Int, endIndex: Int) {
        for (region in regions) {
            if (region.endIndex > startIndex && region.startIndex <= endIndex) {
                result.add(region)
            }
        }
    }

    @Synchronized
    open fun shiftOnInsert(insertStart: Int, insertEnd: Int) {
        if (!shiftEnabled) {
            return
        }
        val length = insertEnd - insertStart
        for (region in regions) {
            // Type 1, text is inserted inside a diagnostic
            if (region.startIndex <= insertStart && region.endIndex >= insertStart) {
                region.endIndex += length
            }
            // Type 2, text is inserted before a diagnostic
            if (region.startIndex > insertStart) {
                region.startIndex += length
                region.endIndex += length
            }
        }
    }

    @Synchronized
    open fun shiftOnDelete(deleteStart: Int, deleteEnd: Int) {
        if (!shiftEnabled) {
            return
        }
        val length = deleteEnd - deleteStart
        val garbage = ArrayList<DiagnosticRegion>()
        for (region in regions) {
            // Compute cross length
            val sharedStart = maxOf(deleteStart, region.startIndex)
            val sharedEnd = minOf(deleteEnd, region.endIndex)
            if (sharedEnd <= sharedStart) {
                // No shared region
                if (region.startIndex >= deleteEnd) {
                    // Shift left
                    region.startIndex -= length
                    region.endIndex -= length
                }
            } else {
                // Has shared region
                val sharedLength = sharedEnd - sharedStart
                region.endIndex -= sharedLength
                if (region.startIndex > deleteStart) {
                    // Shift left
                    val shiftLeftCount = region.startIndex - deleteStart
                    region.startIndex -= shiftLeftCount
                    region.endIndex -= shiftLeftCount
                }

                if (region.startIndex == region.endIndex) {
                    garbage.add(region)
                }
            }
        }
        regions.removeAll(garbage)
    }

    @UnsupportedUserUsage
    open fun attachEditor(editor: CodeEditor) {
        editorRef = WeakReference(editor)
    }

    @UnsupportedUserUsage
    open fun detachEditor() {
        editorRef.clear()
    }

    /**
     * Remove all items
     */
    open fun reset() {
        modifyAndDispatch { regions.clear() }
    }

    @Synchronized
    open fun getRegions(): List<DiagnosticRegion> {
        return ArrayList(regions)
    }
}
