package com.rankingstudio.app.data.repository

import com.rankingstudio.app.data.local.*
import com.rankingstudio.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) {
    fun getAllProjects(): Flow<List<RankingProject>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { entityToProject(it) }
        }
    }

    fun getAllTemplates(): Flow<List<RankingProject>> {
        return projectDao.getAllTemplates().map { entities ->
            entities.map { entityToProject(it) }
        }
    }

    suspend fun getProjectById(id: String): RankingProject? {
        val entity = projectDao.getProjectById(id) ?: return null
        val clips = projectDao.getClipsListForProject(id).map { clipEntityToModel(it) }
        val items = projectDao.getRankingItemsListForProject(id).map { itemEntityToModel(it) }
        return entityToProject(entity).copy(clips = clips, rankingItems = if (items.isNotEmpty()) items else defaultRankingItems(id))
    }

    fun getClipsForProject(projectId: String): Flow<List<VideoClip>> {
        return projectDao.getClipsForProject(projectId).map { list ->
            list.map { clipEntityToModel(it) }
        }
    }

    fun getRankingItemsForProject(projectId: String): Flow<List<RankingSidebarItem>> {
        return projectDao.getRankingItemsForProject(projectId).map { list ->
            list.map { itemEntityToModel(it) }
        }
    }

    suspend fun createProject(name: String): RankingProject {
        val newId = UUID.randomUUID().toString()
        val project = RankingProject(
            id = newId,
            name = name,
            rankingItems = defaultRankingItems(newId)
        )
        saveProject(project)
        return project
    }

    suspend fun saveProject(project: RankingProject) {
        val entity = projectToEntity(project)
        projectDao.insertProject(entity)

        if (project.clips.isNotEmpty()) {
            projectDao.insertClips(project.clips.map { clipModelToEntity(it) })
        }
        if (project.rankingItems.isNotEmpty()) {
            projectDao.insertRankingItems(project.rankingItems.map { itemModelToEntity(it) })
        }
    }

    suspend fun renameProject(id: String, newName: String) {
        val current = getProjectById(id) ?: return
        saveProject(current.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    suspend fun duplicateProject(id: String): RankingProject? {
        val current = getProjectById(id) ?: return null
        val newId = UUID.randomUUID().toString()
        val duplicated = current.copy(
            id = newId,
            name = "${current.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            clips = current.clips.map { it.copy(id = UUID.randomUUID().toString(), projectId = newId) },
            rankingItems = current.rankingItems.map { it.copy(id = UUID.randomUUID().toString(), projectId = newId) }
        )
        saveProject(duplicated)
        return duplicated
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteClipsForProject(id)
        projectDao.deleteProject(id)
    }

    // Mapping helpers
    private fun entityToProject(entity: ProjectEntity): RankingProject {
        return RankingProject(
            id = entity.id,
            name = entity.name,
            isTemplate = entity.isTemplate,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            headerConfig = HeaderConfig(
                line1 = entity.headerLine1,
                line2 = entity.headerLine2,
                line3 = entity.headerLine3,
                fontColorHex = entity.headerColorHex,
                fontSizeSp = entity.headerFontSizeSp
            ),
            progressBarConfig = ProgressBarConfig(
                enabled = entity.progressBarEnabled,
                colorHex = entity.progressBarColorHex,
                thicknessDp = entity.progressBarThicknessDp,
                position = entity.progressBarPosition
            ),
            watermarkConfig = WatermarkConfig(
                uri = entity.watermarkUri,
                opacity = entity.watermarkOpacity,
                scale = entity.watermarkScale,
                positionX = entity.watermarkPositionX,
                positionY = entity.watermarkPositionY
            )
        )
    }

    private fun projectToEntity(project: RankingProject): ProjectEntity {
        return ProjectEntity(
            id = project.id,
            name = project.name,
            isTemplate = project.isTemplate,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
            headerLine1 = project.headerConfig.line1,
            headerLine2 = project.headerConfig.line2,
            headerLine3 = project.headerConfig.line3,
            headerColorHex = project.headerConfig.fontColorHex,
            headerFontSizeSp = project.headerConfig.fontSizeSp,
            progressBarEnabled = project.progressBarConfig.enabled,
            progressBarColorHex = project.progressBarConfig.colorHex,
            progressBarThicknessDp = project.progressBarConfig.thicknessDp,
            progressBarPosition = project.progressBarConfig.position,
            watermarkUri = project.watermarkConfig.uri,
            watermarkOpacity = project.watermarkConfig.opacity,
            watermarkScale = project.watermarkConfig.scale,
            watermarkPositionX = project.watermarkConfig.positionX,
            watermarkPositionY = project.watermarkConfig.positionY
        )
    }

    private fun clipEntityToModel(entity: ClipEntity) = VideoClip(
        id = entity.id,
        projectId = entity.projectId,
        orderIndex = entity.orderIndex,
        videoUri = entity.videoUri,
        durationMs = entity.durationMs,
        trimStartMs = entity.trimStartMs,
        trimEndMs = entity.trimEndMs,
        thumbnailUri = entity.thumbnailUri
    )

    private fun clipModelToEntity(model: VideoClip) = ClipEntity(
        id = model.id,
        projectId = model.projectId,
        orderIndex = model.orderIndex,
        videoUri = model.videoUri,
        durationMs = model.durationMs,
        trimStartMs = model.trimStartMs,
        trimEndMs = model.trimEndMs,
        thumbnailUri = model.thumbnailUri
    )

    private fun itemEntityToModel(entity: RankingItemEntity) = RankingSidebarItem(
        id = entity.id,
        projectId = entity.projectId,
        rankIndex = entity.rankIndex,
        title = entity.title,
        emoji = entity.emoji,
        fontColorHex = entity.fontColorHex,
        backgroundColorHex = entity.backgroundColorHex,
        strokeColorHex = entity.strokeColorHex,
        fontSizeSp = entity.fontSizeSp,
        strokeWidthDp = entity.strokeWidthDp
    )

    private fun itemModelToEntity(model: RankingSidebarItem) = RankingItemEntity(
        id = model.id,
        projectId = model.projectId,
        rankIndex = model.rankIndex,
        title = model.title,
        emoji = model.emoji,
        fontColorHex = model.fontColorHex,
        backgroundColorHex = model.backgroundColorHex,
        strokeColorHex = model.strokeColorHex,
        fontSizeSp = model.fontSizeSp,
        strokeWidthDp = model.strokeWidthDp
    )
}
