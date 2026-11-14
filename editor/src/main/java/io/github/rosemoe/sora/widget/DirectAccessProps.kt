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
package io.github.rosemoe.sora.widget

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import io.github.rosemoe.sora.annotations.Experimental
import io.github.rosemoe.sora.annotations.InvalidateRequired
import io.github.rosemoe.sora.widget.minimap.MinimapConfig
import java.io.Serializable

/**
 * Direct-access properties.
 *
 * This object saves some feature settings of editor. These features are not accessed unless the user
 * does something that requires to check the state of the feature. So we save them here by public fields
 * so that you can modify them easily and do not have to call so many methods.
 */
open class DirectAccessProps : Serializable {

    companion object {
        /**
         * Rendering behavior for [cursorLineBgOverlapBehavior].
         *
         * If the cursor line has a custom background set, then draw the cursor line background on top
         * of the custom background. For backwards compatibility, this is the default behavior.
         */
        const val CURSOR_LINE_BG_OVERLAP_CUSTOM = 0

        /**
         * Rendering behavior for [cursorLineBgOverlapBehavior].
         *
         * If the cursor line has a custom background set, then don't draw the cursor line background.
         */
        const val CURSOR_LINE_BG_OVERLAP_CURSOR = 1

        /**
         * Rendering behavior for [cursorLineBgOverlapBehavior].
         *
         * If the cursor line has a custom background set, then draw the cursor line background on top
         * of the custom background, but make the cursor line background partly transparent so that both
         * background colors are visible.
         */
        const val CURSOR_LINE_BG_OVERLAP_MIXED = 2

        /**
         * The default rendering behavior for [cursorLineBgOverlapBehavior].
         */
        const val CURSOR_LINE_BG_OVERLAP_DEFAULT = CURSOR_LINE_BG_OVERLAP_CUSTOM

        /**
         * Do nothing
         */
        const val LN_ACTION_NOTHING = 0

        /**
         * Select the whole line
         */
        const val LN_ACTION_SELECT_LINE = 1

        /**
         * Set selection to line start
         */
        const val LN_ACTION_PLACE_SELECTION_HOME = 2

        /**
         * Enable mouse mode if a mouse is currently hovering in editor
         */
        const val MOUSE_MODE_AUTO = 0

        /**
         * Always use mouse mode
         */
        const val MOUSE_MODE_ALWAYS = 1

        /**
         * Do not use mouse mode
         */
        const val MOUSE_MODE_NEVER = 2

        /**
         * Show divider line
         */
        const val STICKY_LINE_INDICATOR_LINE = 1

        /**
         * Show shadow
         */
        const val STICKY_LINE_INDICATOR_SHADOW = 1
    }

    /**
     * Define symbol pairs for any language, override language settings.
     */
    val overrideSymbolPairs: SymbolPairMatch = SymbolPairMatch()

    /**
     * If set to be true, the editor will delete the whole line if the current line is empty (only tabs or spaces)
     * when the users press the DELETE key.
     *
     * Default value is `true`
     */
    var deleteEmptyLineFast: Boolean = true

    /**
     * Delete multiple spaces at a time when the user press the DELETE key.
     * This only takes effect when selection is in leading spaces.
     *
     * Default Value: `1`  -> The editor will always delete only 1 space.
     * Special Value: `-1` -> Follow tab size
     */
    var deleteMultiSpaces: Int = 1

    /**
     * Set to `false` if you don't want the editor to go fullscreen on devices with smaller screen size.
     * Otherwise, set to `true`
     *
     * Default value is `false`
     */
    var allowFullscreen: Boolean = false

    /**
     * Control whether auto-completes for symbol pairs.
     *
     * Such as automatically adding a ')' when '(' is entered
     */
    var symbolPairAutoCompletion: Boolean = true

    /**
     * Show auto-completion even when there is composing text set by the IME in editor.
     */
    var autoCompletionOnComposing: Boolean = true

    /**
     * Set whether auto indent should be executed when user enters a NEWLINE.
     */
    var autoIndent: Boolean = true

    /**
     * Disallow suggestions from keyboard forcibly by preventing setComposingText and setComposingRegion.
     */
    var disallowSuggestions: Boolean = false

