package io.github.rosemoe.sora.lsp.editor.text

private val brRegex = Regex("(?i)<br\\s*/?>")
private val headingRegex = Regex("(?is)<h([1-6])[^>]*>(.*?)</h\\1>")
private val blockquoteRegex = Regex("(?is)<blockquote[^>]*>(.*?)</blockquote>")
private val strongRegex = Regex("(?is)<strong[^>]*>(.*?)</strong>")
private val emRegex = Regex("(?is)<em[^>]*>(.*?)</em>")
private val codeRegex = Regex("(?is)<code[^>]*>(.*?)</code>")
private val preRegex = Regex("(?is)<pre[^>]*>(.*?)</pre>")
private val liRegex = Regex("(?is)<li[^>]*>(.*?)</li>")
private val linkRegex = Regex("(?is)<a[^>]+href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>(.*?)</a>")
private val markdownLinkRegex = Regex("(?i)\\[([^\\]]*)\\]\\(([^\\)]*)\\)")
private val autoLinkRegex = Regex("(?i)<([^>]+)>")
private val urlRegex = Regex("(?i)https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&/=]*)")
private val ulRegex = Regex("(?is)</?ul[^>]*>")
private val olRegex = Regex("(?is)</?ol[^>]*>")
private val pOpenRegex = Regex("(?is)<p[^>]*>")
private val pCloseRegex = Regex("(?is)</p>")
private val multiNewlineRegex = Regex("\n{3,}")

internal fun normalizeMarkdownHtml(input: String): String {
    var text = input.replace("\r\n", "\n")
    text = text.replace("\r", "\n")
    text = text.replace(brRegex, "\n")
    text = headingRegex.replace(text) { matchResult ->
        val level = matchResult.groupValues[1].toInt()
        val body = matchResult.groupValues[2].trim()
        "${"#".repeat(level)} $body"
    }
    text = blockquoteRegex.replace(text) { matchResult ->
        val body = matchResult.groupValues[1].trim()
        "> $body"
    }
    text = strongRegex.replace(text, "**$1**")
    text = emRegex.replace(text, "*$1*")
    text = codeRegex.replace(text, "`$1`")
    text = preRegex.replace(text) { matchResult ->
        val body = matchResult.groupValues[1]
        "```\n$body\n```"
    }
    text = liRegex.replace(text) { matchResult ->
        val body = matchResult.groupValues[1].trim()
        "- $body"
    }
    text = linkRegex.replace(text) { matchResult ->
        val href = matchResult.groupValues[1]
        val label = matchResult.groupValues[2]
        "[$label]($href)"
    }
    text = autoLinkRegex.replace(text) { matchResult ->
        val url = matchResult.groupValues[1]
        if (url.matches(urlRegex)) "[$url]($url)" else matchResult.value
    }
    text = normalizePlainUrl(text)
    text = text.replace(ulRegex, "")
    text = text.replace(olRegex, "")
    text = text.replace(pCloseRegex, "\n\n")
    text = pOpenRegex.replace(text, "")
    text = text.replace("&nbsp;", " ")
    text = text.replace("&lt;", "<")
    text = text.replace("&gt;", ">")
    text = text.replace("&amp;", "&")
    text = text.replace(multiNewlineRegex, "\n\n")
    return text.trim()
}

/**
 * Converts plain URLs in the input text into Markdown links, ignoring URLs
 * that are already part of existing Markdown links.
 */
private fun normalizePlainUrl(text: String): String {
    val markdownLinks = markdownLinkRegex.findAll(text).map { it.range }.toList()
    var lastIndex = 0

    return buildString {
        urlRegex.findAll(text).forEach { match ->
            val range = match.range
            append(text.substring(lastIndex, range.first))
            if (markdownLinks.none { range.first >= it.first && range.last <= it.last }) {
                append("[${match.value}](${match.value})")
            } else {
                append(text.substring(range))
            }
            lastIndex = range.last + 1
        }
        append(text.substring(lastIndex))
    }
}
