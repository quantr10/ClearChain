package com.clearchain.app.presentation.admin.transactions

sealed class TransactionsEvent {
    object LoadTransactions : TransactionsEvent()
    object RefreshTransactions : TransactionsEvent()
    data class SearchQueryChanged(val query: String) : TransactionsEvent()
    data class StatusFilterChanged(val status: String?) : TransactionsEvent()
    object ClearError : TransactionsEvent()
}