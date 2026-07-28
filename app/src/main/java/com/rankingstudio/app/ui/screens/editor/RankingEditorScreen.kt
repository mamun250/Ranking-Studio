package com.rankingstudio.app.ui.screens.editor

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rankingstudio.app.data.remote.TikTokApiService
import com.rankingstudio.app.domain.model.HeaderConfig
import com.rankingstudio.app.domain.model.RankingProject
import com.rankingstudio.app.domain.model.RankingSidebarItem
import com.rankingstudio.app.domain.model.VideoClip
import com.rankingstudio.app.ui.components.PapercraftButton
import com.rankingstudio.app.ui.components.PapercraftCard
import com.rankingstudio.app.ui.screens.importdialog.TikTokImportDialog
import com.rankingstudio.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingEditorScreen(
    projectId: String,
    viewModel: RankingEditorViewModel,
    apiService: TikTokApiService,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val activeRankIndex by viewModel.activeRankIndex.collectAsState()

    var showTikTokImportDialog by remember { mutableStateOf(false) }
    var showHeaderEditSheet by remember { mutableStateOf(false) }
    var showSidebarEditSheet by remember { mutableStateOf(false) }
    var editingRankIndex by remember { mutableStateOf(1) }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Update ExoPlayer items when project clips change
    LaunchedEffect(project?.clips) {
        project?.clips?.let { clips ->
            exoPlayer.clearMediaItems()
            clips.forEach { clip ->
                exoPlayer.addMediaItem(MediaItem.fromUri(clip.videoUri))
            }
            if (clips.isNotEmpty()) {
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = project?.name ?: "Ranking Editor",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkCharcoal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = InkCharcoal)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = InkCharcoal)
                    }
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = InkCharcoal)
                    }
                    IconButton(onClick = { showTikTokImportDialog = true }) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Import TikTok", tint = Terracotta)
                    }
                    IconButton(onClick = { /* Trigger Export */ }) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export Video", tint = PrimarySandishBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfacePaper)
            )
        },
        containerColor = SurfacePaper
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Center Area: 1080x1920 Aspect Ratio Vertical Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
                    .background(Color.Black, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Video Render Layer
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Compose Overlays: Header, Ranking Sidebar, Progress Bar, Watermark
                project?.let { currentProject ->
                    // 1. Header Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .clickable { showHeaderEditSheet = true },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentProject.headerConfig.line1,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = currentProject.headerConfig.line2,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Terracotta,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = currentProject.headerConfig.line3,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // 2. Ranking Sidebar Overlay (Left Side)
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentProject.rankingItems.forEach { item ->
                            val isActive = item.rankIndex == activeRankIndex
                            val scaleAnim by animateFloatAsState(
                                targetValue = if (isActive) 1.15f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "scale"
                            )

                            Row(
                                modifier = Modifier
                                    .scale(scaleAnim)
                                    .background(
                                        color = if (isActive) SecondaryContainer else PaperWhite.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isActive) 2.dp else 1.dp,
                                        color = if (isActive) SecondaryOliveGreen else OutlineBrown,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        editingRankIndex = item.rankIndex
                                        showSidebarEditSheet = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "#${item.rankIndex}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) SecondaryOliveGreen else InkCharcoal,
                                    fontSize = 14.sp
                                )
                                Text(text = item.emoji, fontSize = 14.sp)
                                Text(
                                    text = item.title,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = InkCharcoal,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Timeline Toolbar & Clip List (1-7 clips)
            PapercraftCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                backgroundColor = PaperWhite
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Timeline (${project?.clips?.size ?: 0}/7 clips)",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkCharcoal
                    )

                    Row {
                        IconButton(onClick = { showTikTokImportDialog = true }) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "TikTok Import", tint = Terracotta)
                        }
                        IconButton(onClick = {
                            // Add mock sample clip to timeline
                            viewModel.addClipToTimeline("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
                        }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Clip", tint = PrimarySandishBrown)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(project?.clips ?: emptyList()) { index, clip ->
                        TimelineClipCard(
                            clip = clip,
                            rankIndex = index + 1,
                            isActiveRank = (index + 1) == activeRankIndex,
                            onDelete = { viewModel.removeClip(clip.id) }
                        )
                    }
                }
            }
        }
    }

    // TikTok Import Dialog
    if (showTikTokImportDialog) {
        TikTokImportDialog(
            apiService = apiService,
            onDismiss = { showTikTokImportDialog = false },
            onVideoDownloaded = { videoUrl ->
                viewModel.addClipToTimeline(videoUrl)
            }
        )
    }
}

@Composable
fun TimelineClipCard(
    clip: VideoClip,
    rankIndex: Int,
    isActiveRank: Boolean,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(70.dp)
            .background(
                color = if (isActiveRank) SecondaryContainer else CardboardTan,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isActiveRank) 2.dp else 1.dp,
                color = if (isActiveRank) SecondaryOliveGreen else OutlineBrown,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$rankIndex",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkCharcoal
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = ErrorRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = "Clip $rankIndex",
                style = MaterialTheme.typography.labelMedium,
                color = InkCharcoal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
