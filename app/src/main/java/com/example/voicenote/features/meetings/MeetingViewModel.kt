package com.example.voicenote.features.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.repository.VoiceNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val repository: VoiceNoteRepository
) : ViewModel() {

    private val _isDispatching = MutableStateFlow(false)
    val isDispatching: StateFlow<Boolean> = _isDispatching.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun joinMeeting(meetingUrl: String, botName: String, onComplete: () -> Unit) {
        if (meetingUrl.isBlank()) return
        
        viewModelScope.launch {
            _isDispatching.value = true
            _error.value = null
            
            repository.joinMeeting(meetingUrl, botName).collect { result ->
                _isDispatching.value = false
                result.onSuccess {
                    onComplete()
                }.onFailure { e ->
                    _error.value = e.localizedMessage ?: "Failed to dispatch bot"
                }
            }
        }
    }
}
