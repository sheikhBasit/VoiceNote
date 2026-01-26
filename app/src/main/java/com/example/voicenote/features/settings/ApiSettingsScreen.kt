package com.example.voicenote.features.settings

import android.app.Activity
import android.os.Build
import android.provider.Settings
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicenote.core.security.SecurityManager
import com.example.voicenote.data.model.AppConfig
import com.example.voicenote.data.model.User
import com.example.voicenote.data.model.UserRole
import com.example.voicenote.data.repository.FirestoreRepository
import com.example.voicenote.ui.components.GlassyTextField
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ApiSettingsViewModel(private val repository: FirestoreRepository = FirestoreRepository()) : ViewModel() {
    val config: StateFlow<AppConfig?> = repository.getAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun loadUser(deviceId: String) {
        viewModelScope.launch {
            repository.getUserFlow(deviceId).collect {
                _currentUser.value = it
            }
        }
    }

    fun updateUserProfile(
        email: String, 
        primary: UserRole, 
        secondary: UserRole?, 
        custom: String,
        autoEnabled: Boolean, 
        start: Int, 
        end: Int, 
        days: List<Int>
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.saveUser(user.copy(
                email = email,
                primaryRole = primary,
                secondaryRole = secondary,
                customRoleDescription = custom,
                floatingButtonScheduled = autoEnabled,
                workStartHour = start,
                workEndHour = end,
                workDays = days
            ))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApiSettingsScreen(viewModel: ApiSettingsViewModel = viewModel()) {
    val config by viewModel.config.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: Build.SERIAL }
    
    var editEmail by remember { mutableStateOf("") }
    var customRoleDesc by remember { mutableStateOf("") }
    var isEditingEmail by remember { mutableStateOf(false) }

    var localStartHour by remember { mutableFloatStateOf(9f) }
    var localEndHour by remember { mutableFloatStateOf(17f) }
    
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val dayValues = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)

    var hasOverlayPermission by remember { mutableStateOf(true) }
    
    DisposableEffect(Unit) {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        onDispose {}
    }

    LaunchedEffect(deviceId) { viewModel.loadUser(deviceId) }

    LaunchedEffect(user) {
        if (!isEditingEmail) editEmail = user?.email ?: ""
        customRoleDesc = user?.customRoleDescription ?: ""
        user?.let {
            localStartHour = it.workStartHour.toFloat()
            localEndHour = it.workEndHour.toFloat()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Personal Assistant Hub") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            // Section: Identity & Dual Roles
            item {
                Text("Assistant Personalization", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user?.name ?: "Identifying...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Device: ${Build.MODEL}", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text("Primary Role (Mandatory)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserRole.entries.filter { it != UserRole.GENERIC }.forEach { role ->
                                FilterChip(
                                    selected = user?.primaryRole == role,
                                    onClick = { 
                                        viewModel.updateUserProfile(editEmail, role, user?.secondaryRole, customRoleDesc, user?.floatingButtonScheduled ?: false, localStartHour.toInt(), localEndHour.toInt(), user?.workDays ?: emptyList()) 
                                    },
                                    label = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")) }
                                )
                            }
                        }

                        Text("Secondary Role (Optional)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserRole.entries.filter { it != UserRole.GENERIC }.forEach { role ->
                                val isSelected = user?.secondaryRole == role
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        val newSecondary = if (isSelected) null else role
                                        viewModel.updateUserProfile(editEmail, user?.primaryRole ?: UserRole.GENERIC, newSecondary, customRoleDesc, user?.floatingButtonScheduled ?: false, localStartHour.toInt(), localEndHour.toInt(), user?.workDays ?: emptyList()) 
                                    },
                                    label = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")) }
                                )
                            }
                        }

                        if (user?.primaryRole == UserRole.OTHER || user?.secondaryRole == UserRole.OTHER) {
                            GlassyTextField(
                                value = customRoleDesc,
                                onValueChange = { 
                                    if (it.split(" ").size <= 5) {
                                        customRoleDesc = it
                                        viewModel.updateUserProfile(editEmail, user?.primaryRole ?: UserRole.GENERIC, user?.secondaryRole, it, user?.floatingButtonScheduled ?: false, localStartHour.toInt(), localEndHour.toInt(), user?.workDays ?: emptyList()) 
                                    }
                                },
                                label = "Describe your role (max 5 words)",
                                error = if (customRoleDesc.split(" ").size > 5) "Too long" else null
                            )
                        }

                        val isEmailValid = editEmail.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(editEmail).matches()
                        GlassyTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it; isEditingEmail = true },
                            label = "Contact Email",
                            error = if (!isEmailValid) "Invalid email" else null
                        )
                        
                        if (isEditingEmail) {
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { editEmail = user?.email ?: ""; isEditingEmail = false }) { Text("Cancel") }
                                Button(
                                    onClick = {
                                        viewModel.updateUserProfile(editEmail, user?.primaryRole ?: UserRole.GENERIC, user?.secondaryRole, customRoleDesc, user?.floatingButtonScheduled ?: false, localStartHour.toInt(), localEndHour.toInt(), user?.workDays ?: emptyList())
                                        isEditingEmail = false
                                    },
                                    enabled = isEmailValid
                                ) { Text("Save") }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section: Hub Visibility & Schedule
            item {
                Text("Assistant Visibility", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scheduled Visibility", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = user?.floatingButtonScheduled ?: false,
                                onCheckedChange = { 
                                    viewModel.updateUserProfile(user?.email ?: "", user?.primaryRole ?: UserRole.GENERIC, user?.secondaryRole, customRoleDesc, it, localStartHour.toInt(), localEndHour.toInt(), user?.workDays ?: emptyList()) 
                                }
                            )
                        }
                        
                        if (user?.floatingButtonScheduled == true) {
                            Text("Active Days of Week", style = MaterialTheme.typography.labelMedium)
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                dayNames.forEachIndexed { index, name ->
                                    val dayValue = dayValues[index]
                                    val isSelected = user?.workDays?.contains(dayValue) ?: false
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val currentDays = user?.workDays?.toMutableList() ?: mutableListOf()
                                            if (isSelected) currentDays.remove(dayValue) else currentDays.add(dayValue)
                                            viewModel.updateUserProfile(user?.email ?: "", user?.primaryRole ?: UserRole.GENERIC, user?.secondaryRole, customRoleDesc, true, localStartHour.toInt(), localEndHour.toInt(), currentDays)
                                        },
                                        label = { Text(name) }
                                    )
                                }
                            }

                            Text("Active Window: ${localStartHour.toInt()}:00 to ${localEndHour.toInt()}:00", style = MaterialTheme.typography.bodySmall)
                            RangeSlider(
                                value = localStartHour..localEndHour,
                                onValueChange = { range -> localStartHour = range.start; localEndHour = range.endInclusive },
                                onValueChangeFinished = { 
                                    viewModel.updateUserProfile(user?.email ?: "", user?.primaryRole ?: UserRole.GENERIC, user?.secondaryRole, customRoleDesc, true, localStartHour.toInt(), localEndHour.toInt(), user?.workDays ?: emptyList()) 
                                },
                                valueRange = 0f..23f,
                                steps = 23
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section: Security & Logout
            item {
                Text("Privacy & Security", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric Lock", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Authorized fingerprint only", style = MaterialTheme.typography.labelSmall)
                            }
                            var biometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { 
                                    biometricEnabled = it
                                    securityManager.setBiometricEnabled(it)
                                    if (!it) securityManager.setBypassedOnce(false)
                                }
                            )
                        }
                        
                        if (securityManager.hasBypassedOnce()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { 
                                    securityManager.setBypassedOnce(false)
                                    // CLOSE THE APP ON LOGOUT
                                    (context as? Activity)?.finishAffinity()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Logout & Close App")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
