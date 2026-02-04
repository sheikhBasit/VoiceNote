package com.example.voicenote.features.settings

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.theme.*

@Composable
fun AppSettingsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.15f to InsightsPrimary.copy(alpha = 0.15f),
                    1f to Color.Transparent,
                    center = androidx.compose.ui.geometry.Offset(1000f, 0f),
                    radius = 1000f
                )
            )
            .background(
                Brush.radialGradient(
                    0.1f to InsightsPrimary.copy(alpha = 0.1f),
                    1f to Color.Transparent,
                    center = androidx.compose.ui.geometry.Offset(0f, 2000f),
                    radius = 1000f
                )
            )
            .background(InsightsBackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InsightsBackgroundDark.copy(alpha = 0.8f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable { /* Back */ },
                    contentAlignment = Alignment.Center
                ) {
                   Icon(Icons.Default.ArrowBackIosNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "Settings", 
                    color = Color.White, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(end=40.dp), // Balance back button
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                item { AccountSection() }
                item { AIEngineSection() }
                item { PrivacySection() }
                item { PreferencesSection() }
                item { SignOutSection() }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title, 
        color = Color.White.copy(alpha = 0.4f), 
        fontSize = 12.sp, 
        fontWeight = FontWeight.Bold, 
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 12.dp, start = 16.dp)
    )
}

@Composable
private fun AccountSection() {
    Column {
        SectionHeader("ACCOUNT")
        GlassCard(color = InsightsGlassWhite.copy(alpha = 0.03f), borderColor = InsightsGlassBorder) {
            Column {
                // Profile Item
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCIupCARPwErbUpQHp5tiXVkeVkWvhxjYnIaJKJlX-zMo_MgQ77rkvMc1PDky_zPsyg1iYLgppv2zrVBVSJKCQC2FwmyYM7LBwYIGjltvIUE6pbHSBXTApAzGDuZqsx_x2ZP2XtrITtNpoeUGCPBhpA8pq_CuB7_-65Nx181XdngV5RH0YLE8cJNaRSNyV42vnNKYwBd2yG3KZ87lGMBBPEb-dgEKS07vz8FQ-jSSkbw1m_pYbT8B7H7QWSgrIbty913fh_s7BI605Y",
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha=0.2f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(16.dp).background(InsightsPrimary, CircleShape).border(2.dp, InsightsBackgroundDark, CircleShape))
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alex Rivers", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("alex.rivers@ai-recorder.com", color = Color.White.copy(alpha=0.5f), fontSize = 14.sp)
                    }
                    
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha=0.3f))
                }
                
                HorizontalDivider(color = Color.White.copy(alpha=0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                
                // Subscription Item
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = InsightsPrimary)
                        Text("Subscription", color = Color.White, fontSize = 16.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         Box(modifier = Modifier.background(InsightsPrimary.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal=8.dp, vertical=2.dp)) {
                             Text("PREMIUM", color = InsightsPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                         }
                         Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha=0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AIEngineSection() {
    Column {
        SectionHeader("AI ENGINE")
        GlassCard(color = InsightsGlassWhite.copy(alpha = 0.03f), borderColor = InsightsGlassBorder) {
            Column {
                // STT Provider
                Column(modifier = Modifier.padding(16.dp)) {
                     Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                         Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                              Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = InsightsPrimary)
                              Text("STT Provider", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                         }
                         Text("Whisper V3", color = Color.White.copy(alpha=0.4f), fontSize = 14.sp)
                     }
                     // Segmented Control
                     Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha=0.4f), RoundedCornerShape(8.dp)).padding(4.dp)) {
                          SegmentedButton("Whisper", true)
                          SegmentedButton("Google", false)
                          SegmentedButton("Deepgram", false)
                     }
                }
                
                HorizontalDivider(color = Color.White.copy(alpha=0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                
                 // LLM Selection
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = InsightsPrimary)
                        Text("Analysis Model", color = Color.White, fontSize = 16.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         Text("GPT-4o (Default)", color = Color.White.copy(alpha=0.4f), fontSize = 14.sp)
                         Icon(Icons.Default.UnfoldMore, contentDescription = null, tint = Color.White.copy(alpha=0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SegmentedButton(text: String, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = if(isSelected) Color.White else Color.White.copy(alpha=0.4f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrivacySection() {
    Column {
        SectionHeader("PRIVACY & SECURITY")
        GlassCard(color = InsightsGlassWhite.copy(alpha = 0.03f), borderColor = InsightsGlassBorder) {
            Column {
                 ToggleRow(icon = Icons.Default.Fingerprint, title = "FaceID Protection", initialChecked = true)
                 HorizontalDivider(color = Color.White.copy(alpha=0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                 ToggleRow(icon = Icons.Default.Shield, title = "On-Device Mode", subtitle="Processing stays 100% offline", initialChecked = false)
            }
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String? = null, initialChecked: Boolean) {
    var checked by remember { mutableStateOf(initialChecked) }
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = InsightsPrimary)
            Column {
                Text(title, color = Color.White, fontSize = 16.sp)
                if(subtitle != null) {
                    Text(subtitle, color = Color.White.copy(alpha=0.4f), fontSize = 11.sp)
                }
            }
        }
        
        // Glow Toggle
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if(checked) InsightsPrimary else Color.White.copy(alpha=0.1f))
                .clickable { checked = !checked }
        ) {
            val offset by animateFloatAsState(if(checked) 22f else 2f)
            Box(
                 modifier = Modifier
                    .offset(x = offset.dp, y = 2.dp)
                    .size(20.dp)
                    .background(Color.White, CircleShape)
                    .shadow(elevation = if(checked) 8.dp else 0.dp, spotColor = InsightsPrimary, shape = CircleShape)
            )
        }
    }
}

@Composable
private fun PreferencesSection() {
    Column {
        SectionHeader("PREFERENCES")
        GlassCard(color = InsightsGlassWhite.copy(alpha = 0.03f), borderColor = InsightsGlassBorder) {
            Column {
                PreferenceRow(Icons.Default.Language, "Language", "English (US)")
                HorizontalDivider(color = Color.White.copy(alpha=0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                PreferenceRow(Icons.Default.DarkMode, "Theme", "Automatic")
                HorizontalDivider(color = Color.White.copy(alpha=0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                
                // Clear Cache
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFf87171))
                        Text("Clear Cache", color = Color(0xFFf87171), fontSize = 16.sp)
                    }
                    Text("1.2 GB", color = Color(0xFFf87171).copy(alpha=0.4f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun PreferenceRow(icon: ImageVector, title: String, value: String) {
     Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha=0.6f))
            Text(title, color = Color.White, fontSize = 16.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             Text(value, color = Color.White.copy(alpha=0.4f), fontSize = 14.sp)
             Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha=0.3f))
        }
    }
}

@Composable
private fun SignOutSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha=0.05f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=0.05f))
        ) {
            Text("Sign Out", color = Color(0xFFf87171), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("V 2.4.0 (Build 892)", color = Color.White.copy(alpha=0.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("© 2024 AI Recorder Inc. Made with ❤️ in SF", color = Color.White.copy(alpha=0.1f), fontSize = 10.sp, modifier = Modifier.padding(top=4.dp))
    }
}
