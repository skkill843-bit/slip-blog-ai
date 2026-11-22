package com.example.data.html

import com.example.data.model.GroundedSource

data class ParsedArticleOutput(
    val seoTitle: String,
    val metaDescription: String,
    val heroImagePrompt: String,
    val heroImageAlt: String,
    val heroImageCaption: String,
    val inlineImagePrompt: String,
    val inlineImageAlt: String,
    val inlineImageCaption: String,
    val markdownBody: String,
    val plainTextBody: String,
    val wordCount: Int
)

object BloggerHtmlGenerator {

    fun parseAiArticleOutput(
        rawOutput: String,
        fallbackTopicTitle: String
    ): ParsedArticleOutput {
        val marker = "---ARTICLE_START---"
        val splitIndex = rawOutput.indexOf(marker)
        val headerSection = if (splitIndex >= 0) rawOutput.substring(0, splitIndex) else ""
        val bodySection = if (splitIndex >= 0) {
            rawOutput.substring(splitIndex + marker.length).trim()
        } else {
            rawOutput.trim()
        }

        fun extractHeader(key: String, default: String = ""): String {
            val regex = Regex("""(?im)^$key\s*:\s*(.+)$""")
            return regex.find(headerSection)?.groupValues?.getOrNull(1)?.trim() ?: default
        }

        val seoTitle = extractHeader("SEO_TITLE", fallbackTopicTitle).ifBlank { fallbackTopicTitle }
        val metaDesc = extractHeader(
            "META_DESCRIPTION",
            "An in-depth, human-written guide exploring $fallbackTopicTitle with practical insights and verified facts."
        )
        val heroPrompt = extractHeader(
            "HERO_IMAGE_PROMPT",
            "Clean editorial magazine photography representing $fallbackTopicTitle, natural lighting, modern composition, no text"
        )
        val heroAlt = extractHeader(
            "HERO_IMAGE_ALT",
            "Editorial illustration depicting $seoTitle"
        )
        val heroCaption = extractHeader(
            "HERO_IMAGE_CAPTION",
            "A closer look at $fallbackTopicTitle and what it means for readers today."
        )
        val inlinePrompt = extractHeader(
            "INLINE_IMAGE_PROMPT",
            "Minimalist modern editorial graphic illustrating key concepts of $fallbackTopicTitle, clean natural tones, no text"
        )
        val inlineAlt = extractHeader(
            "INLINE_IMAGE_ALT",
            "Visual breakdown related to $fallbackTopicTitle"
        )
        val inlineCaption = extractHeader(
            "INLINE_IMAGE_CAPTION",
            "Key context and practical takeaways surrounding $fallbackTopicTitle."
        )

        val cleanMarkdown = bodySection
            .removePrefix("```markdown")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val plainText = buildString {
            appendLine(seoTitle)
            appendLine()
            appendLine(markdownToPlainText(cleanMarkdown))
        }.trim()

        val wordCount = plainText
            .split(Regex("""\s+"""))
            .count { it.isNotBlank() }

        return ParsedArticleOutput(
            seoTitle = seoTitle,
            metaDescription = metaDesc,
            heroImagePrompt = heroPrompt,
            heroImageAlt = heroAlt,
            heroImageCaption = heroCaption,
            inlineImagePrompt = inlinePrompt,
            inlineImageAlt = inlineAlt,
            inlineImageCaption = inlineCaption,
            markdownBody = cleanMarkdown,
            plainTextBody = plainText,
            wordCount = wordCount
        )
    }

    fun buildBloggerHtml(
        seoTitle: String,
        metaDescription: String,
        category: String,
        language: String,
        wordCount: Int,
        markdownBody: String,
        heroImageAlt: String,
        heroImageCaption: String,
        inlineImageAlt: String,
        inlineImageCaption: String,
        sources: List<GroundedSource>
    ): String {
        val readMinutes = (wordCount / 200).coerceAtLeast(3)
        val lines = markdownBody.lines()
        val headings = mutableListOf<Pair<String, String>>()

        // Collect H2 headings for Table of Contents
        lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.startsWith("## ") && !trimmed.startsWith("### ")) {
                val title = cleanInlineMarkdownText(trimmed.removePrefix("## ").trim())
                if (!title.equals("Table of Contents", ignoreCase = true)) {
                    val slug = slugify(title)
                    headings.add(slug to title)
                }
            }
        }

        val bodyHtml = convertMarkdownBodyToHtml(
            lines = lines,
            headings = headings,
            inlineImageAlt = inlineImageAlt,
            inlineImageCaption = inlineImageCaption
        )

        val sourcesHtml = if (sources.isNotEmpty()) {
            val items = sources.distinctBy { it.url }.take(8).joinToString("\n") { src ->
                """<li><a href="${escapeAttr(src.url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(src.title)}</a></li>"""
            }
            """
            <section class="slip-sources-box">
              <h3>Verified References &amp; Further Reading</h3>
              <ul>$items</ul>
            </section>
            """.trimIndent()
        } else ""

        return """
<style>
  .slip-blog-article {
    font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    color: #1e293b;
    background-color: #ffffff;
    max-width: 780px;
    margin: 0 auto;
    padding: 24px 20px 40px;
    line-height: 1.78;
    font-size: 17px;
    box-sizing: border-box;
  }
  .slip-blog-article * { box-sizing: border-box; }
  .slip-article-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
    margin-bottom: 16px;
    font-size: 13px;
    font-weight: 600;
    color: #475569;
  }
  .slip-badge {
    background: #0a1628;
    color: #ffffff;
    padding: 4px 12px;
    border-radius: 999px;
    font-size: 12px;
    letter-spacing: 0.04em;
    text-transform: uppercase;
  }
  .slip-blog-article h1.slip-title {
    font-family: 'Playfair Display', Georgia, 'Times New Roman', serif;
    color: #0a1628;
    font-size: clamp(28px, 4.5vw, 40px);
    line-height: 1.22;
    margin: 0 0 16px 0;
    font-weight: 700;
    letter-spacing: -0.02em;
  }
  .slip-lead {
    color: #475569;
    font-size: 18px;
    line-height: 1.65;
    margin: 0 0 24px 0;
    padding-left: 16px;
    border-left: 3px solid #0284c7;
  }
  .slip-hero-figure, .slip-inline-figure {
    margin: 26px 0;
    padding: 0;
  }
  .slip-hero-figure img, .slip-inline-figure img {
    width: 100%;
    max-width: 100%;
    height: auto;
    display: block;
    border-radius: 14px;
    box-shadow: 0 12px 30px rgba(10, 22, 40, 0.12);
  }
  .slip-hero-figure figcaption, .slip-inline-figure figcaption {
    margin-top: 10px;
    font-size: 13.5px;
    color: #64748b;
    text-align: center;
    font-style: italic;
  }
  .slip-toc {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-left: 4px solid #0a1628;
    border-radius: 12px;
    padding: 18px 22px;
    margin: 28px 0;
  }
  .slip-toc-title {
    margin: 0 0 10px 0;
    font-size: 15px;
    font-weight: 700;
    color: #0a1628;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }
  .slip-toc ol {
    margin: 0;
    padding-left: 20px;
  }
  .slip-toc li {
    margin: 6px 0;
  }
  .slip-toc a {
    color: #0f4c81;
    text-decoration: none;
    font-weight: 500;
  }
  .slip-toc a:hover { text-decoration: underline; }
  .slip-blog-article h2 {
    font-family: 'Playfair Display', Georgia, serif;
    color: #0a1628;
    font-size: clamp(22px, 3.2vw, 28px);
    line-height: 1.3;
    margin: 38px 0 14px 0;
    padding-bottom: 8px;
    border-bottom: 2px solid #e2e8f0;
    font-weight: 700;
  }
  .slip-blog-article h3 {
    color: #0f172a;
    font-size: 20px;
    line-height: 1.4;
    margin: 26px 0 10px 0;
    font-weight: 700;
  }
  .slip-blog-article p {
    margin: 0 0 18px 0;
    color: #334155;
  }
  .slip-blog-article ul, .slip-blog-article ol {
    margin: 0 0 20px 0;
    padding-left: 24px;
  }
  .slip-blog-article li {
    margin-bottom: 8px;
    color: #334155;
  }
  .slip-highlight-box {
    background: linear-gradient(135deg, #f0f9ff 0%, #f8fafc 100%);
    border: 1px solid #bae6fd;
    border-left: 4px solid #0284c7;
    border-radius: 12px;
    padding: 18px 20px;
    margin: 26px 0;
    color: #0c4a6e;
  }
  .slip-highlight-box strong { color: #0a1628; }
  .slip-table-wrap {
    width: 100%;
    overflow-x: auto;
    margin: 26px 0;
    border-radius: 12px;
    border: 1px solid #e2e8f0;
  }
  .slip-blog-article table {
    width: 100%;
    border-collapse: collapse;
    font-size: 15px;
    text-align: left;
  }
  .slip-blog-article th {
    background: #0a1628;
    color: #ffffff;
    font-weight: 600;
    padding: 12px 14px;
  }
  .slip-blog-article td {
    padding: 11px 14px;
    border-top: 1px solid #e2e8f0;
    color: #334155;
  }
  .slip-blog-article tr:nth-child(even) td {
    background: #f8fafc;
  }
  .slip-sources-box {
    margin-top: 38px;
    padding: 18px 20px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    font-size: 14.5px;
  }
  .slip-sources-box h3 {
    margin: 0 0 10px 0;
    font-size: 16px;
    color: #0a1628;
  }
  .slip-sources-box a {
    color: #0284c7;
    word-break: break-word;
  }
  @media (max-width: 600px) {
    .slip-blog-article {
      padding: 16px 14px 28px;
      font-size: 16px;
    }
    .slip-toc, .slip-highlight-box {
      padding: 14px 16px;
    }
  }
</style>
<article class="slip-blog-article">
  <header>
    <div class="slip-article-meta">
      <span class="slip-badge">${escapeHtml(category)}</span>
      <span>${escapeHtml(language)}</span>
      <span>&bull;</span>
      <span>$readMinutes min read</span>
      <span>&bull;</span>
      <span>~$wordCount words</span>
    </div>
    <h1 class="slip-title">${escapeHtml(seoTitle)}</h1>
    <p class="slip-lead">${escapeHtml(metaDescription)}</p>
  </header>

  <figure class="slip-hero-figure">
    <img src="{{HERO_IMAGE_SRC}}" alt="${escapeAttr(heroImageAlt)}" loading="lazy" />
    <figcaption>${escapeHtml(heroImageCaption)}</figcaption>
  </figure>

  $bodyHtml

  $sourcesHtml
</article>
        """.trim()
    }

    private fun convertMarkdownBodyToHtml(
        lines: List<String>,
        headings: List<Pair<String, String>>,
        inlineImageAlt: String,
        inlineImageCaption: String
    ): String {
        val sb = StringBuilder()
        var i = 0
        var tocInserted = false
        var inlineImageInserted = false
        var h2Count = 0
        val inlineTargetH2 = (headings.size / 2).coerceAtLeast(2)

        fun buildTocHtml(): String {
            if (headings.isEmpty()) return ""
            val items = headings.joinToString("\n") { (slug, title) ->
                """<li><a href="#$slug">${escapeHtml(title)}</a></li>"""
            }
            return """
                <nav class="slip-toc" aria-label="Table of Contents">
                  <p class="slip-toc-title">Table of Contents</p>
                  <ol>$items</ol>
                </nav>
            """.trimIndent()
        }

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (trimmed.isBlank()) {
                i++
                continue
            }

            // Skip top H1 if model repeated title
            if (trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                i++
                continue
            }

            // Skip model-emitted Table of Contents section so we render our crisp hyperlinked TOC
            if (trimmed.startsWith("## ") && trimmed.contains("Table of Contents", ignoreCase = true)) {
                if (!tocInserted) {
                    sb.appendLine(buildTocHtml())
                    tocInserted = true
                }
                i++
                while (i < lines.size && !lines[i].trim().startsWith("## ")) {
                    i++
                }
                continue
            }

            // H2
            if (trimmed.startsWith("## ") && !trimmed.startsWith("### ")) {
                if (!tocInserted) {
                    sb.appendLine(buildTocHtml())
                    tocInserted = true
                }
                val rawTitle = trimmed.removePrefix("## ").trim()
                val cleanTitle = cleanInlineMarkdownText(rawTitle)
                val slug = slugify(cleanTitle)
                h2Count++
                if (!inlineImageInserted && h2Count == inlineTargetH2 + 1) {
                    sb.appendLine(
                        """
                        <figure class="slip-inline-figure">
                          <img src="{{INLINE_IMAGE_SRC}}" alt="${escapeAttr(inlineImageAlt)}" loading="lazy" />
                          <figcaption>${escapeHtml(inlineImageCaption)}</figcaption>
                        </figure>
                        """.trimIndent()
                    )
                    inlineImageInserted = true
                }
                sb.appendLine("""<h2 id="$slug">${formatInlineMarkdownToHtml(rawTitle)}</h2>""")
                i++
                continue
            }

            // H3
            if (trimmed.startsWith("### ")) {
                val rawTitle = trimmed.removePrefix("### ").trim()
                sb.appendLine("""<h3>${formatInlineMarkdownToHtml(rawTitle)}</h3>""")
                i++
                continue
            }

            // Highlight / Blockquote
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    val cleaned = lines[i].trim().removePrefix(">").trim()
                        .replace(Regex("""^\[!(TIP|NOTE|HIGHLIGHT|IMPORTANT)\]\s*""", RegexOption.IGNORE_CASE), "")
                    if (cleaned.isNotBlank()) quoteLines.add(cleaned)
                    i++
                }
                if (quoteLines.isNotEmpty()) {
                    val content = quoteLines.joinToString("<br/>") { formatInlineMarkdownToHtml(it) }
                    sb.appendLine("""<aside class="slip-highlight-box">$content</aside>""")
                }
                continue
            }

            // Markdown Table
            if (trimmed.startsWith("|") && i + 1 < lines.size && lines[i + 1].trim().matches(Regex("""^\|[\s\-:|]+\|?$"""))) {
                val tableRows = mutableListOf<List<String>>()
                val headerCells = parseTableRow(trimmed)
                i += 2 // skip header and separator
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    tableRows.add(parseTableRow(lines[i].trim()))
                    i++
                }
                sb.appendLine("""<div class="slip-table-wrap"><table><thead><tr>""")
                headerCells.forEach { cell ->
                    sb.append("""<th>${formatInlineMarkdownToHtml(cell)}</th>""")
                }
                sb.appendLine("""</tr></thead><tbody>""")
                tableRows.forEach { row ->
                    sb.append("""<tr>""")
                    row.forEach { cell ->
                        sb.append("""<td>${formatInlineMarkdownToHtml(cell)}</td>""")
                    }
                    sb.appendLine("""</tr>""")
                }
                sb.appendLine("""</tbody></table></div>""")
                continue
            }

            // Unordered list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                sb.appendLine("<ul>")
                while (i < lines.size) {
                    val cur = lines[i].trim()
                    if (cur.startsWith("- ") || cur.startsWith("* ")) {
                        val itemText = cur.substring(2).trim()
                        sb.appendLine("""  <li>${formatInlineMarkdownToHtml(itemText)}</li>""")
                        i++
                    } else {
                        break
                    }
                }
                sb.appendLine("</ul>")
                continue
            }

            // Ordered list
            if (trimmed.matches(Regex("""^\d+\.\s+.*"""))) {
                sb.appendLine("<ol>")
                while (i < lines.size) {
                    val cur = lines[i].trim()
                    val match = Regex("""^\d+\.\s+(.*)$""").find(cur)
                    if (match != null) {
                        sb.appendLine("""  <li>${formatInlineMarkdownToHtml(match.groupValues[1].trim())}</li>""")
                        i++
                    } else {
                        break
                    }
                }
                sb.appendLine("</ol>")
                continue
            }

            // Horizontal rule
            if (trimmed == "---" || trimmed == "***") {
                i++
                continue
            }

            // Paragraph
            val paraLines = mutableListOf<String>()
            while (i < lines.size) {
                val cur = lines[i].trim()
                if (cur.isBlank() || cur.startsWith("#") || cur.startsWith(">") ||
                    cur.startsWith("- ") || cur.startsWith("* ") || cur.startsWith("|") ||
                    cur.matches(Regex("""^\d+\.\s+.*"""))
                ) {
                    break
                }
                paraLines.add(cur)
                i++
            }
            if (paraLines.isNotEmpty()) {
                sb.appendLine("<p>${formatInlineMarkdownToHtml(paraLines.joinToString(" "))}</p>")
            }
        }

        if (!inlineImageInserted) {
            sb.appendLine(
                """
                <figure class="slip-inline-figure">
                  <img src="{{INLINE_IMAGE_SRC}}" alt="${escapeAttr(inlineImageAlt)}" loading="lazy" />
                  <figcaption>${escapeHtml(inlineImageCaption)}</figcaption>
                </figure>
                """.trimIndent()
            )
        }

        return sb.toString()
    }

    private fun parseTableRow(row: String): List<String> =
        row.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { it.trim() }

    private fun formatInlineMarkdownToHtml(raw: String): String {
        var text = escapeHtml(raw)
        // Markdown links [label](url)
        text = text.replace(Regex("""\[([^\]]+)]\((https?://[^)]+)\)""")) { match ->
            val label = match.groupValues[1]
            val url = match.groupValues[2]
            """<a href="$url" target="_blank" rel="noopener noreferrer">$label</a>"""
        }
        // Bold **text**
        text = text.replace(Regex("""\*\*(.+?)\*\*"""), "<strong>$1</strong>")
        // Italic *text*
        text = text.replace(Regex("""\*(.+?)\*"""), "<em>$1</em>")
        // Inline code `code`
        text = text.replace(Regex("""`([^`]+)`"""), "<code>$1</code>")
        return text
    }

    private fun cleanInlineMarkdownText(raw: String): String =
        raw.replace("**", "").replace("*", "").replace("`", "").trim()

    private fun markdownToPlainText(markdown: String): String {
        return markdown.lines().joinToString("\n") { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> trimmed.removePrefix("### ").uppercase()
                trimmed.startsWith("## ") -> "\n" + trimmed.removePrefix("## ").uppercase()
                trimmed.startsWith("# ") -> trimmed.removePrefix("# ").uppercase()
                trimmed.startsWith("> ") -> "Highlight: " + trimmed.removePrefix("> ")
                    .replace(Regex("""^\[!(TIP|NOTE|HIGHLIGHT|IMPORTANT)\]\s*""", RegexOption.IGNORE_CASE), "")
                else -> trimmed
            }
                .replace("**", "")
                .replace(Regex("""\[([^\]]+)]\((https?://[^)]+)\)"""), "$1 ($2)")
        }.replace(Regex("""\n{3,}"""), "\n\n").trim()
    }

    private fun slugify(text: String): String =
        text.lowercase()
            .replace(Regex("""[^a-z0-9\s-]"""), "")
            .trim()
            .replace(Regex("""\s+"""), "-")
            .ifBlank { "section" }

    private fun escapeHtml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun escapeAttr(input: String): String =
        escapeHtml(input).replace("\"", "&quot;")
}
