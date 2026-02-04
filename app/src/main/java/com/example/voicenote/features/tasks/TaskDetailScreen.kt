package com.example.voicenote.features.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.voicenote.core.components.PriorityBadge
import com.example.voicenote.data.model.CommunicationType
import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.model.Task
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    onNavigateToNote: (String) -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val task = remember(tasks, taskId) { tasks.find { it.id == taskId } }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = InsightsBackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Task Detail", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = InsightsBackgroundDark),
                actions = {
                    if (task != null) {
                        IconButton(onClick = { viewModel.toggleTask(task) }) {
                            Icon(
                                imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Toggle Status",
                                tint = if (task.isDone) InsightsPrimary else Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = InsightsPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Description Card
                GlassCard(intensity = 0.6f) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Description",
                            style = MaterialTheme.typography.labelMedium,
                            color = InsightsPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            task.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            lineHeight = 24.sp
                        )
                    }
                }

                // Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailInfoBox(
                        modifier = Modifier.weight(1f),
                        label = "Priority",
                        content = { PriorityBadge(priority = task.priority) }
                    )
                    DetailInfoBox(
                        modifier = Modifier.weight(1f),
                        label = "Deadline",
                        content = {
                            Text(
                                task.deadline?.let { dateFormat.format(Date(it)) } ?: "None",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    )
                }

                // Assignment Info
                if (task.assignedContactName != null || task.communicationType != null) {
                    GlassCard(intensity = 0.4f) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Assignment & Action",
                                style = MaterialTheme.typography.labelMedium,
                                color = InsightsPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            if (task.assignedContactName != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(task.assignedContactName ?: "Unknown", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        task.assignedContactPhone?.let {
                                            Text(it, color = Gray400, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            if (task.communicationType != null) {
                                Spacer(Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Chat, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Action: ${task.communicationType?.name}",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Multimedia
                if (task.imageUrl != null) {
                    Column {
                        Text(
                            "Attachments",
                            style = MaterialTheme.typography.labelMedium,
                            color = InsightsPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        AsyncImage(
                            model = task.imageUrl,
                            contentDescription = "Task Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, InsightsGlassBorder, RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Source Note Link
                if (task.noteId != null) {
                    Button(
                        onClick = { onNavigateToNote(task.noteId!!) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = InsightsGlassWhite),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Description, null, tint = InsightsPrimary)
                        Spacer(Modifier.width(12.dp))
                        Text("View Source Note", color = Color.White)
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                // Delete Button
                TextButton(
                    onClick = { 
                        viewModel.deleteTasks(listOf(task.id))
                        onBack()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Task", color = Color(0xFFFF5252))
                }
            }
        }
    }
}

@Composable
private fun DetailInfoBox(
    modifier: Modifier = Modifier,
    label: String,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = modifier, intensity = 0.4f) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
