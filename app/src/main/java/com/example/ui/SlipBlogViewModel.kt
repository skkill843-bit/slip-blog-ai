package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.SlipBlogAiService
import com.example.data.firestore.ArticleRepository
import com.example.data.html.BloggerHtmlGenerator
import com.example.data.model.ArticleLanguage
import com.example.data.model.ArticleLength
import com.example.data.model.ArticleTone
import com.example.data.model.BlogArticle
import com.example.data.model.GroundedSource
import com.example.data.model.TrendCategory
import com.example.data.model.TrendingDiscoveryResult
import com.example.data.model.TrendingTopic
import com.example.data.model.WriterSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface TrendingUiState {
    data object Idle : TrendingUiState
    data object Loading : TrendingUiState
    data class Success(val result: TrendingDiscoveryResult) : TrendingUiState
    data class Error(val message: String) : TrendingUiState
}

sealed interface WriterUiState {
    data object Idle : WriterUiState
    data class Configuring(
        val topicTitle: String,
        val topicContext: String,
        val category: String
    ) : WriterUiState

    data class Researching(
        val topicTitle: String,
        val message: String
    ) : WriterUiState

    data class Streaming(
        val topicTitle: String,
        val streamedText: String,
        val wordCount: Int,
        val verifiedSources: List<GroundedSource>
    ) : WriterUiState

    data class GeneratingVisuals(
        val topicTitle: String,
        val message: String
    ) : WriterUiState

    data class Ready(
        val article: BlogArticle,
        val heroPrompt: String = "",
        val inlinePrompt: String = "",
        val imageWarning: String? = null,
        val isRegeneratingHero: Boolean = false,
        val isRegeneratingInline: Boolean = false
    ) : WriterUiState

    data class Error(
        val topicTitle: String,
        val topicContext: String,
        val category: String,
        val message: String
    ) : WriterUiState
}

