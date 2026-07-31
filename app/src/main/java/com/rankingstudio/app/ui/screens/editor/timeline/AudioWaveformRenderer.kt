package com.rankingstudio.app.ui.screens.editor.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

@Composable
fun AudioWaveformCanvas(
    modifier: Modifier = Modifier,
    waveColor: Color = Color.White.copy(alpha = 0.85f),
    seedKey: Int = 42
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barWidthPx = 4f
        val gapPx = 3f
        val totalBars = (width / (barWidthPx + gapPx)).toInt()

        val centerY = height / 2f
        val maxBarHeight = height * 0.7f

        for (i in 0 until totalBars) {
            val x = i * (barWidthPx + gapPx)
            // Generate rhythmic peak waveforms using pseudo-random harmonic math
            val norm = i.toFloat() / totalBars.coerceAtLeast(1)
            val amplitude = (sin(norm * 18.0 + seedKey) * 0.4 + sin(norm * 42.0) * 0.3 + 0.3).coerceIn(0.15, 0.95).toFloat()
            val barHeight = maxBarHeight * amplitude

            drawRoundRect(
                color = waveColor,
                topLeft = Offset(x, centerY - (barHeight / 2f)),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}
