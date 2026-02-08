
package com.example.voicenote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.voicenote.core.workspace.WorkspaceViewModel
import com.example.voicenote.features.admin.AdminScreen
import com.example.voicenote.features.capture.CaptureScreen
import com.example.voicenote.features.dashboard.DashboardScreen
import com.example.voicenote.features.notes.NoteDetailScreen
import com.example.voicenote.features.onboarding.OnboardingScreen
import com.example.voicenote.features.tasks.TaskCenterScreen
import com.example.voicenote.features.vault.VaultScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    workspaceViewModel: WorkspaceViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding
    ) {
        composable<Screen.Onboarding> { OnboardingScreen(navController) }
        composable<Screen.Dashboard> { DashboardScreen(navController, workspaceViewModel) }
        composable<Screen.Capture> { CaptureScreen(navController, workspaceViewModel) }
        composable<Screen.Vault> { VaultScreen(navController, workspaceViewModel) }
        composable<Screen.NoteDetail> { backStackEntry ->
            val detail: Screen.NoteDetail = backStackEntry.toRoute()
            NoteDetailScreen(detail.noteId, navController)
        }
        composable<Screen.TaskCenter> { TaskCenterScreen(navController) }
        composable<Screen.Admin> { AdminScreen(navController) }
    }
}
