package com.example.voicenote.features.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.data.remote.UserDTO
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.components.GlassyTextField
import com.example.voicenote.ui.components.ShimmerCard
import com.example.voicenote.ui.theme.*

@Composable
fun ApiSettingsScreen(
    onBack: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
        // Subtle Background Mesh
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                0.2f to InsightsPrimary.copy(alpha = 0.08f),
                1f to Color.Transparent,
                center = androidx.compose.ui.geometry.Offset(1000f, 0f),
                radius = 1200f
            )
        ))

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            SettingsTopBar(onBack, isUpdating)

            when (val state = uiState) {
                is SettingsUiState.Loading -> {
                    SettingsLoadingShimmer()
                }
                is SettingsUiState.Success -> {
                    SettingsContent(
                        user = state.user,
                        isFloatingEnabled = viewModel.isFloatingButtonEnabled(),
                        onFloatingToggle = { viewModel.setFloatingButtonEnabled(it) },
                        onUpdate = { viewModel.updateSettings(it) },
                        onAddJargon = { viewModel.addJargon(it) },
                        onRemoveJargon = { viewModel.removeJargon(it) },
                        onLogout = { viewModel.logout { onBack() } },
                        onDestroy = { viewModel.deleteAccount { onBack() } },
                        onHelpClick = onHelpClick
                    )
                }
                is SettingsUiState.Error -> {
                    ErrorState(state.message) { viewModel.loadSettings() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBack: () -> Unit, isUpdating: Boolean) {
    TopAppBar(
        title = { 
            Text(
                "Intelligence Settings", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            ) 
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Back", tint = Color.White)
            }
        },
        actions = {
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 16.dp),
                    color = InsightsPrimary,
                    strokeWidth = 2.dp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(
    user: UserDTO,
    isFloatingEnabled: Boolean,
    onFloatingToggle: (Boolean) -> Unit,
    onUpdate: (Map<String, Any?>) -> Unit,
    onAddJargon: (String) -> Unit,
    onRemoveJargon: (String) -> Unit,
    onLogout: () -> Unit,
    onDestroy: () -> Unit,
    onHelpClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Identity
        item {
            SectionHeader("IDENTITY")
            GlassCard(intensity = 0.6f) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).background(InsightsPrimary.copy(alpha=0.1f), CircleShape).border(1.dp, InsightsPrimary.copy(alpha=0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(user.name.take(1).uppercase(), color = InsightsPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(user.email, color = Color.White.copy(alpha=0.5f), fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.background(InsightsPrimary.copy(alpha=0.2f), RoundedCornerShape(8.dp)).padding(horizontal=8.dp, vertical=4.dp)) {
                        Text("ACTIVE", color = InsightsPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // 2. Preferences
        item {
            SectionHeader("PREFERENCES")
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating Assistant", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Show a floating button for quick access", color = Color.White.copy(alpha=0.5f), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isFloatingEnabled,
                            onCheckedChange = onFloatingToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = InsightsPrimary,
                                checkedTrackColor = InsightsPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }

        // 3. AI Profile
        item {
            SectionHeader("AI PROFILE")
            GlassCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("System Persona", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    GlassyTextField(
                        value = user.systemPrompt ?: "",
                        onValueChange = { onUpdate(mapOf("system_prompt" to it)) },
                        label = "Custom AI Instructions",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Tell the AI how to analyze your notes (e.g. \"Be concise and focus on action items\").", color = Color.White.copy(alpha=0.4f), fontSize = 11.sp)
                }
            }
        }

        // 4. Work Context
        item {
            SectionHeader("WORK CONTEXT")
            GlassCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Role Identity", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), 
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("STUDENT", "TEACHER", "DEVELOPER", "OFFICE_WORKER", "BUSINESS_MAN", "PSYCHOLOGIST", "GENERIC").forEach { role ->
                            val isSelected = user.primaryRole.toString() == role
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdate(mapOf("primary_role" to role)) },
                                label = { Text(role.replace("_", " "), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InsightsPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = InsightsPrimary,
                                    labelColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Text("Domain Terminology", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), 
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        user.jargons.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { onRemoveJargon(tag) },
                                label = { Text(tag, fontSize = 12.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = Color.White.copy(alpha = 0.1f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        
                        var showAddJargon by remember { mutableStateOf(false) }
                        if (showAddJargon) {
                            var newJargon by remember { mutableStateOf("") }
                            GlassyTextField(
                                value = newJargon,
                                onValueChange = { newJargon = it },
                                label = "Add jargon...",
                                modifier = Modifier.width(150.dp),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                    if (newJargon.isNotBlank()) onAddJargon(newJargon)
                                    showAddJargon = false
                                })
                            )
                        } else {
                            AssistChip(
                                onClick = { showAddJargon = true },
                                label = { Text("+ Add Term") },
                                colors = AssistChipDefaults.assistChipColors(labelColor = InsightsPrimary)
                            )
                        }
                    }
                }
            }
        }

        // 5. Availability
        item {
            SectionHeader("ANALYTICS WINDOW")
            GlassCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Operating Hours", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimeChip(label = "Start", time = "${user.workStartHour}:00", onClick = { /* Show clock picker */ })
                        TimeChip(label = "End", time = "${user.workEndHour}:00", onClick = { /* Show clock picker */ })
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Work Days", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { index, day ->
                            val isSelected = (index + 2) in listOf(2, 3, 4, 5, 6)
                            FilterChip(
                                selected = isSelected,
                                onClick = { },
                                label = { Text(day, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InsightsPrimary.copy(alpha = 0.1f),
                                    selectedLabelColor = InsightsPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // 6. Account Actions
        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFef4444).copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFef4444).copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.Logout, null, tint = Color(0xFFef4444))
                Spacer(Modifier.width(12.dp))
                Text("Sign Out Securely", color = Color(0xFFef4444), fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onHelpClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InsightsGlassWhite),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, InsightsGlassBorder)
            ) {
                Icon(Icons.Default.HelpOutline, null, tint = InsightsPrimary)
                Spacer(Modifier.width(12.dp))
                Text("Help & Support", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(12.dp))
            
            TextButton(onClick = onDestroy, modifier = Modifier.fillMaxWidth()) {
                Text("Delete Account & All AI Data", color = Color.White.copy(alpha=0.2f), fontSize = 12.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
            }
        }

        item { 
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Build 2.4.0-AI", color = Color.White.copy(alpha=0.1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title, 
        color = Gray400, 
        fontSize = 11.sp, 
        fontWeight = FontWeight.Black, 
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsLoadingShimmer() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ShimmerCard(height = 100.dp)
        ShimmerCard(height = 150.dp)
        ShimmerCard(height = 120.dp)
        ShimmerCard(height = 80.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = Color.Red, textAlign = TextAlign.Center)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text("Retry")
            }
        }
    }
}
@Composable
private fun TimeChip(label: String, time: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(label, color = Gray400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(time, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
