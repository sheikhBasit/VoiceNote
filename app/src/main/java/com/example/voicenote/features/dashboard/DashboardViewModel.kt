package com.example.voicenote.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.core.security.SecurityManager
import com.example.voicenote.data.local.DashboardDao
import com.example.voicenote.data.local.DashboardStatsEntity
import com.example.voicenote.data.local.TaskStatsEntity
import com.example.voicenote.data.local.AIInsightsEntity
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.core.network.ConnectivityObserver
import com.example.voicenote.core.network.ConnectivityStatus
import com.example.voicenote.core.network.WebSocketManager
import com.example.voicenote.data.remote.AIStats
import com.example.voicenote.data.remote.NoteResponseDTO
import com.example.voicenote.data.remote.TopicHeatmapItem
import com.example.voicenote.data.remote.WalletDTO
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: VoiceNoteRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val securityManager: SecurityManager,
    private val webSocketManager: WebSocketManager,
    private val dashboardDao: DashboardDao,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _taskStatistics = MutableStateFlow<TaskStatistics?>(null)
    val taskStatistics: StateFlow<TaskStatistics?> = _taskStatistics.asStateFlow()

    private val _wallet = MutableStateFlow<WalletDTO?>(null)
    val wallet: StateFlow<WalletDTO?> = _wallet.asStateFlow()

    private val _aiInsights = MutableStateFlow<AIStats?>(null)
    val aiInsights: StateFlow<AIStats?> = _aiInsights.asStateFlow()

    private val _recentNotes = MutableStateFlow<List<NoteResponseDTO>>(emptyList())
    val recentNotes: StateFlow<List<NoteResponseDTO>> = _recentNotes.asStateFlow()

    val userName: String by lazy {
        securityManager.getUserEmail()?.split("@")?.firstOrNull() ?: "User"
    }

    init {
        loadCachedData()
        refreshDashboard()
        observeConnectivity()
        observeWebSocket()
    }

    private fun loadCachedData() {
        viewModelScope.launch {
            // Load Pulse Stats from Cache
            dashboardDao.getDashboardStats().collectLatest { cached ->
                if (cached != null && _uiState.value is DashboardUiState.Loading) {
                    _uiState.value = DashboardUiState.Success(
                        velocity = "${cached.taskVelocity} pts/hr",
                        activeTasks = cached.totalTasks.toString(),
                        heatmap = cached.let { 
                            val listType = object : com.google.gson.reflect.TypeToken<List<TopicHeatmapItem>>() {}.type
                            gson.fromJson(it.topicsJson, listType) ?: emptyList()
                        }
                    )
                }
            }
        }
        
        viewModelScope.launch {
            dashboardDao.getTaskStats().collectLatest { cached ->
                if (cached != null) {
                    _taskStatistics.value = TaskStatistics(
                        totalTasks = cached.totalTasks,
                        completedTasks = cached.completedTasks,
                        pendingTasks = cached.pendingTasks,
                        highPriority = cached.highPriority,
                        mediumPriority = cached.mediumPriority,
                        lowPriority = cached.lowPriority,
                        overdue = cached.overdue,
                        dueToday = cached.dueToday,
                        completionRate = cached.completionRate.toDouble()
                    )
                }
            }
        }

        viewModelScope.launch {
            dashboardDao.getAIInsights().collectLatest { cached ->
                if (cached != null) {
                    _aiInsights.value = AIStats(
                        highPriorityPending = cached.highPriorityPending,
                        totalActiveNotes = cached.totalActiveNotes,
                        suggestion = cached.suggestion
                    )
                }
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                _isOffline.value = status != ConnectivityStatus.Available
                if (status == ConnectivityStatus.Available && _uiState.value is DashboardUiState.Error) {
                    refreshDashboard()
                }
            }
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            webSocketManager.updates.collect { event ->
                if (event["type"] == "STALE_DATA" || event["type"] == "TASK_EXTRACTED") {
                    refreshDashboard()
                }
            }
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            val dashboardDeferred = async { repository.getDashboardData().firstOrNull() }
            val statsDeferred = async { repository.getTaskStatistics().firstOrNull() }
            val walletDeferred = async { repository.getWallet().firstOrNull() }
            val aiStatsDeferred = async { repository.getAIStats().firstOrNull() }
            val notesDeferred = async { repository.getNotes(0, 5).firstOrNull() }

            try {
                val dashboardResult = dashboardDeferred.await()
                val statsResult = statsDeferred.await()
                val walletResult = walletDeferred.await()
                val aiStatsResult = aiStatsDeferred.await()
                val notesResult = notesDeferred.await()

                // 1. Update Task Stats & Cache
                statsResult?.onSuccess { stats ->
                    val uiStats = TaskStatistics(
                        totalTasks = stats.totalTasks,
                        completedTasks = stats.completedTasks,
                        pendingTasks = stats.pendingTasks,
                        highPriority = stats.highPriorityTasks,
                        mediumPriority = stats.byPriority?.get("MEDIUM") ?: 0,
                        lowPriority = stats.byPriority?.get("LOW") ?: 0,
                        overdue = stats.overdueTasks,
                        dueToday = stats.byStatus?.get("due_today") ?: 0,
                        completionRate = stats.completionRate.toDouble()
                    )
                    _taskStatistics.value = uiStats
                    
                    dashboardDao.insertTaskStats(TaskStatsEntity(
                        totalTasks = uiStats.totalTasks,
                        completedTasks = uiStats.completedTasks,
                        pendingTasks = uiStats.pendingTasks,
                        highPriority = uiStats.highPriority,
                        mediumPriority = uiStats.mediumPriority,
                        lowPriority = uiStats.lowPriority,
                        overdue = uiStats.overdue,
                        dueToday = uiStats.dueToday,
                        completionRate = uiStats.completionRate.toFloat()
                    ))
                }

                // 2. Update Wallet (Not cached for security/accuracy)
                walletResult?.onSuccess { _wallet.value = it }

                // 3. Update AI Insights & Cache
                aiStatsResult?.onSuccess { insights ->
                    _aiInsights.value = insights
                    dashboardDao.insertAIInsights(AIInsightsEntity(
                        highPriorityPending = insights.highPriorityPending,
                        totalActiveNotes = insights.totalActiveNotes,
                        suggestion = insights.suggestion
                    ))
                }

                // 4. Update Recent Notes
                if (notesResult != null) {
                    _recentNotes.value = notesResult
                }

                // 5. Update Pulse & Cache
                dashboardResult?.onSuccess { data ->
                    _uiState.value = DashboardUiState.Success(
                        velocity = "${data.taskVelocity} pts/hr",
                        activeTasks = data.totalTasks.toString(),
                        heatmap = data.topicHeatmap
                    )
                    
                    // Note: topicsJson conversion happens via Room converter internally if set up correctly, 
                    // otherwise manual JSON stringification is used.
                    dashboardDao.insertDashboardStats(DashboardStatsEntity(
                        taskVelocity = data.taskVelocity,
                        completedTasks = data.completedTasks,
                        totalTasks = data.totalTasks,
                        topicsJson = gson.toJson(data.topicHeatmap)
                    ))
                }?.onFailure { e ->
                    if (_uiState.value is DashboardUiState.Loading) {
                        _uiState.value = DashboardUiState.Error("Pulse Load Failed: ${e.localizedMessage}")
                    }
                }

            } catch (e: Exception) {
                if (_uiState.value is DashboardUiState.Loading) {
                    _uiState.value = DashboardUiState.Error("Critical Sync Error: ${e.localizedMessage}")
                }
            }
        }
    }
}

data class TaskStatistics(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val highPriority: Int,
    val mediumPriority: Int,
    val lowPriority: Int,
    val overdue: Int,
    val dueToday: Int,
    val completionRate: Double
)

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val velocity: String,
        val activeTasks: String,
        val heatmap: List<TopicHeatmapItem> = emptyList()
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
