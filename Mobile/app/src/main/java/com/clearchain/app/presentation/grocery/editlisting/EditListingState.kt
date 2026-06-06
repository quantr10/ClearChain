package com.clearchain.app.presentation.grocery.editlisting

data class EditListingState(
    val listingId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "FRUITS",
    val quantity: String = "",
    val unit: String = "kg",
    val expiryDate: String = "",
    val imageUrl: String = "",
    val groceryHours: String? = null,

    val titleError: String? = null,
    val descriptionError: String? = null,
    val quantityError: String? = null,
    val unitError: String? = null,
    val expiryDateError: String? = null,

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showCategoryDropdown: Boolean = false,
    val showUnitDropdown: Boolean = false
)
