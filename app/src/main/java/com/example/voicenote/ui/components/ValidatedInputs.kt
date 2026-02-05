package com.example.voicenote.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    label: String = "",
    modifier: Modifier = Modifier,
    error: String? = null,
    isSuccess: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onClear: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.15f else 0.05f,
        animationSpec = tween(300),
        label = "glowAlpha"
    )

    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error != null) {
            repeat(3) {
                offsetX.animateTo(10f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                offsetX.animateTo(-10f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
            }
            offsetX.animateTo(0f)
        }
    }

    Column(modifier = modifier.offset(x = offsetX.value.dp)) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (error != null) Color.Red else if (isSuccess) Color(0xFF4ADE80) else if (isFocused) Primary else Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        
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
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (value.isNotEmpty() && onClear != null) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Close, "Clear", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                        }
                    }
                    trailingIcon?.invoke()
                    AnimatedVisibility(
                        visible = isSuccess && error == null,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Success",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = if (isSuccess) Color(0xFF4ADE80) else Primary,
                unfocusedIndicatorColor = if (isSuccess) Color(0xFF4ADE80).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f),
                cursorColor = if (isSuccess) Color(0xFF4ADE80) else Primary,
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
