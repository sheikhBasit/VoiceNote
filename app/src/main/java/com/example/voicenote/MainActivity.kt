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
import com.example.voicenote.data.model.User
import com.example.voicenote.data.repository.FirestoreRepository
import com.example.voicenote.features.detail.NoteDetailScreen
import com.example.voicenote.features.home.HomeScreen
import com.example.voicenote.features.settings.ApiSettingsScreen
import com.example.voicenote.features.tasks.TasksScreen
import com.example.voicenote.ui.theme.VoiceNoteTheme
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var securityManager: SecurityManager
    private val repository = FirestoreRepository()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        executor = ContextCompat.getMainExecutor(this)
        securityManager = SecurityManager(this)
        
        setContent {
            var isAuthenticated by remember { 
                mutableStateOf(!securityManager.isBiometricEnabled() || securityManager.hasBypassedOnce()) 
            }
            
            val permissionsLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.entries.all { it.value }
                if (allGranted) {
                    checkOverlayPermission()
                } else {
                    Toast.makeText(this, "Permissions required for full functionality", Toast.LENGTH_LONG).show()
                }
            }

            LaunchedEffect(Unit) {
                permissionsLauncher.launch(arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                    "com.android.alarm.permission.SET_ALARM"
                ))
            }

            VoiceNoteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (isAuthenticated) {
                        AppNavigation()
                    } else {
                        AuthScreen { 
                            showBiometricPrompt { 
                                securityManager.setBypassedOnce(true)
                                isAuthenticated = true 
                                syncUser()
                            } 
                        }
                    }
                }
            }
        }
    }

    private fun syncUser() {
        val token = securityManager.getSessionToken() ?: securityManager.generateNewToken()
        val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME) ?: Build.MODEL
        } else {
            Build.MODEL
        }
        
        lifecycleScope.launch {
            repository.saveUser(
                User(
                    token = token,
                    name = deviceName,
                    deviceModel = Build.MODEL,
                    lastLogin = System.currentTimeMillis()
                )
            )
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                startService(Intent(this, OverlayService::class.java))
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

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        val items = listOf("tasks", "notes", "stt_logs", "settings")

        Scaffold(
            bottomBar = {
                NavigationBar {
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
                    TasksScreen(onTaskClick = { noteId -> navController.navigate("detail/$noteId") })
                }
                composable("notes") {
                    HomeScreen(onNoteClick = { note -> navController.navigate("detail/${note.id}") })
                }
                composable("stt_logs") {
                    SttLogsScreen()
                }
                composable("settings") {
                    ApiSettingsScreen()
                }
                composable("detail/{noteId}") { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                    NoteDetailScreen(noteId = noteId, onBack = { navController.popBackStack() })
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
                            Text("voice_recording.mp3", modifier = Modifier.weight(1f))
                            IconButton(onClick = { playAudio(lastFilePath!!) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Whisper Transcription:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (history.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No sessions processed yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { text ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = text,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun playAudio(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}