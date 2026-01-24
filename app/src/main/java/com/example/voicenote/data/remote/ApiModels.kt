package com.example.voicenote.data.remote

import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.model.NoteStatus
import com.example.voicenote.data.model.CommunicationType
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
    val jargons: List<String> = emptyList()
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
    @SerializedName("work_days") val workDays: List<Int>
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
    val tasks: List<TaskSummaryDTO> = emptyList(),
    @SerializedName("is_pinned") val isPinned: Boolean,
    @SerializedName("is_liked") val isLiked: Boolean,
    @SerializedName("is_archived") val isArchived: Boolean,
    @SerializedName("is_deleted") val isDeleted: Boolean
)

data class TaskSummaryDTO(
    val id: String,
    val description: String,
    @SerializedName("is_done") val isDone: Boolean,
    val priority: Priority
)

data class TaskResponseDTO(
    val id: String,
    @SerializedName("note_id") val noteId: String,
    val description: String,
    @SerializedName("is_done") val isDone: Boolean,
    val priority: Priority,
    val deadline: Long?,
    @SerializedName("assigned_entities") val assignedEntities: List<EntityDTO> = emptyList(),
    @SerializedName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerializedName("document_urls") val documentUrls: List<String> = emptyList(),
    @SerializedName("external_links") val externalLinks: List<LinkDTO> = emptyList(),
    @SerializedName("communication_type") val communicationType: CommunicationType?,
    @SerializedName("is_action_approved") val isActionApproved: Boolean
)

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
    @SerializedName("recent_transactions") val recentTransactions: List<TransactionDTO>
)

data class TransactionDTO(
    val amount: Int,
    val type: String,
    val description: String,
    val timestamp: Long
)
