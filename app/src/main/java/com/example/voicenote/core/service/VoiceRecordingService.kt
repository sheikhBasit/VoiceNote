package com.example.voicenote.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.*
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

    companion object {
        const val CHANNEL_ID = "VoiceRecordingChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        
        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording
        
        private val _statusLog = MutableStateFlow<String>("Idle")
        val statusLog: StateFlow<String> = _statusLog

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
        if (intent?.action == ACTION_STOP_RECORDING) {
            stopRecordingAndProcess()
            return START_NOT_STICKY
        }

        startRecording()
        return START_STICKY
    }

    private fun startRecording() {
        if (_isRecording.value) return

        try {
            audioFile = File(cacheDir, "recording_${System.currentTimeMillis()}.mp4")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            enableAudioEffects()
            _isRecording.value = true
            _statusLog.value = "Recording Good Audio..."
            triggerHapticFeedback(true)
            startForeground(NOTIFICATION_ID, createNotification("Capturing high-quality voice..."))
        } catch (e: Exception) {
            Log.e("VoiceRecordingService", "Failed to start recording", e)
            _statusLog.value = "Error starting recording"
            stopSelf()
        }
    }

    private fun triggerHapticFeedback(isStart: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (isStart) {
                VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 70), -1)
            } else {
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            if (isStart) {
                vibrator.vibrate(longArrayOf(0, 70, 50, 70), -1)
            } else {
                vibrator.vibrate(200)
            }
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

    private fun stopRecordingAndProcess() {
        if (!_isRecording.value) return

        try {
            mediaRecorder?.apply {
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
                val chunks = splitAudioByThreshold(file, 20 * 1024 * 1024)
                val transcriptions = mutableListOf<String>()

                chunks.forEachIndexed { index, chunk ->
                    _statusLog.value = "Whisper Transcribing Part ${index + 1}/${chunks.size}..."
                    val text = aiRepository.transcribeAudio(chunk)
                    if (!text.isNullOrBlank()) {
                        transcriptions.add(text)
                        val currentList = _transcriptionHistory.value.toMutableList()
                        currentList.add(0, text)
                        _transcriptionHistory.value = currentList.take(50)
                    }
                }

                if (transcriptions.isNotEmpty()) {
                    _statusLog.value = "AI Brain: Extracting English Tasks..."
                    val aiOutput = aiRepository.processConversationChunks(transcriptions)
                    
                    if (aiOutput != null) {
                        val noteId = UUID.randomUUID().toString()
                        
                        // Derive Note Priority from Tasks
                        val taskPriorities = aiOutput.tasks.map { 
                            try { Priority.valueOf(it.priority.uppercase()) } catch (e: Exception) { Priority.MEDIUM }
                        }
                        val highestTaskPriority = taskPriorities.minByOrNull { it.ordinal } ?: Priority.LOW
                        val llmNotePriority = try { Priority.valueOf(aiOutput.priority.uppercase()) } catch (e: Exception) { Priority.MEDIUM }
                        val finalNotePriority = if (highestTaskPriority.ordinal < llmNotePriority.ordinal) highestTaskPriority else llmNotePriority

                        // FIX: Ensure 'transcript' field from aiOutput is saved!
                        repository.saveNote(Note(
                            id = noteId,
                            title = aiOutput.title,
                            summary = aiOutput.summary,
                            transcript = aiOutput.transcript, // This was missing in the previous save call
                            priority = finalNotePriority,
                            timestamp = System.currentTimeMillis()
                        ))

                        aiOutput.tasks.forEach { aiTask ->
                            val deadlineMillis = parseDate(aiTask.deadline)
                            val taskPriority = try { Priority.valueOf(aiTask.priority.uppercase()) } catch (e: Exception) { Priority.MEDIUM }
                            
                            val task = Task(
                                noteId = noteId,
                                description = aiTask.description,
                                priority = taskPriority,
                                deadline = deadlineMillis,
                                googlePrompt = aiTask.googlePrompt,
                                aiPrompt = aiTask.aiPrompt,
                                createdAt = System.currentTimeMillis()
                            )
                            repository.addTask(task)

                            if (deadlineMillis != null && deadlineMillis > System.currentTimeMillis()) {
                                calendarManager.addEventToCalendar(
                                    title = "AI Task: ${aiTask.description}",
                                    description = "Extracted from: ${aiOutput.title}",
                                    startTimeMillis = deadlineMillis
                                )
                                val calendar = Calendar.getInstance().apply { timeInMillis = deadlineMillis }
                                calendarManager.setAlarm(
                                    message = "Task Deadline: ${aiTask.description}",
                                    hour = calendar.get(Calendar.HOUR_OF_DAY),
                                    minutes = calendar.get(Calendar.MINUTE)
                                )
                            }
                        }
                        _statusLog.value = "Process complete. Tasks synced."
                    }
                }
                chunks.forEach { if (it != file) it.delete() }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecordingService", "Error stopping recording", e)
            _isRecording.value = false
            stopSelf()
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
        val stopIntent = Intent(this, VoiceRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceNote AI Brain")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop & Process", stopPendingIntent)
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
        mediaRecorder?.release()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
