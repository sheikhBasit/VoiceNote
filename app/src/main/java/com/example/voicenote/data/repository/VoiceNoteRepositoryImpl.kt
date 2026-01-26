package com.example.voicenote.data.repository

import com.example.voicenote.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class VoiceNoteRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val webSocketManager: com.example.voicenote.core.network.WebSocketManager
) : VoiceNoteRepository {

    override fun syncUser(request: SyncRequest): Flow<Result<SyncResponse>> = flow {
        try {
            val response = api.syncUser(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                val errorMsg = when (response.code()) {
                    403 -> "Trial already claimed: This device has already used its free package. Please upgrade to continue."
                    401 -> "Unauthorized: Your session has expired or is invalid. Please log in again."
                    404 -> "User not found: We couldn't find an account associated with this email."
                    409 -> "Conflict: This email is already associated with another device."
                    500 -> "Server Error: Our AI Brain is taking a nap. Please try again in a few minutes."
                    else -> "Connection Issue: We're having trouble reaching the server. Please check your internet."
                }
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Network error: Please verify your connection and try again.")))
        }
    }

    override fun login(email: String, password: String): Flow<Result<SyncResponse>> = flow {
        // Implementation similar to syncUser for real auth
        try {
            val dummyRequest = SyncRequest(
                name = "User",
                email = email,
                token = "dummy_token",
                deviceId = "dummy_device",
                deviceModel = "dummy_model",
                primaryRole = "USER"
            )
            val response = api.syncUser(dummyRequest)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Login failed: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getUserProfile(): Flow<Result<UserDTO>> = flow {
        try {
            val response = api.getUserProfile()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Profile unavailable: We couldn't retrieve your details at this time.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Connection error: Unable to fetch profile.")))
        }
    }

    override fun updateUserSettings(update: Map<String, Any?>): Flow<Result<UserDTO>> = flow {
        try {
            val response = api.updateUserSettings(update)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Update failed: Your settings couldn't be synchronized.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Connection error: Settings not saved.")))
        }
    }

    override fun observeTaskStatus(userId: String): Flow<TaskStatusUpdate> = flow {
        webSocketManager.updates.collect { event ->
            if (event["type"] == "NOTE_STATUS" || event["type"] == "TASK_EXTRACTED") {
                val data = event["data"] as? Map<*, *> ?: return@collect
                emit(TaskStatusUpdate(
                    type = event["type"] as String,
                    noteId = data["note_id"] as? String,
                    taskId = data["task_id"] as? String,
                    status = data["status"] as? String,
                    progress = (data["progress"] as? Double)?.toInt()
                ))
            }
        }
    }

    override fun getNotes(skip: Int, limit: Int): Flow<List<NoteResponseDTO>> = flow {
        try {
            val response = api.listNotes(skip, limit)
            if (response.isSuccessful) {
                emit(response.body() ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getNote(noteId: String): Flow<Result<NoteResponseDTO>> = flow {
        try {
            val response = api.getNote(noteId)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Note not found: This note may have been deleted.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Connection error: Could not load note.")))
        }
    }

    override fun uploadVoiceNote(file: File, mode: String): Flow<Result<String>> = flow {
        try {
            val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = api.processNote(body, mode)
            if (response.isSuccessful) {
                emit(Result.success(response.body()?.get("note_id") ?: ""))
            } else {
                val errorMsg = when(response.code()) {
                    413 -> "Recording too large: Please keep voice notes under 50MB."
                    429 -> "Too many requests: You've reached your hourly processing limit."
                    else -> "Processing failed: The AI couldn't analyze this audio. Please try again."
                }
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Upload failed: Check your connection and try again.")))
        }
    }

    override fun updateNote(noteId: String, update: Map<String, Any?>): Flow<Result<NoteResponseDTO>> = flow { 
        try {
            val response = api.updateNote(noteId, update)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Save failed: Changes couldn't be synced.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Connection error: Note not updated.")))
        }
    }

    override fun askAI(noteId: String, question: String): Flow<Result<String>> = flow {
        try {
            val response = api.askAI(noteId, mapOf("query" to question)) 
            if (response.isSuccessful) {
                emit(Result.success(response.body()?.get("message") ?: "AI is thinking..."))
            } else {
                emit(Result.failure(Exception("AI Busy: The Brain is currently overloaded. Try again in a second.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Connection error: AI unreachable.")))
        }
    }

    override fun searchNotes(query: String): Flow<List<NoteResponseDTO>> = flow {
        try {
            val response = api.searchNotes(SearchQuery(query))
            if (response.isSuccessful) {
                emit(response.body() ?: emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getTasks(noteId: String?, priority: String?): Flow<List<TaskResponseDTO>> = flow {
        try {
            val response = api.listTasks(noteId = noteId, priority = priority)
            if (response.isSuccessful) {
                emit(response.body() ?: emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun updateTask(taskId: String, update: Map<String, Any?>): Flow<Result<TaskResponseDTO>> = flow {
        try {
            val response = api.updateTask(taskId, update)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Task sync failed: Change saved locally only.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Connection error: Task not updated.")))
        }
    }

    override fun uploadTaskMultimedia(taskId: String, file: File): Flow<Result<String>> = flow {
        try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = api.addTaskMultimedia(taskId, body)
            if (response.isSuccessful) {
                emit(Result.success(response.body()?.get("message") ?: "Uploaded"))
            } else {
                emit(Result.failure(Exception("Image upload failed.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Connection error: Image not uploaded.")))
        }
    }

    override fun getWallet(): Flow<Result<WalletDTO>> = flow {
        try {
            val response = api.getWallet("me")
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Wallet error: Unable to load balance.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Connection error: Wallet unreachable.")))
        }
    }

    override fun topUpWallet(amount: Int): Flow<Result<String>> = flow {
        try {
            val response = api.createCheckoutSession("me", amount)
            if (response.isSuccessful) {
                emit(Result.success(response.body()?.get("checkout_url") ?: ""))
            } else {
                emit(Result.failure(Exception("Payment failed: Couldn't start checkout.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Connection error: Payment server unreachable.")))
        }
    }
}
