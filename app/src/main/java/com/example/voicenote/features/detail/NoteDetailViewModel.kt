package com.example.voicenote.features.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.core.network.WebSocketManager
import com.example.voicenote.data.model.*
import com.example.voicenote.data.remote.toNote
import com.example.voicenote.data.remote.toTask
import com.example.voicenote.data.repository.VoiceNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VoiceNoteRepository,
    private val webSocketManager: WebSocketManager
) : ViewModel() {
    private val noteId: String = checkNotNull(savedStateHandle["noteId"])
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    
    val note: StateFlow<Note?> = refreshTrigger
        .flatMapLatest { repository.getNote(noteId) }
        .map { it.getOrNull()?.toNote() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tasks: StateFlow<List<Task>> = refreshTrigger
        .flatMapLatest { repository.getTasks(noteId) }
        .map { list -> list.map { it.toTask() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActiveTasks: StateFlow<List<Task>> = repository.getTasks()
        .map { list -> list.map { it.toTask() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    init {
        refreshTrigger.tryEmit(Unit)
        
        viewModelScope.launch {
            repository.observeTaskStatus("").collect { update ->
                if (update.noteId == noteId && update.status == "DONE") {
                    refreshTrigger.emit(Unit)
                }
            }
        }
        
        viewModelScope.launch {
            webSocketManager.updates.collect { event ->
                if (event["type"] == "AI_RESPONSE") {
                    val data = event["data"] as? Map<*, *>
                    val answer = data?.get("answer") as? String
                    if (answer != null) {
                        _aiResponse.value = answer
                        _isProcessingAi.value = false
                    }
                }
            }
        }
    }

    fun askAi(question: String) {
        viewModelScope.launch {
            _isProcessingAi.value = true
            _aiResponse.value = "AI Brain is analyzing your request..."
            repository.askAI(noteId, question).collect { result ->
                result.onSuccess { _aiResponse.value = it }
                result.onFailure { _aiResponse.value = "AI Error: ${it.message}" }
                _isProcessingAi.value = false
            }
        }
    }

    fun updateNote(title: String, summary: String, transcript: String, priority: Priority, status: NoteStatus) {
        viewModelScope.launch {
            repository.updateNote(noteId, mapOf(
                "title" to title,
                "summary" to summary,
                "transcript" to transcript,
                "priority" to priority.name,
                "status" to status.name
            )).collect { refreshTrigger.emit(Unit) }
        }
    }

    fun renameSpeaker(oldName: String, newName: String) {
        viewModelScope.launch {
            val current = note.value ?: return@launch
            val updatedTranscript = current.transcript.replace("$oldName:", "$newName:")
            updateNote(current.title, current.summary, updatedTranscript, current.priority, current.status)
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            repository.updateNote(noteId, mapOf("is_deleted" to true)).collect { }
        }
    }

    fun getWhatsAppDraft(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            repository.getWhatsAppDraft(noteId).collect { result ->
                result.onSuccess { onSuccess(it) }
            }
        }
    }

    fun triggerSemanticAnalysis() {
        viewModelScope.launch {
            repository.triggerSemanticAnalysis(noteId).collect { result ->
                result.onSuccess { refreshTrigger.emit(Unit) }
            }
        }
    }

    fun updateTask(task: Task, description: String, deadline: Long?, assignedName: String? = null, assignedPhone: String? = null, commType: CommunicationType? = null, imageUrl: String? = null) {
        viewModelScope.launch {
            repository.updateTask(task.id, mapOf(
                "description" to description,
                "deadline" to deadline,
                "communication_type" to commType?.name
            )).collect { refreshTrigger.emit(Unit) }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.id, mapOf("is_done" to !task.isDone)).collect { refreshTrigger.emit(Unit) }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.id, mapOf("is_deleted" to true)).collect { refreshTrigger.emit(Unit) }
        }
    }

    fun approveAction(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.id, mapOf("is_action_approved" to true)).collect { refreshTrigger.emit(Unit) }
        }
    }
}
