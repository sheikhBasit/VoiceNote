package com.example.voicenote.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.voicenote.data.remote.TopicHeatmapItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "dashboard_stats")
data class DashboardStatsEntity(
    @PrimaryKey val id: Int = 0,
    val taskVelocity: Float,
    val completedTasks: Int,
    val totalTasks: Int,
    val topicsJson: String, // Stored as JSON
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_stats")
data class TaskStatsEntity(
    @PrimaryKey val id: Int = 0,
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val highPriority: Int,
    val mediumPriority: Int,
    val lowPriority: Int,
    val overdue: Int,
    val dueToday: Int,
    val completionRate: Float
)

@Entity(tableName = "ai_insights")
data class AIInsightsEntity(
    @PrimaryKey val id: Int = 0,
    val highPriorityPending: Int,
    val totalActiveNotes: Int,
    val suggestion: String?
)

class DashboardConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromTopicList(value: List<TopicHeatmapItem>): String = gson.toJson(value)

    @TypeConverter
    fun toTopicList(value: String): List<TopicHeatmapItem> {
        val listType = object : TypeToken<List<TopicHeatmapItem>>() {}.type
        return gson.fromJson(value, listType)
    }
}
