package com.example.voicenote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class Conflict(
    val fact: String,
    val conflict: String,
    val explanation: String,
    val severity: String
)

@Composable
fun ConflictHud(
    conflicts: List<Conflict>,
    onDismiss: () -> Unit,
    onResolve: (Conflict) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3D00))
                Spacer(Modifier.width(8.dp))
                Text(
                    "CONTEXTUAL CONFLICT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF3D00)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "The AI detected contradictions with your previous notes:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(Modifier.height(12.dp))
            
            conflicts.forEach { conflict ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("Current Note:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(conflict.fact, style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text("Previous Note:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(conflict.conflict, style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text("Explanation:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(conflict.explanation, style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Button(
                        onClick = { onResolve(conflict) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Keep Current", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Ignore All", color = Color.Gray)
                }
            }
        }
    }
}
