package com.clearchain.app.presentation.admin.transactions

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    "WEEK" -> today.minusDays(6).toString() to today.toString()
                    "MONTH" -> today.withDayOfMonth(1).toString() to today.toString()
                    else -> null to null
                }
                _state.update {
                    it.copy(
                        selectedDatePreset = event.preset,
                        filterStartDate = start,
                        filterEndDate = end
                    )
                }
                applyFilters()
            }

            is TransactionsEvent.ShowDatePicker ->
                _state.update { it.copy(showDatePickerDialog = true, datePickerForStart = event.forStart) }

            TransactionsEvent.HideDatePicker ->
                _state.update { it.copy(showDatePickerDialog = false) }

            is TransactionsEvent.CustomDateSelected -> {
                val isStart = _state.value.datePickerForStart
                _state.update { s ->
                    if (isStart) {
                        s.copy(filterStartDate = event.dateStr, selectedDatePreset = "CUSTOM", showDatePickerDialog = false)
                    } else {
                        s.copy(filterEndDate = event.dateStr, showDatePickerDialog = false)
                    }
                }
                applyFilters()
            }

            TransactionsEvent.ClearError ->
                _state.update { it.copy(error = null) }

            TransactionsEvent.ExportCsv -> exportCsv()

            TransactionsEvent.ShowFilterSheet ->
                _state.update { it.copy(showFilterSheet = true) }

            TransactionsEvent.HideFilterSheet ->
                _state.update { it.copy(showFilterSheet = false) }
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
            withContext(Dispatchers.IO) {
                try {
                    val sb = StringBuilder()
                    sb.appendLine("Items,Category,Grocery,NGO,Quantity,Pickup Date,Pickup Time,Status,Created At,Notes")
                    transactions.forEach { t ->
                        fun esc(s: String) = if (s.contains(',') || s.contains('"')) "\"${s.replace("\"", "\"\"")}\"" else s
                        sb.appendLine(
                            "${esc(t.itemTitles.joinToString("; "))},${esc(t.listingCategory)}," +
                                "${esc(t.groceryName)},${esc(t.ngoName)},${t.requestedQuantity}," +
                                "${t.pickupDate.take(10)},${esc(t.pickupTime)},${t.status.name}," +
                                "${t.createdAt.take(10)},${esc(t.notes ?: "")}"
                        )
                    }
                    val csv = sb.toString()
                    val fileName = "clearchain_transactions_${tag}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"

                    val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.also { u ->
                            context.contentResolver.openOutputStream(u)?.use { it.write(csv.toByteArray()) }
                        }
                    } else {
                        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                        FileOutputStream(file).use { it.write(csv.toByteArray()) }
                        Uri.fromFile(file)
                    }

                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(shareIntent, "Share Transactions CSV")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_csv_saved)))
                    } else {
                        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_csv_failed)))
                    }
                } catch (e: Exception) {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_export_failed, e.message ?: "")))
                }
            }
        }
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
                        flaggedIds = flaggedIds,
                        isLoading = false
                    )
                }
                // Re-applies whatever status/date/search filter is active — this runs on
                // every SignalR event, so skipping it silently reset the admin's filters
                // to "show everything" each time a transaction event arrived.
                applyFilters()
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
        val end = currentState.filterEndDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
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
            else -> filtered.sortedByDescending { it.createdAt } // "date_desc" and default
        }

        _state.update { it.copy(filteredTransactions = filtered) }
    }
}
