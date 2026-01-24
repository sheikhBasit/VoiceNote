package com.example.voicenote.features.meetings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinMeetingScreen(
    onBack: () -> Unit,
    onBotDispatched: () -> Unit
) {
    var meetingUrl by remember { mutableStateOf("") }
    var botName by remember { mutableStateOf("VoiceNote Assistant") }
    var isDispatching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meeting Bot Hub", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.MeetingRoom,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF00E5FF)
            )

            Text(
                "Dispatch an AI Bot",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "Paste your Zoom, Meet, or Teams link below. Our bot will join, record, and summarize the session for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            GlassCard {
                OutlinedTextField(
                    value = meetingUrl,
                    onValueChange = { meetingUrl = it },
                    label = { Text("Meeting URL") },
                    placeholder = { Text("https://zoom.us/j/...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = botName,
                    onValueChange = { botName = it },
                    label = { Text("Bot Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Button(
                onClick = { 
                    isDispatching = true
                    // In production: viewModel.joinMeeting(meetingUrl, botName)
                    // For now, simulate success
                    onBotDispatched()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = meetingUrl.isNotBlank() && !isDispatching,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                if (isDispatching) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text("Dispatch Bot", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
