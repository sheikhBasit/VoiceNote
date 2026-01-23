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
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Ask your notes anything...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                TextButton(onClick = { viewModel.performSearch(query) }) {
                                    Text("ASK V-RAG", color = Primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Search, contentDescription = "Close")
                    }
                }
            )
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
                        GlassCard {
                            Text("AI ANSWER", style = MaterialTheme.typography.labelSmall, color = Primary)
                            Spacer(Modifier.height(8.dp))
                            Text(result.answer, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (result.source == "local") Icons.Default.Search else Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.Gray
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Source: ${result.source.uppercase()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                    
                    if (result.localResults.isNotEmpty()) {
                        item { Text("Reference Notes", style = MaterialTheme.typography.titleSmall) }
                        items(result.localResults) { note ->
                            GlassCard {
                                Text(note, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