class SlipBlogViewModel(
    private val repository: ArticleRepository,
    private val aiService: SlipBlogAiService,
    private val currentUserId: String
) : ViewModel() {

    val savedArticlesState: StateFlow<UiState<List<BlogArticle>>> =
        repository.observeArticles(currentUserId)
            .map<List<BlogArticle>, UiState<List<BlogArticle>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing saved articles", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load saved articles"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val _selectedCategory = MutableStateFlow(TrendCategory.ALL)
    val selectedCategory: StateFlow<TrendCategory> = _selectedCategory.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _trendingState = MutableStateFlow<TrendingUiState>(TrendingUiState.Idle)
    val trendingState: StateFlow<TrendingUiState> = _trendingState.asStateFlow()

    private val _writerSettings = MutableStateFlow(WriterSettings())
    val writerSettings: StateFlow<WriterSettings> = _writerSettings.asStateFlow()

    private val _writerState = MutableStateFlow<WriterUiState>(WriterUiState.Idle)
    val writerState: StateFlow<WriterUiState> = _writerState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        discoverTrends()
    }

    fun updateCategory(category: TrendCategory) {
        _selectedCategory.value = category
    }

    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun updateLanguage(language: ArticleLanguage) {
        _writerSettings.value = _writerSettings.value.copy(language = language)
    }

    fun updateLength(length: ArticleLength) {
        _writerSettings.value = _writerSettings.value.copy(length = length)
    }

    fun updateTone(tone: ArticleTone) {
        _writerSettings.value = _writerSettings.value.copy(tone = tone)
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun discoverTrends() {
        val category = _selectedCategory.value
        val keyword = _searchKeyword.value
        viewModelScope.launch {
            _trendingState.value = TrendingUiState.Loading
            try {
                val result = aiService.discoverTrendingTopics(category, keyword)
                _trendingState.value = TrendingUiState.Success(result)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Trending discovery timed out", e)
                _trendingState.value = TrendingUiState.Error(
                    "Live trend search timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Trending discovery failed", e)
                _trendingState.value = TrendingUiState.Error(
                    e.localizedMessage ?: "Failed to fetch live trending topics."
                )
            }
        }
    }

    fun selectTopicForWriter(topic: TrendingTopic) {
        _writerState.value = WriterUiState.Configuring(
            topicTitle = topic.title,
            topicContext = "${topic.shortExplanation} (${topic.whyTrendingNow}). Source: ${topic.sourceName}".trim(),
            category = topic.category
        )
    }

    fun startCustomTopicInWriter(keyword: String, categoryLabel: String) {
        val cleanTitle = keyword.trim()
        if (cleanTitle.isBlank()) return
        _writerState.value = WriterUiState.Configuring(
            topicTitle = cleanTitle,
            topicContext = "Custom user topic in $categoryLabel",
            category = categoryLabel
        )
    }

    fun openSavedArticle(article: BlogArticle) {
        _writerState.value = WriterUiState.Ready(
            article = article,
            heroPrompt = article.heroImageAlt,
            inlinePrompt = article.inlineImageAlt
        )
    }

    fun resetWriterToIdle() {
        _writerState.value = WriterUiState.Idle
    }

    fun generateCompleteArticle(
        topicTitle: String,
        topicContext: String,
        category: String
    ) {
        val settings = _writerSettings.value
        viewModelScope.launch {
            _writerState.value = WriterUiState.Researching(
                topicTitle = topicTitle,
                message = "Searching live web sources to verify current facts and examples..."
            )
            try {
                // Stage 1: Live Search Grounding & Fact Research
                val researchBrief = aiService.researchTopicForArticle(
                    topicTitle = topicTitle,
                    topicContext = topicContext,
                    category = category
                )

                // Stage 2: Streaming Human-Quality Long-Form Article
                _writerState.value = WriterUiState.Streaming(
                    topicTitle = topicTitle,
                    streamedText = "",
                    wordCount = 0,
                    verifiedSources = researchBrief.sources
                )

                val fullOutput = StringBuilder()
                withTimeout(150_000L) {
                    aiService.streamLongFormArticle(
                        topicTitle = topicTitle,
                        topicContext = topicContext,
                        category = category,
                        settings = settings,
                        researchBrief = researchBrief
                    ).collect { chunk ->
                        fullOutput.append(chunk)
                        val textNow = fullOutput.toString()
                        val bodyPreview = textNow.substringAfter("---ARTICLE_START---", textNow).trim()
                        val words = bodyPreview.split(Regex("""\s+""")).count { it.isNotBlank() }
                        _writerState.value = WriterUiState.Streaming(
                            topicTitle = topicTitle,
                            streamedText = bodyPreview,
                            wordCount = words,
                            verifiedSources = researchBrief.sources
                        )
                    }
                }

                val rawArticleText = fullOutput.toString()
                if (rawArticleText.isBlank()) {
                    throw IllegalStateException("Article generator returned an empty response.")
                }

                // Stage 3: Parse & Build Blogger HTML Design
                _writerState.value = WriterUiState.GeneratingVisuals(
                    topicTitle = topicTitle,
                    message = "Designing Blogger-compatible HTML and generating custom editorial visuals..."
                )

                val parsed = BloggerHtmlGenerator.parseAiArticleOutput(
                    rawOutput = rawArticleText,
                    fallbackTopicTitle = topicTitle
                )
                val bloggerHtml = BloggerHtmlGenerator.buildBloggerHtml(
                    seoTitle = parsed.seoTitle,
                    metaDescription = parsed.metaDescription,
                    category = category,
                    language = settings.language.label,
                    wordCount = parsed.wordCount,
                    markdownBody = parsed.markdownBody,
                    heroImageAlt = parsed.heroImageAlt,
                    heroImageCaption = parsed.heroImageCaption,
                    inlineImageAlt = parsed.inlineImageAlt,
                    inlineImageCaption = parsed.inlineImageCaption,
                    sources = researchBrief.sources
                )

                // Stage 4: Generate Hero & Inline Editorial Images via Gemini Nano Banana 2.1
                var heroBase64 = ""
                var inlineBase64 = ""
                val imageErrors = mutableListOf<String>()

                try {
                    heroBase64 = aiService.generateCompressedArticleImage(parsed.heroImagePrompt)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Hero image generation failed", e)
                    imageErrors.add("Hero visual: ${e.localizedMessage ?: "Failed to generate image"}")
                }

                try {
                    inlineBase64 = aiService.generateCompressedArticleImage(parsed.inlineImagePrompt)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Inline image generation failed", e)
                    imageErrors.add("Section visual: ${e.localizedMessage ?: "Failed to generate image"}")
                }

                val draftArticle = BlogArticle(
                    userId = currentUserId,
                    topicTitle = topicTitle,
                    category = category,
                    seoTitle = parsed.seoTitle,
                    metaDescription = parsed.metaDescription,
                    language = settings.language.label,
                    targetWordCount = settings.length.wordCount.toLong(),
                    actualWordCount = parsed.wordCount.toLong(),
                    tone = settings.tone.label,
                    plainTextContent = parsed.plainTextBody,
                    bloggerHtmlContent = bloggerHtml,
                    heroImageAlt = parsed.heroImageAlt,
                    heroImageCaption = parsed.heroImageCaption,
                    heroImageBase64 = heroBase64,
                    inlineImageAlt = parsed.inlineImageAlt,
                    inlineImageCaption = parsed.inlineImageCaption,
                    inlineImageBase64 = inlineBase64,
                    sources = researchBrief.sources.map { "${it.title} | ${it.url}" }
                )

                // Stage 5: Save to Firestore so generated articles are preserved
                val saveResult = repository.saveArticle(draftArticle)
                val savedArticle = saveResult.fold(
                    onSuccess = { newId ->
                        _snackbarMessage.value = "Article generated & saved to your library."
                        draftArticle.copy(id = newId)
                    },
                    onFailure = { err ->
                        _snackbarMessage.value = "Generated article ready (cloud save warning: ${err.localizedMessage})"
                        draftArticle
                    }
                )

                _writerState.value = WriterUiState.Ready(
                    article = savedArticle,
                    heroPrompt = parsed.heroImagePrompt,
                    inlinePrompt = parsed.inlineImagePrompt,
                    imageWarning = imageErrors.takeIf { it.isNotEmpty() }?.joinToString("\n")
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Article generation timed out", e)
                _writerState.value = WriterUiState.Error(
                    topicTitle = topicTitle,
                    topicContext = topicContext,
                    category = category,
                    message = "Article generation timed out. Try again or select a shorter target length."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Article generation failed", e)
                _writerState.value = WriterUiState.Error(
                    topicTitle = topicTitle,
                    topicContext = topicContext,
                    category = category,
                    message = e.localizedMessage ?: "Failed to generate article."
                )
            }
        }
    }

    fun regenerateImage(isHero: Boolean) {
        val current = _writerState.value as? WriterUiState.Ready ?: return
        val prompt = if (isHero) {
            current.heroPrompt.ifBlank { current.article.heroImageAlt }
        } else {
            current.inlinePrompt.ifBlank { current.article.inlineImageAlt }
        }

        viewModelScope.launch {
            _writerState.value = if (isHero) {
                current.copy(isRegeneratingHero = true, imageWarning = null)
            } else {
                current.copy(isRegeneratingInline = true, imageWarning = null)
            }

            try {
                val base64 = aiService.generateCompressedArticleImage(prompt)
                val updatedArticle = if (isHero) {
                    current.article.copy(heroImageBase64 = base64)
                } else {
                    current.article.copy(inlineImageBase64 = base64)
                }

                if (updatedArticle.id.isNotBlank()) {
                    repository.updateArticleImages(
                        articleId = updatedArticle.id,
                        heroImageBase64 = if (isHero) base64 else null,
                        inlineImageBase64 = if (!isHero) base64 else null
                    )
                }

                _writerState.value = current.copy(
                    article = updatedArticle,
                    isRegeneratingHero = false,
                    isRegeneratingInline = false,
                    imageWarning = null
                )
                _snackbarMessage.value = if (isHero) "Hero image updated." else "Inline image updated."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image regeneration failed", e)
                _writerState.value = current.copy(
                    isRegeneratingHero = false,
                    isRegeneratingInline = false,
                    imageWarning = e.localizedMessage ?: "Image generation failed."
                )
            }
        }
    }

    fun deleteSavedArticle(articleId: String) {
        viewModelScope.launch {
            val result = repository.deleteArticle(articleId)
            result.fold(
                onSuccess = {
                    _snackbarMessage.value = "Article removed from library."
                    val currentReady = _writerState.value as? WriterUiState.Ready
                    if (currentReady?.article?.id == articleId) {
                        _writerState.value = WriterUiState.Idle
                    }
                },
                onFailure = { err ->
                    _snackbarMessage.value = "Delete failed: ${err.localizedMessage}"
                }
            )
        }
    }

    companion object {
        private const val TAG = "SlipBlogVM"

        fun provideFactory(
            repository: ArticleRepository,
            aiService: SlipBlogAiService,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SlipBlogViewModel(repository, aiService, currentUserId)
            }
        }
    }
}
