/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.lang

import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.SimpleAnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.brackets.OnlineBracketsMatcher
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.lang.brackets.SimpleBracketsCollector
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.folding.FoldingRegion
import io.github.rosemoe.sora.lang.format.AsyncFormatter
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.format.FormatterProvider
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.util.PlainTextAnalyzeManager
import io.github.rosemoe.sora.lang.util.PlainTextSpans
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
class LangFeaturesTest {

    // ==========================================
    // 1. PairedBracket & Brackets Providers
    // ==========================================

    @Test
    fun testPairedBracketPropertiesAndConstructors() {
        val bracket2 = PairedBracket(5, 12)
        assertEquals(5, bracket2.leftIndex)
        assertEquals(1, bracket2.leftLength)
        assertEquals(12, bracket2.rightIndex)
        assertEquals(1, bracket2.rightLength)

        val bracket4 = PairedBracket(3, 2, 10, 2)
        assertEquals(3, bracket4.leftIndex)
        assertEquals(2, bracket4.leftLength)
        assertEquals(10, bracket4.rightIndex)
        assertEquals(2, bracket4.rightLength)

        val copy = bracket4.copy(rightIndex = 15)
        assertEquals(15, copy.rightIndex)
        assertEquals(bracket4, PairedBracket(3, 2, 10, 2))
        assertEquals(bracket4.hashCode(), PairedBracket(3, 2, 10, 2).hashCode())
    }

