package com.example.voicenote.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Users & Identity ---
    @POST("users/sync")
    suspend fun syncUser(@Body request: SyncRequest): Response<SyncResponse>

    @POST("users/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<SyncResponse>

    @GET("users/me")
    suspend fun getUserProfile(): Response<UserDTO>

    @PATCH("users/me")
    suspend fun updateUserSettings(@Body update: Map<String, Any?>): Response<UserDTO>

    // --- Notes ---
    @Multipart
    @POST("notes/process")
    suspend fun processNote(
        @Part file: MultipartBody.Part,
        @Part("mode") mode: String? = "GENERIC"
    ): Response<Map<String, String>>

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

    @POST("notes/{note_id}/ask")
    suspend fun askAI(
        @Path("note_id") noteId: String,
        @Body body: Map<String, String>
    ): Response<Map<String, String>>

    @POST("notes/search")
    suspend fun searchNotes(@Body query: SearchQuery): Response<List<NoteResponseDTO>>

    @GET("billing/wallet")
    suspend fun getWallet(@Query("user_id") userId: String): Response<WalletDTO>

    @POST("billing/checkout")
    suspend fun createCheckoutSession(
        @Query("user_id") userId: String,
        @Query("amount_credits") amount: Int
    ): Response<Map<String, String>>

    @GET("notes/{note_id}/whatsapp")
    suspend fun getWhatsAppDraft(@Path("note_id") noteId: String): Response<Map<String, String>>

    // --- Tasks ---
    @GET("tasks")
    suspend fun listTasks(
        @Query("note_id") noteId: String? = null,
        @Query("priority") priority: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<List<TaskResponseDTO>>

    @PATCH("tasks/{task_id}")
    suspend fun updateTask(
        @Path("task_id") taskId: String,
        @Body update: Map<String, Any?>
    ): Response<TaskResponseDTO>

    @Multipart
    @POST("tasks/{task_id}/multimedia")
    suspend fun addTaskMultimedia(
        @Path("task_id") taskId: String,
        @Part file: MultipartBody.Part
    ): Response<Map<String, String>>
    @GET("ai/stats")
    suspend fun getAIStats(): Response<AIStats>

    @POST("notes/{note_id}/semantic-analysis")
    suspend fun triggerSemanticAnalysis(@Path("note_id") noteId: String): Response<Map<String, String>>

    @GET("notes/dashboard")
    suspend fun getDashboardData(): Response<DashboardResponse>

    @POST("meetings/join")
    suspend fun joinMeeting(@Body request: JoinMeetingRequest): Response<Map<String, Any>>
}
