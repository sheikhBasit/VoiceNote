package com.example.voicenote.core.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.voicenote.MainActivity
import com.example.voicenote.data.model.Priority
import com.example.voicenote.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val repository = FirestoreRepository()

    override suspend fun doWork(): Result {
        val tasks = repository.getAllTasks().firstOrNull() ?: return Result.success()
        val now = System.currentTimeMillis()

        tasks.forEach { task ->
            if (task.isDone || task.isDeleted) return@forEach

            task.deadline?.let { deadline ->
                val timeRemaining = deadline - now

                // 1. Proactive Reminder (30 mins before)
                if (timeRemaining in 0..TimeUnit.MINUTES.toMillis(30)) {
                    showNotification(task.description, "Deadline in ${timeRemaining / 60000} minutes!", task.noteId)
                }

                // 2. Auto-Escalation (3 hours before)
                if (timeRemaining in 0..TimeUnit.HOURS.toMillis(3) && task.priority != Priority.HIGH) {
                    repository.updateTask(task.copy(priority = Priority.HIGH))
                }
            }
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String, noteId: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("note_id_to_open", noteId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            noteId.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(noteId.hashCode(), notification)
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "reminder_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
