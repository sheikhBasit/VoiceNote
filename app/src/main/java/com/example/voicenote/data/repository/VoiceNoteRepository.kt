package com.example.voicenote.data.repository

import com.example.voicenote.data.remote.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface VoiceNoteRepository {
    
    // ==================== USER & IDENTITY MANAGEMENT ====================
    
    fun syncUser(request: SyncRequest): Flow<Result<SyncResponse>>
    fun login(email: String, password: String): Flow<Result<SyncResponse>>
    fun logout(): Flow<Result<String>>
    fun getUserProfile(): Flow<Result<UserDTO>>
    fun updateUserSettings(update: Map<String, Any?>): Flow<Result<UserDTO>>
    
    // NEW: Device verification
    fun verifyDevice(token: String): Flow<Result<String>>
    
    // NEW: User search
    fun searchUsers(query: String = "", role: String? = null, skip: Int = 0, limit: Int = 50): Flow<List<UserDTO>>
    
    // NEW: Account management
    fun deleteAccount(hard: Boolean = false, adminId: String? = null): Flow<Result<String>>
    fun restoreAccount(userId: String): Flow<Result<UserDTO>>
    fun updateUserRole(userId: String, role: String, adminId: String): Flow<Result<UserDTO>>

    // ==================== REAL-TIME STATUS ====================
    
    fun observeTaskStatus(userId: String): Flow<TaskStatusUpdate>

    // ==================== NOTES MANAGEMENT ====================
    
    fun getNotes(skip: Int, limit: Int): Flow<List<NoteResponseDTO>>
    fun getNote(noteId: String): Flow<Result<NoteResponseDTO>>
    fun uploadVoiceNote(file: File, mode: String = "GENERIC"): Flow<Result<String>>
    fun updateNote(noteId: String, update: Map<String, Any?>): Flow<Result<NoteResponseDTO>>
    fun deleteNote(noteId: String, hard: Boolean = false): Flow<Result<String>>
    fun askAI(noteId: String, question: String): Flow<Result<String>>
    fun searchNotes(query: String): Flow<Result<SearchResponseDTO>>
    fun getDashboardData(): Flow<Result<DashboardResponse>>
    
    // NEW: Manual note creation
    fun createNote(note: NoteCreateRequest): Flow<Result<NoteResponseDTO>>
    
    // NEW: WhatsApp integration
    fun getWhatsAppDraft(noteId: String): Flow<Result<String>>
    
    // NEW: Semantic analysis
    fun triggerSemanticAnalysis(noteId: String): Flow<Result<String>>

    // ==================== TASKS MANAGEMENT ====================
    
    fun getTasks(noteId: String? = null, priority: String? = null): Flow<List<TaskResponseDTO>>
    fun updateTask(taskId: String, update: Map<String, Any?>): Flow<Result<TaskResponseDTO>>
    fun uploadTaskMultimedia(taskId: String, file: File): Flow<Result<String>>
    
    // NEW: Task creation
    fun createTask(task: TaskCreateRequest): Flow<Result<TaskResponseDTO>>
    
    // NEW: Task filtering
    fun getTasksDueToday(limit: Int = 100, offset: Int = 0): Flow<List<TaskResponseDTO>>
    fun getOverdueTasks(limit: Int = 100, offset: Int = 0): Flow<List<TaskResponseDTO>>
    fun getTasksAssignedToMe(userEmail: String? = null, userPhone: String? = null, limit: Int = 100, offset: Int = 0): Flow<List<TaskResponseDTO>>
    
    // NEW: Task search & details
    fun searchTasks(queryText: String, limit: Int = 100, offset: Int = 0): Flow<List<TaskResponseDTO>>
    fun getTask(taskId: String): Flow<Result<TaskResponseDTO>>
    fun getTaskStatistics(): Flow<Result<TaskStatisticsDTO>>
    
    // NEW: Task deletion
    fun deleteTask(taskId: String, hard: Boolean = false): Flow<Result<String>>
    
    // NEW: Task multimedia management
    fun removeTaskMultimedia(taskId: String, urlToRemove: String): Flow<Result<String>>
    
    // NEW: Task communication
    fun getTaskCommunicationOptions(taskId: String): Flow<Result<CommunicationOptionsDTO>>
    
    // NEW: Task external links
    fun addTaskExternalLink(taskId: String, link: LinkDTO): Flow<Result<TaskResponseDTO>>
    fun removeTaskExternalLink(taskId: String, linkIndex: Int): Flow<Result<TaskResponseDTO>>
    
    // NEW: Task duplication
    fun duplicateTask(taskId: String): Flow<Result<TaskResponseDTO>>

    // ==================== AI & INSIGHTS ====================
    fun getAIStats(): Flow<Result<AIStats>>

    // ==================== BILLING & MONETIZATION ====================
    
    fun getWallet(): Flow<Result<WalletDTO>>
    fun topUpWallet(amount: Int): Flow<Result<String>>
    
    // ==================== DRAFTS (OFFLINE MODE) ====================
    fun getDrafts(): Flow<List<File>>
    fun deleteDraft(file: File): Flow<Result<Unit>>

    // ==================== MEETINGS ====================
    
    fun joinMeeting(meetingUrl: String, botName: String = "VoiceNote Assistant"): Flow<Result<Map<String, Any>>>
}
