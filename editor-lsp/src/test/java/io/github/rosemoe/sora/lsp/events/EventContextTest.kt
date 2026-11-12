package io.github.rosemoe.sora.lsp.events

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EventContextTest {

    @Test
    fun testEventContextOperations() {
        val context = EventContext()
        context.put("key1", "value1")
        context.put("key2", 42)

        assertThat(context.get<String>("key1")).isEqualTo("value1")
        assertThat(context.getOrDefault("key2", 0)).isEqualTo(42)
        assertThat(context.getOrDefault("missing", 99)).isEqualTo(99)
        assertThat(context.getOrNull<String>("key1")).isEqualTo("value1")
        assertThat(context.getOrNull<String>("nonexistent")).isNull()

        val removed: String? = context.remove("key1")
        assertThat(removed).isEqualTo("value1")
        assertThat(context.getOrNull<String>("key1")).isNull()
    }

    @Test
    fun testEventContextByClass() {
        val context = EventContext()
        val data = StringBuilder("hello")
        context.put(data)

        val retrieved = context.getByClass(StringBuilder::class.java)
        assertThat(retrieved).isNotNull()
        assertThat(retrieved.toString()).isEqualTo("hello")
    }

    @Test
    fun testEventContextMerge() {
        val ctx1 = EventContext()
        ctx1.put("a", 1)
        val ctx2 = EventContext()
        ctx2.put("b", 2)

        ctx1 + ctx2
        assertThat(ctx1.get<Int>("a")).isEqualTo(1)
        assertThat(ctx1.get<Int>("b")).isEqualTo(2)
    }
}
