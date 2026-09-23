package com.clearchain.app.presentation.ngo.inventory

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
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.domain.usecase.inventory.DistributeItemUseCase
import com.clearchain.app.domain.usecase.inventory.GetMyInventoryUseCase
import com.clearchain.app.domain.usecase.inventory.UpdateExpiredItemsUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class InventoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getMyInventoryUseCase: GetMyInventoryUseCase,
    private val updateExpiredItemsUseCase: UpdateExpiredItemsUseCase,
    private val distributeInventoryItemUseCase: DistributeItemUseCase,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadInventory()
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch {
            signalRService.inventoryItemAdded.collect { item ->
                loadInventory()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_inventory_item, item.productName)))
            }
        }
        viewModelScope.launch { signalRService.inventoryItemDistributed.collect { loadInventory() } }
        viewModelScope.launch { signalRService.inventoryItemExpired.collect { loadInventory() } }
        viewModelScope.launch { signalRService.inventoryItemUpdated.collect { loadInventory() } }
    }

    fun onEvent(event: InventoryEvent) {
        when (event) {
            InventoryEvent.LoadInventory -> loadInventory()
            InventoryEvent.RefreshInventory -> refreshInventory()
            InventoryEvent.UpdateExpired -> updateExpiredItems()

            // Search & Sort
            is InventoryEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is InventoryEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }

            // Status Tab — never allow null (All is removed)
            is InventoryEvent.StatusTabChanged -> {
                _state.update { it.copy(selectedStatusTab = event.status ?: InventoryStatus.ACTIVE) }
                applyFilters()
            }

            // Category Filter
            is InventoryEvent.CategoryFilterChanged -> {
                _state.update { it.copy(selectedCategory = event.category) }
                applyFilters()
            }

            is InventoryEvent.DistributeItem -> distributeItem(event.itemId)

            InventoryEvent.ClearError -> _state.update { it.copy(error = null) }

            // Advanced filter sheet
            InventoryEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = true) }
            InventoryEvent.HideFilterSheet -> _state.update { it.copy(showFilterSheet = false) }
            is InventoryEvent.FilterExpiryWithinDaysChanged -> {
                _state.update { it.copy(filterExpiryWithinDays = event.days) }
                applyFilters()
            }
            is InventoryEvent.FilterMinQtyChanged -> {
                _state.update { it.copy(filterMinQty = event.min) }
                applyFilters()
            }
            is InventoryEvent.FilterMaxQtyChanged -> {
                _state.update { it.copy(filterMaxQty = event.max) }
                applyFilters()
            }
            InventoryEvent.ClearAdvancedFilters -> {
                _state.update { it.copy(selectedCategory = null, filterExpiryWithinDays = null, filterMinQty = 0.0, filterMaxQty = null) }
                applyFilters()
            }

            // Bulk selection
            InventoryEvent.ToggleSelectionMode ->
                _state.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }
            is InventoryEvent.ToggleItemSelection ->
                _state.update {
                    val updated = if (event.itemId in it.selectedIds) {
                        it.selectedIds - event.itemId
                    } else {
                        it.selectedIds + event.itemId
                    }
                    it.copy(selectedIds = updated, isSelectionMode = updated.isNotEmpty())
                }
            InventoryEvent.SelectAll ->
                _state.update {
                    val selected = it.filteredItems.map { i -> i.id }.toSet()
                    it.copy(selectedIds = selected, isSelectionMode = selected.isNotEmpty())
                }
            InventoryEvent.DeselectAll ->
                _state.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
            InventoryEvent.BulkDistribute -> bulkDistribute()

            // Manual add
            InventoryEvent.ShowManualAddSheet -> _state.update { it.copy(showManualAddSheet = true) }
            InventoryEvent.HideManualAddSheet -> _state.update { it.copy(showManualAddSheet = false) }
            is InventoryEvent.ManualProductNameChanged -> _state.update { it.copy(manualProductName = event.name) }
            is InventoryEvent.ManualCategoryChanged -> _state.update { it.copy(manualCategory = event.category) }
            is InventoryEvent.ManualQuantityChanged -> _state.update { it.copy(manualQuantity = event.qty) }
            is InventoryEvent.ManualUnitChanged -> _state.update { it.copy(manualUnit = event.unit) }
            is InventoryEvent.ManualExpiryDateChanged -> _state.update { it.copy(manualExpiryDate = event.date) }
            InventoryEvent.SubmitManualAdd -> submitManualAdd()

            InventoryEvent.ExportCsv -> exportCsv()
        }
    }

    private fun exportCsv() {
        val items = _state.value.allItems
        if (items.isEmpty()) {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_no_inventory_export))) }
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val sb = StringBuilder()
                    sb.appendLine("Product Name,Category,Quantity,Unit,Status,Received At,Expiry Date,Distributed At")
                    items.forEach { item ->
                        fun esc(s: String) = if (s.contains(',') || s.contains('"')) "\"${s.replace("\"", "\"\"")}\"" else s
                        sb.appendLine(
                            "${esc(item.productName)},${esc(item.category)},${item.quantity},${esc(item.unit)}," +
                                "${item.status.name},${item.receivedAt.take(10)},${item.expiryDate.take(10)}," +
                                "${item.distributedAt?.take(10) ?: ""}"
                        )
                    }
                    val csv = sb.toString()
                    val fileName = "clearchain_inventory_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"

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
                        val chooser = Intent.createChooser(shareIntent, "Share Inventory CSV")
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

    private fun loadInventory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // expired items first
            updateExpiredItemsUseCase()

            val result = getMyInventoryUseCase()

            result.fold(
                onSuccess = { items ->
                    _state.update {
                        it.copy(
                            allItems = items,
                            isLoading = false
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: context.getString(R.string.error_load_inventory)
                        )
                    }
                }
            )
        }
    }

    private fun refreshInventory() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            // expired items first
            updateExpiredItemsUseCase()

            val result = getMyInventoryUseCase()

            result.fold(
                onSuccess = { items ->
                    _state.update {
                        it.copy(
                            allItems = items,
                            isRefreshing = false
                        )
                    }
                    applyFilters()
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_inventory_refreshed)))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: context.getString(R.string.error_refresh_inventory)
                        )
                    }
                }
            )
        }
    }

    private fun updateExpiredItems() {
        viewModelScope.launch {
            updateExpiredItemsUseCase()
            loadInventory()
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allItems

        // Apply STATUS TAB filter (first priority)
        current.selectedStatusTab?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        // Apply search
        if (current.searchQuery.isNotBlank()) {
            val query = current.searchQuery.lowercase()
            filtered = filtered.filter { item ->
                item.productName.lowercase().contains(query) ||
                    item.category.lowercase().contains(query)
            }
        }

        // Apply CATEGORY CHIP filter
        current.selectedCategory?.let { category ->
            filtered = filtered.filter { item ->
                item.category.equals(category, ignoreCase = true) ||
                    item.category.lowercase().replace(" ", "_") == category.lowercase()
            }
        }

        // Advanced: expiry within N days
        current.filterExpiryWithinDays?.let { days ->
            val today = java.time.LocalDate.now()
            val threshold = today.plusDays(days.toLong())
            filtered = filtered.filter { item ->
                runCatching {
                    val exp = java.time.LocalDate.parse(item.expiryDate.take(10))
                    !exp.isBefore(today) && !exp.isAfter(threshold)
                }.getOrDefault(false)
            }
        }

        // Advanced: quantity range
        if (current.filterMinQty > 0.0) {
            filtered = filtered.filter { it.quantity >= current.filterMinQty }
        }
        current.filterMaxQty?.let { max ->
            filtered = filtered.filter { it.quantity <= max }
        }

        // Apply sort
        filtered = when (current.selectedSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.receivedAt }
            "date_asc" -> filtered.sortedBy { it.receivedAt }
            "expiry_asc" -> filtered.sortedBy { it.expiryDate }
            "expiry_desc" -> filtered.sortedByDescending { it.expiryDate }
            "distributed_date" -> filtered.sortedByDescending { it.distributedAt ?: "" }
            "name_asc" -> filtered.sortedBy { it.productName }
            "name_desc" -> filtered.sortedByDescending { it.productName }
            else -> filtered
        }

        _state.update { it.copy(filteredItems = filtered) }
    }

    private fun distributeItem(itemId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = distributeInventoryItemUseCase(itemId)
            result.fold(
                onSuccess = {
                    val msg = context.getString(R.string.snack_item_distributed)
                    _uiEvent.send(UiEvent.ShowSnackbar(msg))
                    loadInventory()
                },
                onFailure = { error ->
                    _state.update { it.copy(error = error.message ?: context.getString(R.string.error_distribute_item_failed), isLoading = false) }
                }
            )
        }
    }

    private fun bulkDistribute() {
        val ids = _state.value.selectedIds
            .filter { id -> _state.value.filteredItems.any { it.id == id && it.status.name == "ACTIVE" } }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isBulkOperating = true) }
            var succeeded = 0
            ids.forEach { id ->
                distributeInventoryItemUseCase(id).onSuccess { succeeded++ }
            }
            _state.update { it.copy(isBulkOperating = false, isSelectionMode = false, selectedIds = emptySet()) }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_n_items_distributed, succeeded)))
            loadInventory()
        }
    }

    private fun submitManualAdd() {
        val s = _state.value
        if (s.manualProductName.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingManual = true) }
            // Manual add API not yet implemented — show success for now
            kotlinx.coroutines.delay(500)
            _state.update {
                it.copy(
                    isSubmittingManual = false,
                    showManualAddSheet = false,
                    manualProductName = "",
                    manualCategory = "",
                    manualQuantity = "",
                    manualExpiryDate = ""
                )
            }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_item_added)))
            loadInventory()
        }
    }
}
