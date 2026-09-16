package com.clearchain.app.presentation.admin.transactions

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
        CommonSortOptions.CREATED_DATE_ASC,
    ),

    // Date range filter
    val selectedDatePreset: String? = null, // "TODAY" | "WEEK" | "MONTH" | "CUSTOM" | null = all
    val filterStartDate: String? = null,    // "yyyy-MM-dd"
    val filterEndDate: String? = null,      // "yyyy-MM-dd"
    val showDatePickerDialog: Boolean = false,
    val datePickerForStart: Boolean = true,

    // Filter sheet
    val showFilterSheet: Boolean = false,

    val showExportDialog: Boolean = false,
    val exportCsvText: String = "",

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // Flagged: PENDING transactions older than 3 days
    val flaggedIds: Set<String> = emptySet()
) {
    val activeFilterCount: Int get() = if (selectedDatePreset != null) 1 else 0
}