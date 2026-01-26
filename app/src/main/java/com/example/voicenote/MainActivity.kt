package com.example.voicenote

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.example.voicenote.data.remote.SyncRequest
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.features.detail.NoteDetailScreen
import com.example.voicenote.features.home.HomeScreen
import com.example.voicenote.features.settings.ApiSettingsScreen
import com.example.voicenote.features.tasks.TasksScreen
import com.example.voicenote.features.search.SearchScreen
import com.example.voicenote.ui.components.GlassyTextField
import com.example.voicenote.ui.theme.VoiceNoteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    
    @Inject
    lateinit var securityManager: SecurityManager
    
    @Inject
    lateinit var repository: VoiceNoteRepository
    
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        executor = ContextCompat.getMainExecutor(this)
        
        ReminderWorker.schedule(this)
        
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: Build.SERIAL

        setContent {
            var authState by remember { 
                mutableStateOf(if (securityManager.getUserEmail() != null && (!securityManager.isBiometricEnabled() || securityManager.hasBypassedOnce())) 
                    AuthState.Authenticated else AuthState.LoginRequired) 
            }
            
            var showOverlayDialog by remember { mutableStateOf(false) }
            
            val permissionsLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.entries.all { it.value }
                if (allGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    showOverlayDialog = true
                }
            }

            LaunchedEffect(Unit) {
                permissionsLauncher.launch(arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.CAMERA
                ))
            }

            VoiceNoteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (authState) {
                        AuthState.Authenticated -> {
                            val navToNoteId = intent.getStringExtra("note_id_to_open")
                            AppNavigation(navToNoteId)
                        }
                        AuthState.LoginRequired -> {
                            LoginScreen(deviceId) { email ->
                                securityManager.saveUserEmail(email)
                                authState = if (securityManager.isBiometricEnabled()) AuthState.BiometricRequired else AuthState.Authenticated
                            }
                        }
                        AuthState.BiometricRequired -> {
                            BiometricAuthScreen { 
                                showBiometricPrompt { 
                                    securityManager.setBypassedOnce(true)
                                    authState = AuthState.Authenticated 
                                } 
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

    enum class AuthState { LoginRequired, BiometricRequired, Authenticated }

    @Composable
    fun LoginScreen(deviceId: String, onLoginSuccess: (String) -> Unit) {
        var email by remember { mutableStateOf("") }
        var isSyncing by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to VoiceNote", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Identify yourself to sync your AI Brain", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GlassyTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = "Work/Personal Email",
                error = error,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        error = "Please enter a valid email"
                        return@Button
                    }
                    isSyncing = true
                    val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME) ?: Build.MODEL
                    } else Build.MODEL

                    lifecycleScope.launch {
                        val request = SyncRequest(
                            name = deviceName,
                            email = email,
                            token = securityManager.getSessionToken() ?: securityManager.generateNewToken(),
                            deviceId = deviceId,
                            deviceModel = Build.MODEL,
                            primaryRole = "GENERIC"
                        )
                        repository.syncUser(request).collect { result ->
                            isSyncing = false
                            result.onSuccess {
                                securityManager.saveSessionToken(it.accessToken)
                                onLoginSuccess(email)
                            }.onFailure {
                                error = it.message ?: "Sync failed. New devices must use an authorized email."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSyncing && email.isNotEmpty(),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Connect Device")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Hardware ID: $deviceId", style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.5f))
        }
    }

    @Composable
    fun BiometricAuthScreen(onRetry: () -> Unit) {
        LaunchedEffect(Unit) { onRetry() }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Unlock AI Brain") }
            }
        }
    }

    @Composable
    fun AppNavigation(initialNoteId: String? = null) {
        val navController = rememberNavController()
        val items = listOf("tasks", "notes", "stt_logs", "settings")

        LaunchedEffect(initialNoteId) {
            initialNoteId?.let { navController.navigate("detail/$it") }
        }

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    when(screen) {
                                        "notes" -> Icons.Default.Description
                                        "tasks" -> Icons.Default.Checklist
                                        "stt_logs" -> Icons.Default.Mic
                                        else -> Icons.Default.Settings
                                    },
                                    contentDescription = screen
                                )
                            },
                            label = { Text(screen.replaceFirstChar { it.uppercase() }) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen } == true,
                            onClick = {
                                navController.navigate(screen) {
                                    popUpTo("tasks") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = "tasks", Modifier.padding(innerPadding)) {
                composable("tasks") { 
                    TasksScreen(
                        onTaskClick = { noteId -> navController.navigate("detail/$noteId") },
                        onSearchClick = { navController.navigate("search") }
                    ) 
                }
                composable("notes") { 
                    HomeScreen(
                        onNoteClick = { note -> navController.navigate("detail/${note.id}") },
                        onSearchClick = { navController.navigate("search") },
                        onBillingClick = { navController.navigate("billing") },
                        onJoinMeetingClick = { navController.navigate("join_meeting") }
                    ) 
                }
                composable("stt_logs") { SttLogsScreen() }
                composable("settings") { ApiSettingsScreen() }
                composable("search") { SearchScreen(onDismiss = { navController.popBackStack() }) }
                composable("billing") { com.example.voicenote.features.billing.BillingScreen(onBack = { navController.popBackStack() }) }
                composable("join_meeting") { 
                    com.example.voicenote.features.meetings.JoinMeetingScreen(
                        onBack = { navController.popBackStack() },
                        onBotDispatched = { navController.popBackStack() }
                    ) 
                }
                composable("detail/{noteId}") { 
                    NoteDetailScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }

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
                Toast.makeText(this, "File not found at: $path", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
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
