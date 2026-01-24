package com.example.voicenote.data.repository

import com.example.voicenote.data.model.*
import com.example.voicenote.data.network.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AiRepository(private val firestoreRepository: FirestoreRepository) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/")
        .client(okHttpClient)
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
            handleApiError(e)
            null
        }
    }

    suspend fun processConversationChunks(transcriptions: List<String>, role: UserRole): NoteAiOutput? {
        val config = firestoreRepository.getAppConfig().firstOrNull() ?: return null
        if (config.apiKeys.isEmpty()) return null

        val currentKey = config.apiKeys[config.currentKeyIndex]
        val now = Calendar.getInstance().time
        val todayStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(now)
        
        val roleSpecificInstruction = when(role) {
            UserRole.STUDENT -> "Focus on key academic concepts, definitions, formulas, and potential exam questions."
            UserRole.TEACHER -> "Focus on lesson plan points, student participation highlights, and follow-up grading tasks."
            UserRole.DEVELOPER -> "Focus on technical architecture, bug reports, pull request tasks, and code snippet placeholders."
            UserRole.OFFICE_WORKER -> "Focus on action items, meeting minutes, deadlines, and project milestones."
            UserRole.PSYCHIATRIST, UserRole.PSYCHOLOGIST -> "Focus on patient sentiment, recurring themes, behavioral observations, and clinical notes while maintaining strict formal tone."
            UserRole.BUSINESS_MAN -> "Focus on ROI, deal terms, networking contacts, and growth opportunities."
            else -> "Provide a comprehensive professional summary and task list."
        }

        val systemPrompt = """
            You are an advanced AI personal assistant tailored for a ${role.name}. 
            $roleSpecificInstruction
            
            Analyze the transcription and return a structured JSON object.
            Current Time Reference: $todayStr
            
            IMPORTANT: 
            1. All output fields MUST BE IN ENGLISH.
            2. Decide the "priority" for the NOTE overall AND for each TASK independently.
            3. Provide distinct "googlePrompt" (search query) and "aiPrompt" (detailed instruction) for each task.
            4. Deadlines MUST include time in "YYYY-MM-DD HH:mm" format.
            5. Provide a "transcript" field with speaker identification (e.g., Speaker A:, Speaker B:).
            
            JSON Structure:
            {
              "title": "A descriptive title in English",
              "summary": "A role-specific summary in English",
              "priority": "High, Medium, or Low",
              "transcript": "The full formatted transcript with speaker labels",
              "tasks": [
                {
                  "description": "Task description in English",
                  "priority": "High, Medium, or Low",
                  "deadline": "YYYY-MM-DD HH:mm format",
                  "googlePrompt": "A search query for this task",
                  "aiPrompt": "An AI prompt for this task"
                }
              ]
            }
            Ensure the response is ONLY the JSON object.
        """.trimIndent()

        val messages = mutableListOf(Message(role = "system", content = systemPrompt))
        transcriptions.forEach { messages.add(Message(role = "user", content = it)) }

        val request = GroqRequest(model = "llama-3.1-8b-instant", messages = messages)

        return try {
            val response = groqApi.getChatCompletion("Bearer $currentKey", request)
            val jsonContent = response.choices.firstOrNull()?.message?.content
            if (jsonContent == null || jsonContent.trim() == "{}") return null
            
            val cleanedJson = cleanJsonResponse(jsonContent)
            gson.fromJson(cleanedJson, NoteAiOutput::class.java)
        } catch (e: Exception) {
            handleApiError(e)
            null
        }
    }

    suspend fun askAssistant(note: Note, question: String): String {
        val config = firestoreRepository.getAppConfig().firstOrNull() ?: return "API config error."
        if (config.apiKeys.isEmpty()) return "No API keys found."

        val currentKey = config.apiKeys[config.currentKeyIndex]
        
        val systemPrompt = "You are a helpful assistant. You have access to the following note context: ${note.summary}. Transcript: ${note.transcript}. Answer the user's question based strictly on this context."
        
        val request = GroqRequest(
            model = "llama-3.1-8b-instant",
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = question)
            ),
            responseFormat = ResponseFormat(type = "text")
        )

        return try {
            val response = groqApi.getChatCompletion("Bearer $currentKey", request)
            response.choices.firstOrNull()?.message?.content ?: "I couldn't generate an answer."
        } catch (e: Exception) {
            handleApiError(e)
            "Error communicating with AI brain."
        }
    }

    private fun cleanJsonResponse(content: String): String {
        var result = content.trim()
        if (result.startsWith("```json")) {
            result = result.removePrefix("```json")
        }
        if (result.startsWith("```")) {
            result = result.removePrefix("```")
        }
        if (result.endsWith("```")) {
            result = result.removeSuffix("```")
        }
        return result.trim()
    }

    private suspend fun handleApiError(e: Exception) {
        if (e is HttpException) {
            val code = e.code()
            // 401: Unauthorized, 429: Rate Limit
            if (code == 401 || code == 429) {
                firestoreRepository.rotateApiKey()
            }
        }
    }
}
