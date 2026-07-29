package com.rankingstudio.app.exporter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.rankingstudio.app.domain.model.RankingProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object VideoExporter {

    fun getExportOutputFile(context: Context): File {
        val moviesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val studioFolder = File(moviesFolder, "RankingStudio")
        if (!studioFolder.exists()) {
            studioFolder.mkdirs()
        }
        return File(studioFolder, "Ranking_Video_${System.currentTimeMillis()}.mp4")
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

    suspend fun exportProjectVideo(
        context: Context,
        project: RankingProject,
        fps: Int = 30,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (project.clips.isEmpty()) {
            return@withContext false
        }

        val inputArguments = StringBuilder()
        val filterComplex = StringBuilder()
        val concatFilter = StringBuilder()

        project.clips.forEachIndexed { index, clip ->
            val startSec = String.format(Locale.US, "%.3f", clip.trimStartMs / 1000f)
            val durationSec = String.format(Locale.US, "%.3f", ((clip.trimEndMs - clip.trimStartMs).coerceAtLeast(500L)) / 1000f)

            inputArguments.append("-ss $startSec -t $durationSec -i \"${clip.videoUri}\" ")
            filterComplex.append("[$index:v]scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(1080-iw)/2:(1920-ih)/2[v$index]; ")
            concatFilter.append("[v$index][$index:a]")
        }

        val clipCount = project.clips.size
        filterComplex.append("${concatFilter}concat=n=$clipCount:v=1:a=1[vconcat][aout]; ")

        // Header Overlay Colors
        val headerColor = project.headerConfig.fontColorHex.replace("#", "0x") + "FF"

        filterComplex.append("[vconcat]drawtext=text='${escapeFfmpegText(project.headerConfig.line1)}':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=100[vhdr1]; ")
        filterComplex.append("[vhdr1]drawtext=text='${escapeFfmpegText(project.headerConfig.line2)}':fontcolor=${headerColor}:fontsize=54:x=(w-text_w)/2:y=170[vhdr2]; ")
        filterComplex.append("[vhdr2]drawtext=text='${escapeFfmpegText(project.headerConfig.line3)}':fontcolor=white:fontsize=44:x=(w-text_w)/2:y=240[vout]")

        val command = "${inputArguments}-filter_complex \"$filterComplex\" -map \"[vout]\" -map \"[aout]\" -c:v libx264 -preset ultrafast -r $fps -pix_fmt yuv420p \"${outputFile.absolutePath}\""

        val session = FFmpegKit.execute(command)

        val success = ReturnCode.isSuccess(session.returnCode)
        if (success) {
            notifyMediaScanner(context, outputFile)
        }

        return@withContext success
    }

    private fun escapeFfmpegText(text: String): String {
        return text.replace("'", "\\'").replace(":", "\\:")
    }
}

