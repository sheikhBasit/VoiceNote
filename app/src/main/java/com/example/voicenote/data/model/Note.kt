package com.example.voicenote.data.model

import com.google.firebase.firestore.PropertyName

enum class Priority { HIGH, MEDIUM, LOW }
enum class NoteStatus { PENDING, DONE, DELAYED }

data class Note(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val transcript: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Priority = Priority.MEDIUM,
    val status: NoteStatus = NoteStatus.PENDING,
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    @get:PropertyName("deletedAt")
    @set:PropertyName("deletedAt")
    var deletedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)