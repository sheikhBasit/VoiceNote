package com.example.voicenote

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.voicenote.core.security.SecurityManager
import com.example.voicenote.core.service.OverlayService
import com.example.voicenote.core.service.VoiceRecordingService
import com.example.voicenote.core.workers.ReminderWorker
import com.example.voicenote.data.model.User
import com.example.voicenote.data.repository.FirestoreRepository
import com.example.voicenote.features.detail.NoteDetailScreen
import com.example.voicenote.features.home.HomeScreen
import com.example.voicenote.features.settings.ApiSettingsScreen
import com.example.voicenote.features.tasks.TasksScreen
import com.example.voicenote.features.search.SearchScreen
import com.example.voicenote.ui.theme.VoiceNoteTheme
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var securityManager: SecurityManager
    
    @Inject
    lateinit var repository: VoiceNoteRepository
    private var mediaPlayer: MediaPlayer? = null

    @Inject
    lateinit var webSocketManager: com.example.voicenote.core.network.WebSocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        executor = ContextCompat.getMainExecutor(this)
        securityManager = SecurityManager(this)
        
        // Start Real-time Pulse
        webSocketManager.connect()
        
        ReminderWorker.schedule(this)
        
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: Build.SERIAL

        setContent {
            var isAuthenticated by remember { 
                mutableStateOf(!securityManager.isBiometricEnabled() || securityManager.hasBypassedOnce()) 
            }
            var showOverlayDialog by remember { mutableStateOf(false) }
            
            val permissionsLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.entries.all { it.value }
                if (allGranted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                        showOverlayDialog = true
                    } else {
                        startService(Intent(this, OverlayService::class.java))
                    }
                } else {
                    Toast.makeText(this, "Permissions restricted: To proceed with voice recording and synchronization, please grant the necessary permissions in System Settings.", Toast.LENGTH_LONG).show()
                }
            }

            LaunchedEffect(Unit) {
                val permissions = mutableListOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.CAMERA,
                    "com.android.alarm.permission.SET_ALARM"
                )
                permissionsLauncher.launch(permissions.toTypedArray())
            }

    VoiceNoteTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (isAuthenticated) {
                val navController = rememberNavController()
                com.example.voicenote.ui.navigation.AppNavigation(navController)
                
                // Handle deep-link
                LaunchedEffect(intent.getStringExtra("note_id_to_open")) {
                    intent.getStringExtra("note_id_to_open")?.let {
                        navController.navigate("detail/$it")
                    }
                }
            } else {
                com.example.voicenote.features.auth.PremiumAuthScreen { 
                    showBiometricPrompt { 
                        securityManager.setBypassedOnce(true)
                        isAuthenticated = true 
                        handleUserRegistration(deviceId)
                    } 
                }
            }

                    if (showOverlayDialog) {
                        AlertDialog(
                            onDismissRequest = { showOverlayDialog = false },
                            title = { Text("Display Over Other Apps") },
                            text = { Text("Required for the floating assistant button.") },
                            confirmButton = {
                                Button(onClick = {
                                    showOverlayDialog = false
                                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                                }) { Text("Go to Settings") }
                            },
                            dismissButton = { TextButton(onClick = { showOverlayDialog = false }) { Text("Later") } }
                        )
                    }
                }
            }
        }
    }

    private fun handleUserRegistration(deviceId: String) {
        val token = securityManager.getSessionToken() ?: securityManager.generateNewToken()
        val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME) ?: Build.MODEL
        } else {
            Build.MODEL
        }
        
        lifecycleScope.launch {
            val existingUser = repository.getUserByDeviceId(deviceId)
            if (existingUser == null) {
                repository.saveUser(User(token = token, name = deviceName, deviceId = deviceId, deviceModel = Build.MODEL, lastLogin = System.currentTimeMillis()))
            } else {
                repository.saveUser(existingUser.copy(lastLogin = System.currentTimeMillis()))
            }
        }
    }

    @Composable
    fun AuthScreen(onRetry: () -> Unit) {
        LaunchedEffect(Unit) { onRetry() }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onRetry) { Text("Unlock with Biometrics") }
        }
    }

    // Navigation is now handled by com.example.voicenote.ui.navigation.AppNavigation

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SttLogsScreen() {
        val history by VoiceRecordingService.transcriptionHistory.collectAsState(initial = emptyList())
        val status by VoiceRecordingService.statusLog.collectAsState(initial = "Idle")
        val lastFilePath by VoiceRecordingService.lastRecordedFilePath.collectAsState(initial = null)
        
        Scaffold(
            topBar = { TopAppBar(title = { Text("AI Brain Logs") }) }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                Text("Pipeline Status:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(status, style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (lastFilePath != null) {
                    Text("Last Recorded File:", style = MaterialTheme.typography.labelLarge)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("voice_recording.mp4", modifier = Modifier.weight(1f))
                            IconButton(onClick = { playAudio(lastFilePath!!) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Whisper Transcription:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history) { text ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Text(text = text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }

    private fun playAudio(path: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
                Toast.makeText(this, "Resource unavailable: The requested audio file could not be located on this device.", Toast.LENGTH_SHORT).show()
                return
            }
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("Playback", "Error playing: $path", e)
            Toast.makeText(this, "Playback interrupted: We encountered an unexpected error while playing the audio. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Authenticate to access your notes")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            val cipher = securityManager.getInitializedCipher()
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

// Utility to open other apps
fun openApp(context: android.content.Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        context.startActivity(intent)
    } else {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }
}
