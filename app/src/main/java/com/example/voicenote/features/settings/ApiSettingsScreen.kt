package com.example.voicenote.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicenote.data.model.AppConfig
import com.example.voicenote.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ApiSettingsViewModel(private val repository: FirestoreRepository = FirestoreRepository()) : ViewModel() {
    val config: StateFlow<AppConfig?> = repository.getAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addKey(key: String) {
        viewModelScope.launch {
            val currentConfig = config.value ?: AppConfig()
            val newKeys = currentConfig.apiKeys + key
            repository.updateAppConfig(currentConfig.copy(apiKeys = newKeys))
        }
    }

    fun removeKey(index: Int) {
        viewModelScope.launch {
            val currentConfig = config.value ?: return@launch
            val newKeys = currentConfig.apiKeys.toMutableList().apply { removeAt(index) }
            val newIndex = if (currentConfig.currentKeyIndex >= newKeys.size) 0 else currentConfig.currentKeyIndex
            repository.updateAppConfig(currentConfig.copy(apiKeys = newKeys, currentKeyIndex = newIndex))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(viewModel: ApiSettingsViewModel = viewModel()) {
    val config by viewModel.config.collectAsState()
    var newKey by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("API Settings") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Groq API Keys", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it },
                    label = { Text("Add new key") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (newKey.isNotBlank()) {
                        viewModel.addKey(newKey)
                        newKey = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                config?.let { cfg ->
                    itemsIndexed(cfg.apiKeys) { index, key ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Key ${index + 1}", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        key.take(8) + "..." + key.takeLast(4),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (index == cfg.currentKeyIndex) {
                                        Text("Active", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                IconButton(onClick = { viewModel.removeKey(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}