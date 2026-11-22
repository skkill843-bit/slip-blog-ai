package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GroundedSource
import com.example.data.model.TrendCategory
import com.example.data.model.TrendingTopic
import com.example.ui.TrendingUiState
import com.example.ui.theme.ElectricSapphire
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SkyTint
import com.example.ui.theme.SurfaceWhite

@Composable
fun TrendingDiscoverScreen(
    selectedCategory: TrendCategory,
    searchKeyword: String,
    trendingState: TrendingUiState,
    onCategorySelected: (TrendCategory) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onDiscoverClick: () -> Unit,
    onSelectTopic: (TrendingTopic) -> Unit,
    onWriteCustomTopic: (String, String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            modifier = Modifier
                .widthIn(max = 1040.dp)
                .fillMaxSize()
                .testTag("trending_topics_grid"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TrendSearchAndFilterCard(
                    selectedCategory = selectedCategory,
                    searchKeyword = searchKeyword,
                    isLoading = trendingState is TrendingUiState.Loading,
                    onCategorySelected = onCategorySelected,
                    onKeywordChanged = onKeywordChanged,
                    onDiscoverClick = {
                        keyboardController?.hide()
                        onDiscoverClick()
                    },
                    onWriteCustomClick = {
                        keyboardController?.hide()
                        onWriteCustomTopic(searchKeyword, selectedCategory.label)
                    }
                )
            }

            when (trendingState) {
                is TrendingUiState.Idle -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyTrendsPromptCard(onDiscoverClick = onDiscoverClick)
                    }
                }

                is TrendingUiState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = ElectricSapphire)
                                Text(
                                    text = "Scanning live Google Search trends...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Identifying 20 real, active stories in ${selectedCategory.label}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is TrendingUiState.Error -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Live Trend Search Error",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = trendingState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Button(
                                    onClick = onDiscoverClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NavyDeep,
                                        contentColor = SurfaceWhite
                                    ),
                                    modifier = Modifier.testTag("retry_trends_button")
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Retry Live Search")
                                }
                            }
                        }
                    }
                }

                is TrendingUiState.Success -> {
                    val result = trendingState.result
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GroundingAttributionCard(
                            topicCount = result.topics.size,
                            categoryLabel = result.categoryLabel,
                            queries = result.webSearchQueries,
                            sources = result.groundedSources,
                            onSourceClick = { url ->
                                try {
                                    uriHandler.openUri(url)
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }

                    items(result.topics, key = { it.id }) { topic ->
                        TrendingTopicGridCard(
                            topic = topic,
                            onWriteClick = { onSelectTopic(topic) }
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TrendSearchAndFilterCard(
    selectedCategory: TrendCategory,
    searchKeyword: String,
    isLoading: Boolean,
    onCategorySelected: (TrendCategory) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onDiscoverClick: () -> Unit,
    onWriteCustomClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Trend Discovery",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Grounded in real-time Google Search • Top 20 topics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = SkyTint,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Public,
                            contentDescription = "Live web search",
                            tint = ElectricSapphire,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "LIVE WEB",
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyDeep
                        )
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TrendCategory.entries) { category ->
                    val selected = category == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { onCategorySelected(category) },
                        label = {
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyDeep,
                            selectedLabelColor = SurfaceWhite
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("category_chip_${category.name.lowercase()}")
                    )
                }
            }

            OutlinedTextField(
                value = searchKeyword,
                onValueChange = onKeywordChanged,
                placeholder = {
                    Text("Search trending keywords (e.g. AI agents, EV India)")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search keyword"
                    )
                },
                trailingIcon = {
                    if (searchKeyword.isNotBlank()) {
                        IconButton(onClick = { onKeywordChanged("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search keyword"
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onDiscoverClick() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricSapphire,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trend_keyword_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDiscoverClick,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyDeep,
                        contentColor = SurfaceWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("discover_trends_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Find 20 Live Trends")
                }

                AnimatedVisibility(visible = searchKeyword.isNotBlank()) {
                    OutlinedButton(
                        onClick = onWriteCustomClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("write_custom_topic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Write Now")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroundingAttributionCard(
    topicCount: Int,
    categoryLabel: String,
    queries: List<String>,
    sources: List<GroundedSource>,
    onSourceClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SkyTint.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ElectricSapphire.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = "Google Search Grounding",
                    tint = ElectricSapphire,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$topicCount Verified Trends • $categoryLabel",
                    style = MaterialTheme.typography.titleSmall,
                    color = NavyDeep
                )
            }

            if (queries.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    queries.take(4).forEach { q ->
                        Surface(
                            color = SurfaceWhite,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, ElectricSapphire.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "Search: $q",
                                style = MaterialTheme.typography.labelSmall,
                                color = NavyDeep,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (sources.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sources.take(6).forEach { src ->
                        Surface(
                            color = NavyDeep,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onSourceClick(src.url) }
                        ) {
                            Text(
                                text = src.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = SurfaceWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = 180.dp)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingTopicGridCard(
    topic: TrendingTopic,
    onWriteClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onWriteClick)
            .testTag("trending_topic_card_${topic.rank}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NavyDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", topic.rank),
                        style = MaterialTheme.typography.labelSmall,
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (topic.sourceName.isNotBlank()) {
                    Surface(
                        color = SkyTint,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = topic.sourceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyDeep,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = topic.shortExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (topic.whyTrendingNow.isNotBlank()) {
                Text(
                    text = topic.whyTrendingNow,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricSapphire,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onWriteClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyDeep,
                    contentColor = SurfaceWhite
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("write_topic_button_${topic.rank}")
            ) {
                Text("Write Article", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyTrendsPromptCard(onDiscoverClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.TrendingUp,
                contentDescription = null,
                tint = ElectricSapphire,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Ready to Discover Today's Stories",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap Find 20 Live Trends to search the web for breaking stories and emerging reader interests.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onDiscoverClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyDeep,
                    contentColor = SurfaceWhite
                )
            ) {
                Text("Find 20 Live Trends")
            }
        }
    }
}
