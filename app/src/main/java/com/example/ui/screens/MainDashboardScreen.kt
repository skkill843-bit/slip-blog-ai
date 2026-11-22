package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.auth.signOutUser
import com.example.data.ai.SlipBlogAiService
import com.example.data.firestore.ArticleRepository
import com.example.ui.SlipBlogViewModel
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SurfaceWhite
import com.google.firebase.firestore.FirebaseFirestore

private enum class DashboardTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DISCOVER("Discover", Icons.Rounded.TrendingUp, Icons.Outlined.TrendingUp),
    STUDIO("Studio", Icons.Rounded.Create, Icons.Outlined.Create),
    LIBRARY("Library", Icons.Rounded.AutoStories, Icons.Outlined.AutoStories)
}

@Composable
fun MainDashboardScreen(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: SlipBlogViewModel = viewModel(
        key = currentUserId,
        factory = SlipBlogViewModel.provideFactory(
            repository = ArticleRepository(
                FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
            ),
            aiService = SlipBlogAiService(),
            currentUserId = currentUserId
        )
    )

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val trendingState by viewModel.trendingState.collectAsStateWithLifecycle()
    val writerSettings by viewModel.writerSettings.collectAsStateWithLifecycle()
    val writerState by viewModel.writerState.collectAsStateWithLifecycle()
    val savedArticlesState by viewModel.savedArticlesState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(DashboardTab.DISCOVER) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 640.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                Surface(
                    color = NavyDeep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_top_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_art_1795365017496),
                                contentDescription = "SLIP BLOG AI logo",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SurfaceWhite,
                                    modifier = Modifier.testTag("dashboard_app_name")
                                )
                                Text(
                                    text = "Trending Topics. Original Stories. Ready to Publish.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SurfaceWhite.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Sign out ${userEmail ?: ""}",
                                tint = SurfaceWhite
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        DashboardTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.weight(0.1f))
                        DashboardTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    HorizontalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        DashboardTab.DISCOVER -> {
                            TrendingDiscoverScreen(
                                selectedCategory = selectedCategory,
                                searchKeyword = searchKeyword,
                                trendingState = trendingState,
                                onCategorySelected = { cat ->
                                    viewModel.updateCategory(cat)
                                    viewModel.discoverTrends()
                                },
                                onKeywordChanged = viewModel::updateSearchKeyword,
                                onDiscoverClick = viewModel::discoverTrends,
                                onSelectTopic = { topic ->
                                    viewModel.selectTopicForWriter(topic)
                                    currentTab = DashboardTab.STUDIO
                                },
                                onWriteCustomTopic = { kw, catLabel ->
                                    viewModel.startCustomTopicInWriter(kw, catLabel)
                                    currentTab = DashboardTab.STUDIO
                                }
                            )
                        }

                        DashboardTab.STUDIO -> {
                            ArticleStudioScreen(
                                writerState = writerState,
                                writerSettings = writerSettings,
                                onLanguageChange = viewModel::updateLanguage,
                                onLengthChange = viewModel::updateLength,
                                onToneChange = viewModel::updateTone,
                                onStartCustomTopic = { kw, cat ->
                                    viewModel.startCustomTopicInWriter(kw, cat)
                                },
                                onGenerateArticle = viewModel::generateCompleteArticle,
                                onRegenerateImage = viewModel::regenerateImage,
                                onResetToIdle = viewModel::resetWriterToIdle,
                                onGoToDiscover = { currentTab = DashboardTab.DISCOVER },
                                onNotify = viewModel::showMessage
                            )
                        }

                        DashboardTab.LIBRARY -> {
                            SavedLibraryScreen(
                                articlesState = savedArticlesState,
                                onOpenArticle = { article ->
                                    viewModel.openSavedArticle(article)
                                    currentTab = DashboardTab.STUDIO
                                },
                                onDeleteArticle = viewModel::deleteSavedArticle,
                                onGoToDiscover = { currentTab = DashboardTab.DISCOVER }
                            )
                        }
                    }
                }
            }
        }
    }
}
