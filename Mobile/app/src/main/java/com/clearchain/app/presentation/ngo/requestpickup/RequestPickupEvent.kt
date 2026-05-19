package com.clearchain.app.presentation.ngo.requestpickup

sealed class RequestPickupEvent {
    data class LoadListing(val listingId: String) : RequestPickupEvent()
    data class QuantityChanged(val quantity: String) : RequestPickupEvent()
    data class PickupDateChanged(val date: String) : RequestPickupEvent()
    data class PickupTimeChanged(val time: String) : RequestPickupEvent()
    data class NotesChanged(val notes: String) : RequestPickupEvent()
    data class VehicleTypeChanged(val vehicleType: VehicleType) : RequestPickupEvent()
    object ToggleRefrigeration : RequestPickupEvent()
    object ToggleFragile : RequestPickupEvent()
    object ToggleHeavyLoad : RequestPickupEvent()
    data class TimeSlotSelected(val slot: String) : RequestPickupEvent()
    object SubmitRequest : RequestPickupEvent()
    object ClearError : RequestPickupEvent()
}