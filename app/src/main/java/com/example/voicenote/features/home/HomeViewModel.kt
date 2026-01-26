package com.example.voicenote.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.model.Note
import com.example.voicenote.data.model.NoteStatus
import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.data.remote.toNote
import com.example.voicenote.core.network.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VoiceNoteRepository,
    private val webSocketManager: WebSocketManager
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    val notesState: StateFlow<List<Note>> = refreshTrigger
        .flatMapLatest { repository.getNotes(0, 100) }
        .map { list -> list.map { it.toNote() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _walletBalance = MutableStateFlow<Int?>(null)
    val walletBalance: StateFlow<Int?> = _walletBalance.asStateFlow()

    private val _botStatus = MutableStateFlow<Map<String, Any>?>(null)
    val botStatus: StateFlow<Map<String, Any>?> = _botStatus.asStateFlow()

    init {
        refreshTrigger.tryEmit(Unit)
        refreshWallet()
        
        viewModelScope.launch {
            webSocketManager.updates.collect { event ->
                when (event["type"]) {
                    "STALE_DATA", "NOTE_STATUS" -> {
                        refreshTrigger.emit(Unit)
                    }
                    "BOT_STATUS" -> {
                        _botStatus.value = event["data"] as? Map<String, Any>
                    }
                }
            }
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
        // VoiceNoteRepository doesn't have a direct 'saveNote' for new notes in the interface
        // usually uploadVoiceNote is used.
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
    }

    fun refreshWallet() {
        viewModelScope.launch {
            repository.getWallet().collect { result ->
                result.onSuccess { _walletBalance.value = it.balance }
            }
        }
    }
}
