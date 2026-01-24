package com.example.voicenote.data.repository

import com.example.voicenote.data.remote.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VoiceNoteRepository {
    // User & Identity Management
    fun syncUser(request: SyncRequest): Flow<Result<SyncResponse>>
    fun login(email: String, password: String): Flow<Result<SyncResponse>>
    fun getUserProfile(): Flow<Result<UserDTO>>
    fun updateUserSettings(update: Map<String, Any?>): Flow<Result<UserDTO>>

    // Real-time Status (WS Bridge)
    fun observeTaskStatus(userId: String): Flow<TaskStatusUpdate>

    // Notes Management
    fun getNotes(skip: Int, limit: Int): Flow<List<NoteResponseDTO>>
    fun getNote(noteId: String): Flow<Result<NoteResponseDTO>>
    fun uploadVoiceNote(file: File, mode: String = "GENERIC"): Flow<Result<String>>
    fun updateNote(noteId: String, update: Map<String, Any?>): Flow<Result<NoteResponseDTO>>
    fun askAI(noteId: String, question: String): Flow<Result<String>>
    fun searchNotes(query: String): Flow<List<NoteResponseDTO>>

    // Tasks Management
    fun getTasks(noteId: String? = null, priority: String? = null): Flow<List<TaskResponseDTO>>
    fun updateTask(taskId: String, update: Map<String, Any?>): Flow<Result<TaskResponseDTO>>
    fun uploadTaskMultimedia(taskId: String, file: File): Flow<Result<String>>

    // Billing & Monetization
    fun getWallet(): Flow<Result<WalletDTO>>
    fun topUpWallet(amount: Int): Flow<Result<String>>
}
