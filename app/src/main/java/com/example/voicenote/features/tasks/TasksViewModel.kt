package com.example.voicenote.features.tasks

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.model.CommunicationType
import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.model.Task
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.data.remote.toTask
import com.example.voicenote.data.remote.TaskCreateRequest
import com.example.voicenote.data.remote.EntityDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: VoiceNoteRepository
) : ViewModel() {
    
    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    val tasks: StateFlow<List<Task>> = refreshTrigger
        .onEach { _isRefreshing.value = true }
        .flatMapLatest { repository.getTasks() }
        .map { list -> list.map { it.toTask() }.also { _isRefreshing.value = false } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasksDueToday: StateFlow<List<Task>> = refreshTrigger
        .flatMapLatest { repository.getTasksDueToday() }
        .map { list -> list.map { it.toTask() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueTasks: StateFlow<List<Task>> = refreshTrigger
        .flatMapLatest { repository.getOverdueTasks() }
        .map { list -> list.map { it.toTask() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignedToMeTasks: StateFlow<List<Task>> = refreshTrigger
        .flatMapLatest { repository.getTasksAssignedToMe() }
        .map { list -> list.map { it.toTask() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Task>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchTasks(query).map { list -> list.map { it.toTask() } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<Task>> = combine(
        selectedFilter,
        tasks,
        tasksDueToday,
        overdueTasks,
        assignedToMeTasks
    ) { filter, all, dueToday, overdue, assigned ->
        when (filter) {
            TaskFilter.ALL -> all
            TaskFilter.DUE_TODAY -> dueToday
            TaskFilter.OVERDUE -> overdue
            TaskFilter.ASSIGNED_TO_ME -> assigned
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doneTasks: StateFlow<List<Task>> = tasks.map { list ->
        list.filter { it.isDone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun refreshTasks() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun duplicateTask(taskId: String) {
        viewModelScope.launch {
            repository.duplicateTask(taskId).collect { result ->
                result.onSuccess { refreshTrigger.emit(Unit) }
                    .onFailure { _error.emit(it.message ?: "Failed to duplicate task") }
            }
        }
    }

    fun createTask(
        noteId: String?,
        description: String,
        priority: Priority,
        deadline: Long?,
        assignedEntities: List<EntityDTO> = emptyList(),
        communicationType: CommunicationType? = null,
        attachmentFile: File? = null
    ) {
        viewModelScope.launch {
            val request = TaskCreateRequest(
                noteId = noteId,
                description = description,
                priority = priority.name,
                deadline = deadline,
                assignedEntities = assignedEntities,
                communicationType = communicationType?.name
            )
            repository.createTask(request).collect { result ->
                result.onSuccess { taskDto ->
                    if (attachmentFile != null) {
                        repository.uploadTaskMultimedia(taskDto.id, attachmentFile).collect { uploadResult ->
                            uploadResult.onFailure { _error.emit("Task created but attachment upload failed: ${it.message}") }
                            refreshTrigger.emit(Unit)
                        }
                    } else {
                        refreshTrigger.emit(Unit)
                    }
                }
                .onFailure {
                    _error.emit(it.message ?: "Failed to create task")
                }
            }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.id, mapOf("is_done" to !task.isDone)).collect { result ->
                result.onSuccess { refreshTrigger.emit(Unit) }
                    .onFailure { _error.emit(it.message ?: "Failed to update task") }
            }
        }
    }

    fun deleteTasks(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { id ->
                repository.deleteTask(id, hard = false).collect { result ->
                    result.onFailure { _error.emit(it.message ?: "Failed to delete task") }
                }
            }
            refreshTrigger.emit(Unit)
        }
    }

    fun restoreTasks(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { id ->
                repository.updateTask(id, mapOf("is_deleted" to false)).collect { result ->
                    result.onFailure { _error.emit(it.message ?: "Failed to restore task") }
                }
            }
            refreshTrigger.emit(Unit)
        }
    }
}
