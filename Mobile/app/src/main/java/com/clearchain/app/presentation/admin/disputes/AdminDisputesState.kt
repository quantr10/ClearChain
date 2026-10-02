package com.clearchain.app.presentation.admin.disputes

import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.DisputeListItemData
import com.clearchain.app.presentation.components.FilterChipData

data class AdminDisputesState(
    val disputes: List<DisputeListItemData> = emptyList(),

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // null = all statuses. Only "open"/"under_review" are exact-match filterable server-side —
    // the three closed outcomes (resolved_ngo, resolved_grocery, dismissed) are only ever set
    // by this screen's own resolve action, so "All" is how a closed dispute is found again.
    val selectedStatus: String? = "open",
    val availableStatusFilters: List<FilterChipData> = listOf(
        FilterChipData(null, labelResId = R.string.filter_all),
        FilterChipData("open", labelResId = R.string.dispute_status_open),
        FilterChipData("under_review", labelResId = R.string.dispute_status_under_review)
    ),

    val expandedDisputeId: String? = null,

    // Resolve dialog
    val showResolveDialogForId: String? = null,
    val resolveOutcome: String = "resolved_ngo",
    val resolveGroceryStatement: String = "",
    val resolveNote: String = "",
    val isResolving: Boolean = false
)
