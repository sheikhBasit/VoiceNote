package com.example.voicenote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {

    @Query("SELECT * FROM dashboard_stats WHERE id = 0")
    fun getDashboardStats(): Flow<DashboardStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboardStats(stats: DashboardStatsEntity)

    @Query("SELECT * FROM task_stats WHERE id = 0")
    fun getTaskStats(): Flow<TaskStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskStats(stats: TaskStatsEntity)

    @Query("SELECT * FROM ai_insights WHERE id = 0")
    fun getAIInsights(): Flow<AIInsightsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAIInsights(insights: AIInsightsEntity)
    
    @Query("DELETE FROM dashboard_stats")
    suspend fun clearAll()
}
