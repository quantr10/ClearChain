package com.clearchain.app.presentation.ngo.browselistings

import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.Listing

data class BrowseListingsState(
    val listings: List<Listing> = emptyList(),
    val filteredListings: List<Listing> = emptyList(),
    val selectedCategory: FoodCategory? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)