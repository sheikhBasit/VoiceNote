package com.example.voicenote.data.repository

import com.example.voicenote.data.network.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AiRepository(private val firestoreRepository: FirestoreRepository) {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val groqApi = retrofit.create(GroqApi::class.java)
    private val gson = Gson()

    suspend fun transcribeAudio(audioFile: File): String? {
        val config = firestoreRepository.getAppConfig().firstOrNull() ?: return null
        if (config.apiKeys.isEmpty()) return null

        val currentKey = config.apiKeys[config.currentKeyIndex]
        
        val mediaType = "audio/mpeg".toMediaTypeOrNull()
        val requestFile = audioFile.asRequestBody(mediaType)
        val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
        val model = "whisper-large-v3-turbo".toRequestBody("text/plain".toMediaTypeOrNull())

        return try {
            val response = groqApi.transcribeAudio("Bearer $currentKey", body, model)
            response.text
        } catch (e: Exception) {
            firestoreRepository.rotateApiKey()
            null
        }
    }

    suspend fun processConversationChunks(transcriptions: List<String>): NoteAiOutput? {
        val config = firestoreRepository.getAppConfig().firstOrNull() ?: return null
        if (config.apiKeys.isEmpty()) return null

        val currentKey = config.apiKeys[config.currentKeyIndex]
        val now = Calendar.getInstance().time
        val todayStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(now)
        
        val systemPrompt = """
            You are an advanced note-taking assistant. Analyze the conversation chunks and return a structured JSON object.
            Current Time Reference: $todayStr
            
            IMPORTANT: 
            1. All output fields MUST BE IN ENGLISH.
            2. Decide the "priority" for the NOTE overall AND for each TASK independently. 
               - If a deadline is within the next 24 hours, set priority to "High".
            3. Provide distinct "googlePrompt" (search query) and "aiPrompt" (detailed instruction) for each task.
            4. Deadlines MUST include time in "YYYY-MM-DD HH:mm" format. 
               - NEVER set a deadline in the past. If a past date/time is mentioned, assume it refers to the next occurrence in the future.
               - If no time is mentioned, default to 17:00 (5 PM) of the mentioned or current day.
            5. Provide a "transcript" field which is the full conversation text, but formatted with speaker identification (e.g., Speaker A:, Speaker B:).
            
            JSON Structure:
            {
              "title": "A 3-5 word descriptive title in English",
              "summary": "A 2-sentence breakdown of the conversation in English",
              "priority": "High, Medium, or Low",
              "transcript": "The full formatted transcript with speaker labels",
              "tasks": [
                {
                  "description": "Task description in English",
                  "priority": "High, Medium, or Low",
                  "deadline": "YYYY-MM-DD HH:mm format",
                  "googlePrompt": "A specific search query to help gather info for this task",
                  "aiPrompt": "A detailed prompt to give to an AI to help execute this task"
                }
              ]
            }
            Ensure the response is ONLY the JSON object.
        """.trimIndent()

        val messages = mutableListOf(Message(role = "system", content = systemPrompt))
        transcriptions.forEach { chunk ->
            messages.add(Message(role = "user", content = chunk))
        }

        val request = GroqRequest(
            model = "llama-3.1-8b-instant",
            messages = messages,
            temperature = 0.6
        )

        return try {
            val response = groqApi.getChatCompletion("Bearer $currentKey", request)
            val jsonContent = response.choices.firstOrNull()?.message?.content
            if (jsonContent == null || jsonContent.trim() == "{}") return null
            gson.fromJson(jsonContent, NoteAiOutput::class.java)
        } catch (e: Exception) {
            firestoreRepository.rotateApiKey()
            null
        }
    }
}