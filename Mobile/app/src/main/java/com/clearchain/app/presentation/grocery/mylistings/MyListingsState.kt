package com.clearchain.app.presentation.grocery.mylistings

import com.clearchain.app.domain.model.Listing

data class MyListingsState(
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)