package com.clearchain.app.presentation.admin.transactions

import com.clearchain.app.presentation.components.SortOption

sealed class TransactionsEvent {
    object RefreshTransactions : TransactionsEvent()
    data class SearchQueryChanged(val query: String) : TransactionsEvent()
    data class StatusFilterChanged(val status: String?) : TransactionsEvent()
    data class SortOptionChanged(val option: SortOption) : TransactionsEvent()

    object ExportCsv : TransactionsEvent()

    // Advanced filter sheet
    object ShowFilterSheet : TransactionsEvent()
    object HideFilterSheet : TransactionsEvent()
    data class ToggleDisputeFilter(val filter: DisputeFilter) : TransactionsEvent()
    data class FilterCategoryChanged(val category: String?) : TransactionsEvent()
    data class FilterPickupDateChanged(val date: String?) : TransactionsEvent()
    data class FilterExpiresByChanged(val date: String?) : TransactionsEvent()
    object ClearAdvancedFilters : TransactionsEvent()
}
