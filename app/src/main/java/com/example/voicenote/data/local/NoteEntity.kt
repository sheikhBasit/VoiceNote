package com.example.voicenote.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.voicenote.data.model.Note
import com.example.voicenote.data.model.NoteStatus
import com.example.voicenote.data.model.Priority

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val summary: String,
    val transcript: String,
    val audioUrl: String?,
    val timestamp: Long,
    val priority: Priority,
    val status: NoteStatus,
    val isPinned: Boolean,
    val isLiked: Boolean,
    val isArchived: Boolean,
    val isDeleted: Boolean,
    val updatedAt: Long
)

fun NoteEntity.toNote(): Note = Note(
    id = id,
    userId = userId,
    title = title,
    summary = summary,
    transcript = transcript,
    audioUrl = audioUrl,
    timestamp = timestamp,
    priority = priority,
    status = status,
    isPinned = isPinned,
    isLiked = isLiked,
    isArchived = isArchived,
    isDeleted = isDeleted,
    updatedAt = updatedAt
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    userId = userId,
    title = title,
    summary = summary,
    transcript = transcript,
    audioUrl = audioUrl,
    timestamp = timestamp,
    priority = priority,
    status = status,
    isPinned = isPinned,
    isLiked = isLiked,
    isArchived = isArchived,
    isDeleted = isDeleted,
    updatedAt = updatedAt
)
