package com.clearchain.app.presentation.ngo.cart

import com.clearchain.app.presentation.components.SortOption

sealed class CartEvent {
    object LoadCart : CartEvent()
    data class SearchQueryChanged(val query: String) : CartEvent()
    data class SortOptionChanged(val sort: SortOption) : CartEvent()
    data class IncrementItem(val itemId: String, val quantity: Int) : CartEvent()
    data class DecrementItem(val itemId: String, val quantity: Int) : CartEvent()
    data class RemoveItem(val itemId: String) : CartEvent()
    data class ShowCheckout(val groceryId: String) : CartEvent()
    object DismissCheckout : CartEvent()
    data class PickupDateChanged(val value: String) : CartEvent()
    data class PickupTimeChanged(val value: String) : CartEvent()
    data class NotesChanged(val value: String) : CartEvent()
    object ToggleRefrigeration : CartEvent()
    object ToggleFragile : CartEvent()
    object ToggleHeavy : CartEvent()
    object SubmitCheckout : CartEvent()
    object ClearError : CartEvent()
}
