package com.clearchain.app.presentation.grocery.createlisting

import android.net.Uri

sealed class CreateListingEvent {
    data class TitleChanged(val title: String) : CreateListingEvent()
    data class DescriptionChanged(val description: String) : CreateListingEvent()
    data class CategoryChanged(val category: String) : CreateListingEvent()
    data class QuantityChanged(val quantity: String) : CreateListingEvent()
    data class UnitChanged(val unit: String) : CreateListingEvent()
    data class ExpiryDateChanged(val date: String) : CreateListingEvent()
    data class PickupTimeStartChanged(val time: String) : CreateListingEvent()
    data class PickupTimeEndChanged(val time: String) : CreateListingEvent()
    data class ImageUrlChanged(val url: String) : CreateListingEvent()
    data class ImageSelected(val uri: Uri) : CreateListingEvent()

    object ToggleCategoryDropdown : CreateListingEvent()
    object ToggleUnitDropdown : CreateListingEvent()
    object CreateListing : CreateListingEvent()
    object ClearError : CreateListingEvent()
    object AnalyzeImage : CreateListingEvent()
    object ApplyAISuggestions : CreateListingEvent()
    object ToggleImagePicker : CreateListingEvent()
    object ClearImage : CreateListingEvent()
}