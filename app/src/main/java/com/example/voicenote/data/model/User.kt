package com.example.voicenote.data.model

enum class UserRole {
    STUDENT,
    TEACHER,
    OFFICE_WORKER,
    DEVELOPER,
    PSYCHIATRIST,
    PSYCHOLOGIST,
    BUSINESS_MAN,
    OTHER,
    GENERIC
}

data class User(
    val id: String = "",
    val token: String = "",
    val name: String = "",
    val email: String = "",
    val deviceId: String = "",
    val deviceModel: String = "",
    val lastLogin: Long = System.currentTimeMillis(),
    val primaryRole: UserRole = UserRole.GENERIC,
    val secondaryRole: UserRole? = null,
    val customRoleDescription: String = "",
    // Floating Button Schedule
    val floatingButtonScheduled: Boolean = false,
    val workStartHour: Int = 9,
    val workEndHour: Int = 17,
    val workDays: List<Int> = listOf(2, 3, 4, 5, 6) // Mon to Fri
)
