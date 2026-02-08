package com.example.voicenote.data.model

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
data class User(
    val id: String = "",
    val token: String = "",
    val name: String = "",
    val email: String = "",
    val profilePictureUrl: String? = null,
    val deviceId: String = "",
    val deviceModel: String = "",
    val lastLogin: Long = System.currentTimeMillis(),
    val primaryRole: UserRole = UserRole.GENERIC,
    val secondaryRole: UserRole? = null,
    val customRoleDescription: String = "",
    val selectedOrgId: String? = null, // B2B: Current active organization
    val availableOrgs: List<String> = emptyList(), // B2B: Orgs user belongs to
    val balance: Double = 0.0,
    // Floating Button Schedule
    val floatingButtonScheduled: Boolean = false,
    val workStartHour: Int = 9,
    val workEndHour: Int = 17,
    val workDays: List<Int> = listOf(2, 3, 4, 5, 6) // Mon to Fri
)
