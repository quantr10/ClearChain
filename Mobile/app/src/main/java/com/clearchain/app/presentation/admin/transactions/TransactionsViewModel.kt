package com.clearchain.app.presentation.admin.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.domain.model.itemTitles
import com.clearchain.app.domain.model.searchText
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

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
        setupSignalR()  // ✅ ADD
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
            TransactionsEvent.LoadTransactions -> loadTransactions()
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

            is TransactionsEvent.DatePresetSelected -> {
                val today = LocalDate.now()
                val (start, end) = when (event.preset) {
                    "TODAY" -> today.toString() to today.toString()
                    "WEEK"  -> today.minusDays(6).toString() to today.toString()
                    "MONTH" -> today.withDayOfMonth(1).toString() to today.toString()
                    else    -> null to null
                }
                _state.update { it.copy(
                    selectedDatePreset = event.preset,
                    filterStartDate = start,
                    filterEndDate = end
                ) }
                applyFilters()
            }

            is TransactionsEvent.ShowDatePicker ->
                _state.update { it.copy(showDatePickerDialog = true, datePickerForStart = event.forStart) }

            TransactionsEvent.HideDatePicker ->
                _state.update { it.copy(showDatePickerDialog = false) }

            is TransactionsEvent.CustomDateSelected -> {
                val isStart = _state.value.datePickerForStart
                _state.update { s ->
                    if (isStart) s.copy(filterStartDate = event.dateStr, selectedDatePreset = "CUSTOM", showDatePickerDialog = false)
                    else         s.copy(filterEndDate = event.dateStr, showDatePickerDialog = false)
                }
                applyFilters()
            }

            TransactionsEvent.ClearError ->
                _state.update { it.copy(error = null) }

            TransactionsEvent.ShowExportDialog ->
                _state.update { it.copy(showExportDialog = true, exportCsvText = buildCsvExport()) }

            TransactionsEvent.DismissExportDialog ->
                _state.update { it.copy(showExportDialog = false) }

            TransactionsEvent.ShowFilterSheet ->
                _state.update { it.copy(showFilterSheet = true) }

            TransactionsEvent.HideFilterSheet ->
                _state.update { it.copy(showFilterSheet = false) }
        }
    }

    private fun buildCsvExport(): String {
        val header = "ID,Item,Items,Category,Grocery,NGO,Quantity,Pickup Date,Status,Created At\n"
        val rows = _state.value.filteredTransactions.joinToString("\n") { t ->
            listOf(
                t.id.take(8),
                t.listingTitle.replace(",", ";"),
                t.itemTitles.joinToString(" | ").replace(",", ";"),
                t.listingCategory,
                t.groceryName.replace(",", ";"),
                t.ngoName.replace(",", ";"),
                t.requestedQuantity.toString(),
                t.pickupDate,
                t.status.name,
                t.createdAt.take(10)
            ).joinToString(",")
        }
        return header + rows
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val response = adminApi.getAllPickupRequests()
                val allRequests = response.data.map { it.toDomain() }
                    .sortedByDescending { it.createdAt }

                val flaggedIds = computeFlaggedIds(allRequests.filter { it.status == PickupRequestStatus.PENDING })
                _state.update {
                    it.copy(
                        allTransactions = allRequests,
                        filteredTransactions = allRequests,
                        flaggedIds = flaggedIds,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: context.getString(R.string.error_load_transactions),
                        isLoading = false
                    )
                }
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
            _state.update { it.copy(isRefreshing = true, error = null) }

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
                _state.update {
                    it.copy(
                        error = e.message ?: context.getString(R.string.error_refresh_transactions),
                        isRefreshing = false
                    )
                }
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

        // Filter by date range
        val start = currentState.filterStartDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val end   = currentState.filterEndDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (start != null || end != null) {
            filtered = filtered.filter { t ->
                val date = runCatching { LocalDate.parse(t.createdAt.take(10)) }.getOrNull() ?: return@filter true
                (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end))
            }
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
            else       -> filtered.sortedByDescending { it.createdAt } // "date_desc" and default
        }

        _state.update { it.copy(filteredTransactions = filtered) }
    }
}