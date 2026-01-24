package com.example.voicenote.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.theme.Primary

data class SearchResult(
    val query: String,
    val answer: String,
    val source: String,
    val localResults: List<String> = emptyList(),
    val webResults: List<String> = emptyList()
)

class SearchViewModel : ViewModel() {
    var searchResult by mutableStateOf<SearchResult?>(null)
    var isSearching by mutableStateOf(false)

    fun performSearch(query: String) {
        if (query.isBlank()) return
        isSearching = true
        // Logic to call Backend V-RAG API would go here
        // For now, simulating response
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                intensity = 0.5f
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Search, contentDescription = "Close", tint = Color(0xFF00E5FF))
                    }
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Ask your notes anything...", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = Color(0xFF00E5FF),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.performSearch(query) }) {
                            Icon(androidx.compose.material.icons.automirrored.filled.Send, contentDescription = "Send", tint = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            viewModel.searchResult?.let { result ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        GlassCard(intensity = 0.8f) {
                            Text("AI SYNTHESIS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
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
                    
                    if (result.localResults.isNotEmpty()) {
                        item { Text("Contextual References", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.8f)) }
                        items(result.localResults) { note ->
                            GlassCard(intensity = 0.4f) {
                                Text(note, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }
        }
    }
}
