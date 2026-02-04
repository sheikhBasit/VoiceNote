package com.example.voicenote.features.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicenote.core.service.VoiceRecordingService
import com.example.voicenote.ui.theme.*

@Composable
fun FloatingRecordingHub(onBack: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
        // Background Layer: Simulated iOS Home Screen
        AsyncImage(
            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBV6B-fmplIeaPmCN5ZTzCG_0HKEIW3r8MK0s0XvvfH0N-s6i3xaMXHgGk-ka_Zgig0FJNwl47pJ_z69iCbqSgwJTdQo0GoF-HnP4XLjClKLzt09LWDxOvC4Zpvq6U7W1ny2UQEwZM1YNU4dqhQa346CC70KYO9PyxSsK7js5WNADLxVqjXFMnpf2lLL72T5rIst78OMcVbPHiWUh0fAJwEDfZoP8j9KRc37ZtbEQJkbhuugLAdzPmch08LOZPsr0cpUtKZtWfRcCwG",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )

        // Add navigationBarsPadding to avoid overlapping with system gestures at the bottom
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().windowInsetsPadding(WindowInsets.navigationBars)) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha=0.1f)).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                
                Box(
                    modifier = Modifier.background(Color.Black.copy(alpha=0.4f), CircleShape).border(1.dp, Color.White.copy(alpha=0.1f), CircleShape).padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text("System Hub Active", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha=0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "AI Assistant Hub",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your recording is active in the floating glass hub below. Drag it anywhere.",
                color = Color.White.copy(alpha=0.7f),
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(start = 32.dp, top = 8.dp, end = 32.dp),
                textAlign = TextAlign.Center
            )

            // The Floating Hub
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Dashed Circle Simulation
                Canvas(modifier = Modifier.size(256.dp)) {
                    // Custom drawing if needed
                }

                Box(
                    modifier = Modifier
                        .size(256.dp)
                        .shadow(24.dp, CircleShape, spotColor = InsightsPrimary.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(Color(0xFF131022).copy(alpha = 0.7f))
                        .border(1.dp, Color.White.copy(alpha=0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow Rim
                    Box(modifier = Modifier.fillMaxSize().border(2.dp, InsightsPrimary.copy(alpha=0.2f), CircleShape))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Live AI Badge
                        Box(
                            modifier = Modifier
                                .background(InsightsPrimary, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                                Text("LIVE AI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visualizer
                        val amplitude by VoiceRecordingService.amplitude.collectAsState()
                        // Normalize amplitude (0-32767) to height
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(48.dp)) {
                            // Create a simulated visualizer based on single amplitude value for now
                            // We can use random variations based on the base amplitude to make it look alive
                            val baseHeight = (amplitude / 32767f) * 48
                            listOf(0.8f, 1.2f, 1.0f, 0.6f, 0.9f).forEach { scale ->
                                val h = (baseHeight * scale).coerceIn(4f, 48f)
                                Box(modifier = Modifier.width(6.dp).height(h.dp).clip(CircleShape).background(InsightsPrimary.copy(alpha = (h/48f).coerceAtLeast(0.3f))))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Timer
                        val startTime by VoiceRecordingService.recordingStartTime.collectAsState()
                        var durationStr by remember { mutableStateOf("00:00") }
                        
                        LaunchedEffect(startTime) {
                            if (startTime > 0) {
                                while(true) {
                                    val diff = (System.currentTimeMillis() - startTime) / 1000
                                    val min = diff / 60
                                    val sec = diff % 60
                                    durationStr = String.format("%02d:%02d", min, sec)
                                    kotlinx.coroutines.delay(1000)
                                }
                            } else {
                                durationStr = "00:00"
                            }
                        }
                        
                        val timeParts = durationStr.split(":")

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimerDigitHub(if (timeParts.isNotEmpty()) timeParts[0] else "00", "Min")
                            Text(":", color = InsightsPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            TimerDigitHub(if (timeParts.size > 1) timeParts[1] else "00", "Sec")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Stop Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha=0.2f))
                                .border(1.dp, Color.Red.copy(alpha=0.5f), CircleShape)
                                .clickable { onBack() }, // Assuming Back/Stop closes the hub for now
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }

            // Bottom Live Status Card
            val statusLog by VoiceRecordingService.statusLog.collectAsState()
            Box(modifier = Modifier.padding(24.dp).padding(bottom = 24.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF131022).copy(alpha = 0.7f))
                        .border(1.dp, Color.White.copy(alpha=0.05f), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.size(48.dp).background(InsightsPrimary.copy(alpha=0.2f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = InsightsPrimary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Status", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(statusLog, color = Color.White.copy(alpha=0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerDigitHub(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(48.dp, 40.dp).background(Color.White.copy(alpha=0.05f), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha=0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(label.uppercase(), color = Color.White.copy(alpha=0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=4.dp))
    }
}
