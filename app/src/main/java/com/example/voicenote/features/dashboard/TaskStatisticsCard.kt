package com.example.voicenote.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voicenote.ui.components.GlassCard

@Composable
fun TaskStatisticsCard(
    statistics: TaskStatistics?,
    onClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        intensity = 1.2f
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Task Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF)
                )
            }

            if (statistics == null) {
                Text(
                    "Loading statistics...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                // Completion Rate
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Completion Rate",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Text(
                            "${statistics.completionRate.toInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                    LinearProgressIndicator(
                        progress = (statistics.completionRate / 100).toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF00E5FF),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Task Counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Total",
                        value = statistics.totalTasks.toString(),
                        icon = Icons.Default.Assignment,
                        color = Color.White
                    )
                    StatItem(
                        label = "Completed",
                        value = statistics.completedTasks.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50)
                    )
                    StatItem(
                        label = "Pending",
                        value = statistics.pendingTasks.toString(),
                        icon = Icons.Default.PendingActions,
                        color = Color(0xFFFFC107)
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Priority Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "By Priority",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PriorityStatItem("HIGH", statistics.highPriority, Color(0xFFFF5252))
                        PriorityStatItem("MEDIUM", statistics.mediumPriority, Color(0xFFFFC107))
                        PriorityStatItem("LOW", statistics.lowPriority, Color(0xFF4CAF50))
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Deadline Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Overdue",
                        value = statistics.overdue.toString(),
                        icon = Icons.Default.Warning,
                        color = Color(0xFFFF5252)
                    )
                    StatItem(
                        label = "Due Today",
                        value = statistics.dueToday.toString(),
                        icon = Icons.Default.Today,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PriorityStatItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
