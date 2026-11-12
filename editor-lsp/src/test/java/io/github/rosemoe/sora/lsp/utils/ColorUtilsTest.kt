package io.github.rosemoe.sora.lsp.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorUtilsTest {

    @Test
    fun testNormalizeHex() {
        assertThat(ColorUtils.normalizeHex("#abc")).isEqualTo("#aabbcc")
        assertThat(ColorUtils.normalizeHex("#abcd")).isEqualTo("#ddaabbcc")
        assertThat(ColorUtils.normalizeHex("#11223344")).isEqualTo("#44112233")
        assertThat(ColorUtils.normalizeHex("#112233")).isEqualTo("#112233")
    }

    @Test
    fun testParseHex() {
        val color = ColorUtils.parseHex("#ff0000")
        assertThat(color).isNotNull()
        assertThat(color).isEqualTo(android.graphics.Color.RED)
    }

    @Test
    fun testParseRgb() {
        val color = ColorUtils.parseRgb("rgb(0, 255, 0)")
        assertThat(color).isNotNull()
        assertThat(color).isEqualTo(android.graphics.Color.GREEN)
    }
}
