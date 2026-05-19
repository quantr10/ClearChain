package com.clearchain.app.presentation.ngo.listingdetail

import com.clearchain.app.data.remote.api.PublicProfileData
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.OrganizationType

data class ListingDetailState(
    val listing: Listing? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    val currentUserType: OrganizationType? = null,

    val similarListings: List<Listing> = emptyList(),
    val isLoadingSimilar: Boolean = false,

    // Report dialog
    val showReportDialog: Boolean = false,
    val reportReason: String = "",
    val isSubmittingReport: Boolean = false,
    val reportSubmitted: Boolean = false,

    // Real-time availability override from SignalR
    val availabilityOverride: Int? = null,

    // NGO: save/favourite
    val isSaved: Boolean = false,
    val isTogglingFave: Boolean = false,

    // NGO: grocery "About Us" data
    val groceryProfile: PublicProfileData? = null,

    // Grocery: action states
    val isArchiving: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirm: Boolean = false,
)
