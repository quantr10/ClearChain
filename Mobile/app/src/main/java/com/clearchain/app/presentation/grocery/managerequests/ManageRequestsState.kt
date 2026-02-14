package com.clearchain.app.presentation.grocery.managerequests

import com.clearchain.app.domain.model.PickupRequest

data class ManageRequestsState(
    val requests: List<PickupRequest> = emptyList(),
    val filteredRequests: List<PickupRequest> = emptyList(),
    val selectedStatus: String? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)