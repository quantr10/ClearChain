package com.clearchain.app.presentation.ngo.listingdetail

import com.clearchain.app.domain.model.Listing

// ═══ State ═══
data class ListingDetailState(
    val listing: Listing? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
