package com.rankingstudio.app.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rankingstudio.app.data.repository.ProjectRepository
import com.rankingstudio.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RankingEditorViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _project = MutableStateFlow<RankingProject?>(null)
    val project: StateFlow<RankingProject?> = _project.asStateFlow()

    private val _currentPlaybackTimeMs = MutableStateFlow(0L)
    val currentPlaybackTimeMs: StateFlow<Long> = _currentPlaybackTimeMs.asStateFlow()

    private val _activeRankIndex = MutableStateFlow(1)
    val activeRankIndex: StateFlow<Int> = _activeRankIndex.asStateFlow()

    private val _selectedClip = MutableStateFlow<VideoClip?>(null)
    val selectedClip: StateFlow<VideoClip?> = _selectedClip.asStateFlow()

    // Undo / Redo stacks
    private val undoStack = mutableListOf<RankingProject>()
    private val redoStack = mutableListOf<RankingProject>()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val proj = repository.getProjectById(projectId) ?: repository.createProject("Untitled")
            _project.value = proj
            updateActiveRankForTimestamp(0L)
        }
    }

    fun updatePlaybackProgress(positionMs: Long) {
        _currentPlaybackTimeMs.value = positionMs
        updateActiveRankForTimestamp(positionMs)
    }

    private fun updateActiveRankForTimestamp(positionMs: Long) {
        val clips = _project.value?.clips ?: return
        if (clips.isEmpty()) {
            _activeRankIndex.value = 1
            return
        }

        var accumulatedTimeMs = 0L
        var calculatedRank = 1

        for ((index, clip) in clips.withIndex()) {
            val clipDuration = (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(1000L)
            if (positionMs >= accumulatedTimeMs && positionMs < (accumulatedTimeMs + clipDuration)) {
                calculatedRank = (index + 1).coerceIn(1, 7)
                break
            }
            accumulatedTimeMs += clipDuration
        }

        if (_activeRankIndex.value != calculatedRank) {
            _activeRankIndex.value = calculatedRank
        }
    }

    fun addClipToTimeline(videoUri: String, durationMs: Long = 10000L) {
        val current = _project.value ?: return
        if (current.clips.size >= 7) return // Max 7 clips limit

        pushUndoState(current)

        val newClip = VideoClip(
            id = UUID.randomUUID().toString(),
            projectId = current.id,
            orderIndex = current.clips.size,
            videoUri = videoUri,
            durationMs = durationMs,
            trimStartMs = 0,
            trimEndMs = durationMs
        )

        val updatedClips = current.clips + newClip
        val updatedProject = current.copy(clips = updatedClips, updatedAt = System.currentTimeMillis())
        _project.value = updatedProject
        saveCurrentProject()
    }

    fun removeClip(clipId: String) {
        val current = _project.value ?: return
        pushUndoState(current)

        val updatedClips = current.clips.filter { it.id != clipId }
            .mapIndexed { index, clip -> clip.copy(orderIndex = index) }

        val updatedProject = current.copy(clips = updatedClips, updatedAt = System.currentTimeMillis())
        _project.value = updatedProject
        saveCurrentProject()
    }

    fun reorderClips(fromIndex: Int, toIndex: Int) {
        val current = _project.value ?: return
        val clips = current.clips.toMutableList()
        if (fromIndex in clips.indices && toIndex in clips.indices) {
            pushUndoState(current)
            val moved = clips.removeAt(fromIndex)
            clips.add(toIndex, moved)
            val reordered = clips.mapIndexed { index, clip -> clip.copy(orderIndex = index) }
            val updated = current.copy(clips = reordered, updatedAt = System.currentTimeMillis())
            _project.value = updated
            saveCurrentProject()
        }
    }

    fun updateHeaderConfig(config: HeaderConfig) {
        val current = _project.value ?: return
        pushUndoState(current)
        val updated = current.copy(headerConfig = config, updatedAt = System.currentTimeMillis())
        _project.value = updated
        saveCurrentProject()
    }

    fun updateRankingSidebarItem(rankIndex: Int, newTitle: String, newEmoji: String) {
        val current = _project.value ?: return
        pushUndoState(current)
        val updatedItems = current.rankingItems.map {
            if (it.rankIndex == rankIndex) it.copy(title = newTitle, emoji = newEmoji) else it
        }
        val updated = current.copy(rankingItems = updatedItems, updatedAt = System.currentTimeMillis())
        _project.value = updated
        saveCurrentProject()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val lastState = undoStack.removeAt(undoStack.lastIndex)
            _project.value?.let { redoStack.add(it) }
            _project.value = lastState
            saveCurrentProject()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.lastIndex)
            _project.value?.let { undoStack.add(it) }
            _project.value = nextState
            saveCurrentProject()
        }
    }

    private fun pushUndoState(project: RankingProject) {
        undoStack.add(project)
        redoStack.clear()
    }

    private fun saveCurrentProject() {
        viewModelScope.launch {
            _project.value?.let { repository.saveProject(it) }
        }
    }
}
