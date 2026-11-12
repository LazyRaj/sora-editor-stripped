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
    }

    @Test
    fun testIntPairPackIntFloat() {
        val first = 42
        val second = 3.14159f
        val packed = IntPair.packIntFloat(first, second)

        assertEquals(first, IntPair.getFirst(packed))
        assertEquals(second, IntPair.getSecondAsFloat(packed), 0.00001f)
    }

    @Test
    fun testFloatsWithinDelta() {
        assertTrue(Floats.withinDelta(1.0f, 1.05f, 0.1f))
        assertFalse(Floats.withinDelta(1.0f, 1.15f, 0.1f))
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
    }

    @Test
    fun testClipDataToString() {
        assertEquals("", ClipDataUtils.clipDataToString(null))

        val clip = ClipData.newPlainText("label", "Hello world")
        assertEquals("Hello world", ClipDataUtils.clipDataToString(clip))

        val uriClip = ClipData.newRawUri("uri", Uri.parse("https://example.com"))
        assertEquals("https://example.com", ClipDataUtils.clipDataToString(uriClip))
    }

    @Test
    fun testLongArrayList() {
        val list = LongArrayList()
        assertEquals(0, list.size())
        list.add(10L)
        list.add(20L)
        list.add(30L)
        assertEquals(3, list.size())
        assertEquals(20L, list[1])
        list[1] = 25L
        assertEquals(25L, list[1])
        assertEquals(1, list.lowerBound(25L))
        assertEquals(2, list.lowerBound(26L))
        list.clear()
        assertEquals(0, list.size())
    }

    @Test
    fun testMyCharacter() {
        assertTrue(MyCharacter.isJavaIdentifierStart('a'))
        assertTrue(MyCharacter.isJavaIdentifierStart('_'))
        assertFalse(MyCharacter.isJavaIdentifierStart('1'))
        assertTrue(MyCharacter.isJavaIdentifierPart('1'))
        assertTrue(MyCharacter.isAlpha('Z'))
        assertFalse(MyCharacter.isAlpha('9'))
        assertTrue(MyCharacter.couldBeEmoji(0x1F600))
        assertFalse(MyCharacter.couldBeEmoji(0x0041))
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
        assertEquals(30, heap.top())
        heap.update(id1, 40)
        assertEquals(40, heap.top())
        heap.remove(id1)
        assertEquals(30, heap.top())
    }

    @Test
    fun testBlockIntList() {
        val list = BlockIntList(16)
        for (i in 0 until 50) {
            list.add(i)
        }
        assertEquals(50, list.size())
        assertEquals(49, list.getMax())
        assertEquals(25, list[25])
        list[25] = 100
        assertEquals(100, list.getMax())
        list.remove(25)
        assertEquals(49, list.size())
        assertEquals(49, list.getMax())
        list.removeRange(10, 20)
        assertEquals(39, list.size())
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
    }
}
