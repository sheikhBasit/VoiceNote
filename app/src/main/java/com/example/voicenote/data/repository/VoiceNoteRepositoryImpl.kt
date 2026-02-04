package com.example.voicenote.data.repository

import com.example.voicenote.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class VoiceNoteRepositoryImpl @Inject constructor(
    private val context: android.content.Context,
    private val api: ApiService,
    private val webSocketManager: com.example.voicenote.core.network.WebSocketManager
) : VoiceNoteRepository {

    // ==================== USER & IDENTITY MANAGEMENT ====================

    override fun syncUser(request: SyncRequest): Flow<Result<SyncResponse>> = flow {
        val result = try {
            val response = api.syncUser(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    403 -> "Trial already claimed: This device has already used its free package. Please upgrade to continue."
                    401 -> "Unauthorized: Your session has expired or is invalid. Please log in again."
                    404 -> "User not found: We couldn't find an account associated with this email."
                    409 -> "Conflict: This email is already associated with another device."
                    500 -> "Server Error: Our AI Brain is taking a nap. Please try again in a few minutes."
                    else -> "Connection Issue: We're having trouble reaching the server. Please check your internet."
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Network error: Please verify your connection and try again."))
        }
        emit(result)
    }

    override fun login(email: String, password: String): Flow<Result<SyncResponse>> = flow {
        val result = try {
            val credentials = mapOf("username" to email, "password" to password)
            val response = api.login(credentials)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Login failed: Invalid credentials"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun logout(): Flow<Result<String>> = flow {
        val result = try {
            val response = api.logout()
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Logged out")
            } else {
                Result.failure(Exception("Logout failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun getUserProfile(): Flow<Result<UserDTO>> = flow {
        val result = try {
            val response = api.getUserProfile()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Profile unavailable: We couldn't retrieve your details at this time."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Connection error: Unable to fetch profile."))
        }
        emit(result)
    }

    override fun updateUserSettings(update: Map<String, Any?>): Flow<Result<UserDTO>> = flow {
        val result = try {
            val response = api.updateUserSettings(update)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Update failed: Your settings couldn't be synchronized."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Connection error: Settings not saved."))
        }
        emit(result)
    }

    override fun verifyDevice(token: String): Flow<Result<String>> = flow {
        val result = try {
            val response = api.verifyDevice(token)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Device verified")
            } else {
                Result.failure(Exception("Verification failed: Invalid or expired token"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Connection error: Device verification failed"))
        }
        emit(result)
    }

    override fun searchUsers(query: String, role: String?, skip: Int, limit: Int): Flow<List<UserDTO>> = flow {
        val users = try {
            val response = api.searchUsers(query, role, skip, limit)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
        emit(users)
    }

    override fun deleteAccount(hard: Boolean, adminId: String?): Flow<Result<String>> = flow {
        val result = try {
            val response = api.deleteAccount(hard, adminId)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Account deleted")
            } else {
                Result.failure(Exception("Deletion failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun restoreAccount(userId: String): Flow<Result<UserDTO>> = flow {
        val result = try {
            val response = api.restoreAccount(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Restore failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun updateUserRole(userId: String, role: String, adminId: String): Flow<Result<UserDTO>> = flow {
        val result = try {
            val response = api.updateUserRole(userId, role, adminId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Role update failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    // ==================== REAL-TIME STATUS ====================

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

    // ==================== NOTES MANAGEMENT ====================

    override fun getNotes(skip: Int, limit: Int): Flow<List<NoteResponseDTO>> = flow {
        val notes = try {
            val response = api.listNotes(skip, limit)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
        emit(notes)
    }

    override fun getNote(noteId: String): Flow<Result<NoteResponseDTO>> = flow {
        val result = try {
            val response = api.getNote(noteId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Note not found: This note may have been deleted."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Connection error: Could not load note."))
        }
        emit(result)
    }

    override fun uploadVoiceNote(file: File, mode: String): Flow<Result<String>> = flow {
        val result = try {
            val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = api.processNote(body, mode)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("note_id") ?: "")
            } else {
                val errorMsg = when(response.code()) {
                    413 -> "Recording too large: Please keep voice notes under 50MB."
                    429 -> "Too many requests: You've reached your hourly processing limit."
                    else -> "Processing failed: The AI couldn't analyze this audio. Please try again."
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Upload failed: Check your connection and try again."))
        }
        emit(result)
    }

    override fun updateNote(noteId: String, update: Map<String, Any?>): Flow<Result<NoteResponseDTO>> = flow { 
        val result = try {
            val response = api.updateNote(noteId, update)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Save failed: Changes couldn't be synced."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Connection error: Note not updated."))
        }
        emit(result)
    }

    override fun deleteNote(noteId: String, hard: Boolean): Flow<Result<String>> = flow {
        val result = try {
            val response = api.deleteNote(noteId, hard)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Note deleted")
            } else {
                Result.failure(Exception("Delete failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun askAI(noteId: String, question: String): Flow<Result<String>> = flow {
        val result = try {
            val response = api.askAI(noteId, mapOf("query" to question)) 
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "AI is thinking...")
            } else {
                Result.failure(Exception("AI Busy: The Brain is currently overloaded. Try again in a second."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Connection error: AI unreachable."))
        }
        emit(result)
    }

    override fun searchNotes(query: String): Flow<Result<SearchResponseDTO>> = flow {
        val result = try {
            val response = api.searchNotes(SearchQuery(query))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Search failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun getDashboardData(): Flow<Result<DashboardResponse>> = flow {
        val result = try {
            val response = api.getDashboardData()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Dashboard unavailable"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun createNote(note: NoteCreateRequest): Flow<Result<NoteResponseDTO>> = flow {
        val result = try {
            val response = api.createNote(note)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Note creation failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun getWhatsAppDraft(noteId: String): Flow<Result<String>> = flow {
        val result = try {
            val response = api.getWhatsAppDraft(noteId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.draft)
            } else {
                Result.failure(Exception("WhatsApp draft generation failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun triggerSemanticAnalysis(noteId: String): Flow<Result<String>> = flow {
        val result = try {
            val response = api.triggerSemanticAnalysis(noteId)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Analysis started")
            } else {
                Result.failure(Exception("Semantic analysis failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    // ==================== TASKS MANAGEMENT ====================

    override fun getTasks(noteId: String?, priority: String?): Flow<List<TaskResponseDTO>> = flow {
        val tasks = try {
            val response = api.listTasks(noteId = noteId, priority = priority)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
        emit(tasks)
    }

    override fun updateTask(taskId: String, update: Map<String, Any?>): Flow<Result<TaskResponseDTO>> = flow {
        val result = try {
            val response = api.updateTask(taskId, update)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Task sync failed: Change saved locally only."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Connection error: Task not updated."))
        }
        emit(result)
    }

    override fun uploadTaskMultimedia(taskId: String, file: File): Flow<Result<String>> = flow {
        val result = try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = api.addTaskMultimedia(taskId, body)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Uploaded")
            } else {
                Result.failure(Exception("Image upload failed."))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(Exception("Connection error: Image not uploaded."))
        }
        emit(result)
    }

    override fun createTask(task: TaskCreateRequest): Flow<Result<TaskResponseDTO>> = flow {
        val result = try {
            val response = api.createTask(task)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Task creation failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun getTasksDueToday(limit: Int, offset: Int): Flow<List<TaskResponseDTO>> = flow {
        val tasks = try {
            val response = api.getTasksDueToday(limit, offset)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
        emit(tasks)
    }

    override fun getOverdueTasks(limit: Int, offset: Int): Flow<List<TaskResponseDTO>> = flow {
        val tasks = try {
            val response = api.getOverdueTasks(limit, offset)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
        emit(tasks)
    }

    override fun getTasksAssignedToMe(userEmail: String?, userPhone: String?, limit: Int, offset: Int): Flow<List<TaskResponseDTO>> = flow {
        val tasks = try {
            val response = api.getTasksAssignedToMe(userEmail, userPhone, limit, offset)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
        emit(tasks)
    }

    override fun searchTasks(queryText: String, limit: Int, offset: Int): Flow<List<TaskResponseDTO>> = flow {
        val tasks = try {
            val response = api.searchTasks(queryText, limit, offset)
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
        emit(tasks)
    }

    override fun getTask(taskId: String): Flow<Result<TaskResponseDTO>> = flow {
        val result = try {
            val response = api.getTask(taskId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Task not found"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun getTaskStatistics(): Flow<Result<TaskStatisticsDTO>> = flow {
        val result = try {
            val response = api.getTaskStatistics()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Statistics unavailable"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun deleteTask(taskId: String, hard: Boolean): Flow<Result<String>> = flow {
        val result = try {
            val response = api.deleteTask(taskId, hard)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Task deleted")
            } else {
                Result.failure(Exception("Task deletion failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun removeTaskMultimedia(taskId: String, urlToRemove: String): Flow<Result<String>> = flow {
        val result = try {
            val response = api.removeTaskMultimedia(taskId, mapOf("url_to_remove" to urlToRemove))
            if (response.isSuccessful) {
                Result.success(response.body()?.get("message") ?: "Multimedia removed")
            } else {
                Result.failure(Exception("Removal failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun getTaskCommunicationOptions(taskId: String): Flow<Result<CommunicationOptionsDTO>> = flow {
        val result = try {
            val response = api.getTaskCommunicationOptions(taskId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Communication options unavailable"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun addTaskExternalLink(taskId: String, link: LinkDTO): Flow<Result<TaskResponseDTO>> = flow {
        val result = try {
            val response = api.addTaskExternalLink(taskId, link)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Link addition failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun removeTaskExternalLink(taskId: String, linkIndex: Int): Flow<Result<TaskResponseDTO>> = flow {
        val result = try {
            val response = api.removeTaskExternalLink(taskId, linkIndex)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Link removal failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun duplicateTask(taskId: String): Flow<Result<TaskResponseDTO>> = flow {
        val result = try {
            val response = api.duplicateTask(taskId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Task duplication failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    override fun getAIStats(): Flow<Result<AIStats>> = flow {
        val result = try {
            val response = api.getAIStats()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("AI Stats unavailable"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
        emit(result)
    }

    // ==================== BILLING & MONETIZATION ====================

    override fun getWallet(): Flow<Result<WalletDTO>> = flow {
        val result = try {
            // val response = api.getWallet()
            // if (response.isSuccessful && response.body() != null) {
            //    Result.success(response.body()!!)
            // } else {
            val errorMsg = "Wallet error: Billing system is currently webhook-only."
            Result.failure<WalletDTO>(Exception(errorMsg))
            // }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure<WalletDTO>(Exception("Connection error: Wallet unreachable."))
        }
        emit(result)
    }

    override fun topUpWallet(amount: Int): Flow<Result<String>> = flow {
        val result = try {
            // val response = api.createCheckoutSession(amount)
            // if (response.isSuccessful) {
            //    Result.success(response.body()?.get("checkout_url") ?: "")
            // } else {
            val errorMsg = "Payment failed: Public checkout API pending."
            Result.failure<String>(Exception(errorMsg))
            // }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure<String>(Exception("Connection error: Payment server unreachable."))
        }
        emit(result)
    }

    // ==================== DRAFTS (OFFLINE MODE) ====================
    override fun getDrafts(): Flow<List<File>> = flow {
        val draftsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "drafts")
        if (!draftsDir.exists()) draftsDir.mkdirs()
        emit(draftsDir.listFiles()?.toList() ?: emptyList())
    }

    override fun deleteDraft(file: File): Flow<Result<Unit>> = flow {
        if (file.exists()) {
            file.delete()
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("File not found")))
        }
    }

    // ==================== MEETINGS ====================

    override fun joinMeeting(meetingUrl: String, botName: String): Flow<Result<Map<String, Any>>> = flow {
        val result = try {
            val request = JoinMeetingRequest(meetingUrl, botName)
            val response = api.joinMeeting(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body() as Map<String, Any>)
            } else {
                Result.failure<Map<String, Any>>(Exception("Meeting join failed"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure<Map<String, Any>>(e)
        }
        emit(result)
    }
}
