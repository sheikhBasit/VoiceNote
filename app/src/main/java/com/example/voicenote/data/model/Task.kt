package com.example.voicenote.data.model

import com.google.firebase.firestore.PropertyName

enum class CommunicationType { WHATSAPP, SMS, CALL, MEET, SLACK }

data class Task(
    val id: String = "",
    val noteId: String = "",
    val description: String = "",
    @get:PropertyName("isDone")
    @set:PropertyName("isDone")
    var isDone: Boolean = false,
    val deadline: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val googlePrompt: String = "",
    val aiPrompt: String = "",
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    @get:PropertyName("deletedAt")
    @set:PropertyName("deletedAt")
    var deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Assignment and Communication fields
    val assignedContactName: String? = null,
    val assignedContactPhone: String? = null,
    val communicationType: CommunicationType? = null,
    val communicationScheduledTime: Long? = null,
    val customUrl: String? = null,
    @get:PropertyName("isActionApproved")
    @set:PropertyName("isActionApproved")
    var isActionApproved: Boolean = false,
    // Multimedia support
    val imageUrl: String? = null
)
