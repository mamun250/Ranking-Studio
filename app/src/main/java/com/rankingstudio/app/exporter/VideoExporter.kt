package com.rankingstudio.app.exporter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.rankingstudio.app.domain.model.RankingProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object VideoExporter {

    fun getExportOutputFile(context: Context): File {
        return try {
            val moviesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val studioFolder = File(moviesFolder, "RankingStudio")
            if (!studioFolder.exists()) {
                studioFolder.mkdirs()
            }
            File(studioFolder, "Ranking_Video_${System.currentTimeMillis()}.mp4")
        } catch (e: Exception) {
            val fallbackFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "RankingStudio")
            if (!fallbackFolder.exists()) fallbackFolder.mkdirs()
            File(fallbackFolder, "Ranking_Video_${System.currentTimeMillis()}.mp4")
        }
    }

    fun notifyMediaScanner(context: Context, file: File) {
        try {
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(file)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun exportProjectVideoFrameAccurate(
        context: Context,
        project: RankingProject,
        options: ExportOptions = ExportOptions(),
        outputFile: File,
        onProgress: (ExportProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (project.clips.isEmpty()) {
            return@withContext false
        }

        return@withContext try {
            val startTime = System.currentTimeMillis()
            val totalDurationMs = project.clips.sumOf { (it.trimEndMs - it.trimStartMs).coerceAtLeast(1000L) }.coerceAtLeast(1000L)
            val totalFrames = ((totalDurationMs / 1000f) * options.frameRate.fps).toInt().coerceAtLeast(1)

            // 1. Generate Frame-Accurate Overlay Images for each rank state
            val overlayDir = File(context.cacheDir, "export_overlays_${System.currentTimeMillis()}")
            if (!overlayDir.exists()) overlayDir.mkdirs()

            val rankOverlayMap = mutableMapOf<Int, String>()
            for (rank in 1..project.clips.size.coerceAtMost(7)) {
                var tMs = 0L
                for (i in 0 until (rank - 1)) {
                    tMs += (project.clips[i].trimEndMs - project.clips[i].trimStartMs).coerceAtLeast(1000L)
                }
                tMs += 200L

                val bmp = FrameOverlayRenderer.generateOverlayBitmap(
                    context = context,
                    project = project,
                    timestampMs = tMs,
                    width = options.resolution.width,
                    height = options.resolution.height
                )

                val overlayFile = File(overlayDir, "overlay_rank_$rank.png")
                FileOutputStream(overlayFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bmp.recycle() // Prevent OOM memory leak
                rankOverlayMap[rank] = overlayFile.absolutePath
            }

            // 2. Build FFmpeg Filtergraph for video clips concat + rank-synced overlays
            val inputArguments = StringBuilder()
            val filterComplex = StringBuilder()
            val concatInputs = StringBuilder()

            project.clips.forEachIndexed { index, clip ->
                val startSec = String.format(Locale.US, "%.3f", clip.trimStartMs / 1000f)
                val durationSec = String.format(Locale.US, "%.3f", ((clip.trimEndMs - clip.trimStartMs).coerceAtLeast(500L)) / 1000f)
                val resW = options.resolution.width
                val resH = options.resolution.height

                inputArguments.append("-ss $startSec -t $durationSec -i \"${clip.videoUri}\" ")
                filterComplex.append("[$index:v]scale=$resW:$resH:force_original_aspect_ratio=decrease,pad=$resW:$resH:($resW-iw)/2:($resH-ih)/2[vsc$index]; ")

                val overlayPath = rankOverlayMap[index + 1] ?: rankOverlayMap[1]
                val overlayInputIdx = project.clips.size + index
                inputArguments.append("-i \"$overlayPath\" ")

                filterComplex.append("[vsc$index][$overlayInputIdx:v]overlay=0:0[vout$index]; ")
                concatInputs.append("[vout$index]")
            }

            val clipCount = project.clips.size
            filterComplex.append("${concatInputs}concat=n=$clipCount:v=1:a=0[vconcat]")

            val bitrateStr = "${options.bitrate.bitrateBps / 1000}k"
            val command = "${inputArguments}-filter_complex \"$filterComplex\" -map \"[vconcat]\" -c:v libx264 -preset ultrafast -b:v $bitrateStr -r ${options.frameRate.fps} -pix_fmt yuv420p \"${outputFile.absolutePath}\""

            // Setup FFmpeg Progress Statistics Callback
            FFmpegKitConfig.enableStatisticsCallback { stats ->
                val currentFrame = stats.videoFrameNumber.coerceAtLeast(0)
                val progressFraction = (currentFrame.toFloat() / totalFrames.toFloat()).coerceIn(0.0f, 1.0f)
                val elapsedMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)

                val currentFps = stats.videoFps.toFloat().coerceAtLeast(0f)
                val speedRatio = stats.speed.toFloat().coerceAtLeast(0.1f)
                val remainingMs = if (speedRatio > 0.05f) {
                    (((totalDurationMs - (stats.time.toLong())) / speedRatio)).toLong().coerceAtLeast(0L)
                } else 0L

                val estimatedSizeMb = (stats.size.toFloat() / (1024f * 1024f)).coerceAtLeast(0.1f)

                onProgress(
                    ExportProgress(
                        progress = progressFraction,
                        currentFrame = currentFrame,
                        totalFrames = totalFrames,
                        currentFps = currentFps,
                        encodingSpeed = speedRatio,
                        elapsedTimeMs = elapsedMs,
                        remainingTimeMs = remainingMs,
                        estimatedFileSizeMb = estimatedSizeMb,
                        statusText = "Rendering frame $currentFrame / $totalFrames (${(progressFraction * 100).toInt()}%)"
                    )
                )
            }

            val session = FFmpegKit.execute(command)

            // Cleanup temporary overlay PNG files
            try {
                overlayDir.deleteRecursively()
            } catch (_: Exception) {}

            val success = ReturnCode.isSuccess(session.returnCode)
            if (success) {
                notifyMediaScanner(context, outputFile)
                onProgress(
                    ExportProgress(
                        progress = 1.0f,
                        currentFrame = totalFrames,
                        totalFrames = totalFrames,
                        statusText = "Export Completed Successfully!"
                    )
                )
            }
            success
        } catch (e: Throwable) {
            e.printStackTrace()
            onProgress(
                ExportProgress(
                    progress = 0f,
                    statusText = "Export Error: ${e.message}"
                )
            )
            false
        }
    }
}

