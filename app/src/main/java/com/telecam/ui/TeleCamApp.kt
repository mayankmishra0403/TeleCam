package com.telecam.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.telecam.ui.camera.CameraScreen
import com.telecam.ui.settings.SettingsScreen

/**
 * Navigation destinations for the app.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Camera : Screen("camera", "Camera", Icons.Default.Camera)
    data object Queue : Screen("queue", "Queue", Icons.Default.History)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

/**
 * Main app navigation composable.
 */
@Composable
fun TeleCamApp() {
    val navController = rememberNavController()
    
    val screens = listOf(
        Screen.Camera,
        Screen.Queue,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Camera.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Camera.route) {
                CameraScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            
            composable(Screen.Queue.route) {
                QueueScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
