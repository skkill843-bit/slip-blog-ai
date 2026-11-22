package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TrendCategory(val label: String, val searchFocus: String) {
    ALL("All Trends", "global breaking news, technology, business, culture, and viral discussions"),
    TECH_AI("Tech & AI", "artificial intelligence, software engineering, gadgets, cybersecurity, and startups"),
    BUSINESS("Business & Finance", "markets, personal finance, global economy, startups, and investing"),
    HEALTH("Health & Lifestyle", "wellness, fitness, nutrition, productivity, travel, and modern living"),
    SCIENCE("Science & Climate", "scientific breakthroughs, space exploration, climate tech, and medicine"),
    CULTURE("Media & Culture", "digital culture, creator economy, streaming, gaming, and entertainment"),
    INDIA("India Spotlight", "India technology, economy, startups, policy, sports, and viral topics")
}

enum class ArticleLanguage(val label: String) {
    ENGLISH("English"),
    HINDI("Hindi"),
    HINGLISH("Hinglish");

    companion object {
        fun fromLabel(raw: String): ArticleLanguage =
            entries.firstOrNull { it.label == raw } ?: ENGLISH
    }
}

enum class ArticleLength(val wordCount: Int, val label: String, val readTime: String) {
    W1500(1500, "1,500 words", "~7 min read"),
    W2000(2000, "2,000 words", "~10 min read"),
    W3000(3000, "3,000 words", "~15 min read"),
    W4000(4000, "4,000 words", "~20 min read")
}

enum class ArticleTone(val label: String, val description: String) {
    NATURAL_HUMAN("Natural Human", "Warm, sharp, authentic human voice"),
    CONVERSATIONAL("Conversational", "Friendly, direct, story-driven"),
    PROFESSIONAL("Professional", "Authoritative, clear, analytical");

    companion object {
        fun fromLabel(raw: String): ArticleTone =
            entries.firstOrNull { it.label == raw } ?: NATURAL_HUMAN
    }
}

data class WriterSettings(
    val language: ArticleLanguage = ArticleLanguage.ENGLISH,
    val length: ArticleLength = ArticleLength.W2000,
    val tone: ArticleTone = ArticleTone.NATURAL_HUMAN,
    val audience: String = "General Readers"
)

data class TrendingTopic(
    val id: String,
    val rank: Int,
    val title: String,
    val shortExplanation: String,
    val whyTrendingNow: String,
    val suggestedKeyword: String,
    val sourceName: String,
    val category: String
)

data class GroundedSource(
    val title: String,
    val url: String
)

data class TrendingDiscoveryResult(
    val topics: List<TrendingTopic>,
    val webSearchQueries: List<String>,
    val groundedSources: List<GroundedSource>,
    val categoryLabel: String,
    val keyword: String
)

@Keep
@IgnoreExtraProperties
data class BlogArticle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val topicTitle: String = "",
    val category: String = "",
    val seoTitle: String = "",
    val metaDescription: String = "",
    val language: String = ArticleLanguage.ENGLISH.label,
    val targetWordCount: Long = 2000L,
    val actualWordCount: Long = 0L,
    val tone: String = ArticleTone.NATURAL_HUMAN.label,
    val plainTextContent: String = "",
    val bloggerHtmlContent: String = "",
    val heroImageAlt: String = "",
    val heroImageCaption: String = "",
    val heroImageBase64: String = "",
    val inlineImageAlt: String = "",
    val inlineImageCaption: String = "",
    val inlineImageBase64: String = "",
    val sources: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedBloggerHtml(): String {
        var html = bloggerHtmlContent
        html = if (heroImageBase64.isNotBlank()) {
            html.replace("{{HERO_IMAGE_SRC}}", "data:image/jpeg;base64,$heroImageBase64")
        } else {
            html.replace(
                Regex("""<figure class="slip-hero-figure"[\s\S]*?</figure>"""),
                ""
            )
        }
        html = if (inlineImageBase64.isNotBlank()) {
            html.replace("{{INLINE_IMAGE_SRC}}", "data:image/jpeg;base64,$inlineImageBase64")
        } else {
            html.replace(
                Regex("""<figure class="slip-inline-figure"[\s\S]*?</figure>"""),
                ""
            )
        }
        return html
    }

    fun standaloneDownloadHtml(): String {
        val bodyHtml = resolvedBloggerHtml()
        val escapedTitle = seoTitle
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        val escapedDesc = metaDescription
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <meta name="description" content="$escapedDesc" />
              <title>$escapedTitle</title>
            </head>
            <body style="margin:0;padding:20px 12px;background:#f8fafc;">
              $bodyHtml
            </body>
            </html>
        """.trimIndent()
    }
}
