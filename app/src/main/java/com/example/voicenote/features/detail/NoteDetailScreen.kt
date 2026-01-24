package com.example.voicenote.features.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.voicenote.core.components.PriorityBadge
import com.example.voicenote.core.utils.ActionExecutor
import com.example.voicenote.core.utils.CalendarManager
import com.example.voicenote.core.utils.ContactInfo
import com.example.voicenote.core.utils.ContactManager
import com.example.voicenote.core.utils.PdfManager
import com.example.voicenote.data.model.*
import com.example.voicenote.data.repository.AiRepository
import com.example.voicenote.data.repository.FirestoreRepository
import com.example.voicenote.ui.theme.PendingColor
import com.example.voicenote.ui.theme.PriorityHigh
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.components.GlassTabRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class NoteDetailViewModel @javax.inject.Inject constructor(
    private val noteId: String,
    private val repository: com.example.voicenote.data.repository.VoiceNoteRepository
) : ViewModel() {
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    
    val note: StateFlow<Note?> = refreshTrigger
        .flatMapLatest { repository.getNote(noteId) }
        .map { it.getOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshTrigger.tryEmit(Unit)
        
        viewModelScope.launch {
            repository.observeTaskStatus("").collect { update ->
                if (update.noteId == noteId && update.status == "DONE") {
                    refreshTrigger.emit(Unit)
                }
            }
        }
        
        // Listen for AI Responses specifically
        viewModelScope.launch {
            (webSocketManager as? com.example.voicenote.core.network.WebSocketManager)?.updates?.collect { event ->
                if (event["type"] == "AI_RESPONSE") {
                    val data = event["data"] as? Map<*, *>
                    val answer = data?.get("answer") as? String
                    if (answer != null) {
                        _aiResponse.value = answer
                        _isProcessingAi.value = false
                    }
                }
            }
        }
    }

    fun askAi(question: String) {
        val currentBalance = (refreshTrigger as? StateFlow<*>)?.let { 100 } ?: 100 // Logic to check wallet
        // In production, we'd check the balance from a repository StateFlow or local cache
        
        viewModelScope.launch {
            _isProcessingAi.value = true
            _aiResponse.value = "AI Brain is analyzing your request..."
            repository.askAI(noteId, question).collect { result ->
                result.onFailure {
                    if (it.message?.contains("402") == true) {
                        _aiResponse.value = "Insufficient Credits: Please refill your wallet to continue using Advanced AI features."
                    } else {
                        _aiResponse.value = "AI Error: ${it.message}"
                    }
                }
                _isProcessingAi.value = false
            }
        }
    }

    fun updateNote(title: String, summary: String, transcript: String, priority: Priority, status: NoteStatus) {
        viewModelScope.launch {
            val current = note.value ?: return@launch
            repository.saveNote(current.copy(
                title = title, 
                summary = summary, 
                transcript = transcript,
                priority = priority, 
                status = status
            ))
        }
    }

    fun renameSpeaker(oldName: String, newName: String) {
        viewModelScope.launch {
            val current = note.value ?: return@launch
            val updatedTranscript = current.transcript.replace("$oldName:", "$newName:")
            repository.saveNote(current.copy(transcript = updatedTranscript))
        }
    }

    fun deleteNote() {
        viewModelScope.launch { repository.softDeleteNote(noteId) }
    }

    fun updateTask(task: Task, description: String, deadline: Long?, assignedName: String? = null, assignedPhone: String? = null, commType: CommunicationType? = null, imageUrl: String? = null) {
        viewModelScope.launch {
            repository.updateTask(task.copy(
                description = description, 
                deadline = deadline,
                assignedContactName = assignedName,
                assignedContactPhone = assignedPhone,
                communicationType = commType,
                imageUrl = imageUrl
            ))
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch { repository.updateTaskStatus(task.id, !task.isDone) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.softDeleteTask(task.id) }
    }

    fun approveAction(task: Task) {
        viewModelScope.launch { repository.updateTask(task.copy(isActionApproved = true)) }
    }

    companion object {
        fun provideFactory(noteId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NoteDetailViewModel(noteId) as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onBack: () -> Unit
) {
    val viewModel: NoteDetailViewModel = viewModel(factory = NoteDetailViewModel.provideFactory(noteId))
    val note by viewModel.note.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val allActiveTasks by viewModel.allActiveTasks.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isProcessingAi by viewModel.isProcessingAi.collectAsState()
    
    val context = LocalContext.current
    val calendarManager = remember { CalendarManager(context) }
    val contactManager = remember { ContactManager(context) }
    val actionExecutor = remember { ActionExecutor(context) }
    val pdfManager = remember { PdfManager(context) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "Tasks", "Audio", "Ask AI")
    
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editSummary by remember { mutableStateOf("") }
    var editTranscript by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var editStatus by remember { mutableStateOf(NoteStatus.PENDING) }
    
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var speakerToRename by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(note) {
        note?.let {
            editTitle = it.title
            editSummary = it.summary
            editTranscript = it.transcript
            editPriority = it.priority
            editStatus = it.status
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Note" else "Note Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            viewModel.updateNote(editTitle, editSummary, editTranscript, editPriority, editStatus)
                            isEditing = false
                        }) { Icon(Icons.Default.Save, contentDescription = "Save") }
                    } else {
                        IconButton(onClick = { 
                            note?.let { pdfManager.generateNotePdf(it, tasks) }
                        }) { Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF") }
                        IconButton(onClick = { shareRecap(context, note, tasks) }) { Icon(Icons.Default.Share, contentDescription = "Share Recap") }
                        IconButton(onClick = { isEditing = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { viewModel.deleteNote(); onBack() }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                    }
                }
            )
        }
    ) { padding ->
        note?.let { currentNote ->
            Column(modifier = Modifier.padding(padding)) {
                GlassTabRow(
                    selectedTabIndex = selectedTabIndex,
                    tabs = tabs,
                    onTabSelected = { selectedTabIndex = it }
                )

                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (selectedTabIndex) {
                        0 -> SummaryTab(
                            note = currentNote, 
                            isEditing = isEditing,
                            editTitle = editTitle, onTitleChange = { editTitle = it },
                            editSummary = editSummary, onSummaryChange = { editSummary = it },
                            editPriority = editPriority, onPriorityChange = { editPriority = it },
                            editStatus = editStatus, onStatusChange = { editStatus = it }
                        )
                        1 -> TasksTab(tasks, allActiveTasks, viewModel, actionExecutor, onEdit = { editingTask = it })
                        2 -> AudioTab(currentNote, onRenameSpeaker = { speakerToRename = it })
                        3 -> AskAiTab(aiResponse, isProcessingAi, onAsk = { viewModel.askAi(it) })
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        editingTask?.let { task ->
            EditTaskDialog(task, contactManager, onDismiss = { editingTask = null }, onSave = { d, dl, n, p, c, img ->
                viewModel.updateTask(task, d, dl, n, p, c, img)
                if (dl != null && dl > System.currentTimeMillis()) {
                    calendarManager.addEventToCalendar("Task: $d", "Assigned to: $n", dl)
                    val cal = Calendar.getInstance().apply { timeInMillis = dl }
                    calendarManager.setAlarm("Task for $n: $d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
                editingTask = null
            })
        }

        speakerToRename?.let { oldName ->
            var newName by remember { mutableStateOf(oldName) }
            AlertDialog(
                onDismissRequest = { speakerToRename = null },
                title = { Text("Rename Speaker") },
                text = {
                    GlassyTextField(value = newName, onValueChange = { newName = it }, label = "New Name")
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.renameSpeaker(oldName, newName)
                        speakerToRename = null
                    }) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { speakerToRename = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryTab(
    note: Note, 
    isEditing: Boolean,
    editTitle: String, onTitleChange: (String) -> Unit,
    editSummary: String, onSummaryChange: (String) -> Unit,
    editPriority: Priority, onPriorityChange: (Priority) -> Unit,
    editStatus: NoteStatus, onStatusChange: (NoteStatus) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = onTitleChange, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editSummary, onValueChange = onSummaryChange, label = { Text("Summary") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    
                    Text("Priority", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Priority.entries.forEach { p ->
                            FilterChip(selected = editPriority == p, onClick = { onPriorityChange(p) }, label = { Text(p.name) })
                        }
                    }

                    Text("Status", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NoteStatus.entries.forEach { s ->
                            FilterChip(selected = editStatus == s, onClick = { onStatusChange(s) }, label = { Text(s.name) })
                        }
                    }
                }
            } else {
                GlassCard(intensity = 0.8f) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = note.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        PriorityBadge(priority = note.priority)
                        SuggestionChip(
                            onClick = {}, 
                            label = { Text(note.status.name) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (note.status == NoteStatus.PENDING) PendingColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                labelColor = if (note.status == NoteStatus.PENDING) PendingColor else Color.White
                            )
                        )
                        Text(text = note.summary, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayer(audioUrl: String) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }

    LaunchedEffect(audioUrl) {
        mediaPlayer?.release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(audioUrl))
                prepareAsync()
                setOnPreparedListener { 
                    duration = it.duration
                }
                setOnCompletionListener { 
                    isPlaying = false
                    currentPosition = 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = mediaPlayer?.currentPosition ?: 0
            delay(500)
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        intensity = 0.5f
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                    } else {
                        mediaPlayer?.start()
                    }
                    isPlaying = !isPlaying
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF)
                    )
                }
                
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { 
                        currentPosition = it.toInt()
                        mediaPlayer?.seekTo(it.toInt())
                    },
                    valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF)
                    )
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentPosition), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Text(formatTime(duration), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioTab(note: Note, onRenameSpeaker: (String) -> Unit) {
    val speakers = remember(note.transcript) {
        val regex = Regex("Speaker [A-Z]:")
        regex.findAll(note.transcript).map { it.value.removeSuffix(":") }.distinct().toList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            note.audioUrl?.let { AudioPlayer(it) }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (speakers.isNotEmpty()) {
            item {
                Text("Identify Speakers:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    speakers.forEach { speaker ->
                        AssistChip(onClick = { onRenameSpeaker(speaker) }, label = { Text("Rename $speaker") })
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        item {
            // Display transcript with selection support for Ask AI
            SelectionContainer {
                Text(
                    text = note.transcript.ifEmpty { "No transcript available." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        content()
    }
}

@Composable
fun TasksTab(tasks: List<Task>, allActiveTasks: List<Task>, viewModel: NoteDetailViewModel, actionExecutor: ActionExecutor, onEdit: (Task) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(tasks, key = { it.id }) { task ->
            val hasConflict = task.deadline != null && allActiveTasks.any { 
                it.id != task.id && it.deadline != null && 
                abs(it.deadline!! - task.deadline!!) < 600000 
            }

            TaskDetailCard(task, hasConflict,
                onToggle = { viewModel.toggleTask(task) }, 
                onEdit = { onEdit(task) },
                onDelete = { viewModel.deleteTask(task) },
                onApprove = { 
                    actionExecutor.executeTaskAction(task)
                    viewModel.approveAction(task)
                }
            )
        }
    }
}

@Composable
fun AskAiTab(response: String?, isProcessing: Boolean, onAsk: (String) -> Unit) {
    var question by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        GlassyTextField(
            value = question, 
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Ask about this note...",
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { if (question.isNotBlank()) onAsk(question) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing && question.isNotBlank()
        ) {
            if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Consult AI Brain")
        }

        Spacer(modifier = Modifier.height(16.dp))
        response?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(text = it, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    IconButton(onClick = { shareText(context, it) }) { Icon(Icons.Default.Share, "Share Answer") }
                    IconButton(onClick = { sendEmail(context, it, "AI Answer") }) { Icon(Icons.Default.Email, "Email Answer") }
                }
            }
        }
    }
}

@Composable
fun TaskDetailCard(task: Task, hasConflict: Boolean, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onApprove: () -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val isOverdue = task.deadline != null && task.deadline!! < System.currentTimeMillis() && !task.isDone
    
    val timeToDeadline = remember(task.deadline) {
        task.deadline?.let {
            val diff = it - System.currentTimeMillis()
            if (diff > 0) {
                val hours = diff / (1000 * 60 * 60)
                val mins = (diff / (1000 * 60)) % 60
                "In ${hours}h ${mins}m"
            } else "Overdue"
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        intensity = if (task.priority == Priority.HIGH) 1.2f else 0.8f
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (hasConflict) {
                Text("⚠️ Deadline Conflict Detected", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.isDone, 
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    PriorityBadge(priority = task.priority)
                }
            }
            if (isOverdue) Text("OVERDUE", color = Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            task.deadline?.let { 
                Row {
                    Text("Due: ${dateFormat.format(Date(it))}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    if (!task.isDone && !isOverdue) {
                        Spacer(Modifier.width(8.dp))
                        Text(text = "($timeToDeadline)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (task.imageUrl != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = task.imageUrl,
                    contentDescription = "Task image",
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            if (!task.isDone) {
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistantButton("Search", Icons.Default.Search) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(task.googlePrompt)}")))
                    }
                    AssistantButton("ChatGPT", Icons.Default.AutoAwesome) {
                        copyAndOpen(context, task.aiPrompt, "com.openai.chatgpt", "https://chat.openai.com")
                    }
                    AssistantButton("Gemini", Icons.Default.AutoAwesome) {
                        copyAndOpen(context, task.aiPrompt, "com.google.android.apps.bard", "https://gemini.google.com")
                    }
                }
                if (task.assignedContactPhone != null && !task.isActionApproved) {
                    Button(
                        onClick = onApprove, 
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                    ) {
                        Text("Approve ${task.communicationType} to ${task.assignedContactName}")
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(32.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun shareRecap(context: Context, note: Note?, tasks: List<Task>) {
    if (note == null) return
    val taskList = tasks.joinToString("\n") { "- [${if (it.isDone) "x" else " "}] ${it.description} (${it.priority})" }
    val text = "Recap: ${note.title}\nPriority: ${note.priority}\n\nSummary:\n${note.summary}\n\nTasks:\n$taskList"
    shareText(context, text, "Meeting Recap: ${note.title}")
}

private fun shareText(context: Context, text: String, subject: String? = null) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

private fun sendEmail(context: Context, text: String, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        shareText(context, text, subject)
    }
}

private fun copyAndOpen(context: Context, text: String, packageName: String, fallbackUrl: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("AI Prompt", text))
    Toast.makeText(context, "Draft successfully copied to clipboard.", Toast.LENGTH_SHORT).show()
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) context.startActivity(intent)
    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(task: Task, contactManager: ContactManager, onDismiss: () -> Unit, onSave: (String, Long?, String?, String?, CommunicationType?, String?) -> Unit) {
    var description by remember { mutableStateOf(task.description) }
    var contactQuery by remember { mutableStateOf(task.assignedContactName ?: "") }
    var selectedContact by remember { mutableStateOf<ContactInfo?>(if (task.assignedContactName != null && task.assignedContactPhone != null) ContactInfo(task.assignedContactName, task.assignedContactPhone) else null) }
    var commType by remember { mutableStateOf(task.communicationType ?: CommunicationType.SMS) }
    var imageUrl by remember { mutableStateOf(task.imageUrl) }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = task.deadline)
    val timePickerState = rememberTimePickerState(
        initialHour = if (task.deadline != null) Calendar.getInstance().apply { timeInMillis = task.deadline!! }.get(Calendar.HOUR_OF_DAY) else 17,
        initialMinute = if (task.deadline != null) Calendar.getInstance().apply { timeInMillis = task.deadline!! }.get(Calendar.MINUTE) else 0
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUrl = uri?.toString()
    }

    val contactResults = remember(contactQuery) { if (contactQuery.length > 1) contactManager.searchContacts(contactQuery) else emptyList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassyTextField(value = description, onValueChange = { description = it }, label = "Task")
                GlassyTextField(value = contactQuery, onValueChange = { contactQuery = it; selectedContact = null }, label = "Assign to contact")
                if (contactResults.isNotEmpty() && selectedContact == null) {
                    Card(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp)) {
                        LazyColumn { items(contactResults) { contact -> TextButton(onClick = { selectedContact = contact; contactQuery = contact.name }, modifier = Modifier.fillMaxWidth()) { Text("${contact.name} (${contact.phoneNumber})") } } }
                    }
                }
                if (selectedContact != null) {
                    Text("Action Method:", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CommunicationType.entries.forEach { type -> FilterChip(selected = commType == type, onClick = { commType = type }, label = { Text(type.name, style = MaterialTheme.typography.labelSmall) }) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDatePicker = true }) { Text(if (datePickerState.selectedDateMillis != null) SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(datePickerState.selectedDateMillis!!)) else "Set Date") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showTimePicker = true }) { Text("Set Time: ${timePickerState.hour}:${String.format("%02d", timePickerState.minute)}") }
                }
                Button(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Image")
                }
                if (imageUrl != null) {
                    Text("Image Attached ✓", style = MaterialTheme.typography.labelSmall, color = Color.Green)
                }
                if (showDatePicker) DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }) { DatePicker(state = datePickerState) }
                if (showTimePicker) AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = { TextButton(onClick = { showTimePicker = false }) { Text("OK") } }, text = { TimePicker(state = timePickerState) })
            }
        },
        confirmButton = { Button(onClick = {
            val cal = Calendar.getInstance()
            datePickerState.selectedDateMillis?.let { cal.timeInMillis = it }
            cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
            cal.set(Calendar.MINUTE, timePickerState.minute)
            onSave(description, cal.timeInMillis, selectedContact?.name, selectedContact?.phoneNumber, commType, imageUrl)
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
