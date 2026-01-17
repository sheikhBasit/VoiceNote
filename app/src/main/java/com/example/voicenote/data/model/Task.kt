package com.example.voicenote.data.model

import com.google.firebase.firestore.PropertyName

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
    val createdAt: Long = System.currentTimeMillis()
)