package io.github.rosemoe.sora.widget.snippet

import io.github.rosemoe.sora.lang.completion.snippet.ConditionalFormat
import io.github.rosemoe.sora.lang.completion.snippet.FormatString
import io.github.rosemoe.sora.lang.completion.snippet.NextUpperCaseFormat
import io.github.rosemoe.sora.lang.completion.snippet.NoFormat
import io.github.rosemoe.sora.lang.completion.snippet.Transform
import io.github.rosemoe.sora.widget.snippet.variable.ClipboardBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.CommentBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.CompositeSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.FileBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.RandomBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.TimeBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.WorkspaceBasedSnippetVariableResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.regex.Pattern

class SnippetVariableTest {

    @Test
    fun testClipboardResolver() {
        val resolver = ClipboardBasedSnippetVariableResolver(null)
        assertTrue(resolver.getResolvableNames().contains("CLIPBOARD"))
        assertEquals("", resolver.resolve("CLIPBOARD"))
        try {
            resolver.resolve("INVALID")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun testCommentResolver() {
        val resolver = CommentBasedSnippetVariableResolver(arrayOf("//", "/*", "*/"))
        assertEquals("//", resolver.resolve("LINE_COMMENT"))
        assertEquals("/*", resolver.resolve("BLOCK_COMMENT_START"))
        assertEquals("*/", resolver.resolve("BLOCK_COMMENT_END"))

        resolver.commentTokens = arrayOf("#", "'''", "'''")
        assertEquals("#", resolver.resolve("LINE_COMMENT"))

        try {
            resolver.resolve("UNKNOWN")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }

        resolver.commentTokens = null
        try {
            resolver.resolve("LINE_COMMENT")
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }

    @Test
    fun testCompositeResolver() {
        val composite = CompositeSnippetVariableResolver()
        val commentResolver = CommentBasedSnippetVariableResolver(arrayOf("//", "/*", "*/"))
        composite.addResolver(commentResolver)

        assertTrue(composite.canResolve("LINE_COMMENT"))
        assertFalse(composite.canResolve("NON_EXISTENT"))
        assertEquals("//", resolverResult(composite, "LINE_COMMENT"))
        assertEquals("", composite.resolve("NON_EXISTENT"))

        composite.removeResolver(commentResolver)
        assertFalse(composite.canResolve("LINE_COMMENT"))

        try {
            composite.addResolver(CompositeSnippetVariableResolver())
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    private fun resolverResult(resolver: CompositeSnippetVariableResolver, name: String): String {
        return resolver.resolve(name)
    }

    @Test
    fun testRandomResolver() {
        val resolver = RandomBasedSnippetVariableResolver()
        val names = resolver.getResolvableNames()
        assertTrue(names.contains("RANDOM"))
        assertTrue(names.contains("RANDOM_HEX"))
        assertTrue(names.contains("UUID"))

        val randomStr = resolver.resolve("RANDOM")
        assertNotNull(randomStr.toInt())

        val hexStr = resolver.resolve("RANDOM_HEX")
        assertTrue(hexStr.isNotEmpty())

        val uuidStr = resolver.resolve("UUID")
        assertEquals(36, uuidStr.length)
    }

    @Test
    fun testTimeResolver() {
        val resolver = TimeBasedSnippetVariableResolver()
        assertTrue(resolver.resolve("CURRENT_YEAR").toInt() >= 2024)
        assertTrue(resolver.resolve("CURRENT_YEAR_SHORT").length == 2)
        assertTrue(resolver.resolve("CURRENT_MONTH").length == 2)
        assertTrue(resolver.resolve("CURRENT_HOUR").length == 2)
        assertTrue(resolver.resolve("CURRENT_MINUTE").length == 2)
        assertTrue(resolver.resolve("CURRENT_SECOND").length == 2)
        assertTrue(resolver.resolve("CURRENT_DAY_NAME").isNotEmpty())
        assertTrue(resolver.resolve("CURRENT_DAY_NAME_SHORT").isNotEmpty())
        assertTrue(resolver.resolve("CURRENT_SECONDS_UNIX").toLong() > 0)
    }

    @Test
    fun testFileAndWorkspaceNames() {
        val fileResolver = object : FileBasedSnippetVariableResolver() {
            override fun resolve(name: String): String = "file.txt"
        }
        assertTrue(fileResolver.getResolvableNames().contains("TM_FILENAME"))

        val wsResolver = object : WorkspaceBasedSnippetVariableResolver() {
            override fun resolve(name: String): String = "project"
        }
        assertTrue(wsResolver.getResolvableNames().contains("WORKSPACE_NAME"))
    }

    @Test
    fun testTransformApplier() {
        // Null transform returns original text
        assertEquals("hello", TransformApplier.doTransform("hello", null))

        // Transform replacing 'foo' with upper case 'BAR'
        val formatList = listOf<FormatString>(
            NoFormat("BAR")
        )
        val transform = Transform().apply {
            regexp = Pattern.compile("foo")
            format = formatList
            globalMode = false
        }
        val result = TransformApplier.doTransform("say foo to all", transform)
        assertEquals("say BAR to all", result)

        // Test shorthand uppercase transform: pattern (foo) -> upcase group 1
        val upcaseFormatList = listOf<FormatString>(
            ConditionalFormat(group = 1, shorthand = "upcase")
        )
        val upcaseTransform = Transform().apply {
            regexp = Pattern.compile("(foo)")
            format = upcaseFormatList
            globalMode = true
        }
        val upcaseResult = TransformApplier.doTransform("foo and foo", upcaseTransform)
        assertEquals("FOO and FOO", upcaseResult)

        // Test nextUpperCase
        val nextUpperList = listOf<FormatString>(
            NextUpperCaseFormat(),
            ConditionalFormat(group = 1, shorthand = "lowcase")
        )
        val nextUpperTransform = Transform().apply {
            regexp = Pattern.compile("(HELLO)")
            format = nextUpperList
            globalMode = false
        }
        val nextUpperResult = TransformApplier.doTransform("say HELLO now", nextUpperTransform)
        assertEquals("say Hello now", nextUpperResult)
    }
}
