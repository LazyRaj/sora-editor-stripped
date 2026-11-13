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

package io.github.rosemoe.sora.text

import android.view.KeyEvent
import io.github.rosemoe.sora.text.bidi.ContentBidi
import io.github.rosemoe.sora.text.bidi.Directions
import io.github.rosemoe.sora.text.bidi.TextBidi
import io.github.rosemoe.sora.text.bidi.VisualDirections
import io.github.rosemoe.sora.text.breaker.WordBreaker
import io.github.rosemoe.sora.text.breaker.WordBreakerEmpty
import io.github.rosemoe.sora.text.breaker.WordBreakerProgram
import io.github.rosemoe.sora.text.emoji.BuiltinEmojiPropertyProvider
import io.github.rosemoe.sora.text.emoji.EmojiPropertyProvider
import io.github.rosemoe.sora.text.emoji.PlatformEmojiPropertyProvider
import io.github.rosemoe.sora.text.emoji.UnicodeEmojiData
import io.github.rosemoe.sora.text.method.KeyMetaStates
import io.github.rosemoe.sora.text.string.StringLatin1
import io.github.rosemoe.sora.text.string.StringUTF16
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.CodeEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TextSubpackagesTest {

    @Test
    fun testStringLatin1EncodingAndOperations() {
        assertTrue(StringLatin1.canEncode('a'))
        assertTrue(StringLatin1.canEncode('\u00FF'))
        assertFalse(StringLatin1.canEncode('\u0100'))
        assertFalse(StringLatin1.canEncode('中'))

        val bytes = ByteArray(10)
        StringLatin1.putChar(bytes, 0, 'H')
        StringLatin1.putChar(bytes, 1, 'e')
        StringLatin1.putChar(bytes, 2, 'l')
        StringLatin1.putChar(bytes, 3, 'l')
        StringLatin1.putChar(bytes, 4, 'o')

        assertEquals('H', StringLatin1.getChar(bytes, 0))
        assertEquals('e', StringLatin1.getChar(bytes, 1))

        val dstChars = CharArray(5)
        StringLatin1.getChars(bytes, 0, 5, dstChars, 0)
        assertEquals("Hello", String(dstChars))

        val sb = StringBuilder()
        StringLatin1.appendTo(bytes, 5, sb)
        assertEquals("Hello", sb.toString())
        assertEquals("Hello", StringLatin1.newString(bytes, 5))

        val copyDst = ByteArray(10)
        StringLatin1.copyChars(bytes, 0, copyDst, 0, 5)
        assertEquals("Hello", StringLatin1.newString(copyDst, 5))

        val inflated = StringLatin1.inflateToUTF16(bytes, 5, 8)
        assertEquals('H', StringUTF16.getChar(inflated, 0))
        assertEquals('o', StringUTF16.getChar(inflated, 4))
        assertEquals("Hello", StringUTF16.newString(inflated, 5))
    }

    @Test
    fun testStringUTF16Operations() {
        assertEquals(20, StringUTF16.bytesForChars(10))

        val bytes = ByteArray(StringUTF16.bytesForChars(5))
        StringUTF16.putChar(bytes, 0, '中')
        StringUTF16.putChar(bytes, 1, '文')
        StringUTF16.putChar(bytes, 2, '字')
        StringUTF16.putChar(bytes, 3, '符')
        StringUTF16.putChar(bytes, 4, '!')

        assertEquals('中', StringUTF16.getChar(bytes, 0))
        assertEquals('文', StringUTF16.getChar(bytes, 1))
        assertEquals('!', StringUTF16.getChar(bytes, 4))

        val dstChars = CharArray(5)
        StringUTF16.getChars(bytes, 0, 5, dstChars, 0)
        assertEquals("中文字符!", String(dstChars))

        val sb = StringBuilder()
        StringUTF16.appendTo(bytes, 5, sb)
        assertEquals("中文字符!", sb.toString())
        assertEquals("中文字符!", StringUTF16.newString(bytes, 5))

        val copyDst = ByteArray(StringUTF16.bytesForChars(5))
        StringUTF16.copyChars(bytes, 0, copyDst, 0, 5)
        assertEquals("中文字符!", StringUTF16.newString(copyDst, 5))
    }

    @Test
    fun testWordBreakers() {
        assertEquals(10, WordBreakerEmpty.INSTANCE.getOptimizedBreakPoint(0, 10))

        val line = ContentLine("org.example.app.test")
        val breaker = WordBreaker.Factory.newInstance(line)
        assertTrue(breaker is WordBreakerProgram)

        // Whole line fits
        assertEquals(line.length, breaker.getOptimizedBreakPoint(0, line.length))

        // Break optimization at dot boundary
        val optimized = breaker.getOptimizedBreakPoint(0, 8)
        assertTrue("Optimized break point should be > 0 and <= 8", optimized in 1..8)
    }

    @Test
    fun testBidiAndDirections() {
        assertFalse(TextBidi.couldAffectRtl('a'))
        assertFalse(TextBidi.couldAffectRtl('9'))
        assertTrue(TextBidi.couldAffectRtl('\u05D0')) // Hebrew Alef
        assertTrue(TextBidi.couldAffectRtl('\u0627')) // Arabic Alef

        assertTrue(TextBidi.doesNotNeedBidi("Simple LTR ascii string"))
        assertFalse(TextBidi.doesNotNeedBidi("Hello \u05D0\u05D1\u05D2 World"))

        val ltrDirs = TextBidi.getDirections("Hello World")
        assertEquals(1, ltrDirs.runCount)
        assertEquals(0, ltrDirs.getRunStart(0))
        assertEquals(11, ltrDirs.getRunEnd(0))
        assertEquals(0, ltrDirs.getRunLevel(0))
        assertFalse(ltrDirs.isRunRtl(0))

        val customRuns = longArrayOf(
            IntPair.pack(0, 0),
            IntPair.pack(5, 1)
        )
        val dirs = Directions(customRuns, 10)
        assertEquals(2, dirs.runCount)
        assertEquals(0, dirs.getRunStart(0))
        assertEquals(5, dirs.getRunEnd(0))
        assertFalse(dirs.isRunRtl(0))
        assertEquals(5, dirs.getRunStart(1))
        assertEquals(10, dirs.getRunEnd(1))
        assertTrue(dirs.isRunRtl(1))

        dirs.length = 12
        assertEquals(12, dirs.length)
        assertEquals(12, dirs.getRunEnd(1))

        val visual = VisualDirections(dirs)
        assertEquals(2, visual.runCount)
    }

    @Test
    fun testContentBidiCachingAndLifecycle() {
        val content = Content("First line\nSecond line RTL: \u05D0\u05D1\u05D2\nThird line")
        val contentBidi = ContentBidi(content)

        assertFalse(contentBidi.isEnabled)
        val defaultDirs = contentBidi.getLineDirections(content.getLine(0), 0)
        assertEquals(1, defaultDirs.runCount)

        contentBidi.isEnabled = true
        assertTrue(contentBidi.isEnabled)

        val dirLine0 = contentBidi.getLineDirections(content.getLine(0), 0)
        val dirLine1 = contentBidi.getLineDirections(content.getLine(1), 1)
        assertNotNull(dirLine0)
        assertNotNull(dirLine1)

        // Cache hit
        val cachedDir0 = contentBidi.getLineDirections(content.getLine(0), 0)
        assertEquals(dirLine0, cachedDir0)

        // Test insertion event handling
        content.insert(0, 0, "Prefix ")
        // Test deletion event handling
        content.delete(0, 0, 0, 7)

        contentBidi.destroy()
    }

    @Test
    fun testKeyMetaStates() {
        val editor = CodeEditor(RuntimeEnvironment.getApplication())
        val keyMetaStates = KeyMetaStates(editor)

        assertFalse(keyMetaStates.isCtrlPressed)
        assertFalse(keyMetaStates.isShiftPressed)
        assertFalse(keyMetaStates.isAltPressed)
        assertFalse(keyMetaStates.isSelecting)

        val shiftDown = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0)
        keyMetaStates.onKeyDown(shiftDown)
        keyMetaStates.adjustAfterKeyPress()
        keyMetaStates.clearMetaStates(KeyEvent.META_SHIFT_ON)

        val ctrlDown = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT, 0, KeyEvent.META_CTRL_ON)
        keyMetaStates.onKeyDown(ctrlDown)
        assertTrue(keyMetaStates.isCtrlPressed)

        val ctrlUp = KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT, 0, 0)
        keyMetaStates.onKeyUp(ctrlUp)
        assertFalse(keyMetaStates.isCtrlPressed)
    }

    @Test
    fun testEmojiProvidersAndAndroidEmoji() {
        val provider = EmojiPropertyProvider.get()
        assertNotNull(provider)
        assertTrue(provider is PlatformEmojiPropertyProvider)

        val fireEmoji = 0x1F525
        assertTrue(provider.isEmoji(fireEmoji))
        assertTrue(AndroidEmoji.isEmoji(fireEmoji))

        val regInd = 0x1F1E6
        assertTrue(AndroidEmoji.isRegionalIndicatorSymbol(regInd))
        assertFalse(AndroidEmoji.isRegionalIndicatorSymbol('A'.code))

        // Keycap base
        assertTrue(AndroidEmoji.isKeycapBase('5'.code))
        assertTrue(AndroidEmoji.isKeycapBase('#'.code))
        assertTrue(AndroidEmoji.isKeycapBase('*'.code))
        assertFalse(AndroidEmoji.isKeycapBase('A'.code))

        // Tag spec
        assertTrue(AndroidEmoji.isTagSpecChar(0xE0021))
        assertFalse(AndroidEmoji.isTagSpecChar(AndroidEmoji.CANCEL_TAG))

        // Emoji modifier base exceptions
        assertTrue(AndroidEmoji.isEmojiModifierBase(0x1F91D))
        assertTrue(AndroidEmoji.isEmojiModifierBase(0x1F93C))

        // Builtin and UnicodeEmojiData fallback validation
        val builtin = BuiltinEmojiPropertyProvider.INSTANCE
        assertTrue(builtin.isEmoji(fireEmoji))
        assertTrue(builtin.isVariationSelector(0xFE0F))
        assertTrue(UnicodeEmojiData.isEmoji(fireEmoji))
        assertTrue(UnicodeEmojiData.isVariationSelector(0xFE0F))
        assertTrue(UnicodeEmojiData.isEmojiModifier(0x1F3FB))
        assertTrue(UnicodeEmojiData.isEmojiModifierBase(0x1F442))
    }

    @Test
    fun testFunctionCharacters() {
        assertTrue(FunctionCharacters.isFunctionCharacter('\u0000'))
        assertTrue(FunctionCharacters.isFunctionCharacter('\n'))
        assertTrue(FunctionCharacters.isFunctionCharacter('\r'))
        assertTrue(FunctionCharacters.isFunctionCharacter('\t'))
        assertTrue(FunctionCharacters.isFunctionCharacter(127.toChar()))
        assertFalse(FunctionCharacters.isFunctionCharacter('A'))

        // isEditorFunctionChar excludes '\t'
        assertFalse(FunctionCharacters.isEditorFunctionChar('\t'))
        assertTrue(FunctionCharacters.isEditorFunctionChar('\n'))
        assertTrue(FunctionCharacters.isEditorFunctionChar('\u0000'))

        assertEquals("NUL", FunctionCharacters.getNameForFunctionCharacter('\u0000'))
        assertEquals("LF", FunctionCharacters.getNameForFunctionCharacter('\n'))
        assertEquals("CR", FunctionCharacters.getNameForFunctionCharacter('\r'))
        assertEquals("DEL", FunctionCharacters.getNameForFunctionCharacter(127.toChar()))
        assertEquals("UNK", FunctionCharacters.getNameForFunctionCharacter('Z'))
    }

    @Test
    fun testPreserveCaseReplace() {
        assertEquals("World", PreserveCaseReplace.getReplacementSimple("", "World"))
        assertEquals("", PreserveCaseReplace.getReplacementSimple("Hello", ""))
        assertEquals("123", PreserveCaseReplace.getReplacementSimple("456", "123"))

        // Uppercase
        assertEquals("WORLD", PreserveCaseReplace.getReplacementSimple("HELLO", "world"))

        // Lowercase
        assertEquals("world", PreserveCaseReplace.getReplacementSimple("hello", "WORLD"))

        // Capitalized
        assertEquals("World", PreserveCaseReplace.getReplacementSimple("Hello", "world"))
        assertEquals("World", PreserveCaseReplace.getReplacementSimple("Hello", "WORLD"))

        // Mixed case matching
        assertEquals("wOrLd", PreserveCaseReplace.getReplacementSimple("hElLo", "world"))
    }

    @Test
    fun testCharArrayWrapper() {
        val chars = "Hello World".toCharArray()
        val wrapper = CharArrayWrapper(chars, 11)

        assertEquals(11, wrapper.length)
        assertEquals('H', wrapper[0])
        assertEquals('o', wrapper[4])
        assertEquals("Hello", wrapper.subSequence(0, 5).toString())

        val dest = CharArray(5)
        android.text.TextUtils.getChars(wrapper, 6, 11, dest, 0)
        assertEquals("World", String(dest))

        wrapper.setDataCount(5)
        assertEquals(5, wrapper.length)

        assertThrows(StringIndexOutOfBoundsException::class.java) {
            android.text.TextUtils.getChars(wrapper, 0, 6, dest, 0)
        }
    }

    @Test
    fun testContentListener() {
        var beforeReplaceCalled = false
        var insertCalled = false
        var deleteCalled = false

        val listener = object : ContentListener {
            override fun beforeReplace(content: Content) {
                beforeReplaceCalled = true
            }

            override fun afterInsert(
                content: Content,
                startLine: Int,
                startColumn: Int,
                endLine: Int,
                endColumn: Int,
                insertedContent: CharSequence
            ) {
                insertCalled = true
            }

            override fun afterDelete(
                content: Content,
                startLine: Int,
                startColumn: Int,
                endLine: Int,
                endColumn: Int,
                deletedContent: CharSequence
            ) {
                deleteCalled = true
            }
        }

        val content = Content()
        content.addContentListener(listener)

        content.insert(0, 0, "Hello")
        assertTrue(insertCalled)

        content.delete(0, 0, 0, 2)
        assertTrue(deleteCalled)

        content.removeContentListener(listener)
    }
}