    /**
     * Max text length that can be extracted by IPC.
     */
    @IntRange(from = 0)
    var maxIPCTextLength: Int = 32768

    /**
     * Max text length for accessibility node info.
     */
    @IntRange(from = 0)
    var maxAccessibilityTextLength: Int = 100000

    /**
     * Whether over scroll is permitted.
     */
    var overScrollEnabled: Boolean = false

    /**
     * Allow fling scroll
     */
    var scrollFling: Boolean = true

    /**
     * Duration in milliseconds for smooth scrolling animations triggered by the editor.
     */
    @IntRange(from = 0)
    var scrollAnimationDurationMs: Int = 250

    /**
     * If the two completion requests are sent within this time, the completion will not show.
     */
    var cancelCompletionNs: Long = 70L * 1000000L

    /**
     * Whether the editor should adjust its scroll position to make selection visible when its layout height decreases.
     */
    var adjustToSelectionOnResize: Boolean = true

    /**
     * Show scroll bars even when the scroll is caused by editor's adjustment but not user interaction
     */
    var awareScrollbarWhenAdjust: Boolean = false

    /**
     * Wave length of problem indicators. Unit DIP.
     */
    @InvalidateRequired
    @FloatRange(from = 0.0, fromInclusive = false)
    var indicatorWaveLength: Float = 18f

    /**
     * Wave width of problem indicators. Unit DIP.
     */
    @InvalidateRequired
    @FloatRange(from = 0.0, fromInclusive = false)
    var indicatorWaveWidth: Float = 0.9f

    /**
     * Wave amplitude of problem indicators. Unit DIP.
     */
    @InvalidateRequired
    @FloatRange(from = 0.0, fromInclusive = false)
    var indicatorWaveAmplitude: Float = 4f

    /**
     * Compare the text to commit with composing text.
     */
    var trackComposingTextOnCommit: Boolean = true

    /**
     * Try to simplify composing text update as a single insertion or deletion.
     */
    var minimizeComposingTextUpdate: Boolean = true

    /**
     * Draw side block line when in wordwrap mode
     */
    @InvalidateRequired
    var drawSideBlockLine: Boolean = true

    /**
     * Cache RenderNode of long text lines
     */
    var cacheRenderNodeForLongLines: Boolean = false

    /**
     * Use the ICU library to find range of words on double tap or long press.
     */
    var useICULibToSelectWords: Boolean = true

    /**
     * Highlight matching delimiters. This requires language support.
     */
    @InvalidateRequired
    var highlightMatchingDelimiters: Boolean = true

    /**
     * Make matching delimiters bold
     */
    @InvalidateRequired
    var boldMatchingDelimiters: Boolean = true

    /**
     * Whether the editor will use round rectangle for text background
     */
    @InvalidateRequired
    var enableRoundTextBackground: Boolean = true

    /**
     * The text background wraps the actual text, but not the whole line
     */
    @InvalidateRequired
    var textBackgroundWrapTextOnly: Boolean = false

    /**
     * The new cursor position when the user exits selecting mode.
     */
    var positionOfCursorWhenExitSelecting: Boolean = true

    /**
     * Draw custom line background color on current line
     */
    @InvalidateRequired
    var drawCustomLineBgOnCurrentLine: Boolean = false

    /**
     * The factor of round rectangle, affecting the corner radius of the resulting display
     */
    @InvalidateRequired
    var roundTextBackgroundFactor: Float = 0.13f

    /**
     * Specify the icon size factor. result size = row height * sideIconSizeFactor
     */
    @InvalidateRequired
    @FloatRange(from = 0.0, to = 1.0)
    var sideIconSizeFactor: Float = 0.7f

    /**
     * Specify the marker text size factor, such as line-break markers.
     */
    @InvalidateRequired
    @FloatRange(from = 0.0, to = 1.0)
    val miniMarkerSizeFactor: Float = 0.5f

    /**
     * Specify the text size factor for function characters
     */
    @InvalidateRequired
    @FloatRange(from = 0.0, to = 1.0)
    val functionCharacterSizeFactor: Float = 0.85f

