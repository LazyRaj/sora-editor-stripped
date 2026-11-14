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
package io.github.rosemoe.sora.util

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class UtilitiesTest {

    @Test
    fun testIntPairPackAndUnpack() {
        val first = 12345
        val second = -67890
        val packed = IntPair.pack(first, second)

        assertEquals(first, IntPair.getFirst(packed))
        assertEquals(second, IntPair.getSecond(packed))
        assertEquals(first, packed.first)
        assertEquals(second, packed.second)
        val (f, s) = packed
        assertEquals(first, f)
        assertEquals(second, s)
    }

    @Test
    fun testIntPairPackIntFloat() {
        val first = 42
        val second = 3.14159f
        val packed = IntPair.packIntFloat(first, second)

        assertEquals(first, IntPair.getFirst(packed))
        assertEquals(second, IntPair.getSecondAsFloat(packed), 0.00001f)
        assertEquals(second, packed.secondFloat, 0.00001f)
    }

    @Test
    fun testFloatsWithinDelta() {
        assertTrue(Floats.withinDelta(1.0f, 1.05f, 0.1f))
        assertFalse(Floats.withinDelta(1.0f, 1.15f, 0.1f))
        assertTrue(1.0f.isCloseTo(1.05f, 0.1f))
        assertFalse(1.0f.isCloseTo(1.15f, 0.1f))
    }

    @Test
    fun testNumbersStringSizeAndBitOps() {
        assertEquals(1, Numbers.stringSize(0))
        assertEquals(1, Numbers.stringSize(9))
        assertEquals(2, Numbers.stringSize(10))
        assertEquals(2, Numbers.stringSize(-5))
        assertEquals(3, Numbers.stringSize(-10))

        assertEquals(0, Numbers.clearBit(4, 4))
        assertEquals(6, Numbers.clearBit(6, 1))

        assertEquals(5, Numbers.coerceIn(10, 1, 5))
        assertEquals(1, Numbers.coerceIn(-5, 1, 5))
        assertEquals(3, Numbers.coerceIn(3, 1, 5))
    }

    @Test
    fun testMutableInt() {
        val mi = MutableInt(10)
        mi.increase()
        assertEquals(11, mi.value)
        assertEquals(10, mi.decreaseAndGet())
    }

    @Test
    fun testTemporaryBuffers() {
        val charBuf = TemporaryCharBuffer.obtain(50)
        assertTrue(charBuf.size >= 50)
        TemporaryCharBuffer.recycle(charBuf)

        val floatBuf = TemporaryFloatBuffer.obtain(50)
        assertTrue(floatBuf.size >= 50)
        TemporaryFloatBuffer.recycle(floatBuf)

        val charRes = TemporaryCharBuffer.use(32) { buf ->
            buf[0] = 'K'
            buf[0]
        }
        assertEquals('K', charRes)

        val floatRes = TemporaryFloatBuffer.use(32) { buf ->
            buf[0] = 1.23f
            buf[0]
        }
        assertEquals(1.23f, floatRes, 0.001f)
    }

    @Test
    fun testClipDataToString() {
        assertEquals("", ClipDataUtils.clipDataToString(null))
        assertEquals("", (null as ClipData?).toPlainText())

        val clip = ClipData.newPlainText("label", "Hello world")
        assertEquals("Hello world", ClipDataUtils.clipDataToString(clip))
        assertEquals("Hello world", clip.toPlainText())

        val uriClip = ClipData.newRawUri("uri", Uri.parse("https://example.com"))
        assertEquals("https://example.com", ClipDataUtils.clipDataToString(uriClip))
        assertEquals("https://example.com", uriClip.toPlainText())
    }

    @Test
    fun testLongArrayList() {
        val list = LongArrayList()
        assertEquals(0, list.size)
        assertTrue(list.isEmpty)
        assertFalse(list.isNotEmpty)
        list.add(10L)
        list.add(20L)
        list.add(30L)
        assertEquals(3, list.size)
        assertTrue(list.isNotEmpty)
        assertTrue(20L in list)
        assertFalse(99L in list)
        assertEquals(20L, list[1])
        list[1] = 25L
        assertEquals(25L, list[1])
        assertEquals(1, list.lowerBound(25L))
        assertEquals(2, list.lowerBound(26L))
        list.clear()
        assertEquals(0, list.size)
        assertTrue(list.isEmpty)
    }

    @Test
    fun testMyCharacter() {
        assertTrue(MyCharacter.isJavaIdentifierStart('a'))
        assertTrue('a'.isJavaIdentifierStart)
        assertTrue(MyCharacter.isJavaIdentifierStart('_'))
        assertTrue('_'.isJavaIdentifierStart)
        assertFalse(MyCharacter.isJavaIdentifierStart('1'))
        assertFalse('1'.isJavaIdentifierStart)
        assertTrue(MyCharacter.isJavaIdentifierPart('1'))
        assertTrue('1'.isJavaIdentifierPart)
        assertTrue(MyCharacter.isAlpha('Z'))
        assertTrue('Z'.isAlpha)
        assertFalse(MyCharacter.isAlpha('9'))
        assertFalse('9'.isAlpha)
        assertTrue(MyCharacter.couldBeEmoji(0x1F600))
        assertTrue(0x1F600.couldBeEmoji)
        assertFalse(MyCharacter.couldBeEmoji(0x0041))
        assertFalse(0x0041.couldBeEmoji)
    }

    @Test
    fun testRegexBackref() {
        val pattern = java.util.regex.Pattern.compile("([a-z]+)-(\\d+)")
        val matcher = pattern.matcher("hello-123")
        assertTrue(matcher.find())
        val replacement = io.github.rosemoe.sora.util.regex.RegexBackrefHelper.computeReplacement(
            matcher,
            io.github.rosemoe.sora.util.regex.RegexBackrefGrammar.DEFAULT,
            "word: $1, num: $2"
        )
        assertEquals("word: hello, num: 123", replacement)
    }

    @Test
    fun testTrieTree() {
        val trie = TrieTree<Int>()
        trie.put("apple", 1)
        trie.put("app", 2)
        assertEquals(1, trie.get("apple", 0, 5))
        assertEquals(2, trie.get("app", 0, 3))
        assertEquals(null, trie.get("appl", 0, 4))
    }

    @Test
    fun testBinaryHeap() {
        val heap = BinaryHeap()
        val id1 = heap.push(10)
        val id2 = heap.push(30)
        val id3 = heap.push(20)
        assertEquals(30, heap.top)
        heap.update(id1, 40)
        assertEquals(40, heap.top)
        heap.remove(id1)
        assertEquals(30, heap.top)
    }

    @Test
    fun testBlockIntList() {
        val list = BlockIntList(16)
        for (i in 0 until 50) {
            list.add(i)
        }
        assertEquals(50, list.size)
        assertEquals(49, list.max)
        assertEquals(25, list[25])
        list[25] = 100
        assertEquals(100, list.max)
        list.remove(25)
        assertEquals(49, list.size)
        assertEquals(49, list.max)
        list.removeRange(10, 20)
        assertEquals(39, list.size)
    }

    @Test
    fun testObjectPool() {
        var count = 0
        val pool = object : ObjectPool<StringBuilder>(4) {
            override fun allocateNew(): StringBuilder {
                count++
                return StringBuilder()
            }
        }
        val sb1 = pool.obtain()
        assertEquals(1, count)
        pool.recycle(sb1)
        val sb2 = pool.obtain()
        assertTrue(sb1 === sb2)
        assertEquals(1, count)
        pool.recycle(sb2)

        val used = pool.use { sb ->
            sb.append("test")
            sb.toString()
        }
        assertEquals("test", used)
    }
}
