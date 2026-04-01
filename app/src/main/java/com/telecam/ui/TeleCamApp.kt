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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.telecam.ui.onboarding.OnboardingScreen
import com.telecam.ui.camera.CameraScreen
import com.telecam.ui.settings.SettingsScreen
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Navigation destinations for the app.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.Settings)
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
    val appEntryViewModel: AppEntryViewModel = hiltViewModel()
    val onboardingCompleted by appEntryViewModel.onboardingCompleted.collectAsState()
    
    val screens = listOf(
        Screen.Camera,
        Screen.Queue,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val startDestination = when (onboardingCompleted) {
        true -> Screen.Camera.route
        false -> Screen.Onboarding.route
        null -> Screen.Onboarding.route
    }
    val showBottomBar = currentDestination?.route in screens.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = "onboarding?token={token}",
                arguments = listOf(
                    navArgument("token") {
                        defaultValue = ""
                        nullable = true
                    }
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "telecam://auth?token={token}"
                    }
                )
            ) { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token")
                OnboardingScreen(
                    deepLinkToken = token,
                    onSetupCompleted = {
                        navController.navigate(Screen.Camera.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

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
