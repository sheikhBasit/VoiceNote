package com.example.voicenote.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicenote.ui.theme.GlassyCard
import com.example.voicenote.ui.theme.GlassyEffects
import com.example.voicenote.ui.theme.Primary
import com.example.voicenote.ui.theme.Secondary

import androidx.compose.animation.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Warning

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val isOffline by viewModel.isOffline.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassyEffects.PremiumBackground)
    ) {
        Column {
            AnimatedVisibility(
                visible = isOffline,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFF3D00).copy(alpha = 0.15f))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF3D00), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONNECTION LOST: SYNC PAUSED", color = Color(0xFFFF3D00), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is DashboardUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)
                    is DashboardUiState.Error -> Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                    is DashboardUiState.Success -> DashboardContent(state, isOffline)
                }
            }
        }
    }
}

@Composable
fun DashboardContent(state: DashboardUiState.Success, isOffline: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = "Productivity Pulse",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        if (isOffline) {
            item {
                Text(
                    "Actions restricted during offline mode",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        item {
            GlassyCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Velocity", color = Color.Gray, fontSize = 14.sp)
                        Text(state.velocity, color = Primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(
                        Icons.Default.AutoGraph, 
                        contentDescription = null, 
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatMetric(
                    modifier = Modifier.weight(1f),
                    label = "Active Tasks",
                    value = state.activeTasks,
                    icon = Icons.Default.Bolt,
                    color = Secondary
                )
                StatMetric(
                    modifier = Modifier.weight(1f),
                    label = "AI Insights",
                    value = state.aiInsights,
                    icon = Icons.Default.Psychology,
                    color = Primary
                )
            }
        }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Recent Intelligence",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Placeholder for real notes list
            items(5) {
                RecentNoteItem()
            }
        }
    }
}

@Composable
fun StatMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    GlassyCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.Start) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun RecentNoteItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Secondary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Secondary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Project Alpha Strategy", color = Color.White, fontWeight = FontWeight.Medium)
                Text("Synthesized 2 hours ago", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
