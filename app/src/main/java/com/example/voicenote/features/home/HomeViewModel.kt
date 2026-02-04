package com.example.voicenote.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.model.Note
import com.example.voicenote.data.model.NoteStatus
import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.data.remote.toNote
import com.example.voicenote.data.remote.NoteCreateRequest
import com.example.voicenote.core.network.WebSocketManager
import com.example.voicenote.core.security.SecurityManager
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VoiceNoteRepository,
    private val webSocketManager: WebSocketManager,
    private val securityManager: SecurityManager
) : ViewModel() {
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _drafts = MutableStateFlow<List<File>>(emptyList())
    val drafts: StateFlow<List<File>> = _drafts.asStateFlow()

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    val notesState: StateFlow<List<Note>> = refreshTrigger
        .onEach { _isRefreshing.value = true }
        .flatMapLatest { repository.getNotes(0, 100) }
        .map { list -> 
            _isRefreshing.value = false
            list.map { it.toNote() } 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _walletBalance = MutableStateFlow<Int?>(null)
    val walletBalance: StateFlow<Int?> = _walletBalance.asStateFlow()

    private val _botStatus = MutableStateFlow<Map<String, Any>?>(null)
    val botStatus: StateFlow<Map<String, Any>?> = _botStatus.asStateFlow()

    val userName: String = securityManager.getUserEmail()?.substringBefore("@") ?: "User"

    init {
        refreshTrigger.tryEmit(Unit)
        refreshWallet()
        
        viewModelScope.launch {
            webSocketManager.updates.collect { event ->
                when (event["type"]) {
                    "STALE_DATA", "NOTE_STATUS" -> {
                        refreshTrigger.emit(Unit)
                        refreshWallet()
                    }
                    "BOT_STATUS" -> {
                        _botStatus.value = event["data"] as? Map<String, Any>
                    }
                }
            }
        }
        refreshDrafts()
    }

    fun refreshDrafts() {
        viewModelScope.launch {
            repository.getDrafts().collect {
                _drafts.value = it
            }
        }
    }

    fun retryDraft(file: java.io.File) {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.uploadVoiceNote(file).collect { result ->
                result.onSuccess {
                    repository.deleteDraft(file).collect()
                    refreshDrafts()
                    refreshTrigger.emit(Unit)
                }
                _isRefreshing.value = false
            }
        }
    }

    fun deleteDraft(file: java.io.File) {
        viewModelScope.launch {
            repository.deleteDraft(file).collect()
            refreshDrafts()
        }
    }

    fun onDeleteNote(note: Note) {
        viewModelScope.launch { 
            repository.updateNote(note.id, mapOf("is_deleted" to true)).collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch { 
            repository.updateNote(noteId, mapOf("is_deleted" to false)).collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

    fun deleteNotes(noteIds: List<String>) {
        viewModelScope.launch {
            noteIds.forEach { id ->
                repository.updateNote(id, mapOf("is_deleted" to true)).collect()
            }
            refreshTrigger.emit(Unit)
        }
    }

    fun restoreNotes(noteIds: List<String>) {
        viewModelScope.launch {
            noteIds.forEach { id ->
                repository.updateNote(id, mapOf("is_deleted" to false)).collect()
            }
            refreshTrigger.emit(Unit)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.id, mapOf("is_pinned" to !note.isPinned)).collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

    fun toggleLike(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.id, mapOf("is_liked" to !note.isLiked)).collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.id, mapOf("is_archived" to !note.isArchived)).collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

    fun onMarkAsDone(note: Note) {
        viewModelScope.launch { 
            repository.updateNote(note.id, mapOf("status" to NoteStatus.DONE.name)).collect {
                refreshTrigger.emit(Unit)
            }
        }
    }


    fun addNote(title: String, summary: String, priority: Priority) {
        viewModelScope.launch {
            val noteRequest = NoteCreateRequest(
                title = title,
                summary = summary,
                priority = priority
            )
            repository.createNote(noteRequest).collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

    fun updateStatus(noteId: String, status: NoteStatus) {
        viewModelScope.launch {
            repository.updateNote(noteId, mapOf("status" to status.name)).collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

    fun onManualSync() {
        refreshTrigger.tryEmit(Unit)
        refreshWallet()
        refreshDrafts()
    }

    fun refreshWallet() {
        viewModelScope.launch {
            repository.getWallet().collect { result ->
                result.onSuccess { _walletBalance.value = it.balance }
            }
        }
    }

    fun onLogout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logout().collect { result ->
                result.onSuccess { 
                    // WebSocket disconnect is handled by UI/Activity usually observing this or manually calling disconnect
                    webSocketManager.disconnect()
                    onSuccess() 
                }
                result.onFailure {
                    // Force logout on UI side even if server fails?
                    // For now, only on success.
                    webSocketManager.disconnect()
                    onSuccess()
                }
            }
        }
    }

    fun onHardDeleteNote(noteId: String) {
         viewModelScope.launch {
            repository.deleteNote(noteId, hard = true).collect {
                refreshTrigger.emit(Unit)
            }
        }
    }

    fun toggleSelection(noteId: String) {
        val current = _selectedIds.value
        if (current.contains(noteId)) {
            _selectedIds.value = current - noteId
            if (_selectedIds.value.isEmpty()) {
                _isSelectionMode.value = false
            }
        } else {
            _selectedIds.value = current + noteId
            _isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            ids.forEach { id ->
                repository.updateNote(id, mapOf("is_deleted" to true)).collect()
            }
            clearSelection()
            refreshTrigger.emit(Unit)
        }
    }

    fun archiveSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            ids.forEach { id ->
                repository.updateNote(id, mapOf("is_archived" to true)).collect()
            }
            clearSelection()
            refreshTrigger.emit(Unit)
        }
    }
}
