package com.example.voicenote.features.tasks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.core.components.PriorityBadge
import com.example.voicenote.data.model.Task
import com.example.voicenote.data.remote.EntityDTO
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.components.GlassTabRow
import com.example.voicenote.ui.components.ShimmerCard
import com.example.voicenote.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val doneTasks by viewModel.doneTasks.collectAsState()

    var showDoneTasks by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val selectedTaskIds = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedTaskIds.isNotEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshState = rememberPullToRefreshState()

    val currentTasks = if (showDoneTasks) doneTasks else filteredTasks
    val filterTabs = listOf("All", "Today", "Overdue", "Assigned")

    LaunchedEffect(viewModel.error) {
        viewModel.error.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Refresh tasks on mount
    LaunchedEffect(Unit) {
        viewModel.refreshTasks()
    }

    Scaffold(
        containerColor = InsightsBackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(InsightsBackgroundDark).statusBarsPadding()) {
                if (isSelectionMode) {
                    SelectionTopBar(
                        count = selectedTaskIds.size,
                        onClear = { selectedTaskIds.clear() },
                        onDelete = {
                            val idsToDelete = selectedTaskIds.toList()
                            viewModel.deleteTasks(idsToDelete)
                            selectedTaskIds.clear()
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "${idsToDelete.size} tasks deleted",
                                    actionLabel = "Undo"
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreTasks(idsToDelete)
                                }
                            }
                        }
                    )
                } else {
                    TaskBoardTopBar(
                        showDoneTasks = showDoneTasks,
                        onToggleDone = { showDoneTasks = !showDoneTasks },
                        onSearchClick = onSearchClick
                    )
                }

                if (!showDoneTasks) {
                    GlassTabRow(
                        selectedTabIndex = when (selectedFilter) {
                            TaskFilter.ALL -> 0
                            TaskFilter.DUE_TODAY -> 1
                            TaskFilter.OVERDUE -> 2
                            TaskFilter.ASSIGNED_TO_ME -> 3
                        },
                        tabs = filterTabs,
                        onTabSelected = { index ->
                            viewModel.selectFilter(
                                when (index) {
                                    0 -> TaskFilter.ALL
                                    1 -> TaskFilter.DUE_TODAY
                                    2 -> TaskFilter.OVERDUE
                                    else -> TaskFilter.ASSIGNED_TO_ME
                                }
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode && !showDoneTasks) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = InsightsPrimary,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Task")
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshTasks() },
            state = refreshState,
            modifier = Modifier.padding(padding)
        ) {
            if (currentTasks.isEmpty() && !isRefreshing) {
                EmptyTasksState(showDoneTasks, selectedFilter)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isRefreshing && currentTasks.isEmpty()) {
                        items(5) { ShimmerCard(height = 100.dp) }
                    } else {
                        items(currentTasks, key = { it.id }) { task ->
                            val isSelected = selectedTaskIds.contains(task.id)
                            val isOverdue = (task.deadline != null && task.deadline < System.currentTimeMillis() && !task.isDone)

                            TaskBoardCard(
                                task = task,
                                isSelected = isSelected,
                                isOverdue = isOverdue,
                                onToggle = { viewModel.toggleTask(task) },
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedTaskIds.remove(task.id) else selectedTaskIds.add(task.id)
                                    } else {
                                        onTaskClick(task.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedTaskIds.add(task.id)
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCreateDialog) {
        TaskCreateDialog(
            noteId = null,
            onDismiss = { showCreateDialog = false },
            onCreateTask = { desc, prio, deadline, name, phone, commType, attachment ->
                val entities = if (name != null || phone != null) listOf(EntityDTO(name = name, phone = phone)) else emptyList()
                viewModel.createTask(null, desc, prio, deadline, entities, commType, attachment)
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskBoardTopBar(showDoneTasks: Boolean, onToggleDone: () -> Unit, onSearchClick: () -> Unit) {
    Column {
        TopAppBar(
            title = { 
                Text(
                    text = if (showDoneTasks) "Archive" else "Task Board", 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                ) 
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = InsightsBackgroundDark),
            actions = {
                IconButton(onClick = onToggleDone) {
                    Icon(
                        imageVector = if (showDoneTasks) Icons.Default.Assignment else Icons.Default.History,
                        contentDescription = "Archive",
                        tint = if (showDoneTasks) InsightsPrimary else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        )
        
        if (!showDoneTasks) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onSearchClick() },
                intensity = 0.4f
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = InsightsPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Search action items...", 
                        color = Color.White.copy(alpha = 0.5f), 
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClear: () -> Unit, onDelete: () -> Unit) {
    TopAppBar(
        title = { Text("$count Selected", color = InsightsPrimary, fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = InsightsBackgroundDark),
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, "Cancel", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF5252))
            }
        }
    )
}

@Composable
private fun EmptyTasksState(isDoneMode: Boolean, filter: TaskFilter) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isDoneMode) Icons.Default.Inventory else Icons.Default.CheckCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = when {
                    isDoneMode -> "No completed tasks yet."
                    filter == TaskFilter.OVERDUE -> "Zero overdue items. Great job!"
                    filter == TaskFilter.DUE_TODAY -> "Nothing due today. Take a break?"
                    else -> "Your task board is clear."
                },
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TaskBoardCard(
    task: Task,
    isSelected: Boolean,
    isOverdue: Boolean = false,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(isSelected) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            },
        intensity = if (isSelected) 1.5f else 0.8f,
        borderColor = if (isSelected) InsightsPrimary else InsightsGlassBorder
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = InsightsPrimary,
                    uncheckedColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isDone) Color.White.copy(alpha = 0.4f) else Color.White,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityBadge(priority = task.priority)
                    
                    if (task.deadline != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isOverdue) Color(0xFFFF5252) else Color.White.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = dateFormat.format(Date(task.deadline)),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverdue) Color(0xFFFF5252) else Color.White.copy(alpha = 0.4f),
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    
                    if (task.assignedContactName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = InsightsPrimary)
                            Spacer(Modifier.width(4.dp))
                            Text(task.assignedContactName ?: "", color = InsightsPrimary, fontSize = 10.sp)
                        }
                    }
                }
            }

            if (task.imageUrl != null) {
                Icon(
                    Icons.Default.Image,
                    null,
                    tint = InsightsPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
