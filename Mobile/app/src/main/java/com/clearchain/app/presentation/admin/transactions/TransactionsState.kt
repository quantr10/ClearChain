package com.clearchain.app.presentation.admin.transactions

import com.clearchain.app.domain.model.PickupRequest

data class TransactionsState(
    val allTransactions: List<PickupRequest> = emptyList(),
    val filteredTransactions: List<PickupRequest> = emptyList(),

    val searchQuery: String = "",
    val selectedStatus: String? = null,

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)