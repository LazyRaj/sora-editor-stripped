package io.github.rosemoe.sora.lsp.utils

import com.google.common.truth.Truth.assertThat
import io.github.rosemoe.sora.text.CharPosition
import org.eclipse.lsp4j.Position
import org.junit.Test

class LspUtilsTest {

    @Test
    fun testPositionConversion() {
        val charPos = CharPosition(10, 5)
        val lspPos = charPos.asLspPosition()
        assertThat(lspPos.line).isEqualTo(10)
        assertThat(lspPos.character).isEqualTo(5)

        val convertedBack = lspPos.asCharPosition()
        assertThat(convertedBack.line).isEqualTo(10)
        assertThat(convertedBack.column).isEqualTo(5)
    }

    @Test
    fun testRangeCreation() {
        val start = Position(1, 0)
        val end = Position(1, 10)
        val range = createRange(start, end)
        assertThat(range.start.line).isEqualTo(1)
        assertThat(range.end.character).isEqualTo(10)
    }

    @Test
    fun testVersionIncrementAndClear() {
        val uri = "/test/file.rs".toFileUri()
        val params1 = uri.createDidChangeTextDocumentParams(emptyList())
        val params2 = uri.createDidChangeTextDocumentParams(emptyList())

        assertThat(params2.textDocument.version).isGreaterThan(params1.textDocument.version)

        clearVersions { it == uri }
        val params3 = uri.createDidChangeTextDocumentParams(emptyList())
        assertThat(params3.textDocument.version).isEqualTo(1)
    }
}
