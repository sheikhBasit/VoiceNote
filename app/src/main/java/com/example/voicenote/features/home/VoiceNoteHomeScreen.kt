package com.example.voicenote.features.home

import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicenote.ui.theme.*

@Composable
fun VoiceNoteHomeScreen(
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onViewAllClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
        // Background Mesh Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(InsightsPrimary.copy(alpha = 0.15f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset.Infinite,
                        radius = 1000f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            HomeTopBar(onSearchClick, onSettingsClick)
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item { AiStatusCard() }
                item { HeroRecordSection() }
                item { RecentNotesHeader(onViewAllClick) }
                items(dummyNotes) { note ->
                    NoteCard(note, onClick = { onNoteClick(note.id) })
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        HomeBottomNavigation(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun HomeTopBar(onSearchClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCWuQR5W_BeDmrZmdPs7N2U26vVN-CZk2XfADQiRwuePYqjuVVV-kMCBtMANV_wFowADd_mKlB2E_aGsIa_LC2tEskC4sd5ahsSfmuKv6AxfN_xYFmCEx2cGhsYDv41sxO8zZAk9tGBoMxCf-t0ckjUeTMnQG6k6LecQSVbKjmnCPm9aYjSjuZ7MHXWq8vrtxTNRoVndkRQWOsM7TIc9PKuF200A6oWIAas5sPnJTfEHbO4GtRuQCEV1hFmYF3a2t_re7uPDDI1_FQ4",
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "Good Morning", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(text = "Alex Rivera", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeIconButton(icon = Icons.Default.Search, onClick = onSearchClick)
            HomeIconButton(icon = Icons.Default.Settings, onClick = onSettingsClick)
        }
    }
}

@Composable
fun HomeIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun AiStatusCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF22c55e), CircleShape))
                    Text(text = "AI is idle · Ready to listen", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "Waiting for your voice to begin transcription", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .background(InsightsPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = InsightsPrimary)
            }
        }
    }
}

@Composable
fun HeroRecordSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(InsightsPrimary.copy(alpha = 0.2f), CircleShape)
            )
            
            Surface(
                modifier = Modifier.size(112.dp),
                shape = CircleShape,
                color = InsightsPrimary,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        Text(
            text = "TAP TO RECORD",
            modifier = Modifier.padding(top = 32.dp),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun RecentNotesHeader(onViewAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Recent Notes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "View All", 
            color = InsightsPrimary, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onViewAllClick() }
        )
    }
}

data class DummyNote(val id: String, val title: String, val time: String, val duration: String, val snippet: String, val tasks: Int, val sentiment: String)
val dummyNotes = listOf(
    DummyNote("1", "Product Strategy Sync", "Today, 10:30 AM", "42m", "Key objectives include the new dark mode release and API documentation update...", 8, "POSITIVE"),
    DummyNote("2", "Client Feedback: Helios App", "Yesterday", "18m", "The client requested more glassmorphism elements in the dashboard...", 3, "URGENT")
)

@Composable
fun NoteCard(note: DummyNote, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = note.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${note.time} • ${note.duration}", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
            }
            Text(text = "\"${note.snippet}\"", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag(icon = Icons.Default.Checklist, text = "${note.tasks} TASKS", tint = Color(0xFF4ade80))
                Tag(icon = if(note.sentiment == "URGENT") Icons.Default.PriorityHigh else Icons.Default.SentimentSatisfied, text = note.sentiment, tint = if(note.sentiment == "URGENT") Color(0xFFfacc15) else Color(0xFF60a5fa))
            }
        }
    }
}

@Composable
fun Tag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(100.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text = text, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HomeBottomNavigation(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, InsightsBackgroundDark.copy(alpha = 0.8f))))
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIcon(icon = Icons.Default.Home, label = "Home", active = true)
            NavIcon(icon = Icons.Default.Folder, label = "Library")
            NavIcon(icon = Icons.Default.Analytics, label = "Insights")
            NavIcon(icon = Icons.Default.Person, label = "Profile")
        }
    }
}

@Composable
fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = label, tint = if (active) InsightsPrimary else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(26.dp))
        Text(text = label.uppercase(), color = if (active) InsightsPrimary else Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}
