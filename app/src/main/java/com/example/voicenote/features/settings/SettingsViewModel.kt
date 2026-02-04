package com.example.voicenote.features.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.core.security.SecurityManager
import com.example.voicenote.core.service.OverlayService
import com.example.voicenote.data.remote.UserDTO
import com.example.voicenote.data.repository.VoiceNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: VoiceNoteRepository,
    private val securityManager: SecurityManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _isFloatingEnabled = MutableStateFlow(securityManager.isFloatingButtonEnabled())
    val isFloatingEnabled: StateFlow<Boolean> = _isFloatingEnabled.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            
            // Try loading from cache first
            val cachedUser = securityManager.getUserData()
            if (cachedUser != null) {
                _uiState.value = SettingsUiState.Success(cachedUser)
            }

            // Fetch fresh data
            repository.getUserProfile().collect { result ->
                result.onSuccess { user ->
                    securityManager.saveUserData(user)
                    _uiState.value = SettingsUiState.Success(user)
                }.onFailure { e ->
                    if (_uiState.value is SettingsUiState.Loading) {
                        _uiState.value = SettingsUiState.Error(e.localizedMessage ?: "Failed to load profile")
                    }
                }
            }
        }
    }

    fun isFloatingButtonEnabled(): Boolean = _isFloatingEnabled.value

    fun setFloatingButtonEnabled(enabled: Boolean) {
        securityManager.setFloatingButtonEnabled(enabled)
        _isFloatingEnabled.value = enabled
        
        val intent = Intent(context, OverlayService::class.java)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
    }

    fun updateSettings(updates: Map<String, Any?>) {
        viewModelScope.launch {
            _isUpdating.value = true
            repository.updateUserSettings(updates).collect { result ->
                result.onSuccess { updatedUser ->
                    securityManager.saveUserData(updatedUser)
                    _uiState.value = SettingsUiState.Success(updatedUser)
                }
                _isUpdating.value = false
            }
        }
    }

    fun addJargon(jargon: String) {
        val currentState = _uiState.value as? SettingsUiState.Success ?: return
        val currentJargons = currentState.user.jargons.toMutableList()
        if (!currentJargons.contains(jargon)) {
            currentJargons.add(jargon)
            updateSettings(mapOf("jargons" to currentJargons))
        }
    }

    fun removeJargon(jargon: String) {
        val currentState = _uiState.value as? SettingsUiState.Success ?: return
        val currentJargons = currentState.user.jargons.toMutableList()
        if (currentJargons.remove(jargon)) {
            updateSettings(mapOf("jargons" to currentJargons))
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logout().collect { result ->
                result.onSuccess {
                    securityManager.clearSession()
                    onSuccess()
                }
            }
        }
    }
    
    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccount(hard = true).collect { result ->
                result.onSuccess {
                    securityManager.clearSession()
                    onSuccess()
                }
            }
        }
    }
}

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(val user: UserDTO) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
