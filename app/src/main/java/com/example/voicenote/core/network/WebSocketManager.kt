package com.example.voicenote.core.network

import android.util.Log
import com.example.voicenote.core.security.SecurityManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val client: OkHttpClient,
    private val securityManager: SecurityManager,
    private val gson: Gson
) : WebSocketListener() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var isManuallyClosed = false

    private val _updates = MutableSharedFlow<Map<String, Any>>(extraBufferCapacity = 10)
    val updates: SharedFlow<Map<String, Any>> = _updates

    fun connect() {
        val userId = securityManager.getUserId() ?: return
        isManuallyClosed = false
        val request = Request.Builder()
            .url("ws://api.voicenote.ai/api/ws/$userId") // Real-world: Use Secure WSS
            .build()
        
        webSocket = client.newWebSocket(request, this)
        Log.d("WebSocket", "Connecting for user: $userId")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        scope.launch {
            try {
                val type = object : com.google.common.reflect.TypeToken<Map<String, Any>>() {}.type
                val map: Map<String, Any> = gson.fromJson(text, type)
                _updates.emit(map)
                Log.d("WebSocket", "Received: $text")
            } catch (e: Exception) {
                Log.e("WebSocket", "Parse error: $text", e)
            }
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        Log.d("WebSocket", "Closing: $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        if (!isManuallyClosed) {
            scope.launch {
                delay(5000)
                connect() // Auto-reconnect
            }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("WebSocket", "Failure: ${t.message}")
        if (!isManuallyClosed) {
            scope.launch {
                delay(10000)
                connect() // Exponential backoff simulation
            }
        }
    }

    fun disconnect() {
        isManuallyClosed = true
        webSocket?.close(1000, "User logout")
        webSocket = null
    }
}
