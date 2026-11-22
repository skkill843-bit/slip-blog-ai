package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.model.GroundedSource
import com.example.data.model.TrendCategory
import com.example.data.model.TrendingDiscoveryResult
import com.example.data.model.TrendingTopic
import com.example.data.model.WriterSettings
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TopicResearchBrief(
    val verifiedSummary: String,
    val sources: List<GroundedSource>,
    val searchQueries: List<String>
)

class SlipBlogAiService {

    private val searchGroundedModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch())
        )
    }

    private val writerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.78f
                topP = 0.92f
            },
            systemInstruction = content {
                text(
                    """
                    You are a veteran human magazine journalist and senior editorial blogger writing for SLIP BLOG AI.
                    Your writing must read effortlessly like an experienced human author:
                    - Vary sentence cadence naturally (mix crisp short observations with thoughtful, well-paced explanations).
                    - Never use robotic AI clichés or filler phrases such as "In today's fast-paced digital world", "Delve into", "Tapestry", "Game-changer", "Unlock the power of", "In conclusion", "Let's dive in", or "Whether you're a beginner or an expert".
                    - Open immediately with an engaging hook, story, or concrete tension—never a generic dictionary definition.
                    - Explain ideas clearly using practical, relatable examples and real-world context.
                    - Only cite facts, numbers, dates, and sources verified in the provided live research brief. Never fabricate statistics, quotes, or URLs.
                    - Every paragraph must add genuine value; never pad length with repetitive fluff.
                    """.trimIndent()
                )
            }
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
    }

    suspend fun discoverTrendingTopics(
        category: TrendCategory,
        keyword: String
    ): TrendingDiscoveryResult {
        val today = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
        val trimmedKeyword = keyword.trim()
        val searchDirective = if (trimmedKeyword.isNotBlank()) {
            "Focus specifically on live trends, news, and high-interest discussions around \"$trimmedKeyword\" within the ${category.label} space (${category.searchFocus})."
        } else {
            "Focus on the hottest real-world trending stories right now in ${category.label} (${category.searchFocus})."
        }

        val prompt = """
            Today's date is $today.
            Use Google Search to identify 20 REAL, CURRENTLY TRENDING topics happening right now.
            $searchDirective

            STRICT RULES:
            1. Every single topic MUST come from actual current search results and real developments happening right now. NEVER invent fake trends or outdated placeholder topics.
            2. Provide exactly 20 distinct, high-potential blog topics.
            3. Output ONLY a raw valid JSON object (no markdown fences, no commentary) with this exact structure:
            {
              "topics": [
                {
                  "rank": 1,
                  "title": "Clear compelling topic headline",
                  "shortExplanation": "2 concise sentences explaining what is happening and why readers care right now.",
                  "whyTrendingNow": "Short 4-7 word trend catalyst (e.g. Major policy shift announced this week)",
                  "suggestedKeyword": "primary seo keyword phrase",
                  "sourceName": "Main publication or trend source name"
                }
              ]
            }
        """.trimIndent()

        val response = withTimeout(60_000L) {
            searchGroundedModel.generateContent(prompt)
        }
        val rawText = response.text?.trim()
            ?: throw IllegalStateException("Live trend search returned an empty response.")

        val topics = parseTopicsJson(rawText, category.label)
        if (topics.isEmpty()) {
            throw IllegalStateException("Could not extract trending topics from the live search result.")
        }

        val metadata = response.candidates.firstOrNull()?.groundingMetadata
        val queries = metadata?.webSearchQueries?.filter { it.isNotBlank() } ?: emptyList()
        val sources = metadata?.groundingChunks?.mapNotNull { chunk ->
            val web = chunk.web
            val uri = web?.uri
            val title = web?.title
            if (!uri.isNullOrBlank()) {
                GroundedSource(title = title?.ifBlank { uri } ?: uri, url = uri)
            } else null
        }?.distinctBy { it.url } ?: emptyList()

        return TrendingDiscoveryResult(
            topics = topics,
            webSearchQueries = queries,
            groundedSources = sources,
            categoryLabel = category.label,
            keyword = trimmedKeyword
        )
    }

    suspend fun researchTopicForArticle(
        topicTitle: String,
        topicContext: String,
        category: String
    ): TopicResearchBrief {
        val today = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
        val prompt = """
            Today is $today.
            Use Google Search to thoroughly fact-check and research this trending blog topic before writing:
            Topic: "$topicTitle"
            Category: $category
            Context: $topicContext

            Produce a concise factual research dossier containing:
            1. Key verified developments, timeline, and dates.
            2. Real, verified statistics or data points (only include numbers directly found in search results, naming the exact source).
            3. Practical real-world examples or case studies people are experiencing.
            4. Common misconceptions and top questions readers are asking right now.
            5. Key comparison dimensions suitable for a helpful reader comparison table.
        """.trimIndent()

        val response = withTimeout(60_000L) {
            searchGroundedModel.generateContent(prompt)
        }
        val summary = response.text?.trim()
            ?: throw IllegalStateException("Live research returned empty results.")

        val metadata = response.candidates.firstOrNull()?.groundingMetadata
        val queries = metadata?.webSearchQueries?.filter { it.isNotBlank() } ?: emptyList()
        val sources = metadata?.groundingChunks?.mapNotNull { chunk ->
            val web = chunk.web
            val uri = web?.uri
            val title = web?.title
            if (!uri.isNullOrBlank()) {
                GroundedSource(title = title?.ifBlank { uri } ?: uri, url = uri)
            } else null
        }?.distinctBy { it.url } ?: emptyList()

        return TopicResearchBrief(
            verifiedSummary = summary,
            sources = sources,
            searchQueries = queries
        )
    }

    fun streamLongFormArticle(
        topicTitle: String,
        topicContext: String,
        category: String,
        settings: WriterSettings,
        researchBrief: TopicResearchBrief
    ): Flow<String> {
        val languageInstruction = when (settings.language.label) {
            "Hindi" -> "Write the entire article, headings, FAQs, and captions in natural, modern, fluent Hindi (Devanagari script), avoiding stiff literal translations."
            "Hinglish" -> "Write the article in natural, engaging Hinglish (conversational Hindi written in Latin/English script mixed smoothly with everyday English terms—like modern Indian lifestyle and tech blogs)."
            else -> "Write in crisp, natural, idiomatic international English."
        }

        val sourcesContext = if (researchBrief.sources.isNotEmpty()) {
            researchBrief.sources.take(10).joinToString("\n") { "- ${it.title}: ${it.url}" }
        } else {
            "Use only verified facts from the research brief above."
        }

        val prompt = """
            Write a COMPLETE, READY-TO-PUBLISH long-form blog article (NOT an outline, NOT a summary).

            TOPIC: "$topicTitle"
            CATEGORY: $category
            CONTEXT: $topicContext
            LANGUAGE: ${settings.language.label} — $languageInstruction
            TARGET WORD COUNT: Approximately ${settings.length.wordCount} words (deliver genuine depth, practical examples, nuanced explanations, and real substance to naturally reach ~${settings.length.wordCount} words without fluff).
            TONE: ${settings.tone.label} (${settings.tone.description})
            AUDIENCE: ${settings.audience}

            VERIFIED LIVE RESEARCH BRIEF (Use ONLY these verified facts/stats; do NOT invent statistics or fake citations):
            ${researchBrief.verifiedSummary}

            VERIFIED SOURCE LINKS:
            $sourcesContext

            OUTPUT FORMAT (FOLLOW THIS EXACT HEADER + BODY STRUCTURE):
            First output these exact metadata lines (one per line, in English for image prompts, and in ${settings.language.label} for title/meta/alt/caption):
            SEO_TITLE: <compelling human SEO headline, 50-70 chars>
            META_DESCRIPTION: <enticing search meta description, 140-160 chars>
            HERO_IMAGE_PROMPT: <detailed photorealistic editorial scene prompt in English for an image generator, no text in image>
            HERO_IMAGE_ALT: <descriptive accessible image ALT text>
            HERO_IMAGE_CAPTION: <concise editorial photo caption>
            INLINE_IMAGE_PROMPT: <detailed modern editorial illustration/photo prompt in English for mid-article visual, no text in image>
            INLINE_IMAGE_ALT: <descriptive accessible image ALT text>
            INLINE_IMAGE_CAPTION: <concise editorial caption>
            ---ARTICLE_START---
            Now write the complete article in clean Markdown:
            1. Engaging introduction (hook immediately with a real scenario, tension, or insight; 2-3 crisp paragraphs).
            2. ## Table of Contents (brief bullet list of main sections).
            3. Multiple rich ## H2 sections and ### H3 subsections with detailed human-style paragraphs, storytelling, and practical examples.
            4. Helpful bullet points where they improve scannability.
            5. At least one Markdown comparison or summary table (`| Column | Column |`).
            6. At least two highlight callout boxes formatted as blockquotes (`> **Key Takeaway:** ...`).
            7. ## Frequently Asked Questions (4-6 genuine reader questions as ### H3 headings with clear, helpful answers).
            8. ## Final Thoughts (or a natural closing H2 heading — do NOT title it "Conclusion", give it a memorable human heading and a strong, resonant closing).
        """.trimIndent()

        return writerModel.generateContentStream(prompt)
            .mapNotNull { chunk -> chunk.text }
    }

    suspend fun generateCompressedArticleImage(promptText: String): String {
        val cleanPrompt = "$promptText. High-resolution editorial magazine photography style, crisp composition, no text, no watermarks, no logos."
        val response = withTimeout(60_000L) {
            imageModel.generateContent(cleanPrompt)
        }
        val bitmap = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
            ?.image
            ?: throw IllegalStateException("Image model did not return an image.")

        return withContext(Dispatchers.Default) {
            encodeBitmapToBoundedBase64(bitmap)
        }
    }

    private fun encodeBitmapToBoundedBase64(bitmap: Bitmap): String {
        val maxWidth = 800
        val scaled = if (bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
            val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, maxWidth, targetHeight, true)
        } else {
            bitmap
        }

        var quality = 72
        var base64Result: String
        do {
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            base64Result = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            quality -= 10
        } while (base64Result.length > 185_000 && quality >= 35)

        if (scaled !== bitmap) {
            scaled.recycle()
        }
        return base64Result
    }

    private fun parseTopicsJson(rawText: String, categoryLabel: String): List<TrendingTopic> {
        val jsonStart = rawText.indexOf('{')
        val jsonEnd = rawText.lastIndexOf('}')
        if (jsonStart < 0 || jsonEnd <= jsonStart) {
            Log.e(TAG, "No JSON object found in trend response: ${rawText.take(250)}")
            throw IllegalStateException("Live trend response was not formatted properly. Please try again.")
        }

        val jsonSlice = rawText.substring(jsonStart, jsonEnd + 1)
        val root = JSONObject(jsonSlice)
        val array = root.getJSONArray("topics")
        val list = mutableListOf<TrendingTopic>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.optString("title").trim()
            if (title.isBlank()) continue
            val rank = obj.optInt("rank", i + 1)
            list.add(
                TrendingTopic(
                    id = "trend_${i + 1}_${title.hashCode()}",
                    rank = rank,
                    title = title,
                    shortExplanation = obj.optString("shortExplanation").trim(),
                    whyTrendingNow = obj.optString("whyTrendingNow").trim(),
                    suggestedKeyword = obj.optString("suggestedKeyword").trim(),
                    sourceName = obj.optString("sourceName").trim(),
                    category = categoryLabel
                )
            )
        }
        return list.take(20)
    }

    private companion object {
        const val TAG = "SlipBlogAiService"
    }
}
