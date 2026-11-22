package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ArticleLanguage
import com.example.data.model.ArticleLength
import com.example.data.model.ArticleTone
import com.example.data.model.BlogArticle
import com.example.data.model.TrendCategory
import com.example.data.model.WriterSettings
import com.example.ui.WriterUiState
import com.example.ui.theme.ElectricSapphire
import com.example.ui.theme.ElectricSky
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SkyTint
import com.example.ui.theme.SurfaceWhite

@Composable
fun ArticleStudioScreen(
    writerState: WriterUiState,
    writerSettings: WriterSettings,
    onLanguageChange: (ArticleLanguage) -> Unit,
    onLengthChange: (ArticleLength) -> Unit,
    onToneChange: (ArticleTone) -> Unit,
    onStartCustomTopic: (String, String) -> Unit,
    onGenerateArticle: (topicTitle: String, topicContext: String, category: String) -> Unit,
    onRegenerateImage: (isHero: Boolean) -> Unit,
    onResetToIdle: () -> Unit,
    onGoToDiscover: () -> Unit,
    onNotify: (String) -> Unit
) {
    BackHandler(enabled = writerState !is WriterUiState.Idle) {
        onResetToIdle()
    }

    when (writerState) {
        is WriterUiState.Idle -> {
            CustomTopicStartView(
                onStartCustomTopic = onStartCustomTopic,
                onGoToDiscover = onGoToDiscover
            )
        }

        is WriterUiState.Configuring -> {
            WriterConfigView(
                topicTitle = writerState.topicTitle,
                topicContext = writerState.topicContext,
                category = writerState.category,
                settings = writerSettings,
                onLanguageChange = onLanguageChange,
                onLengthChange = onLengthChange,
                onToneChange = onToneChange,
                onGenerateClick = {
                    onGenerateArticle(
                        writerState.topicTitle,
                        writerState.topicContext,
                        writerState.category
                    )
                },
                onBackToTrends = onGoToDiscover
            )
        }

        is WriterUiState.Researching -> {
            GenerationProgressView(
                topicTitle = writerState.topicTitle,
                stepNumber = 1,
                stepTitle = "Verifying Facts with Live Search",
                detailMessage = writerState.message,
                streamedPreview = null,
                wordCount = 0
            )
        }

        is WriterUiState.Streaming -> {
            GenerationProgressView(
                topicTitle = writerState.topicTitle,
                stepNumber = 2,
                stepTitle = "Writing Human-Quality Long-Form Article",
                detailMessage = "Crafting structured sections, examples, comparison table, and FAQs (${writerState.wordCount} words drafted)...",
                streamedPreview = writerState.streamedText,
                wordCount = writerState.wordCount
            )
        }

        is WriterUiState.GeneratingVisuals -> {
            GenerationProgressView(
                topicTitle = writerState.topicTitle,
                stepNumber = 3,
                stepTitle = "Designing Blogger HTML & Generating Visuals",
                detailMessage = writerState.message,
                streamedPreview = null,
                wordCount = 0
            )
        }

        is WriterUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.widthIn(max = 560.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Article Generation Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = writerState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onGenerateArticle(
                                        writerState.topicTitle,
                                        writerState.topicContext,
                                        writerState.category
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NavyDeep,
                                    contentColor = SurfaceWhite
                                )
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Retry")
                            }
                            OutlinedButton(onClick = onResetToIdle) {
                                Text("Start New")
                            }
                        }
                    }
                }
            }
        }

        is WriterUiState.Ready -> {
            CompletedArticleStudio(
                state = writerState,
                onRegenerateImage = onRegenerateImage,
                onNewArticle = onGoToDiscover,
                onNotify = onNotify
            )
        }
    }
}

