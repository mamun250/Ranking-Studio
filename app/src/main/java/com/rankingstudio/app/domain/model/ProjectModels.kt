package com.rankingstudio.app.domain.model

data class RankingProject(
    val id: String,
    val name: String,
    val isTemplate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val headerConfig: HeaderConfig = HeaderConfig(),
    val rankingStyleConfig: RankingStyleConfig = RankingStyleConfig(),
    val textOverlayConfig: TextOverlayConfig = TextOverlayConfig(),
    val canvasConfig: CanvasConfig = CanvasConfig(),
    val exportConfig: ExportConfig = ExportConfig(),
    val progressBarConfig: ProgressBarConfig = ProgressBarConfig(),
    val watermarkConfig: WatermarkConfig = WatermarkConfig(),
    val clips: List<VideoClip> = emptyList(),
    val audioTracks: List<AudioTrackItem> = emptyList(),
    val textTracks: List<TextTrackItem> = emptyList(),
    val rankingItems: List<RankingSidebarItem> = defaultRankingItems()
)

data class VideoClip(
    val id: String,
    val projectId: String,
    val orderIndex: Int,
    val videoUri: String,
    val durationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = durationMs,
    val thumbnailUri: String? = null,
    val volume: Float = 0.8f, // 80% default volume matching wireframe
    val speed: Float = 1.0f,
    val transitionType: String = "NONE" // NONE, FADE, DISSOLVE, SLIDE
)

data class HeaderConfig(
    val line1: String = "RANKING",
    val line2: String = "FUNNIEST",
    val line3: String = "PARKOUR MOMENTS",
    val fontFamily: String = "Anton",
    val line1SizeSp: Float = 48f,
    val line2SizeSp: Float = 56f,
    val line3SizeSp: Float = 40f,
    val line1ColorHex: String = "#FFFFFF",
    val line2ColorHex: String = "#FFEB3B", // Yellow highlight color from wireframe
    val line3ColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#000000", // Dark header background
    val textAlign: String = "CENTER" // LEFT, CENTER, RIGHT
)

data class RankingStyleConfig(
    val fontFamily: String = "Poppins Bold",
    val textColorHex: String = "#FFFFFF",
    val highlightColorHex: String = "#FFEB3B", // Yellow
    val highlightAnimation: String = "Glow", // Glow, Pop, Bounce, None
    val positionAlignment: String = "CENTER_LEFT", // LEFT, CENTER, RIGHT presets
    val spacingDp: Float = 12f
)

data class TextOverlayConfig(
    val enabled: Boolean = false,
    val text: String = "",
    val fontSizeSp: Float = 36f,
    val colorHex: String = "#FFFFFF",
    val positionPreset: String = "TOP", // TOP, CENTER, BOTTOM
    val offsetXDp: Float = 0f,
    val offsetYDp: Float = 0f
)

data class CanvasConfig(
    val scale: Float = 1.0f,
    val offsetXDp: Float = 0f,
    val offsetYDp: Float = 0f
)

data class ExportConfig(
    val resolution: String = "1080x1920", // 1080x1920 (FHD), 720x1280
    val fps: Int = 60, // 30 or 60
    val quality: String = "High" // High, Medium, Standard
)

data class AudioTrackItem(
    val id: String,
    val title: String,
    val artist: String = "",
    val audioUri: String,
    val startOffsetMs: Long,
    val durationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = durationMs,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
)

data class TextTrackItem(
    val id: String,
    val text: String,
    val startOffsetMs: Long,
    val durationMs: Long,
    val fontColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#8E44AD",
    val fontSizeSp: Float = 14f,
    val animation: String = "POP"
)

data class RankingSidebarItem(
    val id: String,
    val projectId: String,
    val rankIndex: Int, // 1 to 7
    val title: String,
    val emoji: String = "🐯",
    val fontColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#1A1A1A",
    val strokeColorHex: String = "#333333",
    val fontSizeSp: Float = 14f,
    val strokeWidthDp: Float = 1f
)

data class ProgressBarConfig(
    val enabled: Boolean = true,
    val colorHex: String = "#FFEB3B",
    val thicknessDp: Float = 4f,
    val position: String = "BOTTOM"
)

data class WatermarkConfig(
    val uri: String? = null,
    val opacity: Float = 0.8f,
    val scale: Float = 1.0f,
    val positionX: Float = 0.85f,
    val positionY: Float = 0.05f
)

fun defaultRankingItems(projectId: String = ""): List<RankingSidebarItem> {
    val defaults = listOf(
        "Tiger" to "🐯",
        "Lion" to "🦁",
        "Cheetah" to "🐆",
        "Elephant" to "🐘",
        "Bear" to "🐻",
        "Wolf" to "🐺",
        "Gorilla" to "🦍"
    )
    return defaults.mapIndexed { index, (title, emoji) ->
        RankingSidebarItem(
            id = "rank_${projectId}_${index + 1}",
            projectId = projectId,
            rankIndex = index + 1,
            title = title,
            emoji = emoji
        )
    }
}

