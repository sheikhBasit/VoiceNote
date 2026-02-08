package com.example.voicenote.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    val id: String,
    val name: String,
    val domain: String,
    val logoUrl: String? = null,
    val billingType: BillingType = BillingType.PERSONAL,
    val geofenceEnabled: Boolean = false,
    val allowedLocations: List<WorkLocation> = emptyList()
)

@Serializable
enum class BillingType {
    PERSONAL,
    CORPORATE
}

@Serializable
data class WorkLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)
