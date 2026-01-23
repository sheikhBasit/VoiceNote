package com.example.voicenote.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.*
import android.provider.CalendarContract
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.voicenote.MainActivity
import com.example.voicenote.core.utils.CalendarManager
import com.example.voicenote.data.model.*
import com.example.voicenote.data.repository.AiRepository
import com.example.voicenote.data.repository.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class VoiceRecordingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val repository = FirestoreRepository()
    private val aiRepository = AiRepository(repository)
    private lateinit var calendarManager: CalendarManager

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isPausedBySilence = false
    private val handler = Handler(Looper.getMainLooper())
    private var currentMeetingTitle: String? = null

    private val SILENCE_THRESHOLD = 800 
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
                            _statusLog.value = "Sleeping (Silence detected)..."
                        } catch (e: Exception) { Log.e("VAD", "Pause failed") }
                    }
                } else if (amplitude >= SILENCE_THRESHOLD && isPausedBySilence) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            mediaRecorder?.resume()
                            isPausedBySilence = false
                            _statusLog.value = "Recording Good Audio..."
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
        const val ACTION_DISCARD_RECORDING = "DISCARD_RECORDING"
        
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
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        calendarManager = CalendarManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RECORDING -> stopRecordingAndProcess()
            ACTION_DISCARD_RECORDING -> discardRecording()
            else -> startRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (_isRecording.value) return

        try {
            currentMeetingTitle = getCurrentCalendarEvent()
            
            // Move recording to a persistent location instead of cache if we want to play it back later locally
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
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            enableAudioEffects()
            _isRecording.value = true
            _statusLog.value = if (currentMeetingTitle != null) "Recording: $currentMeetingTitle" else "Recording Good Audio..."
            isPausedBySilence = false
            handler.postDelayed(vadRunnable, VAD_CHECK_INTERVAL)
            triggerHapticFeedback(true)
            startForeground(NOTIFICATION_ID, createNotification("Capturing: ${currentMeetingTitle ?: "Meeting Voice"}"))
        } catch (e: Exception) {
            Log.e("VoiceRecordingService", "Failed to start recording", e)
            _statusLog.value = "Error starting recording"
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
        _statusLog.value = "Discarded."
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
                if (isPausedBySilence && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    resume()
                }
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            triggerHapticFeedback(false)
            _statusLog.value = "Pre-processing Audio..."
            _lastRecordedFilePath.value = audioFile?.absolutePath

            serviceScope.launch(Dispatchers.IO) {
                val file = audioFile ?: return@launch
                
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: Build.SERIAL
                val user = repository.getUserByDeviceId(deviceId)
                val role = user?.primaryRole ?: UserRole.GENERIC

                val chunks = splitAudioByThreshold(file, 20 * 1024 * 1024)
                val transcriptions = mutableListOf<String>()

                chunks.forEachIndexed { index, chunk ->
                    _statusLog.value = "Whisper Part ${index + 1}/${chunks.size}..."
                    val text = aiRepository.transcribeAudio(chunk)
                    if (!text.isNullOrBlank()) {
                        transcriptions.add(text)
                        val currentList = _transcriptionHistory.value.toMutableList()
                        currentList.add(0, text)
                        _transcriptionHistory.value = currentList.take(50)
                    }
                }

                val totalTranscription = transcriptions.joinToString(" ").trim()
                if (totalTranscription.length > 15) { 
                    _statusLog.value = "AI Brain: Analyzing as ${role.name}..."
                    
                    val contextTranscription = if (currentMeetingTitle != null) {
                        "CONTEXT: This is a recording of the meeting titled '$currentMeetingTitle'. \n\n$totalTranscription"
                    } else totalTranscription

                    val aiOutput = aiRepository.processConversationChunks(transcriptions, role)
                    
                    if (aiOutput != null) {
                        val noteId = UUID.randomUUID().toString()
                        val audioUrl = repository.uploadAudio(deviceId, file)
                        
                        val taskPriorities = aiOutput.tasks.map { 
                            try { Priority.valueOf(it.priority.uppercase()) } catch (e: Exception) { Priority.MEDIUM }
                        }
                        val highestTaskPriority = taskPriorities.minByOrNull { it.ordinal } ?: Priority.LOW
                        val llmNotePriority = try { Priority.valueOf(aiOutput.priority.uppercase()) } catch (e: Exception) { Priority.MEDIUM }
                        val finalNotePriority = if (highestTaskPriority.ordinal < llmNotePriority.ordinal) highestTaskPriority else llmNotePriority

                        repository.saveNote(Note(
                            id = noteId,
                            userId = deviceId,
                            title = aiOutput.title,
                            summary = aiOutput.summary,
                            transcript = aiOutput.transcript,
                            audioUrl = audioUrl ?: file.absolutePath, // Keep local path if upload fails
                            priority = finalNotePriority,
                            timestamp = System.currentTimeMillis()
                        ))

                        aiOutput.tasks.forEach { aiTask ->
                            val deadlineMillis = parseDate(aiTask.deadline)
                            repository.addTask(Task(
                                noteId = noteId,
                                description = aiTask.description,
                                priority = try { Priority.valueOf(aiTask.priority.uppercase()) } catch (e: Exception) { Priority.MEDIUM },
                                deadline = deadlineMillis,
                                googlePrompt = aiTask.googlePrompt,
                                aiPrompt = aiTask.aiPrompt,
                                createdAt = System.currentTimeMillis()
                            ))

                            if (deadlineMillis != null && deadlineMillis > System.currentTimeMillis()) {
                                calendarManager.addEventToCalendar("AI Task: ${aiTask.description}", "From: ${aiOutput.title}", deadlineMillis)
                                val cal = Calendar.getInstance().apply { timeInMillis = deadlineMillis }
                                calendarManager.setAlarm("AI Reminder: ${aiTask.description}", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                            }
                        }
                        _statusLog.value = "Processed Successfully."
                        triggerSuccessHaptic()
                    }
                } else {
                    _statusLog.value = "Filtered: Too short."
                    triggerErrorHaptic()
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
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

    private fun splitAudioByThreshold(file: File, thresholdBytes: Long): List<File> {
        if (file.length() <= thresholdBytes) return listOf(file)
        val chunks = mutableListOf<File>()
        val buffer = ByteArray(thresholdBytes.toInt())
        val fis = FileInputStream(file)
        var bytesRead: Int
        var count = 0
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            val chunkFile = File(cacheDir, "chunk_${count++}_${file.name}")
            val fos = FileOutputStream(chunkFile)
            fos.write(buffer, 0, bytesRead)
            fos.close()
            chunks.add(chunkFile)
        }
        fis.close()
        return chunks
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(dateStr)?.time
        } catch (e: Exception) {
            null
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
