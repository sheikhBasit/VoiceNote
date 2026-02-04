package com.example.voicenote.data.remote

import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.model.NoteStatus
import com.example.voicenote.data.model.CommunicationType
import com.example.voicenote.data.model.Note
import com.example.voicenote.data.model.Task
import com.google.gson.annotations.SerializedName

/**
 * DTOs for FastAPI Backend Communication
 */

data class SyncRequest(
    val id: String? = null,
    val name: String,
    val email: String,
    val password: String? = null, // For device-independent login
    val token: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("primary_role") val primaryRole: String,
    @SerializedName("secondary_role") val secondaryRole: String? = null,
    @SerializedName("system_prompt") val systemPrompt: String? = "",
    @SerializedName("work_start_hour") val workStartHour: Int = 9,
    @SerializedName("work_end_hour") val workEndHour: Int = 17,
    @SerializedName("work_days") val workDays: List<Int> = listOf(2, 3, 4, 5, 6),
    val jargons: List<String> = emptyList(),
    val timezone: String? = "UTC"
)

data class TaskStatusUpdate(
    val type: String, // "note_progress", "task_extracted", etc.
    @SerializedName("note_id") val noteId: String?,
    @SerializedName("task_id") val taskId: String?,
    val status: String?,
    val progress: Int?
)

