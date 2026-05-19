package com.clearchain.app.presentation.admin.transactions

import com.clearchain.app.domain.model.PickupRequest

data class TransactionsState(
    val allTransactions: List<PickupRequest> = emptyList(),
    val filteredTransactions: List<PickupRequest> = emptyList(),

    val searchQuery: String = "",
    val selectedStatus: String? = null,

    // Date range filter
    val selectedDatePreset: String? = null, // "TODAY" | "WEEK" | "MONTH" | "CUSTOM" | null = all
    val filterStartDate: String? = null,    // "yyyy-MM-dd"
    val filterEndDate: String? = null,      // "yyyy-MM-dd"
    val showDatePickerDialog: Boolean = false,
    val datePickerForStart: Boolean = true,

    // Filter sheet
    val showFilterSheet: Boolean = false,

    val expandedTransactionId: String? = null,
    val showExportDialog: Boolean = false,
    val exportCsvText: String = "",

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // Flagged: PENDING transactions older than 3 days
    val flaggedIds: Set<String> = emptySet()
) {
    // Aggregate stats computed from all transactions
    val totalCompleted: Int get() = allTransactions.count { it.status == com.clearchain.app.domain.model.PickupRequestStatus.COMPLETED }
    val totalPending:   Int get() = allTransactions.count { it.status == com.clearchain.app.domain.model.PickupRequestStatus.PENDING }
    val flaggedCount:   Int get() = flaggedIds.size
    val activeFilterCount: Int get() = if (selectedDatePreset != null) 1 else 0
}