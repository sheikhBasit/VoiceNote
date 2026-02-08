package com.example.voicenote.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==================== USERS & IDENTITY ====================
    
    @POST("users/sync")
    suspend fun syncUser(@Body request: SyncRequest): Response<SyncResponse>

    @POST("users/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<SyncResponse>

    @GET("users/me")
    suspend fun getUserProfile(): Response<UserDTO>

    @PATCH("users/me/profile-picture")
    suspend fun updateProfilePicture(@Body body: Map<String, String>): Response<UserDTO>

    @GET("users/balance")
    suspend fun getBalance(): Response<Map<String, Double>>

    @POST("users/logout")
    suspend fun logout(): Response<Map<String, String>>

    // ==================== NOTES ====================
    
    @GET("notes/dashboard")
    suspend fun getDashboardData(): Response<DashboardResponse>

    @GET("notes/presigned-url")
    suspend fun getPresignedUrl(@Query("note_id") noteId: String): Response<PresignedUrlResponse>

    @POST("notes/{note_id}/process")
    suspend fun processNote(@Path("note_id") noteId: String): Response<Map<String, String>>

    @POST("notes/search")
    suspend fun searchNotes(@Body query: SearchQuery): Response<SearchResponseDTO>

    @POST("notes/{note_id}/ask")
    suspend fun askAI(
        @Path("note_id") noteId: String,
        @Body body: Map<String, String>
    ): Response<Map<String, String>>

    @GET("notes")
    suspend fun listNotes(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 10
    ): Response<List<NoteResponseDTO>>

    @GET("notes/{note_id}")
    suspend fun getNote(@Path("note_id") noteId: String): Response<NoteResponseDTO>

    // ==================== TASKS ====================
    
    @GET("tasks/due-today")
    suspend fun getTasksDueToday(): Response<List<TaskResponseDTO>>

    @PATCH("tasks/{task_id}/complete")
    suspend fun completeTask(@Path("task_id") taskId: String): Response<TaskResponseDTO>

    @Multipart
    @POST("tasks/{task_id}/multimedia")
    suspend fun addTaskMultimedia(
        @Path("task_id") taskId: String,
        @Part file: MultipartBody.Part
    ): Response<Map<String, String>>

    // ==================== INTEGRATIONS ====================

    @POST("integrations/google/connect")
    suspend fun connectGoogle(@Body body: Map<String, String>): Response<Map<String, String>>
}

data class PresignedUrlResponse(val url: String)
