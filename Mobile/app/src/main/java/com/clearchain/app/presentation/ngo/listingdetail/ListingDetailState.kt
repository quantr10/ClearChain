package com.clearchain.app.presentation.ngo.listingdetail

import com.clearchain.app.data.remote.api.PublicProfileData
import com.clearchain.app.data.remote.dto.CartItemData
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.OrganizationType

data class ListingDetailState(
    val listing: Listing? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    val currentUserType: OrganizationType? = null,

    val similarListings: List<Listing> = emptyList(),

    // Real-time availability override from SignalR
    val availabilityOverride: Int? = null,

    // NGO: save/favourite
    val isSaved: Boolean = false,
    val isTogglingFave: Boolean = false,

    // NGO: grocery "About Us" data
    val groceryProfile: PublicProfileData? = null,

    // NGO: cart state
    val cartItemsByListingId: Map<String, CartItemData> = emptyMap(),
    /** The listing whose cart call is in flight. */
    val updatingCartListingId: String? = null,

    // Grocery: action states
    val isDeleting: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isArchiving: Boolean = false,
    val showArchiveConfirm: Boolean = false,
    val isRestoring: Boolean = false,
    val showRestoreConfirm: Boolean = false
) {
    val isUpdatingCart: Boolean get() = updatingCartListingId != null
}
