package com.rankingstudio.app.ui.screens.editor.timeline

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rankingstudio.app.domain.model.AudioTrackItem
import com.rankingstudio.app.domain.model.RankingProject
import com.rankingstudio.app.domain.model.TextTrackItem
import com.rankingstudio.app.domain.model.VideoClip
import com.rankingstudio.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun NleTimelineEngine(
    project: RankingProject,
    currentPlaybackTimeMs: Long,
    isPlaying: Boolean,
    onSeekToTime: (Long) -> Unit,
    onSelectClip: (VideoClip?) -> Unit,
    onUpdateClipTrim: (clipId: String, startMs: Long, endMs: Long) -> Unit,
    onSplitClip: (clipId: String, splitTimeMs: Long) -> Unit,
    onDeleteClip: (clipId: String) -> Unit,
    onDuplicateClip: (clipId: String) -> Unit,
    onAddAudioTrack: () -> Unit,
    onAddTextTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var scalePxPerSecond by remember { mutableFloatStateOf(60f) } // pixels per second
    var selectedClipId by remember { mutableStateOf<String?>(null) }
    var selectedAudioId by remember { mutableStateOf<String?>(null) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var isSnappingEnabled by remember { mutableStateOf(true) }

    // Calculate total project duration
    val totalVideoDurationMs = remember(project.clips) {
        project.clips.sumOf { (it.trimEndMs - it.trimStartMs).coerceAtLeast(1000L) }
    }
    val maxTimelineMs = (totalVideoDurationMs + 5000L).coerceAtLeast(10000L)

    // Calculate clip timeline start positions
    val clipStartPositionsMs = remember(project.clips) {
        var acc = 0L
        project.clips.map { clip ->
            val start = acc
            val dur = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(1000L)
            acc += dur
            start to (start + dur)
        }
    }

    val activeClip = remember(selectedClipId, project.clips) {
        project.clips.find { it.id == selectedClipId }
    }

    // Auto-select clip when playhead moves if no clip is explicitly selected
    LaunchedEffect(currentPlaybackTimeMs) {
        if (selectedClipId == null && project.clips.isNotEmpty()) {
            var acc = 0L
            for (clip in project.clips) {
                val clipDur = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(1000L)
                if (currentPlaybackTimeMs >= acc && currentPlaybackTimeMs < acc + clipDur) {
                    selectedClipId = clip.id
                    onSelectClip(clip)
                    break
                }
                acc += clipDur
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141416)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            
            // --- TOP TOOLBAR & TIMECODE READOUT ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timecode Display: Current / Total (e.g., 00:01 / 00:07)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF26262E), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${formatTimecode(currentPlaybackTimeMs)} / ${formatTimecode(totalVideoDurationMs)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isSnappingEnabled) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00C853).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SNAP", color = Color(0xFF00C853), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Zoom & Snap Quick Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { scalePxPerSecond = (scalePxPerSecond * 0.8f).coerceAtLeast(20f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.LightGray)
                    }
                    IconButton(
                        onClick = { scalePxPerSecond = (scalePxPerSecond * 1.25f).coerceAtMost(250f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.LightGray)
                    }
                    IconButton(
                        onClick = { isSnappingEnabled = !isSnappingEnabled },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Magnet,
                            contentDescription = "Toggle Snap",
                            tint = if (isSnappingEnabled) Terracotta else Color.Gray
                        )
                    }
                }
            }

            Divider(color = Color(0xFF2D2D35), thickness = 1.dp)

            // --- MULTI-TRACK TIMELINE CANVAS AREA ---
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color(0xFF121214))
            ) {
                val containerWidthPx = constraints.maxWidth.toFloat()
                val halfWidthPx = containerWidthPx / 2f

                // Horizontal scroll state
                val scrollState = rememberScrollState()

                // Sync timeline scroll with playback
                val playheadOffsetPx = (currentPlaybackTimeMs / 1000f) * scalePxPerSecond
                val targetScrollPx = (playheadOffsetPx).toInt()

                LaunchedEffect(currentPlaybackTimeMs, isPlaying) {
                    if (isPlaying) {
                        scrollState.scrollTo(targetScrollPx)
                    }
                }

                // Detect manual timeline horizontal scroll/drag
                val currentScrollMs = ((scrollState.value) / scalePxPerSecond * 1000f).toLong()
                LaunchedEffect(scrollState.value) {
                    if (!isPlaying && currentScrollMs != currentPlaybackTimeMs && currentScrollMs in 0..maxTimelineMs) {
                        onSeekToTime(currentScrollMs)
                    }
                }

                // Scrollable Tracks Container
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                scalePxPerSecond = (scalePxPerSecond * zoom).coerceIn(25f, 220f)
                            }
                        }
                ) {
                    // Left spacer padding so 00:00 aligns perfectly under center playhead
                    Spacer(modifier = Modifier.width(with(LocalDensity.current) { halfWidthPx.toDp() }))

                    Column(
                        modifier = Modifier.width(
                            with(LocalDensity.current) {
                                ((maxTimelineMs / 1000f) * scalePxPerSecond).toDp()
                            }
                        )
                    ) {
                        // 1. Timecode Ruler (Top)
                        TimelineRuler(
                            totalDurationMs = maxTimelineMs,
                            scalePxPerSecond = scalePxPerSecond
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 2. Text Overlay Track Row (CapCut Purple)
                        TextTrackRow(
                            textTracks = project.textTracks,
                            selectedTextId = selectedTextId,
                            scalePxPerSecond = scalePxPerSecond,
                            onSelectText = {
                                selectedTextId = it.id
                                selectedClipId = null
                                selectedAudioId = null
                            },
                            onAddTextTrack = onAddTextTrack
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Main Video Track Row (Yellow Handle Border Bounding Box + Filmstrips + Duration Badges)
                        VideoTrackRow(
                            project = project,
                            selectedClipId = selectedClipId,
                            scalePxPerSecond = scalePxPerSecond,
                            currentPlaybackTimeMs = currentPlaybackTimeMs,
                            isSnappingEnabled = isSnappingEnabled,
                            onSelectClip = { clip ->
                                selectedClipId = clip.id
                                selectedAudioId = null
                                selectedTextId = null
                                onSelectClip(clip)
                            },
                            onUpdateClipTrim = { clipId, startMs, endMs ->
                                if (isSnappingEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                onUpdateClipTrim(clipId, startMs, endMs)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. Audio Track Row (Meta Edits Magenta + Waveform)
                        AudioTrackRow(
                            audioTracks = project.audioTracks,
                            selectedAudioId = selectedAudioId,
                            scalePxPerSecond = scalePxPerSecond,
                            onSelectAudio = {
                                selectedAudioId = it.id
                                selectedClipId = null
                                selectedTextId = null
                            },
                            onAddAudioTrack = onAddAudioTrack
                        )
                    }

                    // Right spacer padding to scroll past the end of the timeline cleanly
                    Spacer(modifier = Modifier.width(with(LocalDensity.current) { halfWidthPx.toDp() }))
                }

                // --- CENTER FIXED PLAYHEAD NEEDLE LINE ---
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .align(Alignment.Center)
                        .background(Color.White)
                        .shadow(4.dp)
                ) {
                    // Diamond / Cap shape at top of playhead
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(12.dp)
                            .offset(y = (-4).dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }

            Divider(color = Color(0xFF2D2D35), thickness = 1.dp)

            // --- CAPCUT / VN / META EDITS STYLE ACTION TOOLBAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1B20))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Split Action
                ActionButton(
                    icon = Icons.Default.ContentCut,
                    label = "Split",
                    enabled = activeClip != null,
                    onClick = {
                        activeClip?.let { clip ->
                            onSplitClip(clip.id, currentPlaybackTimeMs)
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                    }
                )

                // Duplicate Action
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Duplicate",
                    enabled = activeClip != null,
                    onClick = {
                        activeClip?.let { onDuplicateClip(it.id) }
                    }
                )

                // Delete Action
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    enabled = activeClip != null,
                    color = Terracotta,
                    onClick = {
                        activeClip?.let {
                            onDeleteClip(it.id)
                            selectedClipId = null
                        }
                    }
                )

                // Add Text Track
                ActionButton(
                    icon = Icons.Default.Title,
                    label = "+ Text",
                    color = Color(0xFF9B51E0),
                    onClick = onAddTextTrack
                )

                // Add Audio Track
                ActionButton(
                    icon = Icons.Default.MusicNote,
                    label = "+ Audio",
                    color = Color(0xFFE91E63),
                    onClick = onAddAudioTrack
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .graphicsLayer { alpha = if (enabled) 1.0f else 0.4f }
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// --- TIMECODE RULER COMPONENT ---
@Composable
private fun TimelineRuler(
    totalDurationMs: Long,
    scalePxPerSecond: Float
) {
    val totalSeconds = (totalDurationMs / 1000L).toInt() + 2
    val rulerWidthDp = with(LocalDensity.current) { ((totalDurationMs / 1000f) * scalePxPerSecond).toDp() }

    Box(
        modifier = Modifier
            .width(rulerWidthDp)
            .height(24.dp)
            .background(Color(0xFF1A1A1E))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tickIntervalPx = scalePxPerSecond
            for (sec in 0..totalSeconds) {
                val x = sec * tickIntervalPx

                // Major second tick line
                drawLine(
                    color = Color.Gray,
                    start = Offset(x, size.height * 0.5f),
                    end = Offset(x, size.height),
                    strokeWidth = 2f
                )

                // Minor 0.5 second sub-ticks
                val subX = x + (tickIntervalPx / 2f)
                drawLine(
                    color = Color(0xFF44444C),
                    start = Offset(subX, size.height * 0.75f),
                    end = Offset(subX, size.height),
                    strokeWidth = 1f
                )
            }
        }

        // Time labels (0s, 1s, 2s, 3s...)
        Row(modifier = Modifier.fillMaxSize()) {
            for (sec in 0..totalSeconds) {
                val offsetDp = with(LocalDensity.current) { (sec * scalePxPerSecond).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = offsetDp)
                        .padding(start = 2.dp, top = 2.dp)
                ) {
                    Text(
                        text = "${sec}s",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// --- MAIN VIDEO TRACK ROW (Filmstrips + Yellow Bounding Handles + Duration Badges) ---
@Composable
private fun VideoTrackRow(
    project: RankingProject,
    selectedClipId: String?,
    scalePxPerSecond: Float,
    currentPlaybackTimeMs: Long,
    isSnappingEnabled: Boolean,
    onSelectClip: (VideoClip) -> Unit,
    onUpdateClipTrim: (clipId: String, startMs: Long, endMs: Long) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .height(90.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        project.clips.forEachIndexed { index, clip ->
            val clipDurationMs = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(1000L)
            val clipWidthDp = with(LocalDensity.current) { ((clipDurationMs / 1000f) * scalePxPerSecond).toDp() }
            val isSelected = clip.id == selectedClipId

            Box(
                modifier = Modifier
                    .width(clipWidthDp)
                    .fillMaxHeight()
                    .padding(horizontal = 1.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF222228))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color(0xFFFFD600) else Color(0xFF3D3D48), // CapCut/Meta Edits Yellow highlight
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectClip(clip) }
            ) {
                // Filmstrip Frame Thumbnails
                FilmstripThumbnails(
                    context = context,
                    videoUri = clip.videoUri,
                    clipWidthDp = clipWidthDp,
                    durationMs = clipDurationMs
                )

                // Clip Rank Badge (e.g. #1, #2...)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#${index + 1}",
                        color = Terracotta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Active Selection Handles & Duration Badge
                if (isSelected) {
                    // Precise Duration Tag Badge on top handle (e.g., 1.8s)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-2).dp)
                            .background(Color(0xFFFFD600), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1fs", clipDurationMs / 1000f),
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Left Drag Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(14.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFFFD600))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Trim Left",
                            tint = Color.Black,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(12.dp)
                        )
                    }

                    // Right Drag Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(14.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFFFD600))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Trim Right",
                            tint = Color.Black,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// Filmstrip Frame Thumbnails Generator
@Composable
private fun FilmstripThumbnails(
    context: android.content.Context,
    videoUri: String,
    clipWidthDp: Dp,
    durationMs: Long
) {
    val density = LocalDensity.current
    val clipWidthPx = with(density) { clipWidthDp.toPx() }
    val frameWidthPx = 100f
    val frameCount = (clipWidthPx / frameWidthPx).toInt().coerceAtLeast(1)

    var thumbnails by remember(videoUri, durationMs, frameCount) { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(videoUri, durationMs, frameCount) {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<Bitmap>()
            val intervalUs = (durationMs * 1000L) / frameCount.coerceAtLeast(1)
            for (i in 0 until frameCount) {
                val timeUs = i * intervalUs
                val bmp = TimelineThumbnailCache.getFrameThumbnail(context, videoUri, timeUs)
                if (bmp != null) list.add(bmp)
            }
            thumbnails = list
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (thumbnails.isNotEmpty()) {
            thumbnails.forEach { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2A2A32))
            )
        }
    }
}

// --- TEXT OVERLAY TRACK ROW (CapCut Purple Pills) ---
@Composable
private fun TextTrackRow(
    textTracks: List<TextTrackItem>,
    selectedTextId: String?,
    scalePxPerSecond: Float,
    onSelectText: (TextTrackItem) -> Unit,
    onAddTextTrack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color(0xFF16161A))
    ) {
        if (textTracks.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAddTextTrack() }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF9B51E0), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add text overlay...", color = Color(0xFF9B51E0), fontSize = 11.sp)
            }
        } else {
            textTracks.forEach { textItem ->
                val startDp = with(LocalDensity.current) { ((textItem.startOffsetMs / 1000f) * scalePxPerSecond).toDp() }
                val widthDp = with(LocalDensity.current) { ((textItem.durationMs / 1000f) * scalePxPerSecond).toDp() }
                val isSelected = textItem.id == selectedTextId

                Box(
                    modifier = Modifier
                        .offset(x = startDp)
                        .width(widthDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF8E44AD))
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectText(textItem) }
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = textItem.text,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// --- AUDIO TRACK ROW (Meta Edits Magenta + Waveform) ---
@Composable
private fun AudioTrackRow(
    audioTracks: List<AudioTrackItem>,
    selectedAudioId: String?,
    scalePxPerSecond: Float,
    onSelectAudio: (AudioTrackItem) -> Unit,
    onAddAudioTrack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFF16161A))
    ) {
        if (audioTracks.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAddAudioTrack() }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add audio / music...", color = Color(0xFFE91E63), fontSize = 11.sp)
            }
        } else {
            audioTracks.forEach { audio ->
                val startDp = with(LocalDensity.current) { ((audio.startOffsetMs / 1000f) * scalePxPerSecond).toDp() }
                val widthDp = with(LocalDensity.current) { ((audio.durationMs / 1000f) * scalePxPerSecond).toDp() }
                val isSelected = audio.id == selectedAudioId

                Box(
                    modifier = Modifier
                        .offset(x = startDp)
                        .width(widthDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFC2185B))
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectAudio(audio) }
                ) {
                    AudioWaveformCanvas(modifier = Modifier.fillMaxSize())
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = audio.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// Timecode Formatter Helper (e.g. 1000ms -> 00:01)
private fun formatTimecode(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
