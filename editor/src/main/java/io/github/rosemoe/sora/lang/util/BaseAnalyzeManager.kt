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
package io.github.rosemoe.sora.lang.util

import android.os.Bundle
import androidx.annotation.CallSuper
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.text.ContentReference

/**
 * Convenience base class for simple [AnalyzeManager] implementations
 *
 * @author Rosemoe
 */
abstract class BaseAnalyzeManager : AnalyzeManager {

    var receiver: StyleReceiver? = null
        private set

    var extraArguments: Bundle? = null
        protected set

    var contentRef: ContentReference? = null
        protected set

    override fun setReceiver(receiver: StyleReceiver?) {
        this.receiver = receiver
    }

    @CallSuper
    override fun reset(content: ContentReference, extraArguments: Bundle) {
        this.extraArguments = extraArguments
        this.contentRef = content
        rerun()
    }

    @CallSuper
    override fun destroy() {
        this.receiver = null
        this.contentRef = null
        this.extraArguments = null
    }
}
