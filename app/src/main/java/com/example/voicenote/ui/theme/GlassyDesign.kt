package com.example.voicenote.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Premium 3D Glassy Design System
 */

@Composable
fun GlassyCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.03f),
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            content = content
        )
    }
}

object GlassyEffects {
    val PremiumBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0A0A),
            Color(0xFF1A1A1A),
            Color(0xFF0D0D0D)
        )
    )
    
    val CyberGlow = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00E5FF).copy(alpha = 0.3f),
            Color(0xFF7C4DFF).copy(alpha = 0.3f)
        )
    )

    val BackgroundMesh = Brush.radialGradient(
        colors = listOf(
            Color(0xFF4b2bee).copy(alpha = 0.15f),
            Color.Transparent
        )
    )
}
