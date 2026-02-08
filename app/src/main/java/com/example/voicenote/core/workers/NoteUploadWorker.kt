
package com.example.voicenote.core.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.voicenote.data.remote.ApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class NoteUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val apiService: ApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val filePath = inputData.getString("file_path") ?: return Result.failure()
        val noteId = inputData.getString("note_id") ?: return Result.failure()
        val file = File(filePath)

        return try {
            // 1. Get Presigned URL
            val response = apiService.getPresignedUrl(noteId)
            if (response.isSuccessful) {
                val uploadUrl = response.body()?.url ?: return Result.failure()

                // 2. Upload via PUT request (Directly using OkHttp or a specialized service)
                // Note: Simplified for this guide. Usually involves another Api interface or direct OkHttp call.
                
                // 3. Once 200 OK, call /process to trigger AI
                apiService.processNote(noteId)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
