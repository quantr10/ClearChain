package com.clearchain.app.presentation.admin.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.domain.model.hasItemExpiringBy
import com.clearchain.app.domain.model.itemTitles
import com.clearchain.app.domain.model.searchText
import com.clearchain.app.util.DownloadsExport
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adminApi: AdminApi,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionsState())
    val state: StateFlow<TransactionsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadTransactions()
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch {
            signalRService.pickupRequestCreated.collect { request ->
                loadTransactions()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_request_from, request.ngoName)))
            }
        }
        viewModelScope.launch {
            signalRService.pickupRequestStatusChanged.collect { loadTransactions() }
        }
        viewModelScope.launch {
            signalRService.transactionCompleted.collect { loadTransactions() }
        }
        viewModelScope.launch {
            signalRService.pickupRequestCancelled.collect { loadTransactions() }
        }
    }

    fun onEvent(event: TransactionsEvent) {
        when (event) {
            TransactionsEvent.RefreshTransactions -> refreshTransactions()

            is TransactionsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }

            is TransactionsEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
                applyFilters()
            }

            is TransactionsEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }

            TransactionsEvent.ExportCsv -> exportCsv()

            TransactionsEvent.ShowFilterSheet ->
                _state.update { it.copy(showFilterSheet = true) }

            TransactionsEvent.HideFilterSheet ->
                _state.update { it.copy(showFilterSheet = false) }

            is TransactionsEvent.ToggleDisputeFilter -> {
                _state.update {
                    val selected = it.filterDisputes
                    it.copy(filterDisputes = if (event.filter in selected) selected - event.filter else selected + event.filter)
                }
                applyFilters()
            }

            is TransactionsEvent.FilterCategoryChanged -> {
                _state.update { it.copy(filterCategory = event.category) }
                applyFilters()
            }

            is TransactionsEvent.FilterPickupDateChanged -> {
                _state.update { it.copy(filterPickupDate = event.date) }
                applyFilters()
            }

            is TransactionsEvent.FilterExpiresByChanged -> {
                _state.update { it.copy(filterExpiresBy = event.date) }
                applyFilters()
            }

            TransactionsEvent.ClearAdvancedFilters -> {
                _state.update {
                    it.copy(
                        filterDisputes = emptySet(),
                        filterCategory = null,
                        filterPickupDate = null,
                        filterExpiresBy = null
                    )
                }
                applyFilters()
            }
        }
    }

    private fun exportCsv() {
        val current = _state.value
        val transactions = current.filteredTransactions
        if (transactions.isEmpty()) {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_no_transactions_export))) }
            return
        }
        val tag = current.selectedStatus?.lowercase() ?: "all"
        viewModelScope.launch {
            val message = DownloadsExport.exportCsv(
                context,
                filePrefix = "clearchain_transactions_$tag",
                chooserTitle = "Share Transactions CSV",
                header = listOf("Items", "Category", "Grocery", "NGO", "Quantity", "Pickup Date", "Pickup Time", "Status", "Created At", "Notes"),
                rows = transactions.map { t ->
                    listOf(
                        t.itemTitles.joinToString("; "),
                        t.listingCategory,
                        t.groceryName,
                        t.ngoName,
                        t.requestedQuantity.toString(),
                        t.pickupDate.take(10),
                        t.pickupTime,
                        t.status.name,
                        t.createdAt.take(10),
                        t.notes ?: ""
                    )
                }
            )
            _uiEvent.send(UiEvent.ShowSnackbar(message))
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val response = adminApi.getAllPickupRequests()
                val allRequests = response.data.map { it.toDomain() }
                    .sortedByDescending { it.createdAt }

                val flaggedIds = computeFlaggedIds(allRequests.filter { it.status == PickupRequestStatus.PENDING })
                _state.update {
                    it.copy(
                        allTransactions = allRequests,
                        flaggedIds = flaggedIds,
                        isLoading = false
                    )
                }
                // Re-applies whatever status/date/search filter is active — this runs on
                // every SignalR event, so skipping it silently reset the admin's filters
                // to "show everything" each time a transaction event arrived.
                applyFilters()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_load_transactions)))
            }
        }
    }

    private fun computeFlaggedIds(pendingRequests: List<com.clearchain.app.domain.model.PickupRequest>): Set<String> {
        val cutoff = LocalDate.now().minusDays(3)
        return pendingRequests.filter { req ->
            runCatching {
                val created = LocalDate.parse(req.createdAt.take(10))
                created.isBefore(cutoff)
            }.getOrDefault(false)
        }.map { it.id }.toSet()
    }

    private fun refreshTransactions() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            try {
                val response = adminApi.getAllPickupRequests()
                val allRequests = response.data.map { it.toDomain() }
                    .sortedByDescending { it.createdAt }
                val flaggedIds = computeFlaggedIds(allRequests.filter { it.status == PickupRequestStatus.PENDING })

                _state.update {
                    it.copy(
                        allTransactions = allRequests,
                        flaggedIds = flaggedIds,
                        isRefreshing = false
                    )
                }
                applyFilters()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_transactions_refreshed)))
            } catch (e: Exception) {
                _state.update { it.copy(isRefreshing = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_refresh_transactions)))
            }
        }
    }

    private fun applyFilters() {
        val currentState = _state.value
        var filtered = currentState.allTransactions

        // Filter by status
        currentState.selectedStatus?.let { status ->
            filtered = filtered.filter { it.status.name == status }
        }

        // Both on keeps every disputed transaction; none on skips this filter.
        if (currentState.filterDisputes.isNotEmpty()) {
            filtered = filtered.filter { t -> currentState.filterDisputes.any { it.matches(t.disputeStatus) } }
        }

        currentState.filterCategory?.let { category ->
            filtered = filtered.filter { it.listingCategory == category }
        }

        // Pickup on the picked date
        currentState.filterPickupDate?.let { date ->
            filtered = filtered.filter { it.pickupDate.take(10) == date }
        }

        // Any product expiring on or before the picked date
        currentState.filterExpiresBy?.let { date ->
            filtered = filtered.filter { it.hasItemExpiringBy(date) }
        }

        // Filter by search query
        if (currentState.searchQuery.isNotBlank()) {
            val query = currentState.searchQuery.lowercase()
            filtered = filtered.filter { transaction ->
                transaction.searchText.contains(query)
            }
        }

        filtered = when (currentState.selectedSort.value) {
            "date_asc" -> filtered.sortedBy { it.createdAt }
            else -> filtered.sortedByDescending { it.createdAt } // "date_desc" and default
        }

        _state.update { it.copy(filteredTransactions = filtered) }
    }
}
