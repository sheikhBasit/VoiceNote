
package com.example.voicenote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.core.workspace.WorkspaceViewModel

@Serializable
sealed interface Screen {
    @Serializable object Onboarding : Screen
    @Serializable object Dashboard : Screen
    @Serializable object Capture : Screen
    @Serializable object Vault : Screen
    @Serializable data class NoteDetail(val noteId: String) : Screen
    @Serializable object TaskCenter : Screen
    @Serializable object Admin : Screen
    @Serializable object Profile : Screen
    @Serializable object Settings : Screen
    @Serializable object Billing : Screen
    @Serializable object Search : Screen
    @Serializable object Notifications : Screen
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    workspaceViewModel: WorkspaceViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard // Starting with Dashboard for prototyping
    ) {
        composable<Screen.Onboarding> { 
            // TODO: Implement OnboardingScreen
        }
        composable<Screen.Dashboard> { 
            // TODO: Implement DashboardScreen
        }
        composable<Screen.Capture> { 
            // TODO: Implement CaptureScreen
        }
        composable<Screen.Vault> { 
            // TODO: Implement VaultScreen
        }
        composable<Screen.NoteDetail> { backStackEntry ->
            val detail: Screen.NoteDetail = backStackEntry.toRoute()
            // TODO: Implement NoteDetailScreen(detail.noteId, navController)
        }
        composable<Screen.TaskCenter> { 
            // TODO: Implement TaskCenterScreen
        }
        composable<Screen.Admin> { 
            // TODO: Implement AdminScreen
        }
    }
}
