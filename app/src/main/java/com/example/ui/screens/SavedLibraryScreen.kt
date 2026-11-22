package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BlogArticle
import com.example.ui.UiState
import com.example.ui.theme.ElectricSapphire
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SkyTint
import com.example.ui.theme.SurfaceWhite
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SavedLibraryScreen(
    articlesState: UiState<List<BlogArticle>>,
    onOpenArticle: (BlogArticle) -> Unit,
    onDeleteArticle: (String) -> Unit,
    onGoToDiscover: () -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<BlogArticle?>(null) }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Article?") },
            text = {
                Text("Remove \"${target.seoTitle}\" permanently from your library?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteArticle(target.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (articlesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricSapphire)
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = articlesState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                val articles = articlesState.data
                val filtered = remember(articles, filterQuery) {
                    if (filterQuery.isBlank()) articles
                    else articles.filter {
                        it.seoTitle.contains(filterQuery, ignoreCase = true) ||
                            it.category.contains(filterQuery, ignoreCase = true) ||
                            it.topicTitle.contains(filterQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 880.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Saved Article Library",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${articles.size} preserved articles • Ready to preview or export",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (articles.isNotEmpty()) {
                        item {
                            OutlinedTextField(
                                value = filterQuery,
                                onValueChange = { filterQuery = it },
                                placeholder = { Text("Filter saved articles...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("library_search_input")
                            )
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
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
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoStories,
                                        contentDescription = null,
                                        tint = ElectricSapphire,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Text(
                                        text = if (articles.isEmpty()) "No Saved Articles Yet" else "No Matching Articles",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Generate your first trending blog article and it will automatically sync here.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (articles.isEmpty()) {
                                        Button(
                                            onClick = onGoToDiscover,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NavyDeep,
                                                contentColor = SurfaceWhite
                                            )
                                        ) {
                                            Text("Explore Trending Topics")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { article ->
                            SavedArticleCard(
                                article = article,
                                onOpen = { onOpenArticle(article) },
                                onDelete = { pendingDelete = article }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedArticleCard(
    article: BlogArticle,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbBitmap = remember(article.heroImageBase64) {
        if (article.heroImageBase64.isBlank()) null
        else try {
            val bytes = Base64.decode(article.heroImageBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
    val dateLabel = remember(article.updatedAt, article.createdAt) {
        val ts = article.updatedAt ?: article.createdAt ?: Timestamp.now()
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(ts.toDate())
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .testTag("saved_article_card_${article.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (thumbBitmap != null) {
                Image(
                    bitmap = thumbBitmap.asImageBitmap(),
                    contentDescription = article.heroImageAlt,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = SkyTint, shape = RoundedCornerShape(999.dp)) {
                        Text(
                            text = article.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyDeep,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${article.actualWordCount} words • $dateLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = article.seoTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = article.metaDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_article_${article.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete article",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
