package com.clearchain.app.presentation.ngo.inventory

import com.clearchain.app.domain.model.InventoryItem

data class InventoryState(
    val allItems: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),

    val selectedStatus: String? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)