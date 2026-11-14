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
package io.github.rosemoe.sora.event

import io.github.rosemoe.sora.widget.CodeEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class EventManagerTest {

    private lateinit var editor: CodeEditor
    private lateinit var eventManager: EventManager

    class TestCustomEvent(editor: CodeEditor, val value: String) : Event(editor) {
        override fun canIntercept(): Boolean = true
    }

    class TestResultEvent(editor: CodeEditor) : ResultedEvent<String>(editor) {
        override fun canIntercept(): Boolean = true
    }

    @Before
    fun setUp() {
        editor = CodeEditor(RuntimeEnvironment.getApplication())
        eventManager = EventManager()
    }

    @Test
    fun testBasicSubscriptionAndDispatch() {
        var received = false
        var eventVal = ""
        eventManager.subscribeEvent<TestCustomEvent> { event, _ ->
            received = true
            eventVal = event.value
        }

        eventManager.dispatchEvent(TestCustomEvent(editor, "HelloEvent"))
        assertTrue(received)
        assertEquals("HelloEvent", eventVal)
    }

    @Test
    fun testUnsubscribeFromReceiver() {
        var count = 0
        eventManager.subscribeEvent<TestCustomEvent> { _, unsubscribe ->
            count++
            unsubscribe.unsubscribe()
        }

        eventManager.dispatchEvent(TestCustomEvent(editor, "1"))
        eventManager.dispatchEvent(TestCustomEvent(editor, "2"))
        assertEquals(1, count)
    }

    @Test
    fun testUnsubscribeFromReceipt() {
        var count = 0
        val receipt = eventManager.subscribeEvent(TestCustomEvent::class.java) { _, _ ->
            count++
        }

        eventManager.dispatchEvent(TestCustomEvent(editor, "1"))
        assertEquals(1, count)

        receipt.unsubscribe()
        eventManager.dispatchEvent(TestCustomEvent(editor, "2"))
        assertEquals(1, count)
    }

    @Test
    fun testHierarchicalDispatching() {
        val childManager = EventManager(eventManager)
        var parentCount = 0
        var childCount = 0

        eventManager.subscribeEvent<TestCustomEvent> { _, _ -> parentCount++ }
        childManager.subscribeEvent<TestCustomEvent> { _, _ -> childCount++ }

        childManager.dispatchEventFromRoot(TestCustomEvent(editor, "Hierarchical"))
        assertEquals(1, parentCount)
        assertEquals(1, childCount)
    }

    @Test
    fun testEventInterception() {
        var receiver1Count = 0
        var receiver2Count = 0

        eventManager.subscribeEvent<TestCustomEvent> { event, _ ->
            receiver1Count++
            event.intercept(InterceptTarget.TARGET_RECEIVERS)
        }
        eventManager.subscribeEvent<TestCustomEvent> { _, _ ->
            receiver2Count++
        }

        val targets = eventManager.dispatchEvent(TestCustomEvent(editor, "InterceptMe"))
        assertEquals(1, receiver1Count)
        assertEquals(0, receiver2Count)
        assertEquals(InterceptTarget.TARGET_RECEIVERS, targets)
    }

    @Test
    fun testSubscribeAlwaysAndReifiedSubscribe() {
        var alwaysCount = 0
        val receipt1 = eventManager.subscribeAlways<TestCustomEvent> { _ ->
            alwaysCount++
        }
        var subCount = 0
        val receipt2 = eventManager.subscribe<TestCustomEvent> { _, _ ->
            subCount++
        }

        eventManager.dispatchEvent(TestCustomEvent(editor, "A"))
        assertEquals(1, alwaysCount)
        assertEquals(1, subCount)

        receipt1.unsubscribe()
        receipt2.unsubscribe()
        eventManager.dispatchEvent(TestCustomEvent(editor, "B"))
        assertEquals(1, alwaysCount)
        assertEquals(1, subCount)
    }

    @Test
    fun testResultedEvent() {
        val event = TestResultEvent(editor)
        assertFalse(event.isResultSet)

        event.interceptAndSetResult("Success")
        assertTrue(event.isResultSet)
        assertEquals("Success", event.result)
        assertTrue(event.isIntercepted)
    }
}
