package com.example.voicenote.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.voicenote.data.model.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCreateDialog(
    onDismiss: () -> Unit,
    onCreateNote: (title: String, summary: String, priority: Priority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Note") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("Enter note title...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Summary/Content Input
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary/Content") },
                    placeholder = { Text("Enter note content...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 4,
                    maxLines = 8
                )

                // Priority Selector
                Column {
                    Text("Priority", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Priority.values().forEach { priority ->
                            FilterChip(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority },
                                label = { Text(priority.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateNote(title.trim(), summary.trim(), selectedPriority)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
