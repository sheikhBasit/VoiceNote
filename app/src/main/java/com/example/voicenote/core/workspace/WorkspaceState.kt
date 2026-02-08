
package com.example.voicenote.core.workspace

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class WorkspaceState(
    val selectedOrgId: String? = null, // null means Personal
    val isPersonalMode: Boolean = true,
    val geofenceStatus: GeofenceStatus = GeofenceStatus.UNKNOWN,
    val currentBillingWallet: String = "Personal"
)

enum class GeofenceStatus { INSIDE, OUTSIDE, UNKNOWN }

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    // private val repository: WorkspaceRepository // Repository to be implemented
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceState())
    val state = _state.asStateFlow()

    fun toggleWorkspace(orgId: String?) {
        _state.update { it.copy(
            selectedOrgId = orgId,
            isPersonalMode = orgId == null,
            currentBillingWallet = if (orgId == null) "Personal" else "Corporate"
        )}
    }

    fun updateLocation(lat: Double, lon: Double) {
        // Logic to verify against backend WorkLocations
    }
}