@Composable
private fun CustomTopicStartView(
    onStartCustomTopic: (String, String) -> Unit,
    onGoToDiscover: () -> Unit
) {
    var customTopic by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TrendCategory.TECH_AI) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Article Writer Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select a live trending topic from Discover, or enter any topic below to launch grounded research and long-form Blogger HTML generation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = customTopic,
                    onValueChange = { customTopic = it },
                    label = { Text("Enter blog topic or headline") },
                    placeholder = { Text("e.g. Why Small Businesses Are Adopting Local AI Models") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_custom_topic_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrendCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyDeep,
                                selectedLabelColor = SurfaceWhite
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (customTopic.isNotBlank()) {
                                onStartCustomTopic(customTopic, selectedCategory.label)
                            }
                        },
                        enabled = customTopic.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyDeep,
                            contentColor = SurfaceWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Configure Writer")
                    }
                    OutlinedButton(
                        onClick = onGoToDiscover,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Browse 20 Trends")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WriterConfigView(
    topicTitle: String,
    topicContext: String,
    category: String,
    settings: WriterSettings,
    onLanguageChange: (ArticleLanguage) -> Unit,
    onLengthChange: (ArticleLength) -> Unit,
    onToneChange: (ArticleTone) -> Unit,
    onGenerateClick: () -> Unit,
    onBackToTrends: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selected Topic Header
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = NavyDeep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = ElectricSky.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = category.uppercase(),
                            color = ElectricSky,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = topicTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = SurfaceWhite
                    )
                    if (topicContext.isNotBlank()) {
                        Text(
                            text = topicContext,
                            style = MaterialTheme.typography.bodySmall,
                            color = SurfaceWhite.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            // Settings Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Language
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Article Language", style = MaterialTheme.typography.titleSmall)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ArticleLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = settings.language == lang,
                                    onClick = { onLanguageChange(lang) },
                                    label = { Text(lang.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NavyDeep,
                                        selectedLabelColor = SurfaceWhite
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("lang_chip_${lang.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // Target Word Count
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Article Length", style = MaterialTheme.typography.titleSmall)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ArticleLength.entries.forEach { len ->
                                FilterChip(
                                    selected = settings.length == len,
                                    onClick = { onLengthChange(len) },
                                    label = { Text("${len.label} (${len.readTime})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NavyDeep,
                                        selectedLabelColor = SurfaceWhite
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("length_chip_${len.wordCount}")
                                )
                            }
                        }
                    }

                    // Tone
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Writing Tone", style = MaterialTheme.typography.titleSmall)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ArticleTone.entries.forEach { tone ->
                                FilterChip(
                                    selected = settings.tone == tone,
                                    onClick = { onToneChange(tone) },
                                    label = { Text(tone.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NavyDeep,
                                        selectedLabelColor = SurfaceWhite
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("tone_chip_${tone.name.lowercase()}")
                                )
                            }
                        }
                        Text(
                            text = "Style: ${settings.tone.description} • Audience: ${settings.audience}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onGenerateClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyDeep,
                            contentColor = SurfaceWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("generate_article_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Ready-to-Publish Article")
                    }

                    OutlinedButton(
                        onClick = onBackToTrends,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Change Topic")
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerationProgressView(
    topicTitle: String,
    stepNumber: Int,
    stepTitle: String,
    detailMessage: String,
    streamedPreview: String?,
    wordCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(color = SkyTint, shape = RoundedCornerShape(999.dp)) {
                        Text(
                            text = "STEP $stepNumber OF 3",
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyDeep,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (wordCount > 0) {
                        Text(
                            text = "$wordCount words written",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSuccess
                        )
                    }
                }

                Text(
                    text = stepTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = topicTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = ElectricSapphire
                )
                LinearProgressIndicator(
                    progress = { stepNumber / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = ElectricSapphire
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ElectricSapphire
                    )
                    Text(
                        text = detailMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!streamedPreview.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Live Draft Stream",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = streamedPreview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedArticleStudio(
    state: WriterUiState.Ready,
    onRegenerateImage: (isHero: Boolean) -> Unit,
    onNewArticle: () -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val article = state.article
    val resolvedHtml = remember(article) { article.resolvedBloggerHtml() }
    val downloadHtml = remember(article) { article.standaloneDownloadHtml() }
    var selectedTab by remember { mutableIntStateOf(0) }

    val safeFileSlug = remember(article.seoTitle) {
        article.seoTitle.lowercase()
            .replace(Regex("""[^a-z0-9]+"""), "-")
            .trim('-')
            .take(45)
            .ifBlank { "slip-blog-article" }
    }

    val createHtmlDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(downloadHtml.toByteArray(Charsets.UTF_8))
                }
                onNotify("Downloaded $safeFileSlug.html")
            } catch (e: Exception) {
                onNotify("Download failed: ${e.localizedMessage}")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Compact Top Summary & 5 Export Actions
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = article.seoTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${article.actualWordCount} words • ${article.language} • ${article.tone}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onNewArticle,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("New Topic", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // 5 Required Export Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportActionButton(
                        icon = Icons.Rounded.Code,
                        label = "Copy HTML",
                        isPrimary = true,
                        testTag = "copy_html_button",
                        onClick = {
                            copyToClipboard(context, "Blogger HTML", resolvedHtml)
                            onNotify("Complete Blogger HTML copied!")
                        }
                    )
                    ExportActionButton(
                        icon = Icons.Rounded.Download,
                        label = "Download .html",
                        isPrimary = false,
                        testTag = "download_html_button",
                        onClick = {
                            createHtmlDocumentLauncher.launch("$safeFileSlug.html")
                        }
                    )
                    ExportActionButton(
                        icon = Icons.Rounded.Description,
                        label = "Copy Text",
                        isPrimary = false,
                        testTag = "copy_text_button",
                        onClick = {
                            copyToClipboard(context, "Article Text", article.plainTextContent)
                            onNotify("Plain text article copied!")
                        }
                    )
                    ExportActionButton(
                        icon = Icons.Rounded.Title,
                        label = "Copy Title",
                        isPrimary = false,
                        testTag = "copy_title_button",
                        onClick = {
                            copyToClipboard(context, "Article Title", article.seoTitle)
                            onNotify("Article title copied!")
                        }
                    )
                    ExportActionButton(
                        icon = Icons.Rounded.ContentCopy,
                        label = "Copy Meta",
                        isPrimary = false,
                        testTag = "copy_meta_button",
                        onClick = {
                            copyToClipboard(context, "Meta Description", article.metaDescription)
                            onNotify("Meta description copied!")
                        }
                    )
                }
            }
        }

        val studioTabs = listOf("Preview", "HTML", "SEO & Text", "Visuals")
        TabRow(selectedTabIndex = selectedTab) {
            studioTabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label, maxLines = 1) },
                    modifier = Modifier.testTag("studio_tab_$idx")
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> HtmlPreviewPane(htmlContent = resolvedHtml)
                1 -> HtmlCodePane(
                    htmlContent = resolvedHtml,
                    onCopyClick = {
                        copyToClipboard(context, "Blogger HTML", resolvedHtml)
                        onNotify("Complete Blogger HTML copied!")
                    }
                )
                2 -> SeoAndPlainTextPane(article = article)
                3 -> VisualAssetsPane(
                    state = state,
                    onRegenerateHero = { onRegenerateImage(true) },
                    onRegenerateInline = { onRegenerateImage(false) }
                )
            }
        }
    }
}

