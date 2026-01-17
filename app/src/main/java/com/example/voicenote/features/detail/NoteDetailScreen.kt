package com.example.voicenote.features.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicenote.core.components.PriorityBadge
import com.example.voicenote.core.utils.CalendarManager
import com.example.voicenote.data.model.Note
import com.example.voicenote.data.model.NoteStatus
import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.model.Task
import com.example.voicenote.data.repository.FirestoreRepository
import com.example.voicenote.ui.theme.PendingColor
import com.example.voicenote.ui.theme.PriorityHigh
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NoteDetailViewModel(
    private val noteId: String,
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {
    val note: StateFlow<Note?> = repository.getNoteById(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tasks: StateFlow<List<Task>> = repository.getTasksForNote(noteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateNote(title: String, summary: String, priority: Priority, status: NoteStatus) {
        viewModelScope.launch {
            val current = note.value ?: return@launch
            repository.saveNote(
                current.copy(
                    title = title,
                    summary = summary,
                    priority = priority,
                    status = status
                )
            )
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            repository.softDeleteNote(noteId)
        }
    }

    fun updateTask(task: Task, description: String, deadline: Long?) {
        viewModelScope.launch {
            repository.updateTask(task.copy(description = description, deadline = deadline))
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTaskStatus(task.id, !task.isDone)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.softDeleteTask(task.id)
        }
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
    val viewModel: NoteDetailViewModel = viewModel(
        factory = NoteDetailViewModel.provideFactory(noteId)
    )

    val note by viewModel.note.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val context = LocalContext.current
    val calendarManager = remember { CalendarManager(context) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "Tasks", "Transcript")

    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editSummary by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var editStatus by remember { mutableStateOf(NoteStatus.PENDING) }
    
    var editingTask by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(note) {
        note?.let {
            editTitle = it.title
            editSummary = it.summary
            editPriority = it.priority
            editStatus = it.status
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Note" else "Note Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            viewModel.updateNote(editTitle, editSummary, editPriority, editStatus)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            viewModel.deleteNote()
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        note?.let { currentNote ->
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (selectedTabIndex) {
                        0 -> { // Summary Tab
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                item {
                                    if (isEditing) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = editTitle,
                                                onValueChange = { editTitle = it },
                                                label = { Text("Title") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = editSummary,
                                                onValueChange = { editSummary = it },
                                                label = { Text("Summary") },
                                                modifier = Modifier.fillMaxWidth(),
                                                minLines = 3
                                            )
                                            Text("Priority", style = MaterialTheme.typography.labelLarge)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Priority.entries.forEach { p ->
                                                    FilterChip(
                                                        selected = editPriority == p,
                                                        onClick = { editPriority = p },
                                                        label = { Text(p.name) }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(text = currentNote.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                            PriorityBadge(priority = currentNote.priority)
                                            SuggestionChip(
                                                onClick = {}, 
                                                label = { Text(currentNote.status.name) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (currentNote.status == NoteStatus.PENDING) PendingColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                                    labelColor = if (currentNote.status == NoteStatus.PENDING) PendingColor else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Text(text = currentNote.summary, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Tasks Tab
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(tasks, key = { it.id }) { task ->
                                    TaskItem(
                                        task = task,
                                        onToggle = { viewModel.toggleTask(task) },
                                        onDelete = { viewModel.deleteTask(task) },
                                        onEdit = { editingTask = task }
                                    )
                                }
                            }
                        }
                        2 -> { // Transcript Tab
                            LazyColumn {
                                item {
                                    Text(
                                        text = currentNote.transcript.ifEmpty { "No transcript available for this session." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        editingTask?.let { task ->
            EditTaskDialog(
                task = task,
                onDismiss = { editingTask = null },
                onSave = { desc, deadline ->
                    viewModel.updateTask(task, desc, deadline)
                    if (deadline != null && deadline > System.currentTimeMillis()) {
                        calendarManager.addEventToCalendar("Task: $desc", "VoiceNote Scheduled", deadline)
                        val cal = Calendar.getInstance().apply { timeInMillis = deadline }
                        calendarManager.setAlarm("Task: $desc", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                    }
                    editingTask = null
                }
            )
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
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

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                task.priority == Priority.HIGH -> PriorityHigh.copy(alpha = 0.2f)
                else -> Color(0xFF00BFFF).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.description, 
                        modifier = Modifier.weight(1f), 
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (task.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    PriorityBadge(priority = task.priority)
                }
                
                if (isOverdue) {
                    Text("OVERDUE", style = MaterialTheme.typography.labelSmall, color = Color.Red, fontWeight = FontWeight.Bold)
                }

                task.deadline?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Due: ${dateFormat.format(Date(it))}", 
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!task.isDone && !isOverdue) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "($timeToDeadline)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (task.googlePrompt.isNotEmpty() || task.aiPrompt.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "AI Insights Ready",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onSave: (String, Long?) -> Unit) {
    var description by remember { mutableStateOf(task.description) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = task.deadline)
    val timePickerState = rememberTimePickerState(
        initialHour = if (task.deadline != null) Calendar.getInstance().apply { timeInMillis = task.deadline!! }.get(Calendar.HOUR_OF_DAY) else 17,
        initialMinute = if (task.deadline != null) Calendar.getInstance().apply { timeInMillis = task.deadline!! }.get(Calendar.MINUTE) else 0
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth())
                
                Text("Google Search Prompt:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(task.googlePrompt, style = MaterialTheme.typography.bodySmall)

                Text("AI Assistant Prompt:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(task.aiPrompt, style = MaterialTheme.typography.bodySmall)

                TextButton(onClick = { showDatePicker = true }) { Text(if (datePickerState.selectedDateMillis != null) SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(datePickerState.selectedDateMillis!!)) else "Set Date") }
                TextButton(onClick = { showTimePicker = true }) { Text("Set Time: ${timePickerState.hour}:${timePickerState.minute}") }

                if (showDatePicker) DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }) { DatePicker(state = datePickerState) }
                if (showTimePicker) AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = { TextButton(onClick = { showTimePicker = false }) { Text("OK") } }, text = { TimePicker(state = timePickerState) })
            }
        },
        confirmButton = { Button(onClick = {
            val cal = Calendar.getInstance()
            datePickerState.selectedDateMillis?.let { cal.timeInMillis = it }
            cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
            cal.set(Calendar.MINUTE, timePickerState.minute)
            onSave(description, cal.timeInMillis)
        }) { Text("Save") } }
    )
}