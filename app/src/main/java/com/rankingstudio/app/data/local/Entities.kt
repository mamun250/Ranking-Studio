package com.rankingstudio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isTemplate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Header config stored as JSON or string properties
    val headerLine1: String = "TOP 7",
    val headerLine2: String = "FUNNIEST",
    val headerLine3: String = "MOMENTS",
    val headerColorHex: String = "#FFFFFF",
    val headerFontFamily: String = "SERIF",
    val headerFontSizeSp: Float = 28f,
    // Progress Bar Config
    val progressBarEnabled: Boolean = true,
    val progressBarColorHex: String = "#C15C3D",
    val progressBarThicknessDp: Float = 6f,
    val progressBarPosition: String = "BOTTOM",
    // Watermark Config
    val watermarkUri: String? = null,
    val watermarkOpacity: Float = 0.8f,
    val watermarkScale: Float = 1.0f,
    val watermarkPositionX: Float = 0.85f,
    val watermarkPositionY: Float = 0.05f
)

@Entity(tableName = "clips")
data class ClipEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val orderIndex: Int,
    val videoUri: String,
    val durationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = 0,
    val thumbnailUri: String? = null
)

@Entity(tableName = "ranking_items")
data class RankingItemEntity(
    @PrimaryKey val id: String,
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
