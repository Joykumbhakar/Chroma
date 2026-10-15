package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.GlassIconButton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.chroma.studio.viewmodel.DiscoverViewModel
import com.chroma.studio.data.WorkRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivesScreen(
    userId: String,
    discoverVm: DiscoverViewModel,
    repository: WorkRepository,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val archivedWorks by discoverVm.archivedWorks.collectAsState()
    val isLoading by discoverVm.isLoading.collectAsState()
    val likedWorkIds by discoverVm.likedWorkIds.collectAsState()
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(userId) {
        discoverVm.loadArchivedWorks(userId)
    }

    Scaffold(
        topBar = {
            com.chroma.studio.ui.components.ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "Archives",
                showBackButton = true,
                onBack = onBack
            )
        },
        containerColor = colors.bg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                OscellionLoadingSpinner(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary
                )
            } else if (archivedWorks.isEmpty()) {
                Text(
                    text = "No archived posts.",
                    color = colors.textMuted,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(archivedWorks, key = { it.id }) { work ->
                        FeedPostCard(
                            work = work,
                            isLiked = likedWorkIds.contains(work.id),
                            repository = repository,
                            onRequireLogin = onRequireLogin,
                            onLikeClick = {
                                if (currentUserId != null) discoverVm.toggleLike(work.id, currentUserId) else onRequireLogin()
                            },
                            onAuthorClick = { /* Do nothing here */ },
                            onDownloadClick = { /* Optional */ },
                            onRemixClick = { /* Optional */ },
                            onDeleteWork = { workId -> discoverVm.deleteWork(workId) },
                            onArchiveWork = { workId ->
                                // It's already archived, so clicking it will unarchive
                                discoverVm.unarchiveWork(workId, userId)
                            },
                            onEditWork = { workId, title, desc -> discoverVm.updateWork(workId, title, desc) },
                            onNavigateToOriginal = { _, _ -> }
                        )
                    }
                }
            }
        }
    }
}
