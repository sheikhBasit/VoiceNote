package com.example.voicenote.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.voicenote.core.service.VoiceRecordingService

@Composable
fun RecordingButton(isRecording: Boolean, onClick: () -> Unit) {
    val amplitude by VoiceRecordingService.amplitude.collectAsState()
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isRecording) (amplitude / 32767f).coerceIn(0f, 1f) else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "amplitude"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )

    Box(contentAlignment = Alignment.Center) {
        if (isRecording) {
            // Layer 1: Base Flow
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseScale + (animatedAmplitude * 0.4f))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Layer 2: Rapid Ripple (Sync with Amplitude)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(1f + (animatedAmplitude * 1.2f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.5f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // Layer 3: Outer Glow
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(pulseScale * 1.1f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = if (isRecording) Color(0xFFFF3D00) else MaterialTheme.colorScheme.primary,
            contentColor = if (isRecording) Color.White else Color.Black,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop" else "Voice Note",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
