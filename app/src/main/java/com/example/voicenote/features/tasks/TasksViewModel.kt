package com.example.voicenote.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.model.Task
import com.example.voicenote.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TasksViewModel(private val repository: FirestoreRepository = FirestoreRepository()) : ViewModel() {
    
    // Filter out tasks whose notes are deleted or archived
    val tasks: StateFlow<List<Task>> = combine(
        repository.getAllTasks(),
        repository.getNotes() // This already returns non-deleted notes
    ) { allTasks, activeNotes ->
        val activeNoteIds = activeNotes.map { it.id }.toSet()
        allTasks.filter { it.noteId in activeNoteIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doneTasks: StateFlow<List<Task>> = tasks.map { list ->
        list.filter { it.isDone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTaskStatus(task.id, !task.isDone)
        }
    }

    fun deleteTasks(ids: List<String>) {
        viewModelScope.launch {
            repository.deleteTasks(ids)
        }
    }

    fun restoreTasks(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { repository.restoreTask(it) }
        }
    }
}
