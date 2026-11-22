package com.example

import com.example.data.html.BloggerHtmlGenerator
import com.example.data.model.BlogArticle
import com.example.data.model.GroundedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloggerHtmlGeneratorTest {

    @Test
    fun `parses structured metadata and converts markdown into Blogger HTML`() {
        val sampleAiOutput = """
            SEO_TITLE: Why Local AI Workflows Are Changing Software Teams
            META_DESCRIPTION: A practical guide to how engineering teams use local AI models safely without bloating costs.
            HERO_IMAGE_PROMPT: Minimalist software studio desk with warm morning light
            HERO_IMAGE_ALT: Minimalist software studio desk
            HERO_IMAGE_CAPTION: Teams increasingly pair cloud models with local tooling.
            INLINE_IMAGE_PROMPT: Clean architectural illustration of modular workflow
            INLINE_IMAGE_ALT: Modular workflow diagram
            INLINE_IMAGE_CAPTION: Structured workflow pipelines reduce latency.
            ---ARTICLE_START---
            Most teams do not adopt new tooling because it sounds exciting; they adopt it when Tuesday mornings start feeling slower than they used to.

            ## Table of Contents
            - Why Teams Shift Now
            - Key Comparison

            ## Why Teams Shift Now

            Real engineering workflows reward predictability.

            > **Key Takeaway:** Always measure latency before committing to a complex pipeline.

            ## Key Comparison

            | Approach | Latency | Cost |
            | --- | --- | --- |
            | Local First | Low | Predictable |
            | Hybrid Cloud | Medium | Flexible |

            ## Frequently Asked Questions

            ### Does this work for small teams?
            Yes, small teams often benefit the most because setup stays lean.
        """.trimIndent()

        val parsed = BloggerHtmlGenerator.parseAiArticleOutput(
            rawOutput = sampleAiOutput,
            fallbackTopicTitle = "Fallback Title"
        )

        assertEquals("Why Local AI Workflows Are Changing Software Teams", parsed.seoTitle)
        assertTrue(parsed.wordCount > 40)

        val html = BloggerHtmlGenerator.buildBloggerHtml(
            seoTitle = parsed.seoTitle,
            metaDescription = parsed.metaDescription,
            category = "Tech & AI",
            language = "English",
            wordCount = parsed.wordCount,
            markdownBody = parsed.markdownBody,
            heroImageAlt = parsed.heroImageAlt,
            heroImageCaption = parsed.heroImageCaption,
            inlineImageAlt = parsed.inlineImageAlt,
            inlineImageCaption = parsed.inlineImageCaption,
            sources = listOf(GroundedSource("Engineering Report", "https://example.com/report"))
        )

        assertTrue(html.contains("<nav class=\"slip-toc\""))
        assertTrue(html.contains("<aside class=\"slip-highlight-box\">"))
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("alt=\"Minimalist software studio desk\""))
        assertTrue(html.contains("Verified References"))

        val article = BlogArticle(
            seoTitle = parsed.seoTitle,
            metaDescription = parsed.metaDescription,
            bloggerHtmlContent = html,
            heroImageBase64 = "abc123",
            inlineImageBase64 = ""
        )
        val resolved = article.resolvedBloggerHtml()
        assertTrue(resolved.contains("data:image/jpeg;base64,abc123"))
        assertFalse(resolved.contains("{{INLINE_IMAGE_SRC}}"))
    }
}
