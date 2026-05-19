package com.clearchain.app.presentation.ngo.inventorydetail

import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.PickupRequest

// ═══ State ═══
data class InventoryDetailState(
    val item: InventoryItem? = null,
    val relatedRequest: PickupRequest? = null,
    val isLoading: Boolean = false,
    val isLoadingRequest: Boolean = false,
    val error: String? = null,

    // Edit mode
    val isEditing: Boolean = false,
    val editProductName: String = "",
    val editCategory: String = "",
    val editQuantity: String = "",
    val editUnit: String = "",
    val editExpiryDate: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,

    // QR share
    val showQrSheet: Boolean = false,

    // Photo documentation
    val isUploadingPhoto: Boolean = false,
    val photoUploadError: String? = null
)