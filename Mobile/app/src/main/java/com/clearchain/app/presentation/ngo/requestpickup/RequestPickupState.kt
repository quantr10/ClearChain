package com.clearchain.app.presentation.ngo.requestpickup

import com.clearchain.app.domain.model.Listing

data class RequestPickupState(
    val listing: Listing? = null,
    val quantity: String = "",
    val pickupDate: String = "",
    val pickupTime: String = "",
    val notes: String = "",

    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)