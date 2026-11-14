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
/* Modified by Raj Dave (aka LazyRaj) */
package io.github.rosemoe.sora.langs.textmate.registry

import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.registry.IThemeSource
import java.util.concurrent.CopyOnWriteArrayList

class ThemeRegistry {

    fun interface ThemeChangeListener {
        fun onChangeTheme(newTheme: ThemeModel)
    }

    private val allListener = CopyOnWriteArrayList<ThemeChangeListener>()
    private val allThemeModel = CopyOnWriteArrayList<ThemeModel>()

    var currentThemeModel: ThemeModel = ThemeModel.EMPTY
        private set

    fun loadTheme(themeSource: IThemeSource, isCurrentTheme: Boolean = true) {
        loadTheme(ThemeModel(themeSource), isCurrentTheme)
    }

    @Synchronized
    fun loadTheme(themeModel: ThemeModel, isCurrentTheme: Boolean = true) {
        if (!themeModel.isLoaded) {
            themeModel.load()
        }
        val existingTheme = findThemeByThemeName(themeModel.name)
        if (existingTheme != null) {
            setTheme(existingTheme)
            return
        }
        allThemeModel.add(themeModel)
        if (isCurrentTheme) {
            setTheme(themeModel)
        }
    }

    fun findThemeByFileName(name: String): ThemeModel? {
        return allThemeModel.firstOrNull { it.name == name }
    }

    fun findThemeByThemeName(name: String): ThemeModel? {
        return allThemeModel.firstOrNull { it.rawTheme?.name == name }
    }

    @Synchronized
    fun setTheme(name: String): Boolean {
        var targetModel = findThemeByFileName(name)
        if (targetModel != null) {
            setTheme(targetModel)
            return true
        }

        targetModel = findThemeByThemeName(name)
        if (targetModel != null) {
            setTheme(targetModel)
            return true
        }

        return false
    }

    @Synchronized
    fun setTheme(themeModel: ThemeModel) {
        currentThemeModel = themeModel
        if (!allThemeModel.contains(themeModel)) {
            allThemeModel.add(themeModel)
        }
        if (!themeModel.isLoaded) {
            try {
                themeModel.load()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        for (listener in allListener) {
            listener.onChangeTheme(themeModel)
        }
    }

    fun hasListener(listener: ThemeChangeListener): Boolean {
        return allListener.contains(listener)
    }

    fun addListener(listener: ThemeChangeListener) {
        if (!allListener.contains(listener)) {
            allListener.add(listener)
        }
    }

    fun removeListener(listener: ThemeChangeListener) {
        allListener.remove(listener)
    }

    companion object {
        @Volatile
        private var instance: ThemeRegistry? = null

        @Synchronized
        fun getInstance(): ThemeRegistry {
            return instance ?: ThemeRegistry().also { instance = it }
        }
    }
}
