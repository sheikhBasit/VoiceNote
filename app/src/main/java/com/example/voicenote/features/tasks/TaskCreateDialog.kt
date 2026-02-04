package com.example.voicenote.features.tasks

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.voicenote.data.model.CommunicationType
import com.example.voicenote.data.model.Priority
import com.example.voicenote.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateDialog(
    noteId: String?,
    onDismiss: () -> Unit,
    onCreateTask: (
        description: String, 
        priority: Priority, 
        deadline: Long?,
        assignedName: String?,
        assignedPhone: String?,
        commType: CommunicationType?,
        attachment: File?
    ) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var deadlineTimestamp by remember { mutableStateOf<Long?>(null) }
    
    var assignedName by remember { mutableStateOf("") }
    var assignedPhone by remember { mutableStateOf("") }
    var selectedCommType by remember { mutableStateOf<CommunicationType?>(null) }
    
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachmentUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = InsightsBackgroundDark,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, InsightsGlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    "New Intelligence Task",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Task Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("What needs to be done?", color = Color.White.copy(alpha = 0.6f)) },
                        placeholder = { Text("Task description extracted from intelligence...", color = Color.White.copy(alpha = 0.3f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = InsightsPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            cursorColor = InsightsPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 4
                    )

                    // Assignment Section
                    Column {
                        Text("Assign to Contact (Optional)", color = InsightsPrimary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = assignedName,
                                onValueChange = { assignedName = it },
                                label = { Text("Name", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = assignedPhone,
                                onValueChange = { assignedPhone = it },
                                label = { Text("Phone", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }

                    // Communication Type
                    Column {
                        Text("Action Channel", color = InsightsPrimary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CommunicationType.entries.forEach { type ->
                                val isSelected = selectedCommType == type
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) InsightsPrimary else Color.White.copy(alpha = 0.05f))
                                        .clickable { selectedCommType = if (isSelected) null else type },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when(type) {
                                            CommunicationType.WHATSAPP -> Icons.Default.Chat
                                            CommunicationType.SMS -> Icons.Default.Sms
                                            CommunicationType.CALL -> Icons.Default.Call
                                            CommunicationType.MEET -> Icons.Default.VideoCall
                                            CommunicationType.SLACK -> Icons.Default.AlternateEmail
                                        },
                                        contentDescription = type.name,
                                        tint = if (isSelected) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Priority Selector
                    Column {
                        Text("Priority", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Priority.entries.forEach { priority ->
                                val isSelected = selectedPriority == priority
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .background(
                                            if (isSelected) InsightsPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) InsightsPrimary else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedPriority = priority },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        priority.name,
                                        color = if (isSelected) InsightsPrimary else Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Deadline and Attachment Row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Deadline
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Deadline", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, null, tint = InsightsPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        deadlineTimestamp?.let { dateFormat.format(Date(it)) } ?: "Set Date",
                                        color = if (deadlineTimestamp != null) Color.White else Color.White.copy(alpha = 0.4f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Attachment
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Attachment", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { imagePicker.launch("image/*") }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachFile, null, tint = InsightsPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        attachmentUri?.lastPathSegment ?: "Add File",
                                        color = if (attachmentUri != null) Color.White else Color.White.copy(alpha = 0.4f),
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = {
                            if (description.isNotBlank()) {
                                var file: File? = null
                                attachmentUri?.let { uri ->
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                                        val outputStream = FileOutputStream(tempFile)
                                        inputStream?.copyTo(outputStream)
                                        file = tempFile
                                    } catch (e: Exception) {}
                                }
                                
                                onCreateTask(
                                    description.trim(), 
                                    selectedPriority, 
                                    deadlineTimestamp,
                                    assignedName.takeIf { it.isNotBlank() },
                                    assignedPhone.takeIf { it.isNotBlank() },
                                    selectedCommType,
                                    file
                                )
                            }
                        },
                        enabled = description.isNotBlank(),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InsightsPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Sync Task", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        deadlineTimestamp = selectedDate
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) {
                    Text("OK", color = InsightsPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            colors = DatePickerDefaults.colors(containerColor = InsightsBackgroundDark)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = InsightsBackgroundDark,
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color.White.copy(alpha = 0.6f),
                    subheadContentColor = Color.White.copy(alpha = 0.6f),
                    yearContentColor = Color.White,
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = InsightsPrimary,
                    dayContentColor = Color.White,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = InsightsPrimary,
                    todayContentColor = InsightsPrimary
                )
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineTimestamp?.let { date ->
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = date
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        deadlineTimestamp = calendar.timeInMillis
                    }
                    showTimePicker = false
                }) {
                    Text("OK", color = InsightsPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = InsightsBackgroundDark,
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color.White.copy(alpha = 0.05f),
                        clockDialSelectedContentColor = Color.Black,
                        clockDialUnselectedContentColor = Color.White,
                        selectorColor = InsightsPrimary,
                        periodSelectorBorderColor = InsightsPrimary,
                        periodSelectorSelectedContainerColor = InsightsPrimary.copy(alpha = 0.2f),
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = InsightsPrimary,
                        periodSelectorUnselectedContentColor = Color.White,
                        timeSelectorSelectedContainerColor = InsightsPrimary.copy(alpha = 0.2f),
                        timeSelectorUnselectedContainerColor = Color.White.copy(alpha = 0.05f),
                        timeSelectorSelectedContentColor = InsightsPrimary,
                        timeSelectorUnselectedContentColor = Color.White
                    )
                )
            }
        )
    }
}
