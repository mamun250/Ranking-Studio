package com.rankingstudio.app.exporter

enum class ExportResolution(val width: Int, val height: Int, val label: String) {
    FHD_1080P(1080, 1920, "1080p (Full HD - 1080x1920)"),
    HD_720P(720, 1280, "720p (HD - 720x1280)")
}

enum class ExportFrameRate(val fps: Int, val label: String) {
    FPS_30(30, "30 FPS (Standard)"),
    FPS_60(60, "60 FPS (Ultra Smooth)")
}

enum class ExportBitrate(val bitrateBps: Int, val label: String) {
    AUTO(12_000_000, "Auto (Recommended)"),
    BITRATE_8MBPS(8_000_000, "8 Mbps"),
    BITRATE_12MBPS(12_000_000, "12 Mbps"),
    BITRATE_16MBPS(16_000_000, "16 Mbps"),
    BITRATE_20MBPS(20_000_000, "20 Mbps (High Quality)")
}

data class ExportOptions(
    val resolution: ExportResolution = ExportResolution.FHD_1080P,
    val frameRate: ExportFrameRate = ExportFrameRate.FPS_30,
    val bitrate: ExportBitrate = ExportBitrate.AUTO
)

data class ExportProgress(
    val progress: Float = 0f, // 0.0 to 1.0
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val currentFps: Float = 0f,
    val encodingSpeed: Float = 1.0f, // e.g. 1.5x
    val elapsedTimeMs: Long = 0L,
    val remainingTimeMs: Long = 0L,
    val estimatedFileSizeMb: Float = 0f,
    val statusText: String = "Initializing..."
)
