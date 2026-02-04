package com.example.voicenote.features.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.data.remote.NoteResponseDTO
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.components.ShimmerCard
import com.example.voicenote.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNoteClick: (String) -> Unit = {},
    onViewAllTasks: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onSearchClick: (String?) -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val taskStats by viewModel.taskStatistics.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val recentNotes by viewModel.recentNotes.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    val refreshState = rememberPullToRefreshState()
    val isRefreshing = uiState is DashboardUiState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
        // Dynamic Background Decor
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = InsightsPrimary.copy(alpha = 0.1f),
                radius = 400.dp.toPx(),
                center = Offset(x = size.width, y = 0f),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            DashboardHeader(
                userName = viewModel.userName,
                onSearchClick = { onSearchClick(null) },
                onNotificationClick = onNotificationClick
            )

            if (isOffline) {
                OfflineBanner()
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshDashboard() },
                state = refreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 1. AI Smart Greeting / Insight
                    item {
                        if (aiInsights == null && uiState is DashboardUiState.Loading) {
                            ShimmerCard(height = 80.dp)
                        } else {
                            AiInsightHero(aiInsights?.suggestion ?: "Analyzing your workflow...")
                        }
                    }

                    // 2. Core Metrics Pulse
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (uiState is DashboardUiState.Loading && wallet == null) {
                                ShimmerCard(height = 100.dp, modifier = Modifier.weight(1f))
                                ShimmerCard(height = 100.dp, modifier = Modifier.weight(1f))
                            } else {
                                PulseMetricCard(
                                    modifier = Modifier.weight(1f),
                                    label = "VELOCITY",
                                    value = when (val s = uiState) {
                                        is DashboardUiState.Success -> s.velocity
                                        else -> "--"
                                    },
                                    icon = Icons.Default.Speed,
                                    color = InsightsPrimary
                                )
                                PulseMetricCard(
                                    modifier = Modifier.weight(1f).clickable { onWalletClick() },
                                    label = "CREDITS",
                                    value = wallet?.balance?.toString() ?: "0",
                                    icon = Icons.Default.AccountBalanceWallet,
                                    color = InsightsAccentViolet
                                )
                            }
                        }
                    }

                    // 3. Task Statistics Breakdown
                    item {
                        if (taskStats == null && uiState is DashboardUiState.Loading) {
                            ShimmerCard(height = 200.dp)
                        } else {
                            TaskStatisticsCard(
                                statistics = taskStats,
                                onClick = onViewAllTasks
                            )
                        }
                    }

                    // 4. Topic Heatmap
                    item {
                        if (uiState is DashboardUiState.Loading) {
                            ShimmerCard(height = 120.dp)
                        } else {
                            val heatmap = (uiState as? DashboardUiState.Success)?.heatmap ?: emptyList()
                            TopicHeatmapSection(heatmap, onTopicClick = onSearchClick)
                        }
                    }

                    // 5. Recent Activity List
                    item {
                        Text(
                            "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    if (recentNotes.isEmpty() && uiState is DashboardUiState.Loading) {
                        items(3) { ShimmerCard(height = 72.dp) }
                    } else if (recentNotes.isEmpty()) {
                        item {
                            Text(
                                "No recent recordings found.",
                                color = Gray400,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(recentNotes) { note ->
                            RecentNoteItem(note, onClick = { onNoteClick(note.id) })
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    userName: String,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Welcome back,",
                color = Gray400,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = userName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardIconButton(Icons.Default.Notifications, onClick = onNotificationClick)
            DashboardIconButton(Icons.Default.Search, onClick = onSearchClick)
        }
    }
}

@Composable
private fun AiInsightHero(suggestion: String) {
    GlassCard(
        color = InsightsPrimary.copy(alpha = 0.05f),
        borderColor = InsightsPrimary.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(InsightsPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = InsightsPrimary)
            }
            Text(
                text = suggestion,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun PulseMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, color = Gray400, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicHeatmapSection(
    heatmap: List<com.example.voicenote.data.remote.TopicHeatmapItem>,
    onTopicClick: (String) -> Unit
) {
    Column {
        Text(
            "Intelligence Trends",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
        )
        GlassCard {
            FlowRow(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (heatmap.isEmpty()) {
                    Text("No recurring topics detected yet.", color = Gray500, fontSize = 12.sp)
                } else {
                    heatmap.forEach { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .clickable { onTopicClick(item.topic) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("${item.topic} • ${item.count}", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentNoteItem(note: NoteResponseDTO, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        intensity = 0.5f
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(InsightsPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, null, tint = InsightsPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(note.summary, color = Gray400, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Gray400.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun OfflineBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFf87171))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text("Working Offline • Data might be stale", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DashboardIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(InsightsGlassWhite)
            .border(1.dp, InsightsGlassBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
