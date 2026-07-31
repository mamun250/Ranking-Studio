package com.rankingstudio.app.ui.screens.editor.timeline

enum class TrackType {
    TEXT_OVERLAY,
    MAIN_VIDEO,
    AUDIO
}

enum class DragHandleType {
    NONE,
    START_HANDLE,
    END_HANDLE,
    BODY_MOVE
}

sealed class SelectedTimelineItem {
    data class Video(val clipId: String) : SelectedTimelineItem()
    data class Audio(val audioId: String) : SelectedTimelineItem()
    data class Text(val textId: String) : SelectedTimelineItem()
}

data class SnapPoint(
    val timeMs: Long,
    val label: String,
    val type: SnapPointType
)

enum class SnapPointType {
    PLAYHEAD,
    CLIP_BOUNDARY,
    MARKER
}

data class TimelineViewportState(
    val scalePxPerSecond: Float = 60f, // pixels per second zoom factor
    val minScalePxPerSecond: Float = 20f,
    val maxScalePxPerSecond: Float = 200f,
    val isSnappingEnabled: Boolean = true,
    val snapThresholdMs: Long = 150L
)
