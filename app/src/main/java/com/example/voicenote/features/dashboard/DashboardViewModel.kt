package com.example.voicenote.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.core.network.ConnectivityObserver
import com.example.voicenote.core.network.ConnectivityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: VoiceNoteRepository,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        loadDashboard()
        observeConnectivity()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                _isOffline.value = status != ConnectivityStatus.Available
                if (status == ConnectivityStatus.Available && _uiState.value is DashboardUiState.Error) {
                    loadDashboard() // Auto-retry on reconnect
                }
            }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                repository.getNotes(0, 5).collect { notes ->
                    _uiState.value = DashboardUiState.Success(
                        velocity = "8.4 pts/hr", 
                        activeTasks = "12",
                        aiInsights = notes.size.toString()
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("Dashboard", "Pulse load failed", e)
                _uiState.value = DashboardUiState.Error("Sync Error: ${e.localizedMessage}")
            }
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val velocity: String,
        val activeTasks: String,
        val aiInsights: String
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
