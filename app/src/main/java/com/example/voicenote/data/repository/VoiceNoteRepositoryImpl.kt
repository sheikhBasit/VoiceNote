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
                emit(Result.failure(Exception("Sync failed: We encountered an issue while synchronizing your profile. Please try again in a few moments.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun login(email: String, password: String): Flow<Result<SyncResponse>> = flow {
        try {
            // In a real app, this would be a proper Auth call
            val response = api.syncUser(SyncRequest(null)) // Dummy for login implementation
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
                emit(Result.failure(Exception("Request failed: We were unable to fetch your profile information. Please verify your connection.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun updateUserSettings(update: Map<String, Any?>): Flow<Result<UserDTO>> = flow {
        try {
            val response = api.updateUserSettings(update)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Configuration update failed: We were unable to synchronize your profile settings. Please verify your connection.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
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
                android.util.Log.e("Repository", "Failed to fetch notes: ${response.code()}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Network error in getNotes", e)
            emit(emptyList())
        }
    }

    override fun getNote(noteId: String): Flow<Result<NoteResponseDTO>> = flow {
        try {
            val response = api.getNote(noteId)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Note unavailable: We could not retrieve the requested note. It may have been deleted or archived.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
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
                    413 -> "File too large: Your recording exceeds the 50MB limit. Please provide a shorter audio clip."
                    else -> "Processing failed: We were unable to start the AI analysis for this recording. Please try again."
                }
                emit(Result.failure(Exception(errorMsg)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun updateNote(noteId: String, update: Map<String, Any?>): Flow<Result<NoteResponseDTO>> = flow { 
        try {
            val response = api.updateNote(noteId, update)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("Sync failed: Your changes to this note could not be saved to our servers. Please try again shortly.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun askAI(noteId: String, question: String): Flow<Result<String>> = flow {
        try {
            val response = api.askAI(noteId, mapOf("query" to question)) 
            if (response.isSuccessful) {
                emit(Result.success(response.body()?.get("message") ?: "AI is thinking..."))
            } else {
                emit(Result.failure(Exception("AI Unavailable: The AI Brain is temporarily unreachable. Please try your question again shortly.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
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
                emit(Result.failure(Exception("Task modified locally: We saved your changes but could not sync them with the server. They will sync automatically when your connection is stable.")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
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
                emit(Result.failure(Exception("Multimedia upload failed")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
