package com.example.voicenote.features.home

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicenote.core.components.PriorityBadge
import com.example.voicenote.core.service.VoiceRecordingService
import com.example.voicenote.data.model.Note
import com.example.voicenote.data.model.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNoteClick: (Note) -> Unit = {}
) {
    val notes by viewModel.notesState.collectAsState()
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyLiked by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val isRecording by VoiceRecordingService.isRecording.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Selection State
    val selectedNoteIds = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedNoteIds.isNotEmpty()

    val filteredNotes = remember(notes, searchQuery, showOnlyLiked) {
        val base = if (showOnlyLiked) notes.filter { it.isLiked } else notes
        val filtered = if (searchQuery.isEmpty()) base
        else base.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.summary.contains(searchQuery, ignoreCase = true) 
        }
        // Pinned notes always on top
        filtered.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.timestamp })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedNoteIds.size} Selected") },
                        navigationIcon = {
                            IconButton(onClick = { selectedNoteIds.clear() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                filteredNotes.filter { selectedNoteIds.contains(it.id) }.forEach { viewModel.togglePin(it) }
                                selectedNoteIds.clear()
                            }) { Icon(Icons.Default.PushPin, contentDescription = "Pin") }
                            
                            IconButton(onClick = {
                                filteredNotes.filter { selectedNoteIds.contains(it.id) }.forEach { viewModel.toggleLike(it) }
                                selectedNoteIds.clear()
                            }) { Icon(Icons.Default.Favorite, contentDescription = "Like") }

                            IconButton(onClick = {
                                val idsToDelete = selectedNoteIds.toList()
                                viewModel.deleteNotes(idsToDelete)
                                selectedNoteIds.clear()
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Notes deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreNotes(idsToDelete)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text("My Notes", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        actions = {
                            IconButton(onClick = { showOnlyLiked = !showOnlyLiked }) {
                                Icon(
                                    if (showOnlyLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Show Liked",
                                    tint = if (showOnlyLiked) Color.Red else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { viewModel.onManualSync() }) {
                                Icon(Icons.Default.Sync, contentDescription = "Manual Sync")
                            }
                        }
                    )
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {},
                        active = false,
                        onActiveChange = {},
                        placeholder = { Text("Search notes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {}
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                RecordingButton(
                    isRecording = isRecording,
                    onClick = {
                        if (isRecording) {
                            val intent = Intent(context, VoiceRecordingService::class.java).apply {
                                action = VoiceRecordingService.ACTION_STOP_RECORDING
                            }
                            context.startService(intent)
                        } else {
                            val intent = Intent(context, VoiceRecordingService::class.java)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(1500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(padding)
        ) {
            if (filteredNotes.isEmpty()) {
                EmptyStateUI()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        val isSelected = selectedNoteIds.contains(note.id)
                        SwipeableNoteItem(
                            note = note,
                            isSelected = isSelected,
                            onDelete = { 
                                viewModel.onDeleteNote(note)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Note deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreNote(note.id)
                                    }
                                }
                            },
                            onOpen = { 
                                if (isSelectionMode) {
                                    if (isSelected) selectedNoteIds.remove(note.id) else selectedNoteIds.add(note.id)
                                } else {
                                    onNoteClick(note)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) selectedNoteIds.add(note.id)
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddNoteDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, desc, priority ->
                    viewModel.addNote(title, desc, priority)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun EmptyStateUI() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No meetings recorded yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            "Tap the mic to start your first session",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun RecordingButton(isRecording: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) {
            1.2f
        } else {
            1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(contentAlignment = Alignment.Center) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(scale)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.3f), CircleShape)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop" else "Voice Note"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableNoteItem(
    note: Note,
    isSelected: Boolean,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onLongClick: () -> Unit
) {
    var isDismissed by remember { mutableStateOf(false) }
    
    if (!isDismissed) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { direction ->
                when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        isDismissed = true
                        onDelete()
                        true
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        onOpen()
                        false // Snap back
                    }
                    else -> false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFFFCDD2) // Light Red
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else -> Color.Transparent
                    }, label = "dismiss_color"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            },
            content = {
                NoteCard(
                    note = note, 
                    isSelected = isSelected,
                    onClick = onOpen,
                    onLongClick = onLongClick
                )
            }
        )
    }
}

@Composable
fun NoteCard(
    note: Note, 
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isPinned) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (note.isLiked) {
                    Icon(Icons.Default.Favorite, contentDescription = "Liked", modifier = Modifier.size(16.dp), tint = Color.Red)
                }
            }
            if (note.summary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            PriorityBadge(priority = note.priority)
        }
    }
}

@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onSave: (String, String, Priority) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add New Note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name) }
                        )
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { if (title.isNotBlank()) onSave(title, desc, priority) }, enabled = title.isNotBlank()) { Text("Save") }
                }
            }
        }
    }
}