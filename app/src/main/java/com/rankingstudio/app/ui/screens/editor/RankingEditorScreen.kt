package com.rankingstudio.app.ui.screens.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.rankingstudio.app.exporter.VideoExporter
import com.rankingstudio.app.ui.components.PapercraftButton
import com.rankingstudio.app.ui.components.PapercraftCard
import com.rankingstudio.app.ui.screens.importdialog.TikTokImportDialog
import com.rankingstudio.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingEditorScreen(
    projectId: String,
    viewModel: RankingEditorViewModel,
    apiService: TikTokApiService,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val project by viewModel.project.collectAsState()
    val activeRankIndex by viewModel.activeRankIndex.collectAsState()

    var showAddClipOptionsDialog by remember { mutableStateOf(false) }
    var showTikTokImportDialog by remember { mutableStateOf(false) }
    var showHeaderEditSheet by remember { mutableStateOf(false) }
    var showSidebarEditSheet by remember { mutableStateOf(false) }
    var editingRankIndex by remember { mutableStateOf(1) }

    var selectedClipForTrim by remember { mutableStateOf<VideoClip?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf(false) }
    var exportedFilePath by remember { mutableStateOf("") }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

    // Media Pickers for Phone Gallery Video Import
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val (filePath, durationMs) = processSelectedVideoUri(context, it)
                withContext(Dispatchers.Main) {
                    viewModel.addClipToTimeline(filePath, durationMs)
                }
            }
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val (filePath, durationMs) = processSelectedVideoUri(context, it)
                withContext(Dispatchers.Main) {
                    viewModel.addClipToTimeline(filePath, durationMs)
                }
            }
        }
    }

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
                    IconButton(onClick = { showAddClipOptionsDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Clip", tint = PrimarySandishBrown)
                    }
                    IconButton(onClick = {
                        val currentProj = project
                        if (currentProj == null || currentProj.clips.isEmpty()) {
                            exportErrorMessage = "Please add at least 1 video clip before exporting."
                            showExportDialog = true
                            isExporting = false
                            exportSuccess = false
                        } else {
                            showExportDialog = true
                            isExporting = true
                            exportSuccess = false
                            exportErrorMessage = null

                            scope.launch {
                                val outFile = VideoExporter.getExportOutputFile(context)
                                val success = VideoExporter.exportProjectVideo(
                                    context = context,
                                    project = currentProj,
                                    fps = 30,
                                    outputFile = outFile,
                                    onProgress = {}
                                )
                                isExporting = false
                                if (success) {
                                    exportSuccess = true
                                    exportedFilePath = outFile.absolutePath
                                } else {
                                    exportErrorMessage = "FFmpeg export failed to render MP4 video."
                                }
                            }
                        }
                    }) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export Video", tint = Terracotta)
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
                                color = parseHexColor(currentProject.headerConfig.fontColorHex, Terracotta),
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

                            val itemBgColor = parseHexColor(item.backgroundColorHex, PaperWhite.copy(alpha = 0.9f))
                            val itemTextColor = parseHexColor(item.fontColorHex, InkCharcoal)

                            Row(
                                modifier = Modifier
                                    .scale(scaleAnim)
                                    .background(
                                        color = if (isActive) SecondaryContainer else itemBgColor,
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
                                    color = if (isActive) SecondaryOliveGreen else itemTextColor,
                                    fontSize = 14.sp
                                )
                                Text(text = item.emoji, fontSize = 14.sp)
                                Text(
                                    text = item.title,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = itemTextColor,
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
                        IconButton(onClick = { showAddClipOptionsDialog = true }) {
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
                            onClick = { selectedClipForTrim = clip },
                            onDelete = { viewModel.removeClip(clip.id) }
                        )
                    }
                }
            }
        }
    }

    // Header Customization Dialog
    if (showHeaderEditSheet && project != null) {
        val currentHeader = project!!.headerConfig
        HeaderEditDialog(
            headerConfig = currentHeader,
            onDismiss = { showHeaderEditSheet = false },
            onSave = { updatedConfig ->
                viewModel.updateHeaderConfig(updatedConfig)
                showHeaderEditSheet = false
            }
        )
    }

    // Sidebar Item Customization Dialog
    if (showSidebarEditSheet && project != null) {
        val selectedItem = project!!.rankingItems.find { it.rankIndex == editingRankIndex }
        if (selectedItem != null) {
            SidebarItemEditDialog(
                rankingItem = selectedItem,
                onDismiss = { showSidebarEditSheet = false },
                onSave = { rankIndex, newTitle, newEmoji, fontColorHex, backgroundColorHex ->
                    viewModel.updateRankingSidebarItemFull(rankIndex, newTitle, newEmoji, fontColorHex, backgroundColorHex)
                    showSidebarEditSheet = false
                }
            )
        }
    }

    // Clip Trim Dialog
    selectedClipForTrim?.let { clip ->
        ClipTrimDialog(
            clip = clip,
            onDismiss = { selectedClipForTrim = null },
            onSaveTrim = { startMs, endMs ->
                viewModel.updateClipTrim(clip.id, startMs, endMs)
                selectedClipForTrim = null
            }
        )
    }

    // Add Clip Options Dialog (Phone Gallery vs TikTok Link)
    if (showAddClipOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showAddClipOptionsDialog = false },
            title = { Text("Add Video Clip", color = InkCharcoal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showAddClipOptionsDialog = false
                            try {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            } catch (e: Exception) {
                                getContentLauncher.launch("video/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null, tint = PrimarySandishBrown)
                            Text("📱 Select from Phone Gallery", color = InkCharcoal)
                        }
                    }

                    TextButton(
                        onClick = {
                            showAddClipOptionsDialog = false
                            showTikTokImportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Terracotta)
                            Text("🎵 Import via TikTok Link", color = InkCharcoal)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddClipOptionsDialog = false }) {
                    Text("Cancel", color = InkCharcoal)
                }
            }
        )
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

    // Video Export Status Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { if (!isExporting) showExportDialog = false },
            title = { Text(if (exportSuccess) "Export Successful! 🎉" else if (isExporting) "Exporting MP4 Video..." else "Export Error", color = InkCharcoal) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(color = Terracotta)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Rendering 1080x1920 video with FFmpeg...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkCharcoal
                        )
                    } else if (exportSuccess) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SecondaryOliveGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Saved to Phone Gallery / Movies / RankingStudio folder:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkCharcoal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exportedFilePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = exportErrorMessage ?: "Failed to export video.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (!isExporting) {
                    Button(
                        onClick = { showExportDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySandishBrown)
                    ) {
                        Text("Done")
                    }
                }
            }
        )
    }
}

