package com.example.voicenote.features.home

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.core.components.PriorityBadge
import com.example.voicenote.core.service.VoiceRecordingService
import com.example.voicenote.data.model.Note
import com.example.voicenote.data.model.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.components.RecordingButton
import com.example.voicenote.ui.components.GlassSkeleton
import com.example.voicenote.ui.components.GlassTabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNoteClick: (com.example.voicenote.data.model.Note) -> Unit,
    onSearchClick: () -> Unit,
    onBillingClick: () -> Unit,
    onJoinMeetingClick: () -> Unit,
    viewModel: com.example.voicenote.features.home.HomeViewModel = hiltViewModel()
) {
    val notes by viewModel.notesState.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
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
                                        message = "Selected notes have been moved to the trash.",
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
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("VoiceNote AI")
                                walletBalance?.let { balance ->
                                    Spacer(Modifier.width(12.dp))
                                    SuggestionChip(
                                        onClick = onBillingClick,
                                        label = { Text("$balance Cr", style = MaterialTheme.typography.labelSmall) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (balance < 50) Color.Red.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                            labelColor = if (balance < 50) Color.Red else Color.White
                                        )
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        actions = {
                            IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, contentDescription = "Search") }
                            IconButton(onClick = onJoinMeetingClick) { Icon(Icons.Default.MeetingRoom, contentDescription = "Meetings") }
                            IconButton(onClick = onBillingClick) { Icon(Icons.Default.Wallet, contentDescription = "Wallet") }
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
                            Text("Ask V-RAG about your notes...", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

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
                    viewModel.onManualSync()
                    delay(1500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(padding)
        ) {
            if (notes.isEmpty() && !isRefreshing) {
                EmptyStateUI()
            } else if (isRefreshing) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) { GlassSkeleton() }
                }
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
                                        message = "Note moved to the trash.",
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.wrapContentSize(),
            intensity = 0.8f
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                // Liquid Icon Effect
                val infiniteTransition = rememberInfiniteTransition(label = "liquid")
                val floatAnim by infiniteTransition.animateFloat(
                    initialValue = -10f,
                    targetValue = 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "float"
                )

                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .offset(y = floatAnim.dp),
                    tint = Color(0xFF00E5FF)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Silence is Gold?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Your AI brain is hungry for some voice notes. Tap the mic to begin!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
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
        val context = LocalContext.current
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { direction ->
                when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        isDismissed = true
                        onDelete()
                        true
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            `package` = "com.whatsapp"
                            putExtra(Intent.EXTRA_TEXT, "Note Summary: ${note.summary}\n\nDrafted via VoiceNote AI")
                        }
                        try {
                            context.startActivity(whatsappIntent)
                        } catch (e: Exception) {
                            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Note Summary: ${note.summary}\n\nDrafted via VoiceNote AI")
                            }
                            context.startActivity(Intent.createChooser(genericIntent, "Share note via"))
                        }
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
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF25D366).copy(alpha = 0.4f) // WhatsApp Green
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("WhatsApp Draft", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
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
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(isSelected) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            },
        intensity = if (isSelected) 1.5f else 1.0f
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.isPinned) {
                Icon(
                    Icons.Default.PushPin, 
                    contentDescription = "Pinned", 
                    modifier = Modifier.size(16.dp), 
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        PriorityBadge(priority = note.priority)
        
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
