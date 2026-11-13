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

    @Test
    fun testICUUtils() {
        val text = "val myVariable = 123"
        // Word at "myVariable" (offset 6)
        val range = ICUUtils.getWordRange(text, 6, true)
        val start = IntPair.getFirst(range)
        val end = IntPair.getSecond(range)
        assertEquals("myVariable", text.substring(start, end))

        // Fallback implementation
        val fallbackRange = ICUUtils.getWordRangeFallback(text, 6)
        assertEquals(range, fallbackRange)
    }

    @Test
    fun testTextRange() {
        val pos1 = CharPosition().apply { index = 5; line = 0; column = 5 }
        val pos2 = CharPosition().apply { index = 15; line = 1; column = 5 }
        val range = TextRange(pos1, pos2)

        assertEquals(5, range.startIndex)
        assertEquals(15, range.endIndex)

        val insidePos = CharPosition().apply { index = 10 }
        val outsidePos = CharPosition().apply { index = 20 }
        assertTrue(range.isPositionInside(insidePos))
        assertFalse(range.isPositionInside(outsidePos))
        assertTrue(range.toString().contains("TextRange"))
    }

    @Test
    fun testSpanRecycler() {
        val recycler = SpanRecycler.getInstance()
        assertNotNull(recycler)
        // Should handle null and empty lists gracefully
        recycler.recycle(null)
        recycler.recycle(mutableListOf())
    }

    @Test
    fun testUnicodeIterator() {
        val text = "A\uD83D\uDE00B" // A + 😀 (surrogate pair) + B
        val iterator = UnicodeIterator.obtain(text, 0, text.length)
        assertTrue(iterator.hasNext())

        assertEquals('A'.code, iterator.nextCodePoint())
        assertEquals(0, iterator.startIndex)
        assertEquals(1, iterator.endIndex)

        assertTrue(iterator.hasNext())
        assertEquals(0x1F600, iterator.nextCodePoint()) // 😀
        assertEquals(1, iterator.startIndex)
        assertEquals(3, iterator.endIndex)

        assertTrue(iterator.hasNext())
        assertEquals('B'.code, iterator.nextCodePoint())
        assertEquals(3, iterator.startIndex)
        assertEquals(4, iterator.endIndex)

        assertFalse(iterator.hasNext())
        assertEquals(0, iterator.nextCodePoint())
        iterator.recycle()
    }

    @Test
    fun testComposingText() {
        val comp = ComposingText()
        assertFalse(comp.isComposing)

        comp.set(2, 5)
        assertEquals(2, comp.startIndex)
        assertEquals(5, comp.endIndex)
        assertTrue(comp.isComposing)

        comp.adjustLength(5)
        assertEquals(7, comp.endIndex)

        comp.shiftOnInsert(0, 2)
        assertEquals(4, comp.startIndex)
        assertEquals(9, comp.endIndex)

        comp.shiftOnDelete(0, 2)
        assertEquals(2, comp.startIndex)
        assertEquals(7, comp.endIndex)

        comp.reset()
        assertFalse(comp.isComposing)
        assertEquals(-1, comp.startIndex)
    }

    @Test
    fun testCharSequenceIterator() {
        val text = "Hello"
        val itr = CharSequenceIterator(text)

        assertEquals(0, itr.beginIndex)
        assertEquals(5, itr.endIndex)

        assertEquals('H', itr.first())
        assertEquals(0, itr.index)

        assertEquals('e', itr.next())
        assertEquals('l', itr.next())
        assertEquals('l', itr.next())
        assertEquals('o', itr.next())
        assertEquals(java.text.CharacterIterator.DONE, itr.next())

        assertEquals('o', itr.last())
        assertEquals('l', itr.previous())

        assertEquals('l', itr.setIndex(2))
        assertEquals(2, itr.index)

        val clone = itr.clone() as CharSequenceIterator
        assertEquals(itr.index, clone.index)
        assertEquals(itr.current(), clone.current())

        assertThrows(IllegalArgumentException::class.java) {
            itr.setIndex(10)
        }
    }

    @Test
    fun testInsertTextHelper() {
        val helper = InsertTextHelper.forInsertion("Line1\nLine2\r\nLine3")

        assertEquals(InsertTextHelper.TYPE_LINE_CONTENT, helper.forward())
        assertEquals("Line1", "Line1\nLine2\r\nLine3".substring(helper.index, helper.indexNext))

        assertEquals(InsertTextHelper.TYPE_NEWLINE, helper.forward())
        assertEquals("\n", "Line1\nLine2\r\nLine3".substring(helper.index, helper.indexNext))

        assertEquals(InsertTextHelper.TYPE_LINE_CONTENT, helper.forward())
        assertEquals("Line2", "Line1\nLine2\r\nLine3".substring(helper.index, helper.indexNext))

        assertEquals(InsertTextHelper.TYPE_NEWLINE, helper.forward())
        assertEquals("\r\n", "Line1\nLine2\r\nLine3".substring(helper.index, helper.indexNext))

        assertEquals(InsertTextHelper.TYPE_LINE_CONTENT, helper.forward())
        assertEquals("Line3", "Line1\nLine2\r\nLine3".substring(helper.index, helper.indexNext))

        assertEquals(InsertTextHelper.TYPE_EOF, helper.forward())
        helper.recycle()
    }

    @Test
    fun testTextLayoutHelper() {
        val helper = TextLayoutHelper.get()
        assertNotNull(helper)

        val text = "Line of text for layout helper"
        val left = helper.getCurPosLeft(5, text)
        val right = helper.getCurPosRight(5, text)
        assertTrue(left >= 0)
        assertTrue(right >= 0)
    }

    @Test
    fun testLineSeparator() {
        assertEquals("\n", LineSeparator.LF.content)
        assertEquals(1, LineSeparator.LF.length)
        assertEquals("\r\n", LineSeparator.CRLF.content)
        assertEquals(2, LineSeparator.CRLF.length)
        assertEquals("\r", LineSeparator.CR.content)
        assertEquals("", LineSeparator.NONE.content)

        assertEquals(LineSeparator.LF, LineSeparator.fromSeparatorString("\n"))
        assertEquals(LineSeparator.CRLF, LineSeparator.fromSeparatorString("\r\n"))
        assertEquals(LineSeparator.CR, LineSeparator.fromSeparatorString("\r"))
        assertEquals(LineSeparator.NONE, LineSeparator.fromSeparatorString(""))

        val text = "A\nB\r\nC\rD"
        assertEquals(LineSeparator.LF, LineSeparator.fromSeparatorString(text, 1, 2))
        assertEquals(LineSeparator.CRLF, LineSeparator.fromSeparatorString(text, 3, 5))
        assertEquals(LineSeparator.CR, LineSeparator.fromSeparatorString(text, 6, 7))
        assertEquals(LineSeparator.NONE, LineSeparator.fromSeparatorString(text, 0, 0))

        assertThrows(IllegalArgumentException::class.java) {
            LineSeparator.fromSeparatorString("invalid")
        }
    }

    @Test
    fun testTextReference() {
        val original = "0123456789"
        val ref = TextReference(original, 2, 7) // "23456"

        assertEquals(5, ref.length)
        assertEquals('2', ref[0])
        assertEquals('6', ref[4])
        assertEquals("23456", ref.toString())
        assertEquals("345", ref.subSequence(1, 4).toString())

        var validated = false
        ref.setValidator { validated = true }
        assertEquals(5, ref.length)
        assertTrue(validated)

        ref.setValidator { throw TextReference.ValidateFailedException("Rejected") }
        assertThrows(TextReference.ValidateFailedException::class.java) {
            ref.length
        }
    }

    @Test
    fun testCharPositionLifecycle() {
        val pos = CharPosition()
        assertEquals(0, pos.line)
        assertEquals(0, pos.column)
        assertEquals(0, pos.index)

        val pos2 = CharPosition(3, 7)
        assertEquals(3, pos2.getLine())
        assertEquals(7, pos2.getColumn())
        assertEquals(-1, pos2.getIndex())

        val pos3 = CharPosition(2, 4, 15)
        assertEquals(2, pos3.line)
        assertEquals(4, pos3.column)
        assertEquals(15, pos3.index)

        val packed = pos3.toIntPair()
        assertEquals(2, IntPair.getFirst(packed))
        assertEquals(4, IntPair.getSecond(packed))

        val copy = pos3.fromThis()
        assertEquals(pos3, copy)
        assertEquals(pos3.hashCode(), copy.hashCode())
        assertTrue(copy.toString().contains("line = 2"))

        copy.toBOF()
        assertEquals(0, copy.line)
        assertEquals(0, copy.column)
        assertEquals(0, copy.index)

        copy.set(pos2)
        assertEquals(3, copy.line)
        assertEquals(7, copy.column)
        assertEquals(-1, copy.index)
    }

    @Test
    fun testCursorOperations() {
        val content = Content("Hello\nWorld\nKotlin 2.0")
        val cursor = Cursor(content)

        assertEquals(0, cursor.leftLine)
        assertEquals(0, cursor.leftColumn)
        assertEquals(0, cursor.rightLine)
        assertEquals(0, cursor.rightColumn)
        assertEquals(0, cursor.left)
        assertEquals(0, cursor.right)
        assertFalse(cursor.isSelected)

        cursor.set(1, 2)
        assertEquals(1, cursor.leftLine)
        assertEquals(2, cursor.leftColumn)
        assertEquals(1, cursor.rightLine)
        assertEquals(2, cursor.rightColumn)

        cursor.setLeft(0, 3)
        cursor.setRight(1, 4)
        assertTrue(cursor.isSelected)
        assertTrue(cursor.isInSelectedRegion(0, 3))
        assertTrue(cursor.isInSelectedRegion(0, 4))
        assertTrue(cursor.isInSelectedRegion(1, 2))
        assertFalse(cursor.isInSelectedRegion(0, 2))
        assertFalse(cursor.isInSelectedRegion(1, 4))

        val range = cursor.range
        assertEquals(cursor.left, range.startIndex)
        assertEquals(cursor.right, range.endIndex)

        cursor.selectionDirection = Cursor.DIRECTION_LTR
        assertEquals(Cursor.DIRECTION_LTR, cursor.selectionDirection)

        // In Robolectric environment without native font metrics, cursor positions
        // from TextLayoutHelper are bound to line bounds. We verify boundary navigation:
        val origin = IntPair.pack(0, 0)
        assertEquals(0L, cursor.getLeftOf(origin))

        // Navigation across line breaks
        val lineStart = IntPair.pack(1, 0)
        val beforeLineStart = cursor.getLeftOf(lineStart)
        assertEquals(0, IntPair.getFirst(beforeLineStart))
        assertEquals(5, IntPair.getSecond(beforeLineStart))

        val lineEnd = IntPair.pack(0, 5)
        val afterLineEnd = cursor.getRightOf(lineEnd)
        assertEquals(1, IntPair.getFirst(afterLineEnd))
        assertEquals(0, IntPair.getSecond(afterLineEnd))
    }

    @Test
    fun testCachedIndexerLookups() {
        val content = Content("Line 1\r\nLine 2\nLine 3")
        val indexer = CachedIndexer(content)
        indexer.thresholdIndex = 10

        val pos0 = indexer.getCharPosition(0)
        assertEquals(0, pos0.line)
        assertEquals(0, pos0.column)

        // "Line 1\r\n" -> length is 8 (6 + 2)
        val posLine2 = indexer.getCharPosition(8)
        assertEquals(1, posLine2.line)
        assertEquals(0, posLine2.column)

        assertEquals(8, indexer.getCharIndex(1, 0))
        assertEquals(1, indexer.getCharLine(8))
        assertEquals(0, indexer.getCharColumn(8))

        // Bi-directional lookups
        val dest = CharPosition()
        indexer.findIndexForward(pos0, 10, dest)
        assertEquals(10, dest.index)
        assertEquals(1, dest.line)
        assertEquals(2, dest.column)

        indexer.findIndexBackward(dest, 3, dest)
        assertEquals(3, dest.index)
        assertEquals(0, dest.line)
        assertEquals(3, dest.column)

        indexer.findLiCoForward(pos0, 2, 3, dest)
        assertEquals(2, dest.line)
        assertEquals(3, dest.column)

        indexer.findLiCoBackward(dest, 0, 4, dest)
        assertEquals(0, dest.line)
        assertEquals(4, dest.column)
    }

    @Test
    fun testContentIOStreams() {
        val originalText = "Line A\r\nLine B\nLine C\rLine D"
        val inStream = java.io.ByteArrayInputStream(originalText.toByteArray(Charsets.UTF_8))
        val content = ContentIO.createFrom(inStream, Charsets.UTF_8)

        val outStream = java.io.ByteArrayOutputStream()
        ContentIO.writeTo(content, outStream, Charsets.UTF_8, true)
        val writtenText = outStream.toString(Charsets.UTF_8.name())
        assertEquals(originalText, writtenText)

        val reader = java.io.StringReader("First\r\nSecond\rThird")
        val contentFromReader = ContentIO.createFrom(reader)
        assertEquals("First\r\nSecond\rThird", contentFromReader.toString())
    }

    @Test
    fun testContentLineLifecycleAndOperations() {
        val line = ContentLine("Initial Text")
        assertEquals(12, line.length)
        assertEquals('I', line[0])
        assertEquals("Initial Text", line.toString())

        // Insert Latin1
        line.insert(8, "Modified ")
        assertEquals("Initial Modified Text", line.toString())

        // Insert UTF-16 character (triggers coder inflation)
        line.insert(line.length, '中')
        assertTrue(line.toString().endsWith("中"))

        // Append
        line.append(" End")
        assertTrue(line.toString().endsWith("中 End"))

        // Line separator handling
        line.lineSeparator = LineSeparator.CRLF
        assertEquals(LineSeparator.CRLF, line.lineSeparator)

        // SubSequence
        val sub = line.subSequence(0, 7)
        assertEquals("Initial", sub.toString())

        // Copy and mutation
        val cloned = line.copy()
        assertEquals(line.toString(), cloned.toString())
        cloned.delete(0, 8)
        assertTrue(line.toString() != cloned.toString())

        // ShareableData ref counting
        assertTrue(line.isMutable())
        line.retain()
        assertFalse(line.isMutable())
        val mut = line.toMutable()
        assertNotNull(mut)
        line.release()
        assertTrue(line.isMutable())
    }

    @Test
    fun testUndoManagerOperations() {
        val content = Content("Initial Content")
        val undoManager = UndoManager()
        undoManager.isUndoEnabled = true
        undoManager.maxUndoStackSize = 50

        assertFalse(undoManager.canUndo())
        assertFalse(undoManager.canRedo())

        // Simulate insert
        undoManager.beforeModification(content)
        content.insert(0, 0, "Prefix: ")
        undoManager.afterInsert(content, 0, 0, 0, 8, "Prefix: ")

        assertTrue(undoManager.canUndo())
        assertFalse(undoManager.canRedo())

        // Undo
        val range = undoManager.undo(content)
        assertEquals("Initial Content", content.toString())
        assertTrue(undoManager.canRedo())

        // Redo
        undoManager.redo(content)
        assertEquals("Prefix: Initial Content", content.toString())

        // Max stack size validation
        undoManager.maxUndoStackSize = 10
        assertEquals(10, undoManager.maxUndoStackSize)
        assertThrows(IllegalArgumentException::class.java) {
            undoManager.maxUndoStackSize = 0
        }
    }

    @Test
    fun testContentLifecycleAndDocumentOperations() {
        // Construction & Properties
        val content = Content("Hello\nWorld\nKotlin")
        assertEquals(3, content.lineCount)
        assertEquals(18, content.length)
        assertEquals('H', content[0])
        assertEquals('o', content.charAt(0, 4))
        assertTrue(content.isThreadSafe)
        assertTrue(content.isUndoEnabled)

        // Substring and subSequence
        assertEquals("Hello\n", content.substring(0, 6))
        val subSeq = content.subSequence(6, 11)
        assertEquals("World", subSeq.toString())

        // SubContent
        val sub = content.subContent(0, 0, 1, 5)
        assertEquals("Hello\nWorld", sub.toString())

        // Batch edit
        val versionBefore = content.documentVersion
        content.batchEdit {
            it.insert(0, 5, " Beautiful")
            it.insert(1, 5, "!")
        }
        assertTrue(content.documentVersion > versionBefore)
        assertEquals("Hello Beautiful\nWorld!\nKotlin", content.toString())

        // Line inspection
        assertEquals("Hello Beautiful", content.getLineString(0))
        assertEquals(15, content.getColumnCount(0))

        // Position validation
        val validPos = CharPosition(0, 5, 5)
        assertTrue(content.isValidPosition(validPos))
        val invalidPos = CharPosition(10, 0, 100)
        assertFalse(content.isValidPosition(invalidPos))

        // Replace
        content.replace(0, 6, 0, 15, "Magnificent")
        assertEquals("Hello Magnificent\nWorld!\nKotlin", content.toString())

        // Delete across lines
        content.delete(0, 5, 2, 0)
        assertEquals("HelloKotlin", content.toString())
        assertEquals(1, content.lineCount)

        // Undo & Redo through Content
        assertTrue(content.canUndo())
        content.undo()
        assertEquals("Hello Magnificent\nWorld!\nKotlin", content.toString())
        assertTrue(content.canRedo())
        content.redo()
        assertEquals("HelloKotlin", content.toString())

        // runReadActionsOnLines with Consumer
        var readLines = 0
        content.runReadActionsOnLines(0, content.lineCount - 1, Content.ContentLineConsumer { lineIdx, line, dirs ->
            readLines++
            assertNotNull(line)
            assertNotNull(dirs)
        })
        assertEquals(content.lineCount, readLines)

        // runReadActionsOnLines with Consumer2 and AbortFlag
        var abortCount = 0
        content.runReadActionsOnLines(0, content.lineCount - 1, Content.ContentLineConsumer2 { _, _, flag ->
            abortCount++
            flag.set = true
        })
        assertEquals(1, abortCount)

        // Shallow & Deep Copy
        val shallowCopy = content.copyTextShallow()
        assertEquals(content.toString(), shallowCopy.toString())
        assertFalse(shallowCopy.getLine(0).isMutable())
        shallowCopy.release()
        assertEquals(0, shallowCopy.lineCount)

        val deepCopy = content.copyText()
        assertEquals(content.toString(), deepCopy.toString())
        assertTrue(deepCopy.getLine(0).isMutable())

        // Indexer & Cursor
        assertNotNull(content.cursor)
        assertTrue(content.isCursorCreated)
        assertNotNull(content.indexer)
        assertEquals(5, content.getCharIndex(0, 5))
    }
}