@Composable
fun HeaderEditDialog(
    headerConfig: HeaderConfig,
    onDismiss: () -> Unit,
    onSave: (HeaderConfig) -> Unit
) {
    var line1 by remember { mutableStateOf(headerConfig.line1) }
    var line2 by remember { mutableStateOf(headerConfig.line2) }
    var line3 by remember { mutableStateOf(headerConfig.line3) }
    var fontColorHex by remember { mutableStateOf(headerConfig.fontColorHex) }

    val colors = listOf("#C15C3D", "#F5A623", "#4A6B5D", "#2B7A78", "#2B2B2A", "#FFFFFF")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Header Text & Colors", color = InkCharcoal) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = line1,
                    onValueChange = { line1 = it },
                    label = { Text("Line 1 (Top)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = line2,
                    onValueChange = { line2 = it },
                    label = { Text("Line 2 (Highlight)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = line3,
                    onValueChange = { line3 = it },
                    label = { Text("Line 3 (Bottom)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Line 2 Highlight Color:", style = MaterialTheme.typography.labelLarge, color = InkCharcoal)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(parseHexColor(hex, Color.Gray), shape = CircleShape)
                                .border(
                                    width = if (fontColorHex == hex) 3.dp else 1.dp,
                                    color = if (fontColorHex == hex) InkCharcoal else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { fontColorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(HeaderConfig(line1, line2, line3, fontColorHex)) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySandishBrown)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = InkCharcoal)
            }
        }
    )
}

@Composable
fun SidebarItemEditDialog(
    rankingItem: RankingSidebarItem,
    onDismiss: () -> Unit,
    onSave: (rankIndex: Int, title: String, emoji: String, fontColorHex: String, backgroundColorHex: String) -> Unit
) {
    var title by remember { mutableStateOf(rankingItem.title) }
    var emoji by remember { mutableStateOf(rankingItem.emoji) }
    var fontColorHex by remember { mutableStateOf(rankingItem.fontColorHex) }
    var backgroundColorHex by remember { mutableStateOf(rankingItem.backgroundColorHex) }

    val emojis = listOf("👑", "🥈", "🥉", "🔥", "⭐", "⚡", "🎯", "💎", "🏆", "🚀")
    val fontColors = listOf("#2B2B2A", "#C15C3D", "#4A6B5D", "#FFFFFF")
    val bgColors = listOf("#FCFAF2", "#EFEAD8", "#E3E8E1", "#F9EBE7", "#FFFFFF")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Rank #${rankingItem.rankIndex}", color = InkCharcoal) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Rank Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Choose Emoji:", style = MaterialTheme.typography.labelLarge, color = InkCharcoal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emojis.take(5).forEach { em ->
                        Button(
                            onClick = { emoji = em },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (emoji == em) SecondaryContainer else SurfacePaper
                            ),
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(em, fontSize = 18.sp)
                        }
                    }
                }

                Text("Text Color:", style = MaterialTheme.typography.labelLarge, color = InkCharcoal)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fontColors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(parseHexColor(hex, Color.Black), shape = CircleShape)
                                .border(
                                    width = if (fontColorHex == hex) 3.dp else 1.dp,
                                    color = if (fontColorHex == hex) Terracotta else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { fontColorHex = hex }
                        )
                    }
                }

                Text("Background Card Color:", style = MaterialTheme.typography.labelLarge, color = InkCharcoal)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bgColors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(parseHexColor(hex, Color.White), shape = CircleShape)
                                .border(
                                    width = if (backgroundColorHex == hex) 3.dp else 1.dp,
                                    color = if (backgroundColorHex == hex) PrimarySandishBrown else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { backgroundColorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(rankingItem.rankIndex, title, emoji, fontColorHex, backgroundColorHex) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySandishBrown)
            ) {
                Text("Save Rank")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = InkCharcoal)
            }
        }
    )
}

