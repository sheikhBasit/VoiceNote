package com.example.voicenote.features.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicenote.data.remote.WalletDTO
import com.example.voicenote.data.repository.VoiceNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val repository: VoiceNoteRepository
) : ViewModel() {

    private val _walletState = MutableStateFlow<Result<WalletDTO>?>(null)
    val walletState: StateFlow<Result<WalletDTO>?> = _walletState.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _checkoutUrl = MutableStateFlow<String?>(null)
    val checkoutUrl: StateFlow<String?> = _checkoutUrl.asStateFlow()

    init {
        refreshWallet()
    }

    fun refreshWallet() {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.getWallet().collect { result ->
                _walletState.value = result
                _isProcessing.value = false
            }
        }
    }

    fun initCheckout(amountCredits: Int) {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.topUpWallet(amountCredits).collect { result ->
                result.onSuccess { url ->
                    _checkoutUrl.value = url
                }
                _isProcessing.value = false
            }
        }
    }

    fun clearCheckoutUrl() {
        _checkoutUrl.value = null
    }
}
