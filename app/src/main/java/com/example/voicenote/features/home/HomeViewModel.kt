package com.example.voicenote.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.model.Note
import com.example.voicenote.data.model.NoteStatus
import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel @javax.inject.Inject constructor(
    private val repository: com.example.voicenote.data.repository.VoiceNoteRepository,
    private val webSocketManager: com.example.voicenote.core.network.WebSocketManager
) : ViewModel() {

    private val _botStatus = MutableStateFlow<Map<String, Any>?>(null)
    val botStatus: StateFlow<Map<String, Any>?> = _botStatus.asStateFlow()

    init {
        refreshWallet()
        viewModelScope.launch {
            webSocketManager.updates.collect { event ->
                when (event["type"]) {
                    "STALE_DATA", "NOTE_STATUS" -> {
                        repository.getNotes(0, 50) 
                    }
                    "BOT_STATUS" -> {
                        _botStatus.value = event["data"] as? Map<String, Any>
                    }
                }
            }
        }
    }

    fun onDeleteNote(note: Note) {
        viewModelScope.launch { repository.softDeleteNote(note.id) }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch { repository.restoreNote(noteId) }
    }

    fun deleteNotes(noteIds: List<String>) {
        viewModelScope.launch { repository.deleteNotes(noteIds) }
    }

    fun restoreNotes(noteIds: List<String>) {
        viewModelScope.launch {
            noteIds.forEach { repository.restoreNote(it) }
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.saveNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun toggleLike(note: Note) {
        viewModelScope.launch {
            repository.saveNote(note.copy(isLiked = !note.isLiked))
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            repository.saveNote(note.copy(isArchived = !note.isArchived))
        }
    }

    fun onMarkAsDone(note: Note) {
        viewModelScope.launch { repository.updateStatus(note.id, NoteStatus.DONE) }
    }

    fun addNote(title: String, summary: String, priority: Priority) {
        viewModelScope.launch {
            repository.saveNote(
                Note(
                    title = title,
                    summary = summary,
                    priority = priority
                )
            )
        }
    }

    fun updateStatus(noteId: String, status: NoteStatus) {
        viewModelScope.launch {
            repository.updateStatus(noteId, status)
        }
    }

    fun onManualSync() {
        // This will be handled by sending an intent to VoiceRecordingService 
        // to re-process the last recording if it exists.
    }

    fun refreshWallet() {
        viewModelScope.launch {
            repository.getWallet().collect { result ->
                result.onSuccess { _walletBalance.value = it.balance }
            }
        }
    }

    // Keep for quick testing if needed
    fun addTestNote() {
        addNote("Test Note", "This is a test summary", Priority.MEDIUM)
    }
}