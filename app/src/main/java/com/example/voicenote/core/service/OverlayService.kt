package com.example.voicenote.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.voicenote.core.security.SecurityManager
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.data.repository.VoiceNoteRepositoryImpl
import com.example.voicenote.di.NetworkModule
import com.example.voicenote.ui.theme.VoiceNoteTheme
import com.example.voicenote.ui.theme.InsightsBackgroundDark
import com.example.voicenote.ui.components.RecordingButton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.*

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var params: WindowManager.LayoutParams
    
    private var binView: View? = null
    private lateinit var binParams: WindowManager.LayoutParams
    
    private val isOverBinState = mutableStateOf(false)
    private var lastWasOverBin = false

    private val repository: VoiceNoteRepository by lazy {
        val securityManager = NetworkModule.provideSecurityManager(applicationContext)
        val gson = NetworkModule.provideGson()
        val okHttpClient = NetworkModule.provideOkHttpClient(securityManager)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient, gson)
        val apiService = NetworkModule.provideApiService(retrofit)
        val webSocketManager = NetworkModule.provideWebSocketManager(okHttpClient, securityManager, gson)
        VoiceNoteRepositoryImpl(applicationContext, apiService, webSocketManager)
    }

    private val securityManager: SecurityManager by lazy {
        NetworkModule.provideSecurityManager(applicationContext)
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showFloatingHub()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            showFloatingHub()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceNote Overlay")
            .setContentText("VoiceNote is active in the background")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL) {
            // Clear any heavy caches if necessary
            // For now, let's just log and ensure we cooperate with the system
        }
    }

    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun performHapticFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun showBin() {
        if (binView != null) return
        
        binParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100.dpToPx()
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                VoiceNoteTheme {
                    val isOverBin by isOverBinState
                    val scale by animateFloatAsState(if (isOverBin) 1.2f else 1f)
                    val color by animateColorAsState(if (isOverBin) Color(0xFFFF5252) else Color.Black.copy(alpha = 0.4f))
                    val borderColor by animateColorAsState(if (isOverBin) Color(0xFFFF5252).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (isOverBin) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        binView = composeView
        try {
            windowManager.addView(binView, binParams)
        } catch (e: Exception) {}
    }

    private fun hideBin() {
        binView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        binView = null
        isOverBinState.value = false
        lastWasOverBin = false
    }

    private fun updateOverBinStatus() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Button center relative to TOP|START
        val buttonCenterX = params.x + 24.dpToPx() // (32dp + 16dp padding)/2 = 24dp
        val buttonCenterY = params.y + 24.dpToPx()
        
        // Bin center relative to TOP|START
        val binCenterX = screenWidth / 2f
        val binCenterY = screenHeight - 100.dpToPx() - 28.dpToPx() // y offset + bin height/2
        
        val distance = Math.hypot((buttonCenterX - binCenterX).toDouble(), (buttonCenterY - binCenterY).toDouble())
        val currentlyOverBin = distance < 80.dpToPx()
        
        if (currentlyOverBin != lastWasOverBin) {
            if (currentlyOverBin) {
                performHapticFeedback()
            }
            isOverBinState.value = currentlyOverBin
            lastWasOverBin = currentlyOverBin
        }
    }

    private fun showFloatingHub() {
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 500
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                VoiceNoteTheme {
                    val isRecording by VoiceRecordingService.isRecording.collectAsState()
                    var isVisibleBySchedule by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        launch {
                            repository.getUserProfile().firstOrNull()?.onSuccess { user ->
                                val now = Calendar.getInstance()
                                val hour = now.get(Calendar.HOUR_OF_DAY)
                                val day = now.get(Calendar.DAY_OF_WEEK)
                                if (user.workStartHour != 0 || user.workEndHour != 0) {
                                    isVisibleBySchedule = (hour >= user.workStartHour && hour < user.workEndHour) && 
                                                         (user.workDays.isEmpty() || user.workDays.contains(day))
                                }
                            }
                        }
                    }

                    if (isVisibleBySchedule) {
                        Box(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { showBin() },
                                        onDragEnd = {
                                            if (isOverBinState.value) {
                                                securityManager.setFloatingButtonEnabled(false)
                                                stopSelf()
                                            }
                                            hideBin()
                                        },
                                        onDragCancel = { hideBin() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            params.x += dragAmount.x.toInt()
                                            params.y += dragAmount.y.toInt()
                                            updateOverBinStatus()
                                            try {
                                                if (floatingView?.parent != null) {
                                                    windowManager.updateViewLayout(floatingView, params)
                                                }
                                            } catch (e: Exception) {}
                                        }
                                    )
                                }
                                .padding(8.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(InsightsBackgroundDark.copy(alpha = 0.9f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            RecordingButton(
                                isRecording = isRecording,
                                onClick = { toggleRecording() },
                                isSmall = true
                            )
                        }
                    }
                }
            }
        }

        floatingView = composeView
        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleRecording() {
        val intent = Intent(this, VoiceRecordingService::class.java)
        if (VoiceRecordingService.isRecording.value) {
            intent.action = VoiceRecordingService.ACTION_STOP_RECORDING
            startService(intent)
        } else {
            intent.action = "START"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideBin()
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        floatingView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        _viewModelStore.clear()
    }
}
