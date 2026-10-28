package com.clearchain.app.presentation.admin.transactions

import androidx.annotation.StringRes
import com.clearchain.app.R
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.presentation.components.CommonSortOptions
import com.clearchain.app.presentation.components.SortOption

data class TransactionsState(
    val allTransactions: List<PickupRequest> = emptyList(),
    val filteredTransactions: List<PickupRequest> = emptyList(),

    val searchQuery: String = "",
    val selectedStatus: String? = null,

    // Sort (by submission date — matches the default sort loadTransactions() already applies)
    val selectedSort: SortOption = CommonSortOptions.CREATED_DATE_DESC,
    val availableSortOptions: List<SortOption> = listOf(
        CommonSortOptions.CREATED_DATE_DESC,
        CommonSortOptions.CREATED_DATE_ASC
    ),

    // Filter sheet
    val showFilterSheet: Boolean = false,
    // Toggles like the NGO "Saved only" chip: none on = every transaction, with or without a dispute.
    val filterDisputes: Set<DisputeFilter> = emptySet(),
    val filterCategory: String? = null,
    val filterPickupDate: String? = null, // yyyy-MM-dd; null = any date
    val filterExpiresBy: String? = null, // yyyy-MM-dd; null = any date

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,

    // Flagged: PENDING transactions older than 3 days
    val flaggedIds: Set<String> = emptySet()
) {
    val activeFilterCount: Int get() =
        (if (filterDisputes.isNotEmpty()) 1 else 0) +
            (if (filterCategory != null) 1 else 0) +
            (if (filterPickupDate != null) 1 else 0) +
            (if (filterExpiresBy != null) 1 else 0)
}

/** Which transactions to keep by their dispute, matched on [PickupRequest.disputeStatus]. */
enum class DisputeFilter(@StringRes val labelRes: Int) {
    UNRESOLVED(R.string.filter_dispute_unresolved),
    RESOLVED(R.string.filter_dispute_resolved);

    fun matches(disputeStatus: String?): Boolean = when (this) {
        UNRESOLVED -> disputeStatus == "open"
        // The admin's outcomes, as DisputesController.ClosedStatuses lists them.
        RESOLVED -> disputeStatus == "resolved_ngo" || disputeStatus == "resolved_grocery" ||
            disputeStatus == "dismissed"
    }
}
