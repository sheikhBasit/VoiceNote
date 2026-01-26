package com.example.voicenote.features.tasks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicenote.core.components.PriorityBadge
import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.model.Task
import com.example.voicenote.ui.theme.PriorityHigh
import com.example.voicenote.ui.theme.PriorityLow
import com.example.voicenote.ui.theme.PriorityMedium
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.components.GlassTabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel(),
    onTaskClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val allTasks by viewModel.tasks.collectAsState()
    val doneTasks by viewModel.doneTasks.collectAsState()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showDoneTasks by remember { mutableStateOf(false) }
    
    val tabs = listOf("High", "Medium", "Low")

    val selectedTaskIds = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedTaskIds.isNotEmpty()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentTasks = if (showDoneTasks) doneTasks else allTasks

    val filteredTasks = remember(currentTasks, selectedTabIndex, searchQuery, showDoneTasks) {
        val base = if (showDoneTasks) currentTasks 
        else {
            val priority = when (selectedTabIndex) {
                0 -> Priority.HIGH
                1 -> Priority.MEDIUM
                else -> Priority.LOW
            }
            currentTasks.filter { it.priority == priority && !it.isDone }
        }
        
        if (searchQuery.isEmpty()) base
        else base.filter { it.description.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedTaskIds.size} Selected") },
                        navigationIcon = {
                            IconButton(onClick = { selectedTaskIds.clear() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                val idsToDelete = selectedTaskIds.toList()
                                viewModel.deleteTasks(idsToDelete)
                                selectedTaskIds.clear()
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Tasks deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreTasks(idsToDelete)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text(if (showDoneTasks) "Done Tasks" else "Task Board", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        actions = {
                            IconButton(onClick = { showDoneTasks = !showDoneTasks }) {
                                Icon(
                                    if (showDoneTasks) Icons.Default.Checklist else Icons.Default.DoneAll,
                                    contentDescription = "Show Done",
                                    tint = if (showDoneTasks) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                        }
                    )
                    AnimatedVisibility(
                        visible = !isSelectionMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { onSearchClick() },
                            intensity = 0.5f
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF))
                                Spacer(Modifier.width(8.dp))
                                Text("Ask V-RAG about your tasks...", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                }
                
                if (!showDoneTasks) {
                    GlassTabRow(
                        selectedTabIndex = selectedTabIndex,
                        tabs = tabs,
                        onTabSelected = { selectedTabIndex = it }
                    )
                }
            }
        }
    ) { padding ->
        if (filteredTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(if (showDoneTasks) "No completed tasks yet." else "No ${tabs[selectedTabIndex]} tasks found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    val isSelected = selectedTaskIds.contains(task.id)
                    TaskBoardCard(
                        task = task,
                        isSelected = isSelected,
                        onToggle = { viewModel.toggleTask(task) },
                        onClick = { 
                            if (isSelectionMode) {
                                if (isSelected) selectedTaskIds.remove(task.id) else selectedTaskIds.add(task.id)
                            } else {
                                onTaskClick(task.noteId)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) selectedTaskIds.add(task.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskBoardCard(
    task: Task, 
    isSelected: Boolean,
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
        intensity = if (isSelected) 1.5f else 1.0f
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone, 
                onCheckedChange = { _ -> onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.description,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isDone) Color.White.copy(alpha = 0.4f) else Color.White
                    )
                    if (!task.isDone) {
                        PriorityBadge(priority = task.priority)
                    }
                }
                task.deadline?.let {
                    Text(
                        text = "Due: ${dateFormat.format(Date(it))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        if (isSelected) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFF00E5FF))
            )
        }
    }
}
