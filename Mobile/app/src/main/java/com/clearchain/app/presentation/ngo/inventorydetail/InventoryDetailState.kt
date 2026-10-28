package com.clearchain.app.presentation.ngo.inventorydetail

import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.PickupRequest

data class InventoryDetailState(
    val item: InventoryItem? = null,
    val relatedRequest: PickupRequest? = null,
    val moreFromStore: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingRequest: Boolean = false,
    val error: String? = null,

    val showQrSheet: Boolean = false
)
