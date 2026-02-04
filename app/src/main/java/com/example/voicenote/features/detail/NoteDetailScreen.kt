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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.voicenote.core.components.PriorityBadge
import com.example.voicenote.core.utils.ActionExecutor
import com.example.voicenote.core.utils.CalendarManager
import com.example.voicenote.core.utils.ContactInfo
import com.example.voicenote.core.utils.ContactManager
import com.example.voicenote.core.utils.PdfManager
import com.example.voicenote.data.model.*
import com.example.voicenote.ui.theme.PendingColor
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.components.GlassTabRow
import com.example.voicenote.ui.components.GlassyTextField
import com.example.voicenote.ui.components.NoteDetailShimmer
import com.example.voicenote.ui.theme.InsightsPrimary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    onBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
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
    val tabs = listOf("Summary", "Tasks", "Transcript", "Ask AI")
    
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editSummary by remember { mutableStateOf("") }
    var editTranscript by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var editStatus by remember { mutableStateOf(NoteStatus.PENDING) }
    
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var speakerToRename by remember { mutableStateOf<String?>(null) }

    val refreshState = rememberPullToRefreshState()
    val isRefreshing = note == null

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
                title = { Text(if (isEditing) "Edit Intel" else "Note Analysis") },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    } 
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            viewModel.updateNote(editTitle, editSummary, editTranscript, editPriority, editStatus)
                            isEditing = false
                        }) { Icon(Icons.Default.Save, contentDescription = "Save") }
                    } else {
                        IconButton(onClick = { 
                            viewModel.getWhatsAppDraft { draft ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=${Uri.encode(draft)}"))
                                context.startActivity(intent)
                            }
                        }) { Icon(Icons.Default.Message, contentDescription = "WhatsApp", tint = Color(0xFF25D366)) }
                        IconButton(onClick = { viewModel.triggerSemanticAnalysis() }) { 
                            Icon(Icons.Default.Analytics, contentDescription = "Re-analyze") 
                        }
                        IconButton(onClick = { isEditing = true }) { 
                            Icon(Icons.Default.Edit, contentDescription = "Edit") 
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isEditing && note != null) {
                PersistentNoteActions(
                    onShareRecap = { shareRecap(context, note, tasks) },
                    onExportPdf = { note?.let { pdfManager.generateNotePdf(it, tasks) } },
                    onDelete = { 
                        viewModel.deleteNote()
                        onBack()
                    }
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.triggerSemanticAnalysis() },
            state = refreshState,
            modifier = Modifier.padding(padding)
        ) {
            note?.let { currentNote ->
                Column(modifier = Modifier.fillMaxSize()) {
                    GlassTabRow(
                        selectedTabIndex = selectedTabIndex,
                        tabs = tabs,
                        onTabSelected = { selectedTabIndex = it }
                    )

                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
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
            } ?: NoteDetailShimmer()
        }

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

@Composable
private fun PersistentNoteActions(onShareRecap: () -> Unit, onExportPdf: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = Color.Black.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(Icons.Default.Share, "Share Recap", onShareRecap)
            ActionButton(Icons.Default.PictureAsPdf, "PDF Export", onExportPdf)
            ActionButton(Icons.Default.Delete, "Delete", onDelete, isDanger = true)
        }
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, isDanger: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(icon, null, tint = if (isDanger) Color(0xFFFF5252) else Color.White.copy(alpha = 0.7f))
        Text(label, fontSize = 10.sp, color = if (isDanger) Color(0xFFFF5252) else Color.White.copy(alpha = 0.5f))
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
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GlassyTextField(value = editTitle, onValueChange = onTitleChange, label = "Title")
                    GlassyTextField(value = editSummary, onValueChange = onSummaryChange, label = "Executive Summary")
                    
                    Text("Priority Level", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Priority.entries.forEach { p ->
                            FilterChip(
                                selected = editPriority == p, 
                                onClick = { onPriorityChange(p) }, 
                                label = { Text(p.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF00E5FF)
                                )
                            )
                        }
                    }
                }
            } else {
                GlassCard(intensity = 0.8f) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = note.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriorityBadge(priority = note.priority)
                            Box(modifier = Modifier.size(4.dp).background(Color.White.copy(alpha=0.3f), CircleShape))
                            Text(text = note.status.name, color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha=0.05f))
                        Text(
                            text = note.summary, 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 24.sp
                        )
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
                setOnPreparedListener { duration = it.duration }
                setOnCompletionListener { 
                    isPlaying = false
                    currentPosition = 0
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    DisposableEffect(Unit) { onDispose { mediaPlayer?.release() } }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = mediaPlayer?.currentPosition ?: 0
            delay(500)
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), intensity = 0.5f) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (isPlaying) mediaPlayer?.pause() else mediaPlayer?.start()
                    isPlaying = !isPlaying
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(32.dp)
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
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentPosition), fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                Text(formatTime(duration), fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
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
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            note.audioUrl?.let { AudioPlayer(it) }
        }
        if (speakers.isNotEmpty()) {
            item {
                Text("Speakers in Conversation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    speakers.forEach { speaker ->
                        AssistChip(
                            onClick = { onRenameSpeaker(speaker) }, 
                            label = { Text("Rename $speaker") },
                            leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }
        item {
            GlassCard(intensity = 0.3f) {
                SelectionContainer {
                    Text(
                        text = note.transcript.ifEmpty { "No transcript available." },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TasksTab(tasks: List<Task>, allActiveTasks: List<Task>, viewModel: NoteDetailViewModel, actionExecutor: ActionExecutor, onEdit: (Task) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (tasks.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("No action items extracted.", color = Color.White.copy(alpha = 0.4f))
                }
            }
        }
        items(tasks, key = { it.id }) { task ->
            val hasConflict = task.deadline != null && allActiveTasks.any { 
                it.id != task.id && it.deadline != null && abs(it.deadline!! - task.deadline!!) < 600000 
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
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassyTextField(
            value = question, 
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Ask about this session...",
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send)
        )
        
        Button(
            onClick = { if (question.isNotBlank()) onAsk(question) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !isProcessing && question.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Consult AI Brain", fontWeight = FontWeight.Bold)
        }

        response?.let { aiResponseText ->
            GlassCard(color = InsightsPrimary.copy(alpha = 0.05f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(32.dp).background(InsightsPrimary.copy(alpha=0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, null, tint = InsightsPrimary, modifier = Modifier.size(16.dp))
                        }
                        Text("AI Analysis", color = InsightsPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(text = aiResponseText, style = MaterialTheme.typography.bodyMedium, color = Color.White, lineHeight = 20.sp)
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { shareText(context, aiResponseText) }) { Icon(Icons.Default.Share, null, tint = Color.White.copy(alpha=0.5f)) }
                        IconButton(onClick = { 
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Answer", aiResponseText))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha=0.5f)) }
                    }
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Default.Warning, null, tint = Color.Yellow, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Deadline Conflict Detected", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = task.isDone, 
                    onCheckedChange = { onToggle() }, 
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF))
                )
                Column(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                    Text(
                        text = task.description, 
                        style = MaterialTheme.typography.bodyLarge, 
                        fontWeight = FontWeight.Bold, 
                        color = if (task.isDone) Color.White.copy(alpha=0.4f) else Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    PriorityBadge(priority = task.priority)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha=0.3f), modifier = Modifier.size(20.dp)) }
            }
            
            if (task.deadline != null) {
                Row(modifier = Modifier.padding(start = 48.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, tint = if (isOverdue) Color.Red else Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = dateFormat.format(Date(task.deadline!!)), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (isOverdue) Color.Red else Color.White.copy(alpha = 0.6f)
                    )
                    if (!task.isDone && !isOverdue) {
                        Spacer(Modifier.width(8.dp))
                        Text(text = "($timeToDeadline)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (task.imageUrl != null) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = task.imageUrl,
                    contentDescription = "Task image",
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            if (!task.isDone) {
                Row(modifier = Modifier.padding(top = 12.dp, start = 48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistantButton("Search", Icons.Default.Search) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(task.description)}")))
                    }
                    AssistantButton("AI Draft", Icons.Default.AutoAwesome) {
                        copyAndOpen(context, "Draft for: ${task.description}", "com.openai.chatgpt", "https://chat.openai.com")
                    }
                }
                if (task.assignedContactPhone != null && !task.isActionApproved) {
                    Button(
                        onClick = onApprove, 
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Approve ${task.communicationType} to ${task.assignedContactName}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick, 
        contentPadding = PaddingValues(horizontal = 12.dp), 
        modifier = Modifier.height(32.dp),
        shape = CircleShape
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun shareRecap(context: Context, note: Note?, tasks: List<Task>) {
    if (note == null) return
    val taskList = tasks.joinToString("\n") { "- [${if (it.isDone) "x" else " "}] ${it.description} (${it.priority})" }
    val text = "Intel Recap: ${note.title}\n\nSummary:\n${note.summary}\n\nAction Items:\n$taskList"
    shareText(context, text, "Intel Recap: ${note.title}")
}

private fun shareText(context: Context, text: String, subject: String? = null) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Intelligence"))
}

private fun sendEmail(context: Context, text: String, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try { context.startActivity(intent) } catch (e: Exception) { shareText(context, text, subject) }
}

private fun copyAndOpen(context: Context, text: String, packageName: String, fallbackUrl: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("AI Prompt", text))
    Toast.makeText(context, "Copied to clipboard.", Toast.LENGTH_SHORT).show()
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
        title = { Text("Refine Action Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassyTextField(value = description, onValueChange = { description = it }, label = "Item")
                GlassyTextField(value = contactQuery, onValueChange = { contactQuery = it; selectedContact = null }, label = "Assignee")
                if (contactResults.isNotEmpty() && selectedContact == null) {
                    Card(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp)) {
                        LazyColumn { items(contactResults) { contact -> TextButton(onClick = { selectedContact = contact; contactQuery = contact.name }, modifier = Modifier.fillMaxWidth()) { Text("${contact.name} (${contact.phoneNumber})") } } }
                    }
                }
                if (selectedContact != null) {
                    Text("Outreach Method", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommunicationType.entries.forEach { type -> 
                            FilterChip(
                                selected = commType == type, 
                                onClick = { commType = type }, 
                                label = { Text(type.name, fontSize = 10.sp) },
                                shape = CircleShape
                            ) 
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (datePickerState.selectedDateMillis != null) SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(datePickerState.selectedDateMillis!!)) else "Set Date",
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${timePickerState.hour}:${String.format("%02d", timePickerState.minute)}",
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha=0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=0.1f))
                ) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Visual")
                }
                if (imageUrl != null) {
                    Text("Image attached ✓", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4ADE80), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                if (showDatePicker) DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }) { DatePicker(state = datePickerState) }
                if (showTimePicker) AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = { TextButton(onClick = { showTimePicker = false }) { Text("OK") } }, text = { TimePicker(state = timePickerState) })
            }
        },
        confirmButton = { 
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    datePickerState.selectedDateMillis?.let { cal.timeInMillis = it }
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    onSave(description, cal.timeInMillis, selectedContact?.name, selectedContact?.phoneNumber, commType, imageUrl)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
            ) { Text("Update Item", fontWeight = FontWeight.Bold) } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
