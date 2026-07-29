package com.rankingstudio.app.ui.screens.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rankingstudio.app.domain.model.RankingProject
import com.rankingstudio.app.ui.components.PapercraftButton
import com.rankingstudio.app.ui.components.PapercraftCard
import com.rankingstudio.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectGalleryScreen(
    viewModel: ProjectGalleryViewModel,
    onOpenProject: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var selectedProjectForMenu by remember { mutableStateOf<RankingProject?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ranking Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkCharcoal
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfacePaper
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewProjectDialog = true },
                containerColor = PrimarySandishBrown,
                contentColor = OnPrimaryWhite
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Project")
            }
        },
        containerColor = SurfacePaper
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "My Projects (${projects.size})",
                style = MaterialTheme.typography.headlineSmall,
                color = InkCharcoal
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = OutlineBrown
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Ranking Videos Yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = InkCharcoal
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create a new 1080x1920 ranking video project.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(projects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            onOpen = { onOpenProject(project.id) },
                            onMenuClick = { selectedProjectForMenu = project }
                        )
                    }
                }
            }
        }
    }

    // Context Menu Dropdown / Dialog
    selectedProjectForMenu?.let { proj ->
        AlertDialog(
            onDismissRequest = { selectedProjectForMenu = null },
            title = { Text(proj.name, color = InkCharcoal) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            selectedProjectForMenu = null
                            onOpenProject(proj.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Editor", color = PrimarySandishBrown)
                    }
                    TextButton(
                        onClick = {
                            renameText = proj.name
                            showRenameDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rename", color = InkCharcoal)
                    }
                    TextButton(
                        onClick = {
                            viewModel.duplicateProject(proj.id) { newId ->
                                selectedProjectForMenu = null
                                onOpenProject(newId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Duplicate", color = InkCharcoal)
                    }
                    TextButton(
                        onClick = {
                            viewModel.deleteProject(proj.id)
                            selectedProjectForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete", color = ErrorRed)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedProjectForMenu = null }) {
                    Text("Close")
                }
            }
        )
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Create Ranking Project", color = InkCharcoal) },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g. Top 7 Funny Moments") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNewProjectDialog = false
                        viewModel.createNewProject(newProjectName) { newId ->
                            onOpenProject(newId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimarySandishBrown)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && selectedProjectForMenu != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Project", color = InkCharcoal) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameProject(selectedProjectForMenu!!.id, renameText)
                        showRenameDialog = false
                        selectedProjectForMenu = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimarySandishBrown)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectItemCard(
    project: RankingProject,
    onOpen: () -> Unit,
    onMenuClick: () -> Unit
) {
    PapercraftCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onMenuClick
            ),
        backgroundColor = PaperWhite
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(CardboardTan, shape = RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = PrimarySandishBrown,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = InkCharcoal,
                            maxLines = 1
                        )
                        Text(
                            text = "${project.clips.size} clips • 1080x1920",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = InkCharcoal
                        )
                    }
                }
            }
        }
    }
}
