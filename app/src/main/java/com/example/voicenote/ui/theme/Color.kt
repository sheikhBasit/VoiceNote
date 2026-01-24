package com.example.voicenote.ui.theme

import androidx.compose.ui.graphics.Color

// Obsidian Theme - High Fidelity 2026 Style
val Background = Color(0xFF0A0A0A)
val Surface = Color(0xFF151515)
val Primary = Color(0xFF00E5FF) // Cyber Cyan
val Secondary = Color(0xFF7C4DFF) // Deep Purple
val AccentGlow = Color(0xFF9132FF)

val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color.White
)

@Composable
fun VoiceNoteThemeConsolidated(
    content: @Composable () -> Unit
) {
    // This is a placeholder or should be removed if already in Theme.kt
}
