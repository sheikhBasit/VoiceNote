package com.example.voicenote.features.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.voicenote.data.repository.VoiceNoteRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.example.voicenote.ui.theme.*
import com.example.voicenote.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String? = null,
    viewModel: SearchViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery ?: "") }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            viewModel.performSearch(initialQuery)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.0f to InsightsPrimary.copy(alpha = 0.15f),
                    1.0f to Color.Transparent,
                    center = androidx.compose.ui.geometry.Offset(0f, 0f),
                    radius = 1000f
                )
            )
            .background(InsightsBackgroundDark)
            .windowInsetsPadding(WindowInsets.statusBars) // Avoid status bar overlap
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(InsightsGlassWhite).border(1.dp, InsightsGlassBorder, CircleShape).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White)
                }
                Text("Semantic Search", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(InsightsGlassWhite).border(1.dp, InsightsGlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = InsightsPrimary)
                }
            }

            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(InsightsGlassWhite)
                        .border(1.dp, InsightsGlassBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFa19db9))
                        Spacer(modifier = Modifier.width(16.dp))
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search your brain...", color = Color(0xFFa19db9)) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = InsightsPrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        if (query.isNotEmpty()) {
                             IconButton(onClick = { viewModel.performSearch(query) }) {
                                Icon(Icons.Default.Send, contentDescription = "Search", tint = InsightsPrimary)
                            }
                        } else {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFa19db9))
                        }
                    }
                }
            }
            
            if (viewModel.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = InsightsPrimary)
            }

            viewModel.searchResult?.let { result ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    item {
                        GlassCard(intensity = 0.8f, modifier = Modifier.padding(bottom = 16.dp)) {
                             Column(modifier = Modifier.padding(16.dp)) {
                                Text("AI SYNTHESIS", style = MaterialTheme.typography.labelSmall, color = InsightsPrimary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(result.answer, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (result.source == "local") Icons.Default.Search else Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Powered by ${result.source.uppercase()}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                }
                             }
                        }
                    }
                    
                    if (result.localResults.isNotEmpty()) {
                        item { Text("Contextual References", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 12.dp)) }
                        items(result.localResults) { note ->
                            GlassCard(intensity = 0.4f, modifier = Modifier.padding(bottom = 8.dp)) {
                                Text(note, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
