package com.clearchain.app.presentation.ngo.requestpickup

sealed class RequestPickupEvent {
    data class LoadListing(val listingId: String) : RequestPickupEvent()
    data class QuantityChanged(val quantity: String) : RequestPickupEvent()
    data class PickupDateChanged(val date: String) : RequestPickupEvent()
    data class PickupTimeChanged(val time: String) : RequestPickupEvent()
    data class NotesChanged(val notes: String) : RequestPickupEvent()
    object SubmitRequest : RequestPickupEvent()
    object ClearError : RequestPickupEvent()
}