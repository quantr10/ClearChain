package com.clearchain.app.domain.model

data class LocationPreference(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Int,
    val displayName: String, // "Hanoi" or "Current Location"
    val savedAt: Long = System.currentTimeMillis()
)