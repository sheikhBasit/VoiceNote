package com.example.voicenote.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.05f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        )
    )
}

@Composable
fun ShimmerCard(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .shimmerEffect()
    )
}

@Composable
fun ShimmerNoteItem() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .shimmerEffect()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.width(140.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.1f)))
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
            }
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.1f)))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(80.dp).height(18.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
                Box(modifier = Modifier.width(60.dp).height(18.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
            }
        }
    }
}

@Composable
fun NoteDetailShimmer() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ShimmerCard(height = 200.dp) // Summary area
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).shimmerEffect())
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).shimmerEffect())
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).shimmerEffect())
        }
        repeat(3) {
            ShimmerCard(height = 100.dp) // Tasks/Rows
        }
    }
}