data class SyncResponse(
    val user: UserDTO,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class UserDTO(
    val id: String,
    val name: String,
    val email: String,
    @SerializedName("primary_role") val primaryRole: String,
    @SerializedName("last_login") val lastLogin: Long?,
    @SerializedName("system_prompt") val systemPrompt: String?,
    val jargons: List<String>,
    @SerializedName("work_start_hour") val workStartHour: Int,
    @SerializedName("work_end_hour") val workEndHour: Int,
    @SerializedName("work_days") val workDays: List<Int>,
    val timezone: String?
)

data class NoteResponseDTO(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val title: String,
    val summary: String,
    val priority: Priority,
    val status: NoteStatus,
    val timestamp: Long,
    @SerializedName("updated_at") val updatedAt: Long,
    val transcript: String,
    @SerializedName("audio_url") val audioUrl: String?,
    val tasks: List<TaskSummaryDTO>? = emptyList(),
    @SerializedName("is_pinned") val isPinned: Boolean,
    @SerializedName("is_liked") val isLiked: Boolean,
    @SerializedName("is_archived") val isArchived: Boolean,
    @SerializedName("is_deleted") val isDeleted: Boolean,
    @SerializedName("document_uris") val documentUris: List<String>? = emptyList(),
    @SerializedName("image_uris") val imageUris: List<String>? = emptyList()
)

fun NoteResponseDTO.toNote(): Note {
    return Note(
        id = id,
        userId = userId,
        title = title,
        summary = summary,
        transcript = transcript,
        audioUrl = audioUrl,
        timestamp = timestamp,
        priority = priority,
        status = status,
        isDeleted = isDeleted,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isLiked = isLiked,
        isArchived = isArchived,
        documentUris = documentUris ?: emptyList(),
        imageUris = imageUris ?: emptyList()
    )
}

data class TaskSummaryDTO(
    val id: String,
    val description: String,
    @SerializedName("is_done") val isDone: Boolean,
    val priority: Priority
)

data class TaskResponseDTO(
    val id: String,
    @SerializedName("note_id") val noteId: String? = null,
    val description: String,
    @SerializedName("is_done") val isDone: Boolean,
    val priority: Priority,
    val deadline: Long?,
    @SerializedName("assigned_entities") val assignedEntities: List<EntityDTO>? = emptyList(),
    @SerializedName("image_urls") val imageUrls: List<String>? = emptyList(),
    @SerializedName("image_uris") val imageUris: List<String>? = emptyList(),
    @SerializedName("document_urls") val documentUrls: List<String>? = emptyList(),
    @SerializedName("document_uris") val documentUris: List<String>? = emptyList(),
    @SerializedName("external_links") val externalLinks: List<LinkDTO>? = emptyList(),
    @SerializedName("communication_type") val communicationType: CommunicationType?,
    @SerializedName("is_action_approved") val isActionApproved: Boolean,
    @SerializedName("is_deleted") val isDeleted: Boolean = false
)

fun TaskResponseDTO.toTask(): Task {
    val entities = assignedEntities ?: emptyList()
    val primaryEntity = entities.firstOrNull()
    
    // Some backends use image_urls, some use image_uris. Merge them.
    val combinedImages = (imageUris ?: emptyList()) + (imageUrls ?: emptyList())
    val combinedDocs = (documentUris ?: emptyList()) + (documentUrls ?: emptyList())
    
    return Task(
        id = id,
        noteId = noteId,
        description = description,
        isDone = isDone,
        deadline = deadline,
        priority = priority,
        assignedContactName = primaryEntity?.name,
        assignedContactPhone = primaryEntity?.phone,
        communicationType = communicationType,
        isActionApproved = isActionApproved,
        imageUrl = combinedImages.firstOrNull(),
        documentUris = combinedDocs,
        imageUris = combinedImages,
        isDeleted = isDeleted
    )
}

data class EntityDTO(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null
)

data class LinkDTO(
    val url: String,
    val title: String? = null
)

data class SearchQuery(val query: String)

data class SearchResponseDTO(
    val query: String,
    val answer: String,
    val source: String,
    @SerializedName("local_note_count") val localNoteCount: Int,
    @SerializedName("web_result_count") val webResultCount: Int,
    val results: List<SearchResultItemDTO>
)

data class SearchResultItemDTO(
    val id: String?,
    val title: String,
    val summary: String,
    val transcript: String?,
    val timestamp: Long?,
    @SerializedName("similarity_score") val similarityScore: Float?,
    val url: String? = null,
    val content: String? = null
)

data class DashboardResponse(
    @SerializedName("task_velocity") val taskVelocity: Float,
    @SerializedName("completed_tasks") val completedTasks: Int,
    @SerializedName("total_tasks") val totalTasks: Int,
    @SerializedName("topic_heatmap") val topicHeatmap: List<TopicHeatmapItem>,
    @SerializedName("meeting_roi_hours") val meetingRoiHours: Float,
    @SerializedName("recent_notes_count") val recentNotesCount: Int,
    val status: String
)

data class TopicHeatmapItem(
    val topic: String,
    val count: Int
)

data class AIStats(
    @SerializedName("high_priority_pending_tasks") val highPriorityPending: Int,
    @SerializedName("total_active_notes") val totalActiveNotes: Int,
    val suggestion: String?
)

data class JoinMeetingRequest(
    @SerializedName("meeting_url") val meetingUrl: String,
    @SerializedName("bot_name") val botName: String = "VoiceNote Assistant"
)
data class WalletDTO(
    val balance: Int,
    val currency: String,
    @SerializedName("is_frozen") val isFrozen: Boolean,
    @SerializedName("stripe_subscription_id") val stripeSubscriptionId: String? = null,
    @SerializedName("recent_transactions") val recentTransactions: List<TransactionDTO>
)

data class TransactionDTO(
    val amount: Int,
    val type: String,
    val description: String,
    val timestamp: Long
)

// ==================== NEW DTOs for Complete API Coverage ====================

// Notes
data class NoteCreateRequest(
    val title: String,
    val summary: String? = null,
    val transcript: String? = null,
    val priority: Priority = Priority.MEDIUM
)

// WhatsApp Draft Response
data class WhatsAppDraftDTO(
    @SerializedName("whatsapp_link") val whatsappLink: String,
    val draft: String
)

// Tasks
data class TaskCreateRequest(
    @SerializedName("note_id") val noteId: String? = null,
    val description: String,
    val priority: String = "MEDIUM",
    val deadline: Long? = null,
    @SerializedName("assigned_entities") val assignedEntities: List<EntityDTO>? = emptyList(),
    @SerializedName("communication_type") val communicationType: String? = null,
    @SerializedName("image_urls") val imageUris: List<String>? = emptyList(),
    @SerializedName("document_urls") val documentUris: List<String>? = emptyList()
)

data class TaskStatisticsDTO(
    @SerializedName("total_tasks") val totalTasks: Int,
    @SerializedName("completed_tasks") val completedTasks: Int,
    @SerializedName("pending_tasks") val pendingTasks: Int,
    @SerializedName("high_priority_tasks") val highPriorityTasks: Int = 0,
    @SerializedName("overdue_tasks") val overdueTasks: Int = 0,
    @SerializedName("by_priority") val byPriority: Map<String, Int>? = emptyMap(),
    @SerializedName("by_status") val byStatus: Map<String, Int>? = emptyMap(),
    @SerializedName("completion_rate") val completionRate: Float
)

data class CommunicationOptionsDTO(
    @SerializedName("task_id") val taskId: String,
    val entities: List<EntityDTO>,
    @SerializedName("available_channels") val availableChannels: List<String>
)

// Admin
data class AdminLoginResponse(
    val user: UserDTO,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val permissions: Map<String, Boolean>
)

data class UserStatisticsDTO(
    @SerializedName("total_users") val totalUsers: Int,
    @SerializedName("active_users") val activeUsers: Int,
    @SerializedName("deleted_users") val deletedUsers: Int,
    @SerializedName("admin_users") val adminUsers: Int,
    @SerializedName("users_by_role") val usersByRole: Map<String, Int>,
    @SerializedName("recent_signups") val recentSignups: Int
)

data class AdminStatusDTO(
    val status: String,
    @SerializedName("total_users") val totalUsers: Int,
    @SerializedName("total_notes") val totalNotes: Int,
    @SerializedName("total_tasks") val totalTasks: Int,
    @SerializedName("system_health") val systemHealth: String,
    @SerializedName("active_admins") val activeAdmins: Int
)

data class AISettingsDTO(
    @SerializedName("model_name") val modelName: String,
    val temperature: Float,
    @SerializedName("max_tokens") val maxTokens: Int,
    @SerializedName("embedding_model") val embeddingModel: String,
    @SerializedName("enable_semantic_search") val enableSemanticSearch: Boolean,
    @SerializedName("enable_auto_tagging") val enableAutoTagging: Boolean
)

data class AISettingsUpdateDTO(
    @SerializedName("model_name") val modelName: String? = null,
    val temperature: Float? = null,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    @SerializedName("embedding_model") val embeddingModel: String? = null,
    @SerializedName("enable_semantic_search") val enableSemanticSearch: Boolean? = null,
    @SerializedName("enable_auto_tagging") val enableAutoTagging: Boolean? = null
)
