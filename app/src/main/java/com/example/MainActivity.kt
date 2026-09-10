package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.camera.CameraScreen
import com.example.ui.gallery.GalleryScreen
import com.example.ui.gallery.PhotoDetailScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate950
import com.example.viewmodel.CameraViewModel
import com.example.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()
    private val galleryViewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Slate950
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "camera"
                    ) {
                        composable("camera") {
                            CameraScreen(
                                viewModel = cameraViewModel,
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToGallery = { navController.navigate("gallery") },
                                onNavigateToDetail = { photoId -> navController.navigate("detail/$photoId") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = cameraViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("gallery") {
                            GalleryScreen(
                                viewModel = galleryViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onPhotoClick = { photoId -> navController.navigate("detail/$photoId") }
                            )
                        }

                        composable(
                            route = "detail/{photoId}",
                            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
                            PhotoDetailScreen(
                                photoId = photoId,
                                viewModel = galleryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

