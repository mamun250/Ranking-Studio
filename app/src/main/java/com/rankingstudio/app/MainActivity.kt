package com.rankingstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rankingstudio.app.data.remote.TikTokApiService
import com.rankingstudio.app.ui.screens.editor.RankingEditorScreen
import com.rankingstudio.app.ui.screens.editor.RankingEditorViewModel
import com.rankingstudio.app.ui.screens.gallery.ProjectGalleryScreen
import com.rankingstudio.app.ui.screens.gallery.ProjectGalleryViewModel
import com.rankingstudio.app.ui.theme.RankingStudioTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tiktokApiService: TikTokApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RankingStudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RankingStudioNavHost(tiktokApiService = tiktokApiService)
                }
            }
        }
    }
}

@Composable
fun RankingStudioNavHost(tiktokApiService: TikTokApiService) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "gallery"
    ) {
        composable("gallery") {
            val galleryViewModel: ProjectGalleryViewModel = hiltViewModel()
            ProjectGalleryScreen(
                viewModel = galleryViewModel,
                onOpenProject = { projectId ->
                    navController.navigate("editor/$projectId")
                }
            )
        }

        composable(
            route = "editor/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val editorViewModel: RankingEditorViewModel = hiltViewModel()

            RankingEditorScreen(
                projectId = projectId,
                viewModel = editorViewModel,
                apiService = tiktokApiService,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
