package com.example.voicenote.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface GroqApi {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: GroqRequest
    ): GroqResponse

    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Header("Authorization") authHeader: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("language") language: RequestBody? = null,
        @Part("temperature") temperature: RequestBody? = null,
        @Part("response_format") responseFormat: RequestBody? = null
    ): TranscriptionResponse
}

data class TranscriptionResponse(val text: String)

data class GroqRequest(
    val model: String = "llama-3.1-8b-instant",
    val messages: List<Message>,
    val temperature: Double = 0.6,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
)

data class Message(val role: String, val content: String)
data class ResponseFormat(val type: String = "json_object")

data class GroqResponse(
    val choices: List<Choice>,
    val usage: Usage? = null
)

data class Choice(
    val message: Message,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

data class NoteAiOutput(
    val title: String,
    val summary: String,
    val priority: String,
    val transcript: String,
    val tasks: List<AiTask>
)

data class AiTask(
    val description: String,
    val priority: String,
    val deadline: String?, 
    val googlePrompt: String,
    val aiPrompt: String
)
