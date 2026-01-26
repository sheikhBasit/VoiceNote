package com.example.voicenote.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicenote.ui.theme.Primary

@Composable
fun GlassyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    isSuccess: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Animation for focus glow
    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.15f else 0.05f,
        animationSpec = tween(300),
        label = "glowAlpha"
    )

    // Error shake animation
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error != null) {
            repeat(3) {
                offsetX.animateTo(
                    targetValue = 10f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
                )
                offsetX.animateTo(
                    targetValue = -10f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
                )
            }
            offsetX.animateTo(0f)
        }
    }

    Column(modifier = modifier.offset(x = offsetX.value.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (error != null) Color.Red else if (isFocused) Primary else Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = glowAlpha)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Primary,
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f),
                cursorColor = Primary,
                errorIndicatorColor = Color.Red
            ),
            isError = error != null,
            singleLine = true
        )

        AnimatedVisibility(
            visible = error != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = error ?: "",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
