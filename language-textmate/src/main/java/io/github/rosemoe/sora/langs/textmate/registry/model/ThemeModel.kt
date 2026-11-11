/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
package io.github.rosemoe.sora.langs.textmate.registry.model

import io.github.rosemoe.sora.langs.textmate.utils.StringUtils
import org.eclipse.tm4e.core.internal.theme.Theme
import org.eclipse.tm4e.core.internal.theme.raw.IRawTheme
import org.eclipse.tm4e.core.internal.theme.raw.RawThemeReader
import org.eclipse.tm4e.core.registry.IThemeSource

class ThemeModel {
    val themeSource: IThemeSource?
    var rawTheme: IRawTheme? = null
        private set
    var theme: Theme? = null
        private set
    val name: String
    var isDark: Boolean = false

    constructor(themeSource: IThemeSource) {
        this.themeSource = themeSource
        this.name = StringUtils.getFileNameWithoutExtension(themeSource.filePath)
    }

    constructor(themeSource: IThemeSource, name: String) {
        this.themeSource = themeSource
        this.name = name
    }

    private constructor(name: String) {
        this.themeSource = null
        this.rawTheme = null
        this.name = name
        this.theme = Theme.createFromRawTheme(null, null)
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun load(colorMap: List<String>? = null) {
        val raw = RawThemeReader.readTheme(themeSource)
        rawTheme = raw
        theme = Theme.createFromRawTheme(raw, colorMap)
    }

    val isLoaded: Boolean
        get() = theme != null

    companion object {
        @JvmField
        val EMPTY = ThemeModel("EMPTY")
    }
}