    @Test
    fun testOnlineBracketsMatcherValidation() {
        try {
            OnlineBracketsMatcher(charArrayOf('('), 100)
            fail("Expected IllegalArgumentException for odd length pairs")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun testOnlineBracketsMatcherForwardAndBackward() {
        val pairs = charArrayOf('(', ')', '{', '}', '[', ']')
        val matcher = OnlineBracketsMatcher(pairs, 100)
        val content = Content("val x = (1 + {2 * [3 + 4]})")

        // '(' is at index 8, ')' is at index 26
        val openParen = matcher.getPairedBracketAt(content, 9)
        assertNotNull(openParen)
        assertEquals(8, openParen!!.leftIndex)
        assertEquals(26, openParen.rightIndex)

        // Close paren at index 26 (cursor after at 27)
        val closeParen = matcher.getPairedBracketAt(content, 27)
        assertNotNull(closeParen)
        assertEquals(8, closeParen!!.leftIndex)
        assertEquals(26, closeParen.rightIndex)

        // '{' is at index 13, '}' is at index 25
        val openBrace = matcher.getPairedBracketAt(content, 14)
        assertNotNull(openBrace)
        assertEquals(13, openBrace!!.leftIndex)
        assertEquals(25, openBrace.rightIndex)

        // '[' is at index 18, ']' is at index 24
        val openBracket = matcher.getPairedBracketAt(content, 19)
        assertNotNull(openBracket)
        assertEquals(18, openBracket!!.leftIndex)
        assertEquals(24, openBracket.rightIndex)
    }

    @Test
    fun testOnlineBracketsMatcherSearchLimit() {
        val pairs = charArrayOf('(', ')')
        // limit = 5 means it won't search beyond 5 chars
        val matcher = OnlineBracketsMatcher(pairs, 5)
        val content = Content("(          )")
        // Distance between '(' (0) and ')' (11) is 11 > 5
        val result = matcher.getPairedBracketAt(content, 1)
        assertNull(result)
    }

    @Test
    fun testSimpleBracketsCollector() {
        val collector = SimpleBracketsCollector()
        collector.add(4, 20)
        collector.add(8, 15)

        val content = Content("0123456789012345678901")

        // Query at index 5 (cursor after left bracket at 4)
        val res1 = collector.getPairedBracketAt(content, 5)
        assertNotNull(res1)
        assertEquals(4, res1!!.leftIndex)
        assertEquals(20, res1.rightIndex)

        // Query at index 21 (cursor after right bracket at 20)
        val res2 = collector.getPairedBracketAt(content, 21)
        assertNotNull(res2)
        assertEquals(4, res2!!.leftIndex)
        assertEquals(20, res2.rightIndex)

        // Clear collector
        collector.clear()
        assertNull(collector.getPairedBracketAt(content, 5))
    }

    // ==========================================
    // 2. FoldingRegion
    // ==========================================

    @Test
    fun testFoldingRegionLifecycle() {
        val region = FoldingRegion(0, 0, 10, 5)
        assertEquals(0, region.startLine)
        assertEquals(0, region.startColumn)
        assertEquals(10, region.endLine)
        assertEquals(5, region.endColumn)
        assertFalse(region.isCollapsed)

        region.isCollapsed = true
        assertTrue(region.isCollapsed)

        // Create child
        val child = region.createChild(2, 4, 8, 2)
        assertEquals(2, child.startLine)
        assertEquals(4, child.startColumn)
        assertEquals(8, child.endLine)
        assertEquals(2, child.endColumn)
        assertEquals(1, region.children.size)
        assertEquals(child, region.children[0])
    }

    @Test
    fun testFoldingRegionValidation() {
        try {
            FoldingRegion(10, 0, 5, 0)
            fail("Expected IllegalArgumentException when startLine > endLine")
        } catch (_: IllegalArgumentException) {
        }

        try {
            FoldingRegion(5, 10, 5, 2)
            fail("Expected IllegalArgumentException when startColumn > endColumn on same line")
        } catch (_: IllegalArgumentException) {
        }

        val parent = FoldingRegion(2, 5, 10, 20)
        try {
            parent.createChild(1, 0, 5, 0)
            fail("Expected IllegalArgumentException when child starts before parent")
        } catch (_: IllegalArgumentException) {
        }

        try {
            parent.createChild(3, 0, 11, 0)
            fail("Expected IllegalArgumentException when child ends beyond parent")
        } catch (_: IllegalArgumentException) {
        }
    }

    // ==========================================
    // 3. Smart Enter (NewlineHandleResult & Handler)
    // ==========================================

    @Test
    fun testNewlineHandleResult() {
        val result = NewlineHandleResult("\n    ", 4)
        assertEquals("\n    ", result.text)
        assertEquals(4, result.shiftLeft)

        try {
            NewlineHandleResult("\n  ", -1)
            fail("Expected IllegalArgumentException for negative shiftLeft")
        } catch (_: IllegalArgumentException) {
        }

        try {
            NewlineHandleResult("\n  ", 10)
            fail("Expected IllegalArgumentException for shiftLeft > text.length")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun testNewlineHandler() {
        val handler = object : NewlineHandler {
            override fun matchesRequirement(text: Content, position: CharPosition, style: Styles?): Boolean {
                return position.line == 0
            }

            override fun handleNewline(
                text: Content,
                position: CharPosition,
                style: Styles?,
                tabSize: Int
            ): NewlineHandleResult {
                return NewlineHandleResult("\n" + " ".repeat(tabSize), 0)
            }
        }

        val content = Content("fun hello() {\n}")
        assertTrue(handler.matchesRequirement(content, CharPosition(0, 5, 5), null))
        assertFalse(handler.matchesRequirement(content, CharPosition(1, 1, 15), null))

        val result = handler.handleNewline(content, CharPosition(0, 5, 5), null, 4)
        assertEquals("\n    ", result.text)
        assertEquals(0, result.shiftLeft)
    }

    // ==========================================
    // 4. Formatting (FormatterProvider & AsyncFormatter)
    // ==========================================

    @Test
    fun testFormatterProviderSam() {
        val provider = FormatterProvider { _ -> null }
        // Verify SAM conversion compiles and works
        assertNotNull(provider)
    }

    private class TestAsyncFormatter(
        private val shouldFail: Boolean = false
    ) : AsyncFormatter() {
        override fun formatAsync(text: Content, cursorRange: TextRange): TextRange? {
            if (shouldFail) {
                throw RuntimeException("Simulated format error")
            }
            text.insert(0, 0, "// formatted\n")
            return null
        }

        override fun formatRegionAsync(
            text: Content,
            rangeToFormat: TextRange,
            cursorRange: TextRange
        ): TextRange? {
            if (shouldFail) {
                throw RuntimeException("Simulated formatRegion error")
            }
            text.insert(0, 0, "/* region */")
            return null
        }
    }

    @Test
    fun testAsyncFormatterSuccess() {
        val formatter = TestAsyncFormatter(shouldFail = false)
        val content = Content("val a = 1")
        val latch = CountDownLatch(1)
        val resultHolder = AtomicReference<String>()

        val receiver = object : Formatter.FormatResultReceiver {
            override fun onFormatSucceed(applyContent: CharSequence, cursorRange: TextRange?) {
                resultHolder.set(applyContent.toString())
                latch.countDown()
            }

            override fun onFormatFail(throwable: Throwable?) {
                latch.countDown()
            }
        }
        formatter.setReceiver(receiver)

        formatter.format(content, TextRange(CharPosition(), CharPosition()))
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("// formatted\nval a = 1", resultHolder.get())
        formatter.destroy()
    }

    @Test
    fun testAsyncFormatterFailure() {
        val formatter = TestAsyncFormatter(shouldFail = true)
        val content = Content("val a = 1")
        val latch = CountDownLatch(1)
        val errorHolder = AtomicReference<Throwable>()

        val receiver = object : Formatter.FormatResultReceiver {
            override fun onFormatSucceed(applyContent: CharSequence, cursorRange: TextRange?) {
                latch.countDown()
            }

            override fun onFormatFail(throwable: Throwable?) {
                errorHolder.set(throwable)
                latch.countDown()
            }
        }
        formatter.setReceiver(receiver)

        formatter.format(content, TextRange(CharPosition(), CharPosition()))
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertNotNull(errorHolder.get())
        assertEquals("Simulated format error", errorHolder.get()?.message)
        formatter.destroy()
    }

    @Test
    fun testAsyncFormatterRegionAndCancel() {
        val formatter = TestAsyncFormatter(shouldFail = false)
        val content = Content("line 1\nline 2")
        val latch = CountDownLatch(1)
        val resultHolder = AtomicReference<String>()

        val receiver = object : Formatter.FormatResultReceiver {
            override fun onFormatSucceed(applyContent: CharSequence, cursorRange: TextRange?) {
                resultHolder.set(applyContent.toString())
                latch.countDown()
            }

            override fun onFormatFail(throwable: Throwable?) {
                latch.countDown()
            }
        }
        formatter.setReceiver(receiver)

        val range = TextRange(CharPosition(0, 0, 0), CharPosition(0, 6, 6))
        formatter.formatRegion(content, range, range)
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("/* region */line 1\nline 2", resultHolder.get())

        formatter.cancel()
        formatter.destroy()
        assertFalse(formatter.isRunning())
    }

    // ==========================================
    // 5. Diagnostics (DiagnosticRegion & DiagnosticsContainer)
    // ==========================================

    @Test
    fun testDiagnosticRegionPropertiesAndComparison() {
        val r1 = DiagnosticRegion(5, 10, DiagnosticRegion.SEVERITY_WARNING, 100L)
        val r2 = DiagnosticRegion(5, 10, DiagnosticRegion.SEVERITY_ERROR, 101L)
        val r3 = DiagnosticRegion(5, 12, DiagnosticRegion.SEVERITY_WARNING, 100L)
        val r4 = DiagnosticRegion(0, 10, DiagnosticRegion.SEVERITY_WARNING, 100L)

        assertEquals(5, r1.startIndex)
        assertEquals(10, r1.endIndex)
        assertEquals(DiagnosticRegion.SEVERITY_WARNING, r1.severity)
        assertEquals(100L, r1.id)
        assertNull(r1.detail)

        assertTrue(r4 < r1) // startIndex 0 < 5
        assertTrue(r1 < r3) // endIndex 10 < 12
        assertTrue(r1 < r2) // severity WARNING < ERROR
    }

    @Test
    fun testDiagnosticsContainerOperationsAndShifting() {
        val container = DiagnosticsContainer(shiftEnabled = true)
        val r1 = DiagnosticRegion(10, 20, DiagnosticRegion.SEVERITY_ERROR, 1L)
        val r2 = DiagnosticRegion(30, 40, DiagnosticRegion.SEVERITY_WARNING, 2L)

        container.addDiagnostics(listOf(r1, r2))
        assertEquals(2, container.getRegions().size)

        // Query in region [5, 15] should find r1
        val queried = ArrayList<DiagnosticRegion>()
        container.queryInRegion(queried, 5, 15)
        assertEquals(1, queried.size)
        assertEquals(r1, queried[0])

        // shiftOnInsert inside r1 (insert 5 chars at index 12)
        container.shiftOnInsert(12, 17) // length = 5
        // r1 was [10, 20], text inserted inside at 12 -> r1 becomes [10, 25]
        assertEquals(10, r1.startIndex)
        assertEquals(25, r1.endIndex)
        // r2 was [30, 40], text inserted before at 12 -> r2 becomes [35, 45]
        assertEquals(35, r2.startIndex)
        assertEquals(45, r2.endIndex)

        // shiftOnDelete across r1 (delete [5, 15] length 10)
        container.shiftOnDelete(5, 15)
        // r1 shared region [10, 15] (len 5). r1 was [10, 25] -> endIndex becomes 20, startIndex shifted left by 5 -> [5, 15]
        assertEquals(5, r1.startIndex)
        assertEquals(15, r1.endIndex)

        // Reset
        container.reset()
        assertTrue(container.getRegions().isEmpty())
    }

    // ==========================================
    // 6. Analysis (PlainTextSpans, PlainTextAnalyzeManager, SimpleAnalyzeManager)
    // ==========================================

    @Test
    fun testPlainTextSpans() {
        val spans = PlainTextSpans(10)
        assertEquals(10, spans.getLineCount())
        assertFalse(spans.supportsModify())

        spans.adjustOnInsert(CharPosition(0, 0, 0), CharPosition(2, 0, 10))
        assertEquals(12, spans.getLineCount())

        spans.adjustOnDelete(CharPosition(0, 0, 0), CharPosition(1, 0, 5))
        assertEquals(11, spans.getLineCount())

        spans.setLineCount(5)
        assertEquals(5, spans.getLineCount())
        assertNotNull(spans.read())

        try {
            spans.modify()
            fail("Expected UnsupportedOperationException")
        } catch (_: UnsupportedOperationException) {
        }
    }

    @Test
    fun testPlainTextAnalyzeManager() {
        val manager = PlainTextAnalyzeManager()
        val content = Content("line 1\nline 2\nline 3")
        val latch = CountDownLatch(1)
        val stylesHolder = AtomicReference<Styles>()

        val receiver = object : StyleReceiver {
            override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?) {
                stylesHolder.set(styles)
                latch.countDown()
            }

            override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?, action: Runnable?) {
                setStyles(sourceManager, styles)
            }

            override fun setDiagnostics(sourceManager: AnalyzeManager, diagnostics: DiagnosticsContainer?) {}
            override fun setInlayHints(sourceManager: AnalyzeManager, inlayHints: io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer?) {}
            override fun updateBracketProvider(sourceManager: AnalyzeManager, provider: io.github.rosemoe.sora.lang.brackets.BracketsProvider?) {}
        }
        manager.setReceiver(receiver)

        manager.reset(io.github.rosemoe.sora.text.ContentReference(content), android.os.Bundle())
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertNotNull(stylesHolder.get())
        assertEquals(3, stylesHolder.get()?.spans?.getLineCount())
        manager.destroy()
    }

    private class TestSimpleAnalyzeManager : SimpleAnalyzeManager<String>() {
        override fun analyze(text: StringBuilder, delegate: Delegate<String>): Styles? {
            delegate.data = "analyzed: ${text.length}"
            return Styles().apply {
                spans = PlainTextSpans(1)
            }
        }
    }

    @Test
    fun testSimpleAnalyzeManagerExecution() {
        val manager = TestSimpleAnalyzeManager()
        val content = Content("hello world")
        val latch = CountDownLatch(1)

        val receiver = object : StyleReceiver {
            override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?) {
                latch.countDown()
            }

            override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?, action: Runnable?) {
                setStyles(sourceManager, styles)
            }

            override fun setDiagnostics(sourceManager: AnalyzeManager, diagnostics: DiagnosticsContainer?) {}
            override fun setInlayHints(sourceManager: AnalyzeManager, inlayHints: io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer?) {}
            override fun updateBracketProvider(sourceManager: AnalyzeManager, provider: io.github.rosemoe.sora.lang.brackets.BracketsProvider?) {}
        }
        manager.setReceiver(receiver)

        manager.reset(io.github.rosemoe.sora.text.ContentReference(content), android.os.Bundle())
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("analyzed: 11", manager.data)
        manager.destroy()
    }
}
