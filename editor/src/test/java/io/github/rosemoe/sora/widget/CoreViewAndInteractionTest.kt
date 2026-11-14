/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
package io.github.rosemoe.sora.widget

import io.github.rosemoe.sora.I18nConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CoreViewAndInteractionTest {

    @Test
    fun testSymbolPairMatchAndDestructuring() {
        val pairs = SymbolPairMatch()
        val p1 = SymbolPairMatch.SymbolPair("(", ")")
        val (open, close) = p1
        assertEquals("(", open)
        assertEquals(")", close)

        pairs.putPair('(', p1)
        pairs.putPair('{', SymbolPairMatch.SymbolPair("{", "}"))

        val matchedParen = pairs['(']
        assertNotNull(matchedParen)
        assertEquals(")", matchedParen!!.close)

        val matchedBrace = pairs.matchBestPairBySingleChar('{')
        assertNotNull(matchedBrace)
        assertEquals("}", matchedBrace!!.close)

        assertNull(pairs['x'])

        pairs.putPair("/*", SymbolPairMatch.SymbolPair("/*", "*/"))
        val multList = pairs.matchBestPairList('*')
        assertTrue(multList.isNotEmpty())
        val (mOpen, mClose) = multList[0]
        assertEquals("/*", mOpen)
        assertEquals("*/", mClose)
    }

    @Test
    fun testEditorSearchOptionsDestructuring() {
        val searchOpts = EditorSearcher.SearchOptions(
            type = EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION,
            caseInsensitive = true,
            regexBackrefGrammar = null
        )
        val (sType, sCaseInsensitive, sRegexGrammar) = searchOpts
        assertEquals(EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION, sType)
        assertTrue(sCaseInsensitive)
        assertNull(sRegexGrammar)

        val replaceOpts = EditorSearcher.ReplaceOptions(preserveCase = true)
        val (preserveCase) = replaceOpts
        assertTrue(preserveCase)
    }

    @Test
    fun testSymbolInputViewSecondaryConstructors() {
        val context = RuntimeEnvironment.getApplication()
        val view = SymbolInputView(context)
        assertNotNull(view)
        view.addSymbols(arrayOf("{", "}", "(", ")"), arrayOf("{", "}", "(", ")"))
        assertEquals(4, view.childCount)
    }

    @Test
    fun testI18nConfigPureKotlin() {
        I18nConfig.mapTo(9999, 8888)
        assertEquals(8888, I18nConfig.getResourceId(9999))
        assertEquals(7777, I18nConfig.getResourceId(7777))
    }
}
