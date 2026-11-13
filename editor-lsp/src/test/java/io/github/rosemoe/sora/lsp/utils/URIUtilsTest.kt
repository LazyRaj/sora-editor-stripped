package io.github.rosemoe.sora.lsp.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.net.URI

@RunWith(RobolectricTestRunner::class)
class URIUtilsTest {

    @Test
    fun testFileUriConversion() {
        val path = "/path/to/test/File.kt"
        val fileUri = path.toFileUri()
        assertThat(fileUri.path).isEqualTo(path)
        assertThat(fileUri.toFile()).isEqualTo(File(path))

        val uriString = fileUri.toFileUri()
        assertThat(uriString).startsWith("file:")

        val uri = URI(uriString)
        val roundTripFileUri = uri.toFileUri()
        assertThat(roundTripFileUri.path).isEqualTo(path)
    }

    @Test
    fun testStringToURI() {
        val uriStr = "file:///workspace/project/main.c"
        val uri = uriStr.toURI()
        assertThat(uri.scheme).isEqualTo("file")
        assertThat(uri.path).isEqualTo("/workspace/project/main.c")
    }
}
