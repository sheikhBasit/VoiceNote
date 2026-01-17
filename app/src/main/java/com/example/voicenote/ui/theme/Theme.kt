package com.example.voicenote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onPrimary = OnPrimary,
    error = Error
)

@Composable
fun VoiceNoteTheme(
    content: @Composable () -> Unit
) {
    // Force Dark Mode Only as per requirements
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}