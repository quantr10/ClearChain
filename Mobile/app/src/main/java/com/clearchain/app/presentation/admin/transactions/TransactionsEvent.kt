package com.clearchain.app.presentation.admin.transactions

import com.clearchain.app.presentation.components.SortOption

sealed class TransactionsEvent {
    object RefreshTransactions : TransactionsEvent()
    data class SearchQueryChanged(val query: String) : TransactionsEvent()
    data class StatusFilterChanged(val status: String?) : TransactionsEvent()
    data class SortOptionChanged(val option: SortOption) : TransactionsEvent()

    // Date range
    data class DatePresetSelected(val preset: String?) : TransactionsEvent() // null = clear
    data class CustomDateSelected(val dateStr: String) : TransactionsEvent() // "yyyy-MM-dd"
    data class ShowDatePicker(val forStart: Boolean) : TransactionsEvent()
    object HideDatePicker : TransactionsEvent()

    object ExportCsv : TransactionsEvent()

    object ShowFilterSheet : TransactionsEvent()
    object HideFilterSheet : TransactionsEvent()
}
