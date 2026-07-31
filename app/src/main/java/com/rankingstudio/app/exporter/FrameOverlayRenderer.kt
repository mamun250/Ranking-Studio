package com.rankingstudio.app.exporter

import android.content.Context
import android.graphics.*
import com.rankingstudio.app.domain.model.RankingProject

object FrameOverlayRenderer {

    fun generateOverlayBitmap(
        context: Context,
        project: RankingProject,
        timestampMs: Long,
        width: Int = 1080,
        height: Int = 1920
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val totalDurationMs = project.clips.sumOf { (it.trimEndMs - it.trimStartMs).coerceAtLeast(1000L) }.coerceAtLeast(1000L)
        val activeRankIndex = calculateActiveRankForTimestamp(project, timestampMs)

        // 1. Render Header Text Lines
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Header Line 1
        textPaint.color = Color.WHITE
        textPaint.textSize = width * 0.045f
        canvas.drawText(project.headerConfig.line1, width / 2f, height * 0.08f, textPaint)

        // Header Line 2 (Highlighted)
        val line2Color = try { Color.parseColor(project.headerConfig.fontColorHex) } catch (_: Exception) { Color.parseColor("#C15C3D") }
        textPaint.color = line2Color
        textPaint.textSize = width * 0.065f
        canvas.drawText(project.headerConfig.line2, width / 2f, height * 0.13f, textPaint)

        // Header Line 3
        textPaint.color = Color.WHITE
        textPaint.textSize = width * 0.040f
        canvas.drawText(project.headerConfig.line3, width / 2f, height * 0.17f, textPaint)

        // 2. Render Ranking Sidebar Items (1 to 7)
        val startY = height * 0.22f
        val itemHeight = height * 0.045f
        val itemSpacing = height * 0.012f
        val startX = width * 0.04f
        val itemWidth = width * 0.42f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        project.rankingItems.forEachIndexed { index, item ->
            val isActive = item.rankIndex == activeRankIndex
            val y = startY + index * (itemHeight + itemSpacing)

            val itemBgColor = if (isActive) {
                Color.parseColor("#E0D8C3") // Highlight container background
            } else {
                try { Color.parseColor(item.backgroundColorHex) } catch (_: Exception) { Color.parseColor("#FCFAF2") }
            }

            val itemBorderColor = if (isActive) {
                Color.parseColor("#656046")
            } else {
                Color.parseColor("#84736E")
            }

            val itemTextColor = try { Color.parseColor(item.fontColorHex) } catch (_: Exception) { Color.parseColor("#2B2B2A") }

            // Active rank bounce scale offset
            val scale = if (isActive) 1.08f else 1.0f
            val scaledWidth = itemWidth * scale
            val scaledHeight = itemHeight * scale

            val rect = RectF(startX, y, startX + scaledWidth, y + scaledHeight)

            // Draw Background Card
            bgPaint.color = itemBgColor
            canvas.drawRoundRect(rect, 16f, 16f, bgPaint)

            // Draw Border
            strokePaint.color = itemBorderColor
            strokePaint.strokeWidth = if (isActive) 6f else 3f
            canvas.drawRoundRect(rect, 16f, 16f, strokePaint)

            // Draw Item Text (#Rank Emoji Title)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = itemTextColor
            textPaint.textSize = scaledHeight * 0.45f

            val labelText = "#${item.rankIndex} ${item.emoji} ${item.title}"
            canvas.drawText(labelText, startX + 16f, y + scaledHeight * 0.65f, textPaint)
        }

        // 3. Render Progress Bar
        if (project.progressBarConfig.enabled) {
            val progress = (timestampMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            val barHeight = height * 0.008f
            val barY = height * 0.98f
            val progressWidth = width * progress

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = try { Color.parseColor(project.progressBarConfig.colorHex) } catch (_: Exception) { Color.parseColor("#C15C3D") }
                style = Paint.Style.FILL
            }

            canvas.drawRect(0f, barY, progressWidth, barY + barHeight, barPaint)
        }

        return bitmap
    }

    fun calculateActiveRankForTimestamp(project: RankingProject, positionMs: Long): Int {
        if (project.clips.isEmpty()) return 1

        var accumulatedTimeMs = 0L
        for ((index, clip) in project.clips.withIndex()) {
            val clipDuration = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(1000L)
            if (positionMs >= accumulatedTimeMs && positionMs < (accumulatedTimeMs + clipDuration)) {
                return (index + 1).coerceIn(1, 7)
            }
            accumulatedTimeMs += clipDuration
        }
        return project.clips.size.coerceIn(1, 7)
    }
}
