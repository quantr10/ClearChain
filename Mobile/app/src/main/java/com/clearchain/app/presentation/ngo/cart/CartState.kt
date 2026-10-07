package com.clearchain.app.presentation.ngo.cart

import com.clearchain.app.data.remote.dto.CartGroupData
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.SortOption

data class CartState(
    val groups: List<CartGroupData> = emptyList(),
    val searchQuery: String = "",
    val selectedSort: SortOption = CommonSortOptions.NAME_ASC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.NAME_ASC,
        CommonSortOptions.NAME_DESC,
        CommonSortOptions.EXPIRY_ASC,
        CommonSortOptions.EXPIRY_DESC
    ),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    /** The cart item whose quantity change is in flight (its stepper spins). */
    val updatingItemId: String? = null,
    /** The cart item being removed (its delete button spins). */
    val removingItemId: String? = null,
    val error: String? = null,
    val checkoutGroceryId: String? = null,
    val pickupDate: String = "",
    val pickupTime: String = "",
    val pickupDateError: String? = null,
    val pickupTimeError: String? = null,
    val notes: String = "",
    val requiresRefrigeration: Boolean = false,
    val isFragile: Boolean = false,
    val isHeavy: Boolean = false
)
