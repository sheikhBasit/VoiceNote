package com.example.voicenote.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.*
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.voicenote.core.utils.CalendarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

@dagger.hilt.android.AndroidEntryPoint
class VoiceRecordingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    @Inject lateinit var repository: com.example.voicenote.data.repository.VoiceNoteRepository
    private lateinit var calendarManager: CalendarManager

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isPausedBySilence = false
    private val handler = Handler(Looper.getMainLooper())
    private var currentMeetingTitle: String? = null

    private val SILENCE_THRESHOLD = 400 
    private val VAD_CHECK_INTERVAL = 50L 

    private val vadRunnable = object : Runnable {
        override fun run() {
            if (mediaRecorder != null && isRecording.value) {
                val amp = try { mediaRecorder?.maxAmplitude ?: 0 } catch (e: Exception) { 0 }
                _amplitude.value = amp
                
                if (amp < SILENCE_THRESHOLD && !isPausedBySilence) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            mediaRecorder?.pause()
                            isPausedBySilence = true
                            _statusLog.value = "Capture paused: Silence detected..."
                        } catch (e: Exception) { Log.e("VAD", "Pause failed") }
                    }
                } else if (amp >= SILENCE_THRESHOLD && isPausedBySilence) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            mediaRecorder?.resume()
                            isPausedBySilence = false
                            _statusLog.value = "Capture active: Priority audio detected..."
                        } catch (e: Exception) { Log.e("VAD", "Resume failed") }
                    }
                }
                handler.postDelayed(this, VAD_CHECK_INTERVAL)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "VoiceRecordingChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val ACTION_DISCARD_RECORDING = "ACTION_DISCARD_RECORDING"
        
        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording
        
        private val _statusLog = MutableStateFlow<String>("Idle")
        val statusLog: StateFlow<String> = _statusLog

        private val _amplitude = MutableStateFlow(0)
        val amplitude: StateFlow<Int> = _amplitude

        private val _lastRecordedFilePath = MutableStateFlow<String?>(null)
        val lastRecordedFilePath: StateFlow<String?> = _lastRecordedFilePath

        private val _transcriptionHistory = MutableStateFlow<List<String>>(emptyList())
        val transcriptionHistory: StateFlow<List<String>> = _transcriptionHistory
        
        private val _recordingStartTime = MutableStateFlow<Long>(0L)
        val recordingStartTime: StateFlow<Long> = _recordingStartTime
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        calendarManager = CalendarManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RECORDING, "STOP" -> stopRecordingAndProcess()
            ACTION_DISCARD_RECORDING -> discardRecording()
            else -> startRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (_isRecording.value) return

        try {
            currentMeetingTitle = getCurrentCalendarEvent()
            
            val persistentDir = getExternalFilesDir(null) ?: filesDir
            audioFile = File(persistentDir, "recording_${System.currentTimeMillis()}.mp4")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1) // Mono only
                setAudioSamplingRate(16000) // 16kHz for STT consistency
                setAudioEncodingBitRate(64000)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            enableAudioEffects()
            _isRecording.value = true
            _recordingStartTime.value = System.currentTimeMillis()
            _statusLog.value = if (currentMeetingTitle != null) "Archiving: $currentMeetingTitle" else "Capture active: Recording in progress..."
            isPausedBySilence = false
            handler.postDelayed(vadRunnable, VAD_CHECK_INTERVAL)
            triggerHapticFeedback(true)
            
            val notificationContent = if (currentMeetingTitle != null) {
                "Capturing: $currentMeetingTitle\n💡 Tip: Place phone on soft surface for better audio."
            } else {
                "Meeting Voice Capture\n💡 Tip: Point bottom mic toward speaker."
            }
            startForeground(NOTIFICATION_ID, createNotification(notificationContent))
        } catch (e: Exception) {
            Log.e("VoiceRecordingService", "Failed to start recording", e)
            _statusLog.value = "System Error: Failed to initialize recording hardware."
            stopSelf()
        }
    }

    private fun enableAudioEffects() {
        try {
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(0)
            }
            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler.create(0)
            }
        } catch (e: Exception) {
            Log.w("AudioEffects", "Hardware effects not available")
        }
    }

    private fun getCurrentCalendarEvent(): String? {
        return try {
            val now = System.currentTimeMillis()
            val cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events.TITLE),
                "(${CalendarContract.Events.DTSTART} <= ?) AND (${CalendarContract.Events.DTEND} >= ?)",
                arrayOf(now.toString(), now.toString()),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun discardRecording() {
        handler.removeCallbacks(vadRunnable)
        mediaRecorder?.apply {
            try { stop() } catch (e: Exception) {}
            release()
        }
        mediaRecorder = null
        _isRecording.value = false
        audioFile?.delete()
        _statusLog.value = "Recording discarded."
        triggerHapticFeedback(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun stopRecordingAndProcess() {
        if (!_isRecording.value) return

        try {
            handler.removeCallbacks(vadRunnable)
            mediaRecorder?.apply {
                try {
                    if (isPausedBySilence && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        resume()
                    }
                    stop()
                } catch (e: Exception) {
                    Log.e("VoiceRecordingService", "Stop failed", e)
                }
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            triggerHapticFeedback(false)
            _statusLog.value = "Optimizing audio payload..."
            _lastRecordedFilePath.value = audioFile?.absolutePath

            serviceScope.launch {
                val file = audioFile ?: return@launch
                _statusLog.value = "Synchronizing with AI Brain..."
                
                repository.uploadVoiceNote(file).collect { result ->
                    result.onSuccess { noteId ->
                        _statusLog.value = "Synchronization complete. Analytics pending."
                        triggerSuccessHaptic()
                    }.onFailure { error ->
                        _statusLog.value = "Synchronization failed: ${error.message}"
                        triggerErrorHaptic()
                        
                        // Save as draft
                        try {
                            val draftsDir = File(getExternalFilesDir(null) ?: filesDir, "drafts")
                            if (!draftsDir.exists()) draftsDir.mkdirs()
                            val draftFile = File(draftsDir, file.name)
                            file.renameTo(draftFile)
                            Log.i("VoiceNote", "Saved as draft: ${draftFile.absolutePath}")
                        } catch (e: Exception) {
                            Log.e("VoiceNote", "Failed to save draft", e)
                        }
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceRecordingService", "Error stopping recording", e)
            _isRecording.value = false
            triggerErrorHaptic()
            stopSelf()
        }
    }

    private fun triggerSuccessHaptic() {
        val vibrator = getVibrator()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50, 50, 50), -1))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 50, 50, 50, 50, 50), -1)
        }
    }

    private fun triggerErrorHaptic() {
        val vibrator = getVibrator()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(500)
        }
    }

    private fun triggerHapticFeedback(isStart: Boolean) {
        val vibrator = getVibrator()
        if (isStart) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 70), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 70, 50, 70), -1)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun createNotification(content: String): Notification {
        val stopIntent = Intent(this, VoiceRecordingService::class.java).apply { action = ACTION_STOP_RECORDING }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        
        val discardIntent = Intent(this, VoiceRecordingService::class.java).apply { action = ACTION_DISCARD_RECORDING }
        val discardPendingIntent = PendingIntent.getService(this, 1, discardIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceNote AI Hub")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop & Process", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, "Discard", discardPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Voice Recognition Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(vadRunnable)
        mediaRecorder?.release()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
