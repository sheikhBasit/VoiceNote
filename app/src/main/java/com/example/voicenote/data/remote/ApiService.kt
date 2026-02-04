package com.example.voicenote.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==================== USERS & IDENTITY ====================
    
    @POST("users/sync")
    suspend fun syncUser(@Body request: SyncRequest): Response<SyncResponse>

    @POST("users/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<SyncResponse>

    @GET("users/me")
    suspend fun getUserProfile(): Response<UserDTO>

    @PATCH("users/me")
    suspend fun updateUserSettings(@Body update: Map<String, Any?>): Response<UserDTO>

    @POST("users/logout")
    suspend fun logout(): Response<Map<String, String>>

    // NEW: Device verification
    @GET("users/verify-device")
    suspend fun verifyDevice(@Query("token") token: String): Response<Map<String, String>>

    // NEW: User search
    @GET("users/search")
    suspend fun searchUsers(
        @Query("query") query: String = "",
        @Query("role") role: String? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<List<UserDTO>>

    // NEW: Account deletion
    @DELETE("users/me")
    suspend fun deleteAccount(
        @Query("hard") hard: Boolean = false,
        @Query("admin_id") adminId: String? = null
    ): Response<Map<String, String>>

    // NEW: Restore account
    @PATCH("users/{user_id}/restore")
    suspend fun restoreAccount(@Path("user_id") userId: String): Response<UserDTO>

    // NEW: Update user role (Admin)
    @PATCH("users/{user_id}/role")
    suspend fun updateUserRole(
        @Path("user_id") userId: String,
        @Query("role") role: String,
        @Query("admin_id") adminId: String
    ): Response<UserDTO>

    // ==================== NOTES ====================
    
    @Multipart
    @POST("notes/process")
    suspend fun processNote(
        @Part file: MultipartBody.Part,
        @Part("mode") mode: String? = "GENERIC"
    ): Response<Map<String, String>>

    // NEW: Create note manually (non-voice)
    @POST("notes/create")
    suspend fun createNote(@Body note: NoteCreateRequest): Response<NoteResponseDTO>

    @GET("notes")
    suspend fun listNotes(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 10
    ): Response<List<NoteResponseDTO>>

    @GET("notes/{note_id}")
    suspend fun getNote(@Path("note_id") noteId: String): Response<NoteResponseDTO>

    @PATCH("notes/{note_id}")
    suspend fun updateNote(
        @Path("note_id") noteId: String,
        @Body update: Map<String, Any?>
    ): Response<NoteResponseDTO>

    @DELETE("notes/{note_id}")
    suspend fun deleteNote(
        @Path("note_id") noteId: String,
        @Query("hard") hard: Boolean = false
    ): Response<Map<String, String>>

    @POST("notes/{note_id}/ask")
    suspend fun askAI(
        @Path("note_id") noteId: String,
        @Body body: Map<String, String>
    ): Response<Map<String, String>>

    @POST("notes/search")
    suspend fun searchNotes(@Body query: SearchQuery): Response<SearchResponseDTO>

    @GET("notes/dashboard")
    suspend fun getDashboardData(): Response<DashboardResponse>

    @GET("notes/{note_id}/whatsapp")
    suspend fun getWhatsAppDraft(@Path("note_id") noteId: String): Response<WhatsAppDraftDTO>

    @POST("notes/{note_id}/semantic-analysis")
    suspend fun triggerSemanticAnalysis(@Path("note_id") noteId: String): Response<Map<String, String>>

    // ==================== TASKS ====================
    
    // NEW: Create task manually
    @POST("tasks")
    suspend fun createTask(@Body task: TaskCreateRequest): Response<TaskResponseDTO>

    @GET("tasks")
    suspend fun listTasks(
        @Query("note_id") noteId: String? = null,
        @Query("priority") priority: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<List<TaskResponseDTO>>

    // NEW: Get tasks due today
    @GET("tasks/due-today")
    suspend fun getTasksDueToday(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<List<TaskResponseDTO>>

    // NEW: Get overdue tasks
    @GET("tasks/overdue")
    suspend fun getOverdueTasks(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<List<TaskResponseDTO>>

    // NEW: Get tasks assigned to me
    @GET("tasks/assigned-to-me")
    suspend fun getTasksAssignedToMe(
        @Query("user_email") userEmail: String? = null,
        @Query("user_phone") userPhone: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<List<TaskResponseDTO>>

    // NEW: Search tasks
    @GET("tasks/search")
    suspend fun searchTasks(
        @Query("query_text") queryText: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<List<TaskResponseDTO>>

    // NEW: Get task statistics
    @GET("tasks/stats")
    suspend fun getTaskStatistics(): Response<TaskStatisticsDTO>

    // NEW: Get single task
    @GET("tasks/{task_id}")
    suspend fun getTask(@Path("task_id") taskId: String): Response<TaskResponseDTO>

    @PATCH("tasks/{task_id}")
    suspend fun updateTask(
        @Path("task_id") taskId: String,
        @Body update: Map<String, Any?>
    ): Response<TaskResponseDTO>

    // NEW: Delete task
    @DELETE("tasks/{task_id}")
    suspend fun deleteTask(
        @Path("task_id") taskId: String,
        @Query("hard") hard: Boolean = false
    ): Response<Map<String, String>>

    @Multipart
    @POST("tasks/{task_id}/multimedia")
    suspend fun addTaskMultimedia(
        @Path("task_id") taskId: String,
        @Part file: MultipartBody.Part
    ): Response<Map<String, String>>

    // NEW: Remove multimedia
    @PATCH("tasks/{task_id}/multimedia")
    suspend fun removeTaskMultimedia(
        @Path("task_id") taskId: String,
        @Body body: Map<String, String> // {"url_to_remove": "..."}
    ): Response<Map<String, String>>

    // NEW: Get communication options
    @GET("tasks/{task_id}/communication-options")
    suspend fun getTaskCommunicationOptions(@Path("task_id") taskId: String): Response<CommunicationOptionsDTO>

    // NEW: Add external link
    @POST("tasks/{task_id}/external-links")
    suspend fun addTaskExternalLink(
        @Path("task_id") taskId: String,
        @Body link: LinkDTO
    ): Response<TaskResponseDTO>

    // NEW: Remove external link
    @DELETE("tasks/{task_id}/external-links/{link_index}")
    suspend fun removeTaskExternalLink(
        @Path("task_id") taskId: String,
        @Path("link_index") linkIndex: Int
    ): Response<TaskResponseDTO>

    // NEW: Duplicate task
    @POST("tasks/{task_id}/duplicate")
    suspend fun duplicateTask(@Path("task_id") taskId: String): Response<TaskResponseDTO>

    // ==================== AI & INSIGHTS ====================
    
    @GET("ai/stats")
    suspend fun getAIStats(): Response<AIStats>

    // ==================== BILLING ====================
    
    // NOTE: Backend currently supports Billing via Stripe Webhooks only. Public endpoints pending.
    // @GET("billing/wallet")
    // suspend fun getWallet(): Response<WalletDTO>

    // @POST("billing/checkout")
    // suspend fun createCheckoutSession(
    //    @Query("amount_credits") amount: Int
    // ): Response<Map<String, String>>

    // ==================== MEETINGS ====================
    
    @POST("meetings/join")
    suspend fun joinMeeting(@Body request: JoinMeetingRequest): Response<Map<String, Any>>
}
