package com.example.voicenote.features.home

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.core.service.VoiceRecordingService
import com.example.voicenote.data.model.Note
import com.example.voicenote.ui.components.ShimmerNoteItem
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    viewModel: HomeViewModel = hiltViewModel(),
    onNoteClick: (Note) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onBillingClick: () -> Unit = {},
    onJoinMeetingClick: () -> Unit = {},
    onViewAllNotes: () -> Unit = {}
) {
    val notes by viewModel.notesState.collectAsState()
    val isRecording by VoiceRecordingService.isRecording.collectAsState()
    val statusLog by VoiceRecordingService.statusLog.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val drafts by viewModel.drafts.collectAsState()
    val isLoading by viewModel.isRefreshing.collectAsState()
    
    val refreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
        // Mesh Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(InsightsPrimary.copy(alpha = 0.15f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                        radius = 1000f
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBarSimplified(
                userName = userName, 
                onSearchClick = onSearchClick,
                walletBalance = walletBalance,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedIds.size,
                onClearSelection = { viewModel.clearSelection() },
                onDeleteSelected = { viewModel.deleteSelected() },
                onArchiveSelected = { viewModel.archiveSelected() }
            )
            
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.onManualSync() },
                state = refreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item { AiStatusCardSimplified(isRecording, statusLog) }
                    
                    if (drafts.isNotEmpty()) {
                        item {
                            DraftsSection(
                                drafts = drafts,
                                onRetry = { viewModel.retryDraft(it) },
                                onDelete = { viewModel.deleteDraft(it) }
                            )
                        }
                    }
                    
                    // Hero Record Section Removed as requested
                    
                    item { 
                        QuickActionsRow(
                            onJoinMeetingClick = onJoinMeetingClick,
                            onBillingClick = onBillingClick
                        )
                    }
                    
                    item { 
                        RecentNotesHeaderSimplified(onViewAllClick = onViewAllNotes) 
                    }
                    
                    if (isLoading && notes.isEmpty()) {
                        items(5) { ShimmerNoteItem() }
                    } else if (notes.isEmpty()) {
                        item {
                            EmptyNotesPlaceholder()
                        }
                    } else {
                        items(notes, key = { it.id }) { note ->
                            HomeScreenNoteItemSimplified(
                                note = note, 
                                isSelected = selectedIds.contains(note.id),
                                isSelectionMode = isSelectionMode,
                                onClick = { 
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(note.id)
                                    } else {
                                        onNoteClick(note)
                                    }
                                },
                                onLongClick = { viewModel.toggleSelection(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onJoinMeetingClick: () -> Unit,
    onBillingClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onJoinMeetingClick,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InsightsGlassWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, InsightsGlassBorder)
        ) {
            Icon(Icons.Default.Groups, null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Meeting", fontWeight = FontWeight.Bold, color = Color.White)
        }
        Button(
            onClick = onBillingClick,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InsightsGlassWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, InsightsGlassBorder)
        ) {
            Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Billing", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun EmptyNotesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp), 
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Description, null, modifier = Modifier.size(48.dp), tint = Gray400.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text("No notes yet. Start recording to see them here.", color = Gray400, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HomeTopBarSimplified(
    userName: String, 
    onSearchClick: () -> Unit,
    walletBalance: Int?,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onArchiveSelected: () -> Unit = {}
) {
    if (isSelectionMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SimplifiedIconButton(icon = Icons.Default.Close, onClick = onClearSelection)
                Text("$selectedCount selected", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SimplifiedIconButton(icon = Icons.Default.Archive, onClick = onArchiveSelected)
                SimplifiedIconButton(icon = Icons.Default.Delete, onClick = onDeleteSelected)
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(InsightsPrimary.copy(alpha = 0.1f))
                        .border(1.dp, InsightsPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        color = InsightsPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Column {
                    Text("Welcome back,", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (walletBalance != null) {
                    Surface(
                        color = InsightsPrimary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, InsightsPrimary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            "$walletBalance 🪙", 
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = InsightsPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                SimplifiedIconButton(icon = Icons.Default.Search, onClick = onSearchClick)
            }
        }
    }
}

@Composable
private fun SimplifiedIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(InsightsGlassWhite, CircleShape)
            .border(1.dp, InsightsGlassBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AiStatusCardSimplified(isRecording: Boolean, statusLog: String) {
    val isProcessing = statusLog.contains("Synchronizing") || statusLog.contains("Optimizing") || statusLog.contains("Analytics pending")
    
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(InsightsGlassWhite)
                .border(1.dp, if (isProcessing) InsightsPrimary.copy(alpha = 0.5f) else InsightsGlassBorder, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when {
                                        isRecording -> InsightsPrimary
                                        isProcessing -> Color(0xFFFACC15) // Yellow for processing
                                        else -> Color(0xFF22c55e)
                                    }, 
                                    CircleShape
                                )
                        )
                        Text(
                            when {
                                isRecording -> "AI is Capturing..."
                                isProcessing -> "AI is Processing..."
                                else -> "AI is idle · Ready"
                            }, 
                            color = Color.White, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        statusLog, 
                        color = Color.White.copy(alpha = 0.4f), 
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Box(modifier = Modifier.background(InsightsPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = InsightsPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            if (isRecording) Icons.Default.GraphicEq else Icons.Default.AutoAwesome, 
                            contentDescription = null, 
                            tint = InsightsPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentNotesHeaderSimplified(onViewAllClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Recent Intel", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "View All", 
            color = InsightsPrimary, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onViewAllClick() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreenNoteItemSimplified(
    note: Note, 
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) InsightsPrimary.copy(alpha = 0.1f) else InsightsGlassWhite)
            .border(
                1.dp, 
                if (isSelected) InsightsPrimary else InsightsGlassBorder, 
                RoundedCornerShape(24.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = InsightsPrimary,
                        uncheckedColor = Color.White.copy(alpha = 0.3f),
                        checkmarkColor = Color.White
                    )
                )
            }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(note.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        val dateStr = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(note.timestamp))
                        Text(dateStr, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                    if (!isSelectionMode) {
                        Icon(
                            Icons.Default.ChevronRight, 
                            contentDescription = null, 
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Text(
                    "\"${note.summary}\"", 
                    color = Color.White.copy(alpha = 0.7f), 
                    fontSize = 14.sp, 
                    maxLines = 2,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteTagSimplified(icon = Icons.Default.Timer, label = "AI PROCESSED", color = Color(0xFF4ade80))
                    if (note.isPinned) {
                        NoteTagSimplified(icon = Icons.Default.PushPin, label = "PRIORITY", color = Color(0xFFfacc15))
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteTagSimplified(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), CircleShape)
            .border(1.dp, color.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DraftsSection(
    drafts: List<java.io.File>,
    onRetry: (java.io.File) -> Unit,
    onDelete: (java.io.File) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pending Uploads", color = InsightsPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${drafts.size} local drafts", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
        
        drafts.forEach { file ->
            DraftItem(file = file, onRetry = onRetry, onDelete = onDelete)
        }
    }
}

@Composable
private fun DraftItem(
    file: java.io.File,
    onRetry: (java.io.File) -> Unit,
    onDelete: (java.io.File) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRetry(file) },
        borderColor = InsightsPrimary.copy(alpha = 0.3f),
        intensity = 0.6f
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = {
                    if (isPlaying) {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        isPlaying = false
                    } else {
                        try {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(file.absolutePath)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    isPlaying = false
                                    it.release()
                                    mediaPlayer = null
                                }
                            }
                            isPlaying = true
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Cannot play draft", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = InsightsPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(file.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("Tap to sync with AI...", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
            Row {
                IconButton(onClick = { onDelete(file) }) {
                    Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onRetry(file) }) {
                    Icon(Icons.Default.CloudUpload, null, tint = InsightsPrimary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
