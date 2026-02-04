package com.example.voicenote.features.help

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicenote.ui.theme.*
import com.example.voicenote.ui.components.GlassCard

@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
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
                    "Help & Support",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { HelpCategoryHeader("HOW IT WORKS") }
                item { HelpItem("Voice-to-Insight", "Speak naturally. Our AI extracts tasks, summaries, and key topics automatically.") }
                item { HelpItem("RAG Search", "Use semantic search to ask questions about your previous notes.") }
                
                item { HelpCategoryHeader("BILLING") }
                item { HelpItem("Credits System", "Processing costs 10 credits per minute of raw audio.") }
                item { HelpItem("Service Plans", "Upgrade to Pro for unlimited storage and faster AI models.") }

                item { HelpCategoryHeader("CONTACT") }
                item { SupportButton("Email Support", Icons.Default.Email) }
                item { SupportButton("WhatsApp Support", Icons.Default.Chat) }
            }
        }
    }
}

@Composable
private fun HelpCategoryHeader(title: String) {
    Text(
        title,
        color = InsightsPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun HelpItem(question: String, answer: String) {
    GlassCard(intensity = 0.3f) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(question, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text(answer, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun SupportButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Button(
        onClick = { /* Action would go here */ },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = InsightsGlassWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, InsightsGlassBorder)
    ) {
        Icon(icon, null, tint = InsightsPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
