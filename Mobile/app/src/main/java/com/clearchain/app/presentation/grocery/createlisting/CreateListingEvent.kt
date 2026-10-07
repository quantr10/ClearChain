package com.clearchain.app.presentation.grocery.createlisting

import android.net.Uri

sealed class CreateListingEvent {
    data class TitleChanged(val title: String) : CreateListingEvent()
    data class DescriptionChanged(val description: String) : CreateListingEvent()
    data class CategoryChanged(val category: String) : CreateListingEvent()
    data class QuantityChanged(val quantity: String) : CreateListingEvent()
    data class UnitChanged(val unit: String) : CreateListingEvent()
    data class ExpiryDateChanged(val date: String) : CreateListingEvent()

    object ToggleCategoryDropdown : CreateListingEvent()
    object ToggleUnitDropdown : CreateListingEvent()
    object CreateListing : CreateListingEvent()
    object ApplyAISuggestions : CreateListingEvent()
    object ToggleImagePicker : CreateListingEvent()
    object ClearImage : CreateListingEvent()
    object DismissAnalysis : CreateListingEvent()

    // Multi-image
    data class AddImage(val uri: Uri) : CreateListingEvent()

    // Preview
    object TogglePreview : CreateListingEvent()
}
