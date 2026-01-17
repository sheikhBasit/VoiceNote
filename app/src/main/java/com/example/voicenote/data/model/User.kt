package com.example.voicenote.data.model

data class User(
    val token: String = "",
    val name: String = "",
    val deviceModel: String = "",
    val lastLogin: Long = System.currentTimeMillis()
)