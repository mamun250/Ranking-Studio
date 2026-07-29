package com.rankingstudio.app.exporter

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.rankingstudio.app.domain.model.RankingProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoExporter {

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

        // Build FFmpeg command to stitch clips and overlay text & sidebar graphics
        // 1080x1920 video canvas output format
        val inputArguments = StringBuilder()
        val filterComplex = StringBuilder()
        val concatFilter = StringBuilder()

        project.clips.forEachIndexed { index, clip ->
            inputArguments.append("-i \"${clip.videoUri}\" ")
            filterComplex.append("[$index:v]scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(1080-iw)/2:(1920-ih)/2[v$index]; ")
            concatFilter.append("[v$index][$index:a]")
        }

        val clipCount = project.clips.size
        filterComplex.append("${concatFilter}concat=n=$clipCount:v=1:a=1[vconcat][aout]; ")

        // Draw 3 Line Header Overlay
        val headerText = "${project.headerConfig.line1}\n${project.headerConfig.line2}\n${project.headerConfig.line3}"
        filterComplex.append("[vconcat]drawtext=text='${project.headerConfig.line1}':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=100[vhdr1]; ")
        filterComplex.append("[vhdr1]drawtext=text='${project.headerConfig.line2}':fontcolor=yellow:fontsize=52:x=(w-text_w)/2:y=170[vhdr2]; ")
        filterComplex.append("[vhdr2]drawtext=text='${project.headerConfig.line3}':fontcolor=white:fontsize=44:x=(w-text_w)/2:y=240[vout]")

        val command = "${inputArguments}-filter_complex \"$filterComplex\" -map \"[vout]\" -map \"[aout]\" -c:v libx264 -preset ultrafast -r $fps -pix_fmt yuv420p \"${outputFile.absolutePath}\""

        val session = FFmpegKit.execute(command)

        return@withContext ReturnCode.isSuccess(session.returnCode)
    }
}
