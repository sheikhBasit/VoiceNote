package com.example.voicenote.data.model

import com.google.firebase.firestore.PropertyName

enum class Priority { HIGH, MEDIUM, LOW }
enum class NoteStatus { PENDING, DONE, DELAYED }

data class Note(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val summary: String = "",
    val transcript: String = "",
    val audioUrl: String? = null,
    val locationName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Priority = Priority.MEDIUM,
    val status: NoteStatus = NoteStatus.PENDING,
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    @get:PropertyName("deletedAt")
    @set:PropertyName("deletedAt")
    var deletedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("isSynced")
    @set:PropertyName("isSynced")
    var isSynced: Boolean = true,
    @get:PropertyName("isPinned")
    @set:PropertyName("isPinned")
    var isPinned: Boolean = false,
    @get:PropertyName("isLiked")
    @set:PropertyName("isLiked")
    var isLiked: Boolean = false,
    @get:PropertyName("isArchived")
    @set:PropertyName("isArchived")
    var isArchived: Boolean = false,
    val documentUris: List<String> = emptyList(),
    val imageUris: List<String> = emptyList()
)
