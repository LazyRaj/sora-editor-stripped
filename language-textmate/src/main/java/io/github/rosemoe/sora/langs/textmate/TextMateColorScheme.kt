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
package io.github.rosemoe.sora.langs.textmate

import android.graphics.Color
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.utils.ColorUtils
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.internal.theme.Theme
import org.eclipse.tm4e.core.internal.theme.raw.IRawTheme
import org.eclipse.tm4e.core.internal.theme.raw.RawTheme

class TextMateColorScheme(
    val themeRegistry: ThemeRegistry,
    themeModel: ThemeModel
) : EditorColorScheme(), ThemeRegistry.ThemeChangeListener {

    private var theme: Theme? = null
    private var rawTheme: IRawTheme? = null
    var currentTheme: ThemeModel = themeModel
        private set

    init {
        try {
            themeRegistry.loadTheme(currentTheme)
        } catch (_: Exception) {
        }
        setTheme(themeModel)
    }

    companion object {
        fun create(themeModel: ThemeModel): TextMateColorScheme =
            create(ThemeRegistry.getInstance(), themeModel)

        fun create(themeRegistry: ThemeRegistry = ThemeRegistry.getInstance()): TextMateColorScheme =
            create(themeRegistry, themeRegistry.currentThemeModel)

        fun create(themeRegistry: ThemeRegistry, themeModel: ThemeModel): TextMateColorScheme =
            TextMateColorScheme(themeRegistry, themeModel)
    }

    fun setTheme(themeModel: ThemeModel) {
        currentTheme = themeModel
        colors.clear()
        this.rawTheme = themeModel.rawTheme
        this.theme = themeModel.theme
        applyDefault()
    }

    override fun onChangeTheme(newTheme: ThemeModel) {
        setTheme(newTheme)
    }

    override fun applyDefault() {
        super.applyDefault()

        @Suppress("SENSELESS_COMPARISON")
        if (themeRegistry != null && !themeRegistry.hasListener(this)) {
            themeRegistry.addListener(this)
        }

        val raw = rawTheme ?: return
        val settings = raw.settings

        val rawSubTheme: RawTheme?
        if (settings == null) {
            rawSubTheme = (raw as? RawTheme)?.get("colors") as? RawTheme
            if (rawSubTheme != null) {
                applyVSCTheme(rawSubTheme)
            }
        } else {
            val firstSetting = (settings as? List<*>)?.firstOrNull() as? RawTheme
            rawSubTheme = firstSetting?.setting as? RawTheme
            if (rawSubTheme != null) {
                applyTMTheme(rawSubTheme)
            }
        }
    }

    private fun applyVSCTheme(rawTheme: RawTheme) {
        setColor(LINE_DIVIDER, Color.TRANSPARENT)

        (rawTheme.get("editorCursor.foreground") as? String)?.let {
            setColor(SELECTION_INSERT, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editor.selectionBackground") as? String)?.let {
            setColor(SELECTED_TEXT_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editorWhitespace.foreground") as? String)?.let {
            setColor(NON_PRINTABLE_CHAR, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editor.lineHighlightBackground") as? String)?.let {
            setColor(CURRENT_LINE, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editor.background") as? String)?.let {
            val parsed = ColorUtils.parseRGBAToARGB(it)
            setColor(WHOLE_BACKGROUND, parsed)
            setColor(LINE_NUMBER_BACKGROUND, parsed)
        }

        (rawTheme.get("editorLineNumber.foreground") as? String)?.let {
            setColor(LINE_NUMBER, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editorLineNumber.activeForeground") as? String)?.let {
            setColor(LINE_NUMBER_CURRENT, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editor.foreground") as? String)?.let {
            setColor(TEXT_NORMAL, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("highlightedDelimitersForeground") as? String)?.let {
            setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("tooltipBackground") as? String)?.let {
            setColor(DIAGNOSTIC_TOOLTIP_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("tooltipBriefMessageColor") as? String)?.let {
            setColor(DIAGNOSTIC_TOOLTIP_BRIEF_MSG, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("tooltipDetailedMessageColor") as? String)?.let {
            setColor(DIAGNOSTIC_TOOLTIP_DETAILED_MSG, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("tooltipActionColor") as? String)?.let {
            setColor(DIAGNOSTIC_TOOLTIP_ACTION, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editorSuggestWidget.highlightForeground") as? String)?.let {
            setColor(COMPLETION_WND_TEXT_MATCHED, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editorSuggestWidget.background") as? String)?.let {
            setColor(COMPLETION_WND_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editorSuggestWidget.foreground") as? String)?.let {
            setColor(COMPLETION_WND_TEXT_PRIMARY, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editorSuggestWidget.selectedBackground") as? String)?.let {
            setColor(COMPLETION_WND_ITEM_CURRENT, ColorUtils.parseRGBAToARGB(it))
        }

        val editorIndentGuideBackground = rawTheme.get("editorIndentGuide.background") as? String
        val blockLineColor = ((getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2) and 0x00FFFFFF or 0x88000000.toInt()
        val blockLineColorCur = blockLineColor or 0xFF000000.toInt()

        if (editorIndentGuideBackground != null) {
            setColor(BLOCK_LINE, ColorUtils.parseRGBAToARGB(editorIndentGuideBackground))
        } else {
            setColor(BLOCK_LINE, blockLineColor)
        }

        val editorIndentGuideActiveBackground = rawTheme.get("editorIndentGuide.activeBackground") as? String
        if (editorIndentGuideActiveBackground != null) {
            setColor(BLOCK_LINE_CURRENT, ColorUtils.parseRGBAToARGB(editorIndentGuideActiveBackground))
        } else {
            setColor(BLOCK_LINE_CURRENT, blockLineColorCur)
        }

        (rawTheme.get("editor.wordHighlightStrongBackground") as? String)?.let {
            setColor(TEXT_HIGHLIGHT_STRONG_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editor.wordHighlightBackground") as? String)?.let {
            setColor(TEXT_HIGHLIGHT_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("editor.findMatchBackground") as? String)?.let {
            setColor(MATCHED_TEXT_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }
    }

    override fun isDark(): Boolean {
        if (super.isDark()) {
            return true
        }
        @Suppress("SENSELESS_COMPARISON")
        if (currentTheme != null) {
            return currentTheme.isDark
        }
        return false
    }

    private fun applyTMTheme(rawTheme: RawTheme) {
        setColor(LINE_DIVIDER, Color.TRANSPARENT)

        (rawTheme.get("caret") as? String)?.let {
            setColor(SELECTION_INSERT, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("selection") as? String)?.let {
            setColor(SELECTED_TEXT_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("invisibles") as? String)?.let {
            setColor(NON_PRINTABLE_CHAR, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("lineHighlight") as? String)?.let {
            setColor(CURRENT_LINE, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("background") as? String)?.let {
            val parsed = ColorUtils.parseRGBAToARGB(it)
            setColor(WHOLE_BACKGROUND, parsed)
            setColor(LINE_NUMBER_BACKGROUND, parsed)
        }

        (rawTheme.get("foreground") as? String)?.let {
            setColor(TEXT_NORMAL, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("highlightedDelimitersForeground") as? String)?.let {
            setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("completionWindowBackground") as? String)?.let {
            setColor(COMPLETION_WND_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (rawTheme.get("completionWindowBackgroundCurrent") as? String)?.let {
            setColor(COMPLETION_WND_ITEM_CURRENT, ColorUtils.parseRGBAToARGB(it))
        }

        val blockLineColor = ((getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2) and 0x00FFFFFF or 0x88000000.toInt()
        setColor(BLOCK_LINE, blockLineColor)
        val blockLineColorCur = blockLineColor or 0xFF000000.toInt()
        setColor(BLOCK_LINE_CURRENT, blockLineColorCur)
    }

    override fun getColor(type: Int): Int {
        if (type >= 255) {
            val superColor = super.getColor(type)
            if (superColor == 0) {
                val activeTheme = theme ?: return super.getColor(TEXT_NORMAL)
                val color = try {
                    activeTheme.getColor(type - 255)
                } catch (_: IndexOutOfBoundsException) {
                    return super.getColor(TEXT_NORMAL)
                }
                val newColor = if (color != null && !color.equals("@default", ignoreCase = true)) {
                    ColorUtils.parseRGBAToARGB(color)
                } else {
                    super.getColor(TEXT_NORMAL)
                }
                colors.put(type, newColor)
                return newColor
            } else {
                return superColor
            }
        }
        return super.getColor(type)
    }

    override fun detachEditor(editor: CodeEditor) {
        super.detachEditor(editor)
        themeRegistry.removeListener(this)
    }

    override fun attachEditor(editor: CodeEditor) {
        super.attachEditor(editor)
        try {
            themeRegistry.loadTheme(currentTheme)
        } catch (_: Exception) {
        }
        setTheme(currentTheme)
        editor.rerunAnalysis()
    }
}
