package com.example.voicenote.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.voicenote.ui.components.GlassyTextField
import com.example.voicenote.ui.theme.GlassyCard
import com.example.voicenote.ui.theme.GlassyEffects
import com.example.voicenote.ui.theme.Primary
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun PremiumAuthScreen(onAuthenticate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassyEffects.PremiumBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "VoiceNote AI",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your Second Brain, Refined.",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            GlassyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var emailError by remember { mutableStateOf<String?>(null) }
                var passwordError by remember { mutableStateOf<String?>(null) }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Secure Onboarding",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    GlassyTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = if (it.contains("@")) null else "Enter a valid email"
                        },
                        label = "Business Email",
                        error = emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassyTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = if (it.length >= 6) null else "Password too short"
                        },
                        label = "Access Key (Min 6 chars)",
                        error = passwordError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            if (email.contains("@") && password.length >= 6) {
                                onAuthenticate()
                            } else {
                                if (!email.contains("@")) emailError = "Required"
                                if (password.length < 6) passwordError = "Required"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.8f)),
                        shape = CircleShape
                    ) {
                        Text("Unlock Second Brain", fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onAuthenticate) {
                        Text("Bypass with Biometrics (Internal)", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