@Composable
private fun ExportActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPrimary: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyDeep,
                contentColor = SurfaceWhite
            ),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HtmlPreviewPane(htmlContent: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
            }
        },
        update = { webView ->
            val wrapped = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                </head>
                <body style="margin:0;padding:0;background:#ffffff;">
                  $htmlContent
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, wrapped, "text/html", "UTF-8", null)
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("blogger_html_webview")
    )
}

@Composable
private fun HtmlCodePane(
    htmlContent: String,
    onCopyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blogger-Compatible HTML Output",
                style = MaterialTheme.typography.titleSmall
            )
            Button(
                onClick = onCopyClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyDeep,
                    contentColor = SurfaceWhite
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy All")
            }
        }
        Surface(
            color = NavyDeep,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SelectionContainer {
                Text(
                    text = htmlContent,
                    color = ElectricSky,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SeoAndPlainTextPane(article: BlogArticle) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("SEO Title", style = MaterialTheme.typography.labelMedium, color = ElectricSapphire)
                SelectionContainer {
                    Text(article.seoTitle, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Meta Description (${article.metaDescription.length} chars)",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricSapphire
                )
                SelectionContainer {
                    Text(article.metaDescription, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (article.sources.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Verified Web Sources", style = MaterialTheme.typography.titleSmall)
                    article.sources.forEach { raw ->
                        val parts = raw.split(" | ", limit = 2)
                        val title = parts.getOrNull(0) ?: raw
                        val url = parts.getOrNull(1)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !url.isNullOrBlank()) {
                                    if (!url.isNullOrBlank()) {
                                        try { uriHandler.openUri(url) } catch (_: Exception) {}
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Link,
                                contentDescription = null,
                                tint = ElectricSapphire,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricSapphire
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Plain Text Article", style = MaterialTheme.typography.titleSmall)
                SelectionContainer {
                    Text(
                        text = article.plainTextContent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualAssetsPane(
    state: WriterUiState.Ready,
    onRegenerateHero: () -> Unit,
    onRegenerateInline: () -> Unit
) {
    val article = state.article
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!state.imageWarning.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.imageWarning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        ArticleImageCard(
            label = "Hero Cover Visual",
            base64 = article.heroImageBase64,
            altText = article.heroImageAlt,
            caption = article.heroImageCaption,
            isLoading = state.isRegeneratingHero,
            onRegenerate = onRegenerateHero
        )

        ArticleImageCard(
            label = "In-Article Editorial Visual",
            base64 = article.inlineImageBase64,
            altText = article.inlineImageAlt,
            caption = article.inlineImageCaption,
            isLoading = state.isRegeneratingInline,
            onRegenerate = onRegenerateInline
        )
    }
}

@Composable
private fun ArticleImageCard(
    label: String,
    base64: String,
    altText: String,
    caption: String,
    isLoading: Boolean,
    onRegenerate: () -> Unit
) {
    val bitmap = remember(base64) { decodeBase64Bitmap(base64) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                OutlinedButton(
                    onClick = onRegenerate,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (bitmap == null) "Generate Image" else "Regenerate")
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = altText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isLoading) "Synthesizing AI visual (30–60s)..." else "Image not generated yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "ALT: $altText",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricSapphire
            )
            Text(
                text = "Caption: $caption",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun decodeBase64Bitmap(base64: String): Bitmap? {
    if (base64.isBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
