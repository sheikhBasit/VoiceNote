package com.example.voicenote.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.repository.VoiceNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: VoiceNoteRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refreshNotifications()
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // In a real app, you'd have a specific endpoint for notifications.
            // Here we'll derive them from notes and tasks to make it dynamic.
            combine(
                repository.getNotes(0, 20),
                repository.getTasksDueToday()
            ) { notes, tasks ->
                val list = mutableListOf<NotificationItem>()
                
                tasks.forEach { task ->
                    list.add(NotificationItem(
                        id = task.id,
                        title = "Task Due Today",
                        message = task.description,
                        timestamp = task.deadline ?: System.currentTimeMillis(),
                        type = NotificationType.TASK,
                        targetId = task.noteId ?: task.id
                    ))
                }

                notes.take(5).forEach { note ->
                    list.add(NotificationItem(
                        id = note.id,
                        title = "Recent Note",
                        message = if (note.title.isBlank()) "Untitled Transcription" else note.title,
                        timestamp = note.timestamp,
                        type = NotificationType.NOTE,
                        targetId = note.id
                    ))
                }

                list.sortedByDescending { it.timestamp }
            }.collect {
                _notifications.value = it
                _isRefreshing.value = false
            }
        }
    }
}

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: NotificationType,
    val targetId: String
)

enum class NotificationType {
    TASK, NOTE, SYSTEM
}
