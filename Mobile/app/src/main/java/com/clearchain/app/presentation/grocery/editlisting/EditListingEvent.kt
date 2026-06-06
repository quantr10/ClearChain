package com.clearchain.app.presentation.grocery.editlisting

sealed class EditListingEvent {
    data class TitleChanged(val title: String)         : EditListingEvent()
    data class DescriptionChanged(val description: String) : EditListingEvent()
    data class CategoryChanged(val category: String)   : EditListingEvent()
    data class QuantityChanged(val quantity: String)   : EditListingEvent()
    data class UnitChanged(val unit: String)           : EditListingEvent()
    data class ExpiryDateChanged(val date: String)     : EditListingEvent()

    object ToggleCategoryDropdown : EditListingEvent()
    object ToggleUnitDropdown     : EditListingEvent()
    object SaveListing            : EditListingEvent()
    object ClearError             : EditListingEvent()
}
