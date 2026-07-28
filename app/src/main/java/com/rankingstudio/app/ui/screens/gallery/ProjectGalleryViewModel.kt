package com.rankingstudio.app.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rankingstudio.app.data.repository.ProjectRepository
import com.rankingstudio.app.domain.model.RankingProject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectGalleryViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    val projects: StateFlow<List<RankingProject>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<RankingProject>> = repository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNewProject(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newProject = repository.createProject(name.ifBlank { "Untitled Project" })
            onCreated(newProject.id)
        }
    }

    fun renameProject(id: String, newName: String) {
        viewModelScope.launch {
            repository.renameProject(id, newName)
        }
    }

    fun duplicateProject(id: String, onDuplicated: (String) -> Unit) {
        viewModelScope.launch {
            val copy = repository.duplicateProject(id)
            if (copy != null) {
                onDuplicated(copy.id)
            }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }
}
