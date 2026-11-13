/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.lang.completion

import android.os.Handler
import android.os.Looper
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.CodeEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
class CompletionCoreTest {

    @Test
    fun testCompletionItemPropertiesAndBuilders() {
        val item = object : CompletionItem("myLabel", "myDesc") {
            override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
                text.insert(line, column, "completed")
            }
        }

        assertEquals("myLabel", item.label)
        assertEquals("myDesc", item.desc)
        assertNull(item.icon)
        assertNull(item.detail)
        assertFalse(item.deprecated)
        assertNull(item.kind)
        assertEquals(0, item.prefixLength)

        item.label("newLabel")
            .desc("newDesc")
            .kind(CompletionItemKind.Function)

        assertEquals("newLabel", item.label)
        assertEquals("newDesc", item.desc)
        assertEquals(CompletionItemKind.Function, item.kind)
    }

    @Test
    fun testSimpleCompletionItemPerformCompletion() {
        val content = Content("val x = fo")
        val item = SimpleCompletionItem(2, "fooBar")

        assertEquals("fooBar", item.commitText)
        assertEquals(2, item.prefixLength)

        // Replace "fo" at column 10 (line 0, col 10)
        item.performCompletion(CodeEditor(org.robolectric.RuntimeEnvironment.getApplication()), content, 0, 10)
        assertEquals("val x = fooBar", content.toString())

        // Insert at prefixLength = 0
        val insertItem = SimpleCompletionItem(";", 0, ";")
        insertItem.performCompletion(CodeEditor(org.robolectric.RuntimeEnvironment.getApplication()), content, 0, 14)
        assertEquals("val x = fooBar;", content.toString())
    }

    @Test
    fun testCompletionCancelledException() {
        val ex1 = CompletionCancelledException()
        assertNull(ex1.message)

        val ex2 = CompletionCancelledException("cancelled")
        assertEquals("cancelled", ex2.message)
    }

    @Test
    fun testCompletionHelperComputePrefix() {
        val content = Content("val myIdentifier = 42")
        val ref = ContentReference(content)
        val pos = CharPosition(0, 16, 16) // right after "myIdentifier"

        val prefix = CompletionHelper.computePrefix(ref, pos) { ch ->
            ch.isLetterOrDigit() || ch == '_'
        }
        assertEquals("myIdentifier", prefix)

        val emptyPos = CharPosition(0, 4, 4) // at space after "val"
        val emptyPrefix = CompletionHelper.computePrefix(ref, emptyPos) { ch ->
            ch.isLetterOrDigit()
        }
        assertEquals("", emptyPrefix)

        assertFalse(CompletionHelper.checkCancelled())
    }

    @Test
    fun testMatchHelperStartsWith() {
        val matcher = MatchHelper()
        matcher.ignoreCase = false
        matcher.matchFirstCase = false

        val res1 = matcher.startsWith("TestString", "Test")
        assertNotNull(res1)
        assertEquals("TestString", res1.toString())

        val res2 = matcher.startsWith("TestString", "test")
        assertNull(res2)

        matcher.ignoreCase = true
        val res3 = matcher.startsWith("TestString", "test")
        assertNotNull(res3)

        matcher.matchFirstCase = true
        val res4 = matcher.startsWith("TestString", "test")
        assertNull(res4)
        val res5 = matcher.startsWith("TestString", "Test")
        assertNotNull(res5)
    }

    @Test
    fun testMatchHelperContains() {
        val matcher = MatchHelper()
        matcher.ignoreCase = false

        val res1 = matcher.contains("banana", "nan")
        assertNotNull(res1)
        assertEquals("banana", res1.toString())

        val res2 = matcher.contains("banana", "NAN")
        assertNull(res2)

        matcher.ignoreCase = true
        val res3 = matcher.contains("banana", "NAN")
        assertNotNull(res3)
    }

    @Test
    fun testMatchHelperCommonSub() {
        val matcher = MatchHelper()
        matcher.ignoreCase = false

        val res1 = matcher.commonSub("apple", "ale")
        assertNotNull(res1)
        assertEquals("apple", res1.toString())

        val res2 = matcher.commonSub("apple", "alx")
        assertNull(res2)

        val res3 = matcher.commonSub("apple", "ALE", true)
        assertNotNull(res3)
    }

    @Test
    fun testCompletionPublisherItemsAndSorting() {
        val handler = Handler(Looper.getMainLooper())
        val callbackRan = AtomicBoolean(false)
        val publisher = CompletionPublisher(handler, { callbackRan.set(true) }, Language.INTERRUPTION_LEVEL_SLIGHT)

        assertFalse(publisher.hasData())
        assertEquals(CompletionPublisher.DEFAULT_UPDATE_THRESHOLD, publisher.updateThreshold)

        val item1 = SimpleCompletionItem("beta", 0, "beta")
        val item2 = SimpleCompletionItem("alpha", 0, "alpha")

        publisher.addItem(item1)
        publisher.addItem(item2)
        assertTrue(publisher.hasData())

        publisher.updateList(true)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(callbackRan.get())
        assertEquals(2, publisher.items.size)

        // Set comparator and verify sorting
        publisher.setComparator { a, b -> a.label.toString().compareTo(b.label.toString()) }
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("alpha", publisher.items[0].label)
        assertEquals("beta", publisher.items[1].label)

        // Test cancel
        publisher.cancel()
        try {
            publisher.checkCancelled()
            fail("Expected CompletionCancelledException")
        } catch (expected: CompletionCancelledException) {
            // Success
        }
    }

    @Test
    fun testIdentifierAutoCompleteKeywordsAndDisposable() {
        val keywords = arrayOf("abstract", "boolean", "class", "default")
        val auto = IdentifierAutoComplete(keywords)

        assertNotNull(auto.keywords)
        assertEquals(4, auto.keywords!!.size)

        val items = auto.createCompletionItemList("cl", null)
        assertTrue(items.any { it.label == "class" })

        // Test DisposableIdentifiers
        val disposable = IdentifierAutoComplete.DisposableIdentifiers()
        disposable.beginBuilding()
        disposable.addIdentifier("myVar")
        disposable.addIdentifier("myFunction")
        disposable.addIdentifier("myVar") // Duplicate should be deduplicated
        disposable.finishBuilding()

        val dest = mutableListOf<String>()
        disposable.filterIdentifiers("my", dest)
        assertEquals(2, dest.size)
        assertTrue(dest.contains("myVar"))
        assertTrue(dest.contains("myFunction"))
    }

    @Test
    fun testIdentifierAutoCompleteSyncIdentifiers() {
        val sync = IdentifierAutoComplete.SyncIdentifiers()
        sync.identifierIncrease("counter")
        sync.identifierIncrease("counter")
        sync.identifierIncrease("foo")

        val dest = mutableListOf<String>()
        sync.filterIdentifiers("c", dest)
        assertTrue(dest.contains("counter"))

        sync.identifierDecrease("counter")
        dest.clear()
        sync.filterIdentifiers("c", dest)
        assertTrue(dest.contains("counter"))

        sync.identifierDecrease("counter")
        dest.clear()
        sync.filterIdentifiers("c", dest)
        assertFalse(dest.contains("counter"))

        sync.clear()
        dest.clear()
        sync.filterIdentifiers("f", dest)
        assertFalse(dest.contains("foo"))
    }
}
