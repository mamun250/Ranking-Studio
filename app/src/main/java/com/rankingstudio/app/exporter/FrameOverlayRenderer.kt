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

        // 1. Render Header Container & Text Lines
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = when (project.headerConfig.textAlign) {
                "LEFT" -> Paint.Align.LEFT
                "RIGHT" -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw Header Box Background if set
        val headerBgColor = try { Color.parseColor(project.headerConfig.backgroundColorHex) } catch (_: Exception) { Color.BLACK }
        val headerBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = headerBgColor
            style = Paint.Style.FILL
        }
        val headerRect = RectF(width * 0.05f, height * 0.03f, width * 0.95f, height * 0.20f)
        canvas.drawRoundRect(headerRect, 24f, 24f, headerBoxPaint)

        val textX = when (project.headerConfig.textAlign) {
            "LEFT" -> width * 0.10f
            "RIGHT" -> width * 0.90f
            else -> width / 2f
        }

        // Header Line 1
        textPaint.color = try { Color.parseColor(project.headerConfig.line1ColorHex) } catch (_: Exception) { Color.WHITE }
        textPaint.textSize = width * (project.headerConfig.line1SizeSp / 1000f * 0.9f).coerceIn(0.03f, 0.08f)
        canvas.drawText(project.headerConfig.line1, textX, height * 0.08f, textPaint)

        // Header Line 2 (Highlighted)
        textPaint.color = try { Color.parseColor(project.headerConfig.line2ColorHex) } catch (_: Exception) { Color.parseColor("#FFEB3B") }
        textPaint.textSize = width * (project.headerConfig.line2SizeSp / 1000f * 1.0f).coerceIn(0.04f, 0.10f)
        canvas.drawText(project.headerConfig.line2, textX, height * 0.13f, textPaint)

        // Header Line 3
        textPaint.color = try { Color.parseColor(project.headerConfig.line3ColorHex) } catch (_: Exception) { Color.WHITE }
        textPaint.textSize = width * (project.headerConfig.line3SizeSp / 1000f * 0.8f).coerceIn(0.025f, 0.07f)
        canvas.drawText(project.headerConfig.line3, textX, height * 0.17f, textPaint)

        // 2. Render Ranking Sidebar Items (1 to 7)
        val startY = height * 0.23f
        val itemHeight = height * 0.045f
        val itemSpacing = height * (project.rankingStyleConfig.spacingDp / 1000f).coerceIn(0.008f, 0.025f)
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
                try { Color.parseColor(project.rankingStyleConfig.highlightColorHex) } catch (_: Exception) { Color.parseColor("#FFEB3B") }
            } else {
                try { Color.parseColor(item.backgroundColorHex) } catch (_: Exception) { Color.parseColor("#1A1A1A") }
            }

            val itemBorderColor = if (isActive) {
                Color.parseColor("#FFFFFF")
            } else {
                try { Color.parseColor(item.strokeColorHex) } catch (_: Exception) { Color.parseColor("#333333") }
            }

            val itemTextColor = if (isActive) {
                Color.BLACK
            } else {
                try { Color.parseColor(item.fontColorHex) } catch (_: Exception) { Color.WHITE }
            }

            // Active rank bounce scale offset
            val scale = if (isActive) 1.10f else 1.0f
            val scaledWidth = itemWidth * scale
            val scaledHeight = itemHeight * scale

            val rect = RectF(startX, y, startX + scaledWidth, y + scaledHeight)

            // Draw Background Card
            bgPaint.color = itemBgColor
            canvas.drawRoundRect(rect, 16f, 16f, bgPaint)

            // Draw Border
            strokePaint.color = itemBorderColor
            strokePaint.strokeWidth = if (isActive) 6f else 2f
            canvas.drawRoundRect(rect, 16f, 16f, strokePaint)

            // Draw Item Text (#Rank Emoji Title)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = itemTextColor
            textPaint.textSize = scaledHeight * 0.45f

            val labelText = "#${item.rankIndex} ${item.emoji} ${item.title}"
            canvas.drawText(labelText, startX + 16f, y + scaledHeight * 0.65f, textPaint)
        }

        // 3. Render Floating Text Overlay if enabled
        if (project.textOverlayConfig.enabled && project.textOverlayConfig.text.isNotEmpty()) {
            val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = try { Color.parseColor(project.textOverlayConfig.colorHex) } catch (_: Exception) { Color.WHITE }
                textSize = width * (project.textOverlayConfig.fontSizeSp / 1000f).coerceIn(0.03f, 0.09f)
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val overlayY = when (project.textOverlayConfig.positionPreset) {
                "TOP" -> height * 0.28f
                "CENTER" -> height * 0.50f
                else -> height * 0.85f
            }
            canvas.drawText(project.textOverlayConfig.text, width / 2f, overlayY, overlayPaint)
        }

        // 4. Render Progress Bar
        if (project.progressBarConfig.enabled) {
            val progress = (timestampMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            val barHeight = height * 0.008f
            val barY = height * 0.98f
            val progressWidth = width * progress

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = try { Color.parseColor(project.progressBarConfig.colorHex) } catch (_: Exception) { Color.parseColor("#FFEB3B") }
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

