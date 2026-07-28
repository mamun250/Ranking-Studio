package com.rankingstudio.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isTemplate = 0 ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isTemplate = 1 ORDER BY updatedAt DESC")
    fun getAllTemplates(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getClipsForProject(projectId: String): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY orderIndex ASC")
    suspend fun getClipsListForProject(projectId: String): List<ClipEntity>

    @Query("SELECT * FROM ranking_items WHERE projectId = :projectId ORDER BY rankIndex ASC")
    fun getRankingItemsForProject(projectId: String): Flow<List<RankingItemEntity>>

    @Query("SELECT * FROM ranking_items WHERE projectId = :projectId ORDER BY rankIndex ASC")
    suspend fun getRankingItemsListForProject(projectId: String): List<RankingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<ClipEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRankingItems(items: List<RankingItemEntity>)

    @Query("DELETE FROM clips WHERE projectId = :projectId")
    suspend fun deleteClipsForProject(projectId: String)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)
}
