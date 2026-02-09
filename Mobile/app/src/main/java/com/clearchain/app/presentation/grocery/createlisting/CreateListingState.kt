package com.clearchain.app.presentation.grocery.createlisting

data class CreateListingState(
    val title: String = "",
    val description: String = "",
    val category: String = "FRUITS",
    val quantity: String = "",
    val unit: String = "kg",
    val expiryDate: String = "",
    val pickupTimeStart: String = "",
    val pickupTimeEnd: String = "",
    val imageUrl: String = "",

    // Errors
    val titleError: String? = null,
    val descriptionError: String? = null,
    val quantityError: String? = null,
    val unitError: String? = null,
    val expiryDateError: String? = null,
    val pickupTimeStartError: String? = null,
    val pickupTimeEndError: String? = null,

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCategoryDropdown: Boolean = false,
    val showUnitDropdown: Boolean = false
)