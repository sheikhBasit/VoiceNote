package com.example.voicenote

import android.Manifest
import android.content.Context
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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.voicenote.core.security.SecurityManager
import com.example.voicenote.core.service.VoiceRecordingService
import com.example.voicenote.core.service.OverlayService
import com.example.voicenote.core.workers.ReminderWorker
import com.example.voicenote.data.remote.SyncRequest
import com.example.voicenote.data.repository.VoiceNoteRepository
import com.example.voicenote.features.dashboard.DashboardScreen
import com.example.voicenote.features.home.HomeScreen
import com.example.voicenote.features.settings.ApiSettingsScreen
import com.example.voicenote.features.tasks.TasksScreen
import com.example.voicenote.features.search.SearchScreen
import com.example.voicenote.features.billing.UsageBillingScreen
import com.example.voicenote.ui.components.GlassyTextField
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        executor = ContextCompat.getMainExecutor(this)
        
        ReminderWorker.schedule(this)
        
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: Build.SERIAL

        setContent {
            var authState by remember { 
                mutableStateOf(if (securityManager.getUserEmail() != null && (!securityManager.isBiometricEnabled() || securityManager.hasBypassedOnce())) 
                    AuthState.Authenticated else if (securityManager.getUserEmail() != null) AuthState.BiometricRequired else AuthState.LoginRequired) 
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

            LaunchedEffect(authState) {
                if (authState == AuthState.Authenticated) {
                    startFloatingHubIfEnabled()
                    checkAndRequestBatteryOptimization()
                }
            }

            VoiceNoteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = InsightsBackgroundDark) {
                    if (authState == AuthState.Authenticated) {
                        val navToNoteId = intent.getStringExtra("note_id_to_open")
                        MainAppContent(navToNoteId)
                    } else {
                        UnifiedAuthScreen(
                            deviceId = deviceId,
                            initialState = authState,
                            onAuthenticated = { authState = AuthState.Authenticated }
                        )
                    }

                    if (showOverlayDialog) {
                        AlertDialog(
                            onDismissRequest = { showOverlayDialog = false },
                            title = { Text("Display Over Other Apps") },
                            text = { Text("Required for the floating assistant button.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showOverlayDialog = false
                                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = InsightsPrimary)
                                ) { Text("Go to Settings") }
                            },
                            dismissButton = { 
                                TextButton(onClick = { showOverlayDialog = false }) { 
                                    Text("Later", color = Gray400) 
                                } 
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback for some devices
                    intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    startActivity(intent)
                }
            }
        }
    }

    private fun startFloatingHubIfEnabled() {
        if (securityManager.isFloatingButtonEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayService::class.java)
                startService(intent)
            }
        }
    }

    enum class AuthState { LoginRequired, BiometricRequired, Authenticated }

    @Composable
    fun UnifiedAuthScreen(
        deviceId: String,
        initialState: AuthState,
        onAuthenticated: () -> Unit
    ) {
        var currentEmail by remember { mutableStateOf(securityManager.getUserEmail() ?: "") }
        var isSyncing by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val canUseBiometric = remember {
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        }
        
        var isBiometricMode by remember { 
            mutableStateOf(initialState == AuthState.BiometricRequired && canUseBiometric) 
        }
        
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val scrollState = rememberScrollState()
        val isValidEmail = Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()

        val performSync: () -> Unit = {
            if (!isValidEmail) {
                error = "Please enter a valid email"
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                focusManager.clearFocus()
                isSyncing = true
                
                val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) ?: Build.MODEL
                } else Build.MODEL

                lifecycleScope.launch {
                    val request = SyncRequest(
                        name = deviceName,
                        email = currentEmail,
                        token = securityManager.getSessionToken() ?: securityManager.generateNewToken(),
                        deviceId = deviceId,
                        deviceModel = Build.MODEL,
                        primaryRole = "GENERIC"
                    )
                    repository.syncUser(request).collect { result ->
                        isSyncing = false
                        result.onSuccess {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            securityManager.saveSessionToken(it.accessToken)
                            securityManager.saveUserEmail(currentEmail)
                            onAuthenticated()
                        }.onFailure {
                            error = it.message ?: "Sync failed. Try again."
                        }
                    }
                }
            }
        }

        LaunchedEffect(isBiometricMode) {
            if (isBiometricMode) {
                showBiometricPrompt { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    securityManager.setBypassedOnce(true)
                    onAuthenticated() 
                }
            }
        }

        OnboardingBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val transition = rememberInfiniteTransition(label = "logoPulse")
                val logoScale by transition.animateFloat(
                    initialValue = 1f, targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "logoScale"
                )
                
                Crossfade(targetState = isBiometricMode, label = "iconFade") { isBio ->
                    Icon(
                        imageVector = if (isBio) Icons.Default.Fingerprint else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).scale(logoScale),
                        tint = InsightsPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if (isBiometricMode) "Secure Unlock" else "Sync Intelligence", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = if (isBiometricMode) "Verify your identity to access your AI notes." 
                           else "Connect your account to synchronize your session data.", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = Gray400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp)
                )
                
                Spacer(modifier = Modifier.height(48.dp))

                AnimatedContent(targetState = isBiometricMode, label = "formContent") { isBiometric ->
                    if (isBiometric) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GlassCard(intensity = 0.5f) {
                                Text(
                                    text = securityManager.getUserEmail() ?: "User",
                                    color = InsightsPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { 
                                    showBiometricPrompt { 
                                        securityManager.setBypassedOnce(true)
                                        onAuthenticated() 
                                    } 
                                },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = InsightsPrimary)
                            ) {
                                Text("Continue with Biometrics", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            TextButton(
                                onClick = { isBiometricMode = false },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Switch Account", color = InsightsPrimary.copy(alpha = 0.6f))
                            }
                        }
                    } else {
                        Column {
                            GlassyTextField(
                                value = currentEmail,
                                onValueChange = { 
                                    currentEmail = it
                                    error = null 
                                    if (Patterns.EMAIL_ADDRESS.matcher(it).matches()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                label = "Email Address",
                                error = error,
                                isSuccess = isValidEmail,
                                onClear = { currentEmail = "" },
                                modifier = Modifier.focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { performSync() })
                            )
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Button(
                                onClick = performSync,
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                enabled = !isSyncing && currentEmail.isNotEmpty(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = InsightsPrimary)
                            ) {
                                if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                else Text("Sync & Connect", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = Gray400.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Encrypted Sync • Secure Session", style = MaterialTheme.typography.labelSmall, color = Gray400.copy(alpha = 0.5f))
                    }
                    Text("ID: ${deviceId.take(12).uppercase()}", style = MaterialTheme.typography.labelSmall, color = Gray400.copy(alpha = 0.2f))
                }
            }
        }
    }

    @Composable
    private fun OnboardingBackground(content: @Composable () -> Unit) {
        val infiniteTransition = rememberInfiniteTransition(label = "bgShift")
        val shiftX by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 500f,
            animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), label = "shiftX"
        )
        val shiftY by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 500f,
            animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "shiftY"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.2f to InsightsPrimary.copy(alpha = 0.15f),
                            1f to Color.Transparent,
                            center = Offset(shiftX, shiftY),
                            radius = 2000f
                        )
                    )
                    .background(
                        Brush.radialGradient(
                            0.15f to InsightsAccentViolet.copy(alpha = 0.1f),
                            1f to Color.Transparent,
                            center = Offset(2000f - shiftX, 3000f - shiftY),
                            radius = 2000f
                        )
                    )
            )
            content()
        }
    }

    @Composable
    fun MainAppContent(initialNoteId: String?) {
        com.example.voicenote.ui.navigation.AppNavigation(initialNoteId = initialNoteId)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SttLogsScreen(onBack: () -> Unit) {
        val history by VoiceRecordingService.transcriptionHistory.collectAsState(initial = emptyList())
        val status by VoiceRecordingService.statusLog.collectAsState(initial = "Idle")
        val lastFilePath by VoiceRecordingService.lastRecordedFilePath.collectAsState(initial = null)
        
        Box(modifier = Modifier.fillMaxSize().background(InsightsBackgroundDark)) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(InsightsGlassWhite).border(1.dp, InsightsGlassBorder, CircleShape).clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
                    }
                    Text("AI Brain Logs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(40.dp))
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        GlassCard(color = InsightsGlassWhite.copy(alpha=0.03f), borderColor = InsightsGlassBorder) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Pipeline Status", color = InsightsPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(status, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    
                    if (lastFilePath != null) {
                        item {
                            Text("Last Recorded Session", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                            AudioLogPlayer(lastFilePath!!)
                        }
                    }

                    item {
                        Text("Live Transcription History", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    if (history.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No transcription logs yet.", color = Gray400, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(history) { text ->
                            GlassCard(color = InsightsGlassWhite.copy(alpha=0.02f), borderColor = InsightsGlassBorder) {
                                Text(
                                    text = text, 
                                    modifier = Modifier.padding(16.dp), 
                                    color = Color.White.copy(alpha=0.8f),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AudioLogPlayer(path: String) {
        val context = LocalContext.current
        var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
        var isPlaying by remember { mutableStateOf(false) }
        var currentPos by remember { mutableIntStateOf(0) }
        var duration by remember { mutableIntStateOf(0) }

        LaunchedEffect(path) {
            mediaPlayer?.release()
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    duration = this.duration
                }
            } catch (e: Exception) {
                Log.e("AudioLog", "Error loading file", e)
            }
        }

        DisposableEffect(Unit) {
            onDispose { mediaPlayer?.release() }
        }

        LaunchedEffect(isPlaying) {
            while (isPlaying) {
                currentPos = mediaPlayer?.currentPosition ?: 0
                delay(200)
            }
        }

        GlassCard(color = InsightsPrimary.copy(alpha=0.05f), borderColor = InsightsPrimary.copy(alpha=0.2f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier.size(48.dp).background(InsightsPrimary, CircleShape).clickable { 
                            if (isPlaying) mediaPlayer?.pause() else mediaPlayer?.start()
                            isPlaying = !isPlaying
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if(isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text("voice_recording.mp4", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = currentPos.toFloat(),
                            onValueChange = { 
                                currentPos = it.toInt()
                                mediaPlayer?.seekTo(it.toInt())
                            },
                            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(thumbColor = InsightsPrimary, activeTrackColor = InsightsPrimary)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatMillis(currentPos), color = Gray400, fontSize = 10.sp)
                            Text(formatMillis(duration), color = Gray400, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    private fun formatMillis(ms: Int): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
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
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun UnifiedAuthScreenPreview() {
    VoiceNoteTheme {
        MainActivity().UnifiedAuthScreen(
            deviceId = "8a2f-b3c4-9d1e",
            initialState = MainActivity.AuthState.LoginRequired,
            onAuthenticated = { }
        )
    }
}
