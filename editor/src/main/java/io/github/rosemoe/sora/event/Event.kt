/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.event

import io.github.rosemoe.sora.widget.CodeEditor

/**
 * An Event object describes an event of editor.
 * It includes several attributes such as time and the editor object.
 * Subclasses of Event will define their own fields or methods.
 *
 * @author Rosemoe
 */
abstract class Event @JvmOverloads constructor(
    val editor: CodeEditor,
    open val eventTime: Long = System.currentTimeMillis()
) {

    /**
     * Get intercepted dispatch targets
     */
    var interceptTargets: Int = 0
        private set

    /**
     * Check whether this event can be intercepted (so that the event is not sent to other
     * receivers after being intercepted)
     */
    open fun canIntercept(): Boolean = false

    /**
     * Intercept the event for all targets.
     *
     * Make sure [canIntercept] returns true. Otherwise, an [UnsupportedOperationException]
     * will be thrown.
     *
     * @see InterceptTarget
     */
    fun intercept() {
        if (!canIntercept()) {
            throw UnsupportedOperationException("intercept() not supported")
        }
        interceptTargets = InterceptTarget.TARGET_EDITOR or InterceptTarget.TARGET_RECEIVERS
    }

    /**
     * Intercept the event for some targets
     *
     * @param targets Masks for target types
     * @see InterceptTarget
     */
    fun intercept(targets: Int) {
        if (!canIntercept()) {
            throw UnsupportedOperationException("intercept() not supported")
        }
        interceptTargets = targets
    }

    /**
     * Check whether this event is intercepted for some types of targets
     */
    val isIntercepted: Boolean
        get() = interceptTargets != 0
}
