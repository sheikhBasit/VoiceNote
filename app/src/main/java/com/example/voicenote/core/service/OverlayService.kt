package com.example.voicenote.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.voicenote.MainActivity
import com.example.voicenote.data.repository.FirestoreRepository
import com.example.voicenote.ui.theme.VoiceNoteTheme
import com.example.voicenote.ui.components.RecordingButton
import com.example.voicenote.ui.theme.GlassBackground
import com.example.voicenote.ui.theme.GlassBorder
import kotlinx.coroutines.launch
import java.util.*

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private val repository = FirestoreRepository()

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val isExpandedState = mutableStateOf(false)
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
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
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 400
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                VoiceNoteTheme {
                    val isExpanded by isExpandedState
                    val isRecording by VoiceRecordingService.isRecording.collectAsState()
                    var isVisibleBySchedule by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        launch {
                            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: Build.SERIAL
                            val user = repository.getUserByDeviceId(deviceId)
                            if (user != null && user.floatingButtonScheduled) {
                                val now = Calendar.getInstance()
                                val hour = now.get(Calendar.HOUR_OF_DAY)
                                val day = now.get(Calendar.DAY_OF_WEEK)
                                isVisibleBySchedule = (hour >= user.workStartHour && hour < user.workEndHour) && 
                                                     user.workDays.contains(day)
                            } else {
                                isVisibleBySchedule = true
                            }
                        }
                    }

                    if (isVisibleBySchedule) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(32.dp)
                                )
                                .padding(4.dp)
                        ) {
                            RecordingButton(
                                isRecording = isRecording,
                                onClick = { toggleRecording() }
                            )

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandHorizontally(),
                                exit = shrinkHorizontally()
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                                    ShortcutIcon(Icons.Default.Checklist) { launchApp("tasks"); isExpandedState.value = false }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    ShortcutIcon(Icons.Default.Description) { launchApp("notes"); isExpandedState.value = false }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    ShortcutIcon(Icons.Default.Settings) { launchApp("settings"); isExpandedState.value = false }
                                }
                            }
                        }
                    }
                }
            }
        }

        floatingView = composeView
        
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isMoving = false
            private var longClickTriggered = false

            private val longClickRunnable = Runnable {
                isExpandedState.value = !isExpandedState.value
                longClickTriggered = true
            }

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        longClickTriggered = false
                        handler.postDelayed(longClickRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            if (!isMoving) {
                                handler.removeCallbacks(longClickRunnable)
                            }
                            isMoving = true
                            params.x = initialX + deltaX.toInt()
                            params.y = initialY + deltaY.toInt()
                            windowManager.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        handler.removeCallbacks(longClickRunnable)
                        if (!isMoving && !longClickTriggered) {
                            toggleRecording()
                        }
                        return true
                    }
                    MotionEvent.ACTION_OUTSIDE -> {
                        isExpandedState.value = false
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    @Composable
    private fun ShortcutIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
        }
    }

    private fun launchApp(route: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("navigate_to", route)
        }
        startActivity(intent)
    }

    private fun toggleRecording() {
        if (VoiceRecordingService.isRecording.value) {
            val intent = Intent(this, VoiceRecordingService::class.java).apply {
                action = VoiceRecordingService.ACTION_STOP_RECORDING
            }
            startService(intent)
        } else {
            val intent = Intent(this, VoiceRecordingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        if (floatingView != null) windowManager.removeView(floatingView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
