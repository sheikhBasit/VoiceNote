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

class HomeViewModel(private val repository: FirestoreRepository = FirestoreRepository()) : ViewModel() {
    val notesState: StateFlow<List<Note>> = repository.getNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onDeleteNote(note: Note) {
        viewModelScope.launch { repository.softDeleteNote(note.id) }
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

    // Keep for quick testing if needed
    fun addTestNote() {
        addNote("Test Note", "This is a test summary", Priority.MEDIUM)
    }
}