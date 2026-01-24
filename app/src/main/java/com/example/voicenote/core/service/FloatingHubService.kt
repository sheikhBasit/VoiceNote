package com.example.voicenote.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.voicenote.features.home.RecordingButton
import com.example.voicenote.ui.theme.VoiceNoteTheme

class FloatingHubService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showFloatingHub()
    }

    private fun showFloatingHub() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        val composeView = ComposeView(this).apply {
            setContent {
                VoiceNoteTheme {
                    val isRecording by VoiceRecordingService.isRecording.collectAsState()
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    params.x += dragAmount.x.toInt()
                                    params.y += dragAmount.y.toInt()
                                    windowManager.updateViewLayout(floatingView, params)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        RecordingButton(
                            isRecording = isRecording,
                            onClick = {
                                val securityManager = com.example.voicenote.core.security.SecurityManager(this@FloatingHubService)
                                if (securityManager.getSessionToken() == null) {
                                    android.widget.Toast.makeText(this@FloatingHubService, "Authentication required: Please log in to the main application to enable voice recording.", android.widget.Toast.LENGTH_LONG).show()
                                    return@RecordingButton
                                }
                                
                                val intent = Intent(this@FloatingHubService, VoiceRecordingService::class.java)
                                if (isRecording) {
                                    intent.action = VoiceRecordingService.ACTION_STOP_RECORDING
                                    startService(intent)
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(intent)
                                    } else {
                                        startService(intent)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Lifecycle and SavedState owners are required for ComposeView in Service
        val lifecycleOwner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this)
        }
        lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
        
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        
        floatingView = composeView
        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
