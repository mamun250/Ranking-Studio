package com.rankingstudio.app.ui.screens.export

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rankingstudio.app.domain.model.RankingProject
import com.rankingstudio.app.exporter.ExportBitrate
import com.rankingstudio.app.exporter.ExportFrameRate
import com.rankingstudio.app.exporter.ExportResolution
import com.rankingstudio.app.ui.components.PapercraftButton
import com.rankingstudio.app.ui.components.PapercraftCard
import com.rankingstudio.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    project: RankingProject,
    viewModel: ExportViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val options by viewModel.options.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportSuccess by viewModel.exportSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Export Project Video",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkCharcoal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = InkCharcoal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmCream)
            )
        },
        containerColor = WarmCream
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Overview Header Card
            PapercraftCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = PaperWhite
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = InkCharcoal
                        )
                        Text(
                            text = "${project.clips.size} clips • Duration: ${formatTimecode(project.clips.sumOf { (it.trimEndMs - it.trimStartMs).coerceAtLeast(1000L) })}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimarySandishBrown
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Export Options Selector Card
            PapercraftCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = PaperWhite
            ) {
                Text(
                    text = "⚙ Export Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkCharcoal
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Resolution Selector
                Text("Resolution", style = MaterialTheme.typography.labelLarge, color = InkCharcoal)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportResolution.values().forEach { res ->
                        val isSelected = options.resolution == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) SecondaryContainer else WarmCream,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Terracotta else OutlineBrown,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = !isExporting) { viewModel.updateResolution(res) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = res.label.split(" ").first(),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Terracotta else InkCharcoal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Frame Rate Selector
                Text("Frame Rate", style = MaterialTheme.typography.labelLarge, color = InkCharcoal)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFrameRate.values().forEach { fps ->
                        val isSelected = options.frameRate == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) SecondaryContainer else WarmCream,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Terracotta else OutlineBrown,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = !isExporting) { viewModel.updateFrameRate(fps) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fps.label.split(" ").first(),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Terracotta else InkCharcoal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Bitrate Selector
                Text("Video Bitrate", style = MaterialTheme.typography.labelLarge, color = InkCharcoal)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExportBitrate.values().take(3).forEach { bit ->
                        val isSelected = options.bitrate == bit
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) SecondaryContainer else WarmCream,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Terracotta else OutlineBrown,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = !isExporting) { viewModel.updateBitrate(bit) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bit.label.split(" ").first(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Terracotta else InkCharcoal
                            )
                        }
                    }
                }
            }

            // Export Action & Progress Display Card
            PapercraftCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = PaperWhite
            ) {
                if (isExporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎬 Rendering Frame-Accurate Video...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Terracotta
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Real-time Progress Bar
                        LinearProgressIndicator(
                            progress = progress.progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = Terracotta,
                            trackColor = WarmCream
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${(progress.progress * 100).toInt()}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = InkCharcoal
                        )

                        Text(
                            text = progress.statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimarySandishBrown
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metrics Grid (Speed, FPS, Time Remaining, Est. Size)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MetricBadge(label = "Speed", value = String.format(Locale.US, "%.1fx", progress.encodingSpeed))
                            MetricBadge(label = "FPS", value = "${progress.currentFps.toInt()}")
                            MetricBadge(label = "ETA", value = formatTimecode(progress.remainingTimeMs))
                            MetricBadge(label = "Est. Size", value = String.format(Locale.US, "%.1fMB", progress.estimatedFileSizeMb))
                        }
                    }
                } else if (exportSuccess) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(54.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Export Completed!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Text(
                            text = "Video saved to Movies/RankingStudio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimarySandishBrown
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PapercraftButton(
                                text = "▶ Open Video",
                                onClick = { viewModel.openExportedVideo(context) },
                                modifier = Modifier.weight(1f)
                            )
                            PapercraftButton(
                                text = "📤 Share Video",
                                onClick = { viewModel.shareExportedVideo(context) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        errorMessage?.let { error ->
                            Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        PapercraftButton(
                            text = "🚀 Start Frame-Accurate Export",
                            onClick = { viewModel.startExport(context, project) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = PrimarySandishBrown)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkCharcoal)
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
