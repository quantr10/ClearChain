package com.clearchain.app.presentation.grocery.createlisting

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

    object ToggleCategoryDropdown : CreateListingEvent()
    object ToggleUnitDropdown : CreateListingEvent()
    object CreateListing : CreateListingEvent()
    object ClearError : CreateListingEvent()
}