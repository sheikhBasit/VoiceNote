
package com.example.voicenote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EnterpriseDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00B0FF),       // Neon Blue (Active)
    secondary = Color(0xFF7C4DFF),     // Deep Purple (AI Actions)
    surface = Color(0xFF121212),       // Dashboard Background
    onSurface = Color(0xFFE0E0E0),     // Transcript Text
    error = Color(0xFFFF5252)          // Conflict Warnings
)

@Composable
fun VoiceNoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EnterpriseDarkColorScheme,
        typography = Typography, // Custom enterprise font configuration
        content = content
    )
}
