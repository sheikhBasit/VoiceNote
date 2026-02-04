package com.example.voicenote.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.ui.theme.*
import com.example.voicenote.ui.components.GlassCard

@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String, NotificationType) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(InsightsPrimary.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(InsightsGlassWhite)
                        .border(1.dp, InsightsGlassBorder, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Intelligence Center",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (notifications.isEmpty() && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(64.dp), tint = Gray400.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text("No updates yet", color = Gray400, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(notifications) { item ->
                        NotificationCard(item) {
                            onNavigateToDetail(item.targetId, item.type)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationItem, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.clickable { onClick() },
        intensity = 0.5f
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(InsightsPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val icon = when(item.type) {
                    NotificationType.TASK -> Icons.Default.Assignment
                    NotificationType.NOTE -> Icons.Default.Description
                    NotificationType.SYSTEM -> Icons.Default.Verified
                }
                Icon(icon, null, tint = InsightsPrimary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = InsightsPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(item.message, color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp)),
                    color = Gray400,
                    fontSize = 10.sp
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        }
    }
}