@Composable
fun ClipTrimDialog(
    clip: VideoClip,
    onDismiss: () -> Unit,
    onSaveTrim: (startMs: Long, endMs: Long) -> Unit
) {
    val totalDurationSec = (clip.durationMs / 1000f).coerceAtLeast(1f)
    var startSec by remember { mutableStateOf(clip.trimStartMs / 1000f) }
    var endSec by remember { mutableStateOf((clip.trimEndMs / 1000f).coerceIn(startSec + 0.5f, totalDurationSec)) }

    val trimmedDurationSec = (endSec - startSec).coerceAtLeast(0.5f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trim Video Clip", color = InkCharcoal) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Clip Duration: ${String.format(Locale.US, "%.1f", trimmedDurationSec)}s (Original: ${String.format(Locale.US, "%.1f", totalDurationSec)}s)",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimarySandishBrown
                )

                Text("Start Cut Point: ${String.format(Locale.US, "%.1f", startSec)}s", style = MaterialTheme.typography.bodyMedium, color = InkCharcoal)
                Slider(
                    value = startSec,
                    onValueChange = { newStart ->
                        startSec = newStart.coerceIn(0f, endSec - 0.5f)
                    },
                    valueRange = 0f..totalDurationSec,
                    colors = SliderDefaults.colors(thumbColor = Terracotta, activeTrackColor = Terracotta)
                )

                Text("End Cut Point: ${String.format(Locale.US, "%.1f", endSec)}s", style = MaterialTheme.typography.bodyMedium, color = InkCharcoal)
                Slider(
                    value = endSec,
                    onValueChange = { newEnd ->
                        endSec = newEnd.coerceIn(startSec + 0.5f, totalDurationSec)
                    },
                    valueRange = 0f..totalDurationSec,
                    colors = SliderDefaults.colors(thumbColor = PrimarySandishBrown, activeTrackColor = PrimarySandishBrown)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startMs = (startSec * 1000).toLong()
                    val endMs = (endSec * 1000).toLong()
                    onSaveTrim(startMs, endMs)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySandishBrown)
            ) {
                Text("Apply Trim")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = InkCharcoal)
            }
        }
    )
}

/**
 * Copies a selected local video Uri to internal app storage and extracts its duration.
 */
private fun processSelectedVideoUri(context: Context, uri: Uri): Pair<String, Long> {
    val file = File(context.filesDir, "clip_${System.currentTimeMillis()}.mp4")
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    var durationMs = 10000L
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        if (time != null) {
            durationMs = time.toLong()
        }
        retriever.release()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return Pair(file.absolutePath, durationMs)
}

private fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val cleanHex = if (hex.startsWith("#")) hex.substring(1) else hex
        val colorInt = android.graphics.Color.parseColor("#$cleanHex")
        Color(colorInt)
    } catch (e: Exception) {
        defaultColor
    }
}

@Composable
fun TimelineClipCard(
    clip: VideoClip,
    rankIndex: Int,
    isActiveRank: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val durationSec = String.format(Locale.US, "%.1f", (clip.trimEndMs - clip.trimStartMs) / 1000f)

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(75.dp)
            .background(
                color = if (isActiveRank) SecondaryContainer else CardboardTan,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isActiveRank) 2.dp else 1.dp,
                color = if (isActiveRank) SecondaryOliveGreen else OutlineBrown,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
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
                text = "Clip $rankIndex (${durationSec}s)",
                style = MaterialTheme.typography.labelMedium,
                color = InkCharcoal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1
            )
        }
    }
}


