package com.clearchain.app.presentation.ngo.requestpickup

import androidx.annotation.StringRes
import com.clearchain.app.R
import com.clearchain.app.domain.model.Listing

enum class VehicleType(@StringRes val labelResId: Int, val icon: String) {
    WALK(R.string.vehicle_walk, "🚶"),
    BICYCLE(R.string.vehicle_bicycle, "🚲"),
    MOTORCYCLE(R.string.vehicle_motorcycle, "🏍️"),
    CAR(R.string.vehicle_car, "🚗"),
    VAN(R.string.vehicle_van, "🚐")
}

data class RequestPickupState(
    val listing: Listing? = null,
    val quantity: String = "",
    val pickupDate: String = "",
    val pickupTime: String = "",
    val notes: String = "",

    // Vehicle & special handling
    val vehicleType: VehicleType? = null,
    val needsRefrigeration: Boolean = false,
    val fragileItems: Boolean = false,
    val heavyLoad: Boolean = false,

    // Time slot picker
    val availableTimeSlots: List<String> = emptyList(),
    val selectedTimeSlot: String? = null,

    // Estimated trip info
    val estimatedTripMinutes: Int? = null,

    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)