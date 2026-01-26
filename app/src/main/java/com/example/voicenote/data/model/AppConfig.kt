package com.example.voicenote.data.model

data class AppConfig(
    // Office Hours Logic
    val autoListenEnabled: Boolean = false,
    val workStartHour: Int = 9,  // 24h format
    val workEndHour: Int = 17,
    val workDays: List<Int> = listOf(2, 3, 4, 5, 6), // Mon to Fri (Calendar.MONDAY = 2)

    // API Keys Management
    val apiKeys: List<String> = emptyList(),
    val currentKeyIndex: Int = 0
)