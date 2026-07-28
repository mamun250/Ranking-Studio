package com.rankingstudio.app.domain.model

data class RankingProject(
    val id: String,
    val name: String,
    val isTemplate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val headerConfig: HeaderConfig = HeaderConfig(),
    val progressBarConfig: ProgressBarConfig = ProgressBarConfig(),
    val watermarkConfig: WatermarkConfig = WatermarkConfig(),
    val clips: List<VideoClip> = emptyList(),
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
    val thumbnailUri: String? = null
)

data class RankingSidebarItem(
    val id: String,
    val projectId: String,
    val rankIndex: Int, // 1 to 7
    val title: String,
    val emoji: String = "🔥",
    val fontColorHex: String = "#2B2B2A",
    val backgroundColorHex: String = "#FCFAF2",
    val strokeColorHex: String = "#84736E",
    val fontSizeSp: Float = 16f,
    val strokeWidthDp: Float = 2f
)

data class HeaderConfig(
    val line1: String = "TOP 7",
    val line2: String = "FUNNIEST",
    val line3: String = "MOMENTS",
    val fontColorHex: String = "#FFFFFF",
    val fontSizeSp: Float = 28f
)

data class ProgressBarConfig(
    val enabled: Boolean = true,
    val colorHex: String = "#C15C3D",
    val thicknessDp: Float = 6f,
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
        "Grand Winner" to "👑",
        "Runner Up" to "🥈",
        "3rd Place" to "🥉",
        "Top Candidate" to "🔥",
        "Rising Star" to "⭐",
        "Wildcard Entry" to "⚡",
        "Honorable Mention" to "🎯"
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
