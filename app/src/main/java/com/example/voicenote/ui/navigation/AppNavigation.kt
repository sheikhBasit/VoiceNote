package com.example.voicenote.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.voicenote.features.dashboard.DashboardScreen
import com.example.voicenote.features.home.HomeScreen
import com.example.voicenote.features.settings.ApiSettingsScreen
import com.example.voicenote.features.tasks.TasksScreen
import com.example.voicenote.features.search.SearchScreen
import com.example.voicenote.features.detail.NoteDetailScreen
import com.example.voicenote.features.billing.BillingScreen
import com.example.voicenote.ui.theme.Background

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Pulse", Icons.Default.Dashboard)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Checklist)
    object Notes : Screen("notes", "Notes", Icons.Default.Description)
    object Settings : Screen("settings", "Deep AI", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(Screen.Dashboard, Screen.Tasks, Screen.Notes, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Background, tonalElevation = 0.dp) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
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
            startDestination = Screen.Dashboard.route, 
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Tasks.route) { 
                TasksScreen(
                    onTaskClick = { noteId -> navController.navigate("detail/$noteId") },
                    onSearchClick = { navController.navigate("search") }
                ) 
            }
            composable(Screen.Notes.route) { 
                HomeScreen(
                    onNoteClick = { note -> navController.navigate("detail/${note.id}") },
                    onSearchClick = { navController.navigate("search") },
                    onBillingClick = { navController.navigate("billing") },
                    onJoinMeetingClick = { navController.navigate("join_meeting") }
                ) 
            }
            composable(Screen.Settings.route) { ApiSettingsScreen() }
            composable("billing") { BillingScreen(onBack = { navController.popBackStack() }) }
            composable("join_meeting") { 
                com.example.voicenote.features.meetings.JoinMeetingScreen(
                    onBack = { navController.popBackStack() },
                    onBotDispatched = { navController.popBackStack() }
                ) 
            }
            composable("search") { SearchScreen(onDismiss = { navController.popBackStack() }) }
            composable("detail/{noteId}") { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                NoteDetailScreen(noteId = noteId, onBack = { navController.popBackStack() })
            }
        }
    }
}
