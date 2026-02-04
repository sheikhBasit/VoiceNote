package com.example.voicenote.ui.navigation

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voicenote.core.service.VoiceRecordingService
import com.example.voicenote.features.dashboard.DashboardScreen
import com.example.voicenote.features.home.HomeScreen
import com.example.voicenote.features.home.HomeViewModel
import com.example.voicenote.features.settings.ApiSettingsScreen
import com.example.voicenote.features.tasks.TasksScreen
import com.example.voicenote.features.tasks.TaskDetailScreen
import com.example.voicenote.features.search.SearchScreen
import com.example.voicenote.features.detail.NoteDetailScreen
import com.example.voicenote.features.billing.UsageBillingScreen
import com.example.voicenote.features.notifications.NotificationScreen
import com.example.voicenote.features.notifications.NotificationType
import com.example.voicenote.ui.theme.Background
import com.example.voicenote.ui.theme.InsightsGlassBorder
import com.example.voicenote.ui.theme.InsightsPrimary

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Pulse", Icons.Default.Dashboard)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Checklist)
    object Notes : Screen("notes", "Notes", Icons.Default.Description)
    object Settings : Screen("settings", "Deep AI", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    initialNoteId: String? = null,
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(Screen.Dashboard, Screen.Tasks, Screen.Notes, Screen.Settings)

    LaunchedEffect(initialNoteId) {
        initialNoteId?.let { navController.navigate("detail/$it") }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isRecording by VoiceRecordingService.isRecording.collectAsState()
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            val showBottomBar = currentRoute in listOf(
                Screen.Dashboard.route, 
                Screen.Tasks.route, 
                Screen.Notes.route, 
                Screen.Settings.route,
                "stt_logs"
            )
            
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    NavigationBar(
                        containerColor = Background, 
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(80.dp)
                    ) {
                        val currentDestination = navBackStackEntry?.destination
                        
                        // First two items
                        items.take(2).forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label, color = Color.White) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = InsightsPrimary,
                                    unselectedIconColor = Color.Gray,
                                    indicatorColor = Color.Transparent,
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.Gray
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        // Placeholder for center button
                        Spacer(Modifier.weight(1f))

                        // Last two items
                        items.takeLast(2).forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label, color = Color.White) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = InsightsPrimary,
                                    unselectedIconColor = Color.Gray,
                                    indicatorColor = Color.Transparent,
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.Gray
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }

                    // Center Floating Mic Button
                    Box(
                        modifier = Modifier
                            .offset(y = (-20).dp)
                            .size(64.dp)
                            .shadow(12.dp, CircleShape, spotColor = InsightsPrimary)
                            .background(if(isRecording) Color(0xFFFF5252) else InsightsPrimary, CircleShape)
                            .border(4.dp, Background, CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                val intent = Intent(context, VoiceRecordingService::class.java)
                                intent.action = if (isRecording) "STOP" else "START"
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Notes.route, 
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { 
                DashboardScreen(
                    onNoteClick = { noteId -> navController.navigate("detail/$noteId") },
                    onViewAllTasks = { navController.navigate(Screen.Tasks.route) },
                    onWalletClick = { navController.navigate("billing") },
                    onSearchClick = { query -> 
                        val route = if (query != null) "search?query=$query" else "search"
                        navController.navigate(route) 
                    },
                    onNotificationClick = { navController.navigate("notifications") }
                ) 
            }
            composable(Screen.Tasks.route) { 
                TasksScreen(
                    onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") },
                    onSearchClick = { navController.navigate("search") }
                ) 
            }
            composable(Screen.Notes.route) { 
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    userName = viewModel.userName,
                    viewModel = viewModel,
                    onNoteClick = { note -> navController.navigate("detail/${note.id}") },
                    onSearchClick = { navController.navigate("search") },
                    onBillingClick = { navController.navigate("billing") },
                    onJoinMeetingClick = { navController.navigate("join_meeting") }
                ) 
            }
            composable(Screen.Settings.route) { 
                ApiSettingsScreen(
                    onHelpClick = { navController.navigate("help") }
                ) 
            }
            
            composable("billing") { UsageBillingScreen(onBack = { navController.popBackStack() }) }
            composable("notifications") { 
                NotificationScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { id, type ->
                        when(type) {
                            NotificationType.NOTE -> navController.navigate("detail/$id")
                            NotificationType.TASK -> navController.navigate("task_detail/$id")
                            else -> {}
                        }
                    }
                ) 
            }
            composable("help") { 
                com.example.voicenote.features.help.HelpScreen(onBack = { navController.popBackStack() }) 
            }
            composable("join_meeting") { 
                com.example.voicenote.features.meetings.JoinMeetingScreen(
                    onBack = { navController.popBackStack() },
                    onBotDispatched = { navController.popBackStack() }
                ) 
            }
            composable(
                route = "search?query={query}",
                arguments = listOf(navArgument("query") { nullable = true; defaultValue = null })
            ) { backStackEntry ->
                val initialQuery = backStackEntry.arguments?.getString("query")
                SearchScreen(
                    initialQuery = initialQuery,
                    onDismiss = { navController.popBackStack() }
                ) 
            }
            composable("detail/{noteId}") { 
                NoteDetailScreen(onBack = { navController.popBackStack() })
            }
            composable("task_detail/{taskId}") { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                TaskDetailScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() },
                    onNavigateToNote = { noteId -> navController.navigate("detail/$noteId") }
                )
            }
        }
    }
}
