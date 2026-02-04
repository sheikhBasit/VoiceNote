package com.example.voicenote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DashboardStatsEntity::class,
        TaskStatsEntity::class,
        AIInsightsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DashboardConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dashboardDao(): DashboardDao
}
