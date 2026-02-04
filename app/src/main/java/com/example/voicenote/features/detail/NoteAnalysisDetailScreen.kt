package com.example.voicenote.features.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.theme.*

@Composable
fun NoteAnalysisDetailScreen(onBack: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnalysisTopBar(onBack)
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item { MediaPlayerSection() }
                item { AiInsightsBadges() }
                item { SummarySection() }
                item { ActionItemsSection() }
            }
        }
        
        TranscriptCollapsibleBar(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun AnalysisTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InsightsBackgroundDark.copy(alpha = 0.8f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
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
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Product Strategy Sync", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("OCT 24 • 14:20", color = Color(0xFFa19db9), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(InsightsGlassWhite)
                .border(1.dp, InsightsGlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MediaPlayerSection() {
    Box(modifier = Modifier.padding(24.dp)) {
        GlassCard(color = InsightsGlassWhite.copy(alpha = 0.04f), borderColor = InsightsGlassBorder) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.height(80.dp).fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val bars = listOf(24, 40, 64, 48, 32, 56, 40, 48, 64, 40, 24, 32, 56, 48, 24, 40, 64, 48, 32)
                    bars.forEachIndexed { index, h ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(h.dp)
                                .clip(RoundedCornerShape(100))
                                .background(if (index < 7) InsightsPrimary else InsightsPrimary.copy(alpha = 0.3f))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .fillMaxHeight()
                                .background(InsightsPrimary)
                                .shadow(10.dp, spotColor = InsightsPrimary)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("12:45", color = Color(0xFFa19db9), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("28:10", color = Color(0xFFa19db9), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Icon(Icons.Default.Replay10, contentDescription = null, tint = Color.White.copy(alpha=0.6f), modifier = Modifier.size(24.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(InsightsPrimary, CircleShape)
                            .shadow(16.dp, CircleShape, spotColor = InsightsPrimary.copy(alpha=0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Icon(Icons.Default.Forward30, contentDescription = null, tint = Color.White.copy(alpha=0.6f), modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AiInsightsBadges() {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InsightBadge(icon = Icons.Default.SentimentSatisfied, label = "Sentiment", value = "Positive", color = Color(0xFF22c55e))
        InsightBadge(icon = Icons.Default.Campaign, label = "Tone", value = "Collaborative", color = InsightsPrimary)
    }
}

@Composable
private fun RowScope.InsightBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(InsightsGlassWhite)
            .border(1.dp, InsightsGlassBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(32.dp).background(color.copy(alpha=0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(label.uppercase(), color = Color(0xFFa19db9), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SummarySection() {
    Column(modifier = Modifier.padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("AI Executive Summary", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = InsightsPrimary)
        }
        
        GlassCard(color = InsightsGlassWhite.copy(alpha = 0.04f), borderColor = InsightsGlassBorder) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryItem("Finalized the Q4 product roadmap focus areas, prioritizing the mobile-first redesign.")
                SummaryItem("Identified three key technical blockers for the API integration phase.")
                SummaryItem("Agreed on increasing the marketing budget for the launch event in November.")
            }
        }
    }
}

@Composable
private fun SummaryItem(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.padding(top = 8.dp).size(6.dp).background(InsightsPrimary, CircleShape))
        Text(text = text, color = Color(0xFFd1d0da), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun ActionItemsSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Action Items", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.background(InsightsPrimary.copy(alpha=0.2f), CircleShape).padding(horizontal=8.dp, vertical=2.dp)) {
                Text("4 PENDING", color = InsightsPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("Review API documentation", "Assigned to: Sarah M.", "HIGH", Color(0xFFef4444))
            ActionCard("Draft Q4 budget proposal", "Completed", "MED", Color(0xFFeab308), isCompleted = true)
            ActionCard("Schedule demo with engineering", "Assigned to: James K.", "LOW", Color(0xFF3b82f6))
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, priority: String, priorityColor: Color, isCompleted: Boolean = false) {
    GlassCard(color = InsightsGlassWhite.copy(alpha = 0.04f), borderColor = InsightsGlassBorder) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) InsightsPrimary else Color.Transparent)
                    .border(2.dp, if (isCompleted) InsightsPrimary else InsightsPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                )
                Text(subtitle, color = Color(0xFFa19db9), fontSize = 10.sp)
            }
            
            Box(modifier = Modifier.background(priorityColor.copy(alpha=0.1f), RoundedCornerShape(8.dp)).padding(horizontal=8.dp, vertical=4.dp)) {
                Text(priority, color = priorityColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TranscriptCollapsibleBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(InsightsBackgroundDark)
            .border(1.dp, Color.White.copy(alpha=0.1f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .shadow(40.dp, spotColor = Color.Black.copy(alpha = 0.5f))
            .padding(24.dp)
    ) {
        Column {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).width(48.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha=0.2f)))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Transcript", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("2,450 words analyzed", color = Color(0xFFa19db9), fontSize = 12.sp)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                    AsyncImage(model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDYj2NVmsTZefyE0ZpaJ4O-rch6Njr9fzgJqL8zgLci6lhE_3lABnGqWvRml-RUv-YwWWsYHhXbFtpLSfgaoZIzGLjd1cHR9Fft9uePUTy3hqPNPanbGGQzzqfk-feW1EobRTi0fsmYiKsuc0nSOn_xDyYw-rKu_nOXLfzm945WkmQbC-Qv315pfopr7RZ5rbMJVe_O0CO3h6A_vSvbPV_RToEb8gEmV55dQmn5ZQoR-U-32hEQtVYfsp7uTjp6qArEoHzp9RQU_9u8", contentDescription = null, modifier = Modifier.size(32.dp).clip(CircleShape).border(2.dp, InsightsBackgroundDark, CircleShape))
                    AsyncImage(model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCIefKb9K1-6VvKZuLvb8OW9tvUpNcuCvIFJ-9z4PqKXoGhqhTZ7DofaRRmbC2C3rP_pMqo7hQlP7EoFxrmgxOvd9SMAKTQJ4VFq0cPGv0C_-n-1vnTRwMF41FjkxShcIAoKDlbbFO9x9I6qdXzgNR5vXFn6IIYBEj7ZDohe9xFhW-sMWTrleZPKJnca_gWYbIRWAkIY5evZTquQcmbkErlr4PdgrY-dJvL7vj0GhAb41GSDvCfOx3_gwGN8jAGa4LbbgMiO2rvWUiL", contentDescription = null, modifier = Modifier.size(32.dp).clip(CircleShape).border(2.dp, InsightsBackgroundDark, CircleShape))
                    Box(modifier = Modifier.size(32.dp).background(Color(0xFF333333), CircleShape).border(2.dp, InsightsBackgroundDark, CircleShape), contentAlignment = Alignment.Center) {
                        Text("+2", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.clickable { }) {
                    Text("OPEN", color = InsightsPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ExpandLess, contentDescription = null, tint = InsightsPrimary)
                }
            }
        }
    }
}