    /**
     * Specify editor behavior when line number is clicked.
     */
    var actionWhenLineNumberClicked: Int = LN_ACTION_PLACE_SELECTION_HOME

    /**
     * Format pasted text
     */
    var formatPastedText: Boolean = false

    /**
     * Use enhanced function of home and end.
     */
    var enhancedHomeAndEnd: Boolean = true

    /**
     * Show hard wrap marker near the column.
     */
    @InvalidateRequired
    var hardwrapColumn: Int = 0

    /**
     * Select words even if some texts are already selected when the editor is long-pressed.
     */
    var reselectOnLongPress: Boolean = true

    /**
     * Enable drag-select after a long-press.
     */
    var dragSelectAfterLongPress: Boolean = true

    /**
     * Show selection above selection handle when text is selected
     */
    @InvalidateRequired
    var showSelectionWhenSelected: Boolean = false

    /**
     * Limit length for copying text to clipboard.
     */
    var clipboardTextLengthLimit: Int = 512 * 1024

    /**
     * Scrolling speed multiplier when ALT key is pressed.
     */
    @FloatRange(from = 1.0)
    var fastScrollSensitivity: Float = 5f

    /**
     * Enable/disable sticky scroll mode
     */
    @InvalidateRequired
    var stickyScroll: Boolean = false

    /**
     * Control the count of lines that can be stuck to the top of the editor
     */
    @IntRange(from = 1)
    @InvalidateRequired
    var stickyScrollMaxLines: Int = 3

    /**
     * Prefer inner scopes if true.
     */
    @InvalidateRequired
    var stickyScrollPreferInnerScope: Boolean = false

    /**
     * Hide partially or all of the stuck lines when text is selected
     */
    @InvalidateRequired
    var stickyScrollAutoCollapse: Boolean = true

    /**
     * Fling scroll in single direction
     */
    var singleDirectionFling: Boolean = true

    /**
     * Dragging scroll in single direction
     */
    var singleDirectionDragging: Boolean = true

    /**
     * Report cursor anchor info to system.
     */
    var reportCursorAnchor: Boolean = true

    /**
     * Place selection on previous line after cutting line
     */
    var placeSelOnPreviousLineAfterCut: Boolean = false

    /**
     * When to enable mouse mode.
     */
    var mouseMode: Int = MOUSE_MODE_AUTO

    /**
     * Try to show context menu for mouse
     */
    var mouseContextMenu: Boolean = true

    /**
     * Always show scrollbars when the editor is in mouse mode
     */
    var mouseModeAlwaysShowScrollbars: Boolean = true

    /**
     * Adjust scrolling speed in mouse wheel scrolling
     */
    var mouseWheelScrollFactor: Float = 1.2f

    /**
     * Disable getExtractedText for IME
     */
    var disableTextExtracting: Boolean = false

    /**
     * Specifies the cursor line background rendering behavior.
     */
    @InvalidateRequired
    var cursorLineBgOverlapBehavior: Int = CURSOR_LINE_BG_OVERLAP_DEFAULT

    /**
     * If true, Home and End shortcuts will be based on visual lines.
     */
    var rowBasedHomeEnd: Boolean = true

    /**
     * Check thread when the text in editor is changed.
     */
    @Experimental
    var checkModificationThread: Boolean = false

    /**
     * Show direction indicator on selection for bidirectional text.
     */
    @InvalidateRequired
    var showBidiDirectionIndicator: Boolean = true

    /**
     * How to show the sticky line divider
     */
    @InvalidateRequired
    var stickyLineIndicator: Int = STICKY_LINE_INDICATOR_LINE or STICKY_LINE_INDICATOR_SHADOW

    /**
     * Move selection to first item if physical keyboard is connected.
     */
    var moveSelectionToFirstForKeyboard: Boolean = true

    /**
     * Select the first completion item on enter for software keyboard
     */
    var selectCompletionItemOnEnterForSoftKbd: Boolean = true

    /**
     * Show minimap for content
     */
    @Experimental
    @InvalidateRequired
    var showMinimap: Boolean = false

    /**
     * Minimap config
     */
    @Experimental
    @InvalidateRequired
    var minimapConfig: MinimapConfig = MinimapConfig.DefaultConfig
}
