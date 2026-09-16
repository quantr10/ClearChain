package com.clearchain.app.presentation.admin.transactions

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRequestDetail: (String) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    if (state.showFilterSheet) {
        TransactionsFilterSheet(
            state     = state,
            onEvent   = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(TransactionsEvent.HideFilterSheet) }
        )
    }

    // Date picker dialog
    if (state.showDatePickerDialog) {
        DatePickerForTransaction(
            isStart   = state.datePickerForStart,
            initial   = if (state.datePickerForStart) state.filterStartDate else state.filterEndDate,
            onConfirm = { viewModel.onEvent(TransactionsEvent.CustomDateSelected(it)) },
            onDismiss = { viewModel.onEvent(TransactionsEvent.HideDatePicker) }
        )
    }

    // Export dialog
    if (state.showExportDialog) {
        ExportDialog(
            csvText = state.exportCsvText,
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, state.exportCsvText)
                    putExtra(Intent.EXTRA_SUBJECT, "ClearChain Transaction Export")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share transactions"))
                viewModel.onEvent(TransactionsEvent.DismissExportDialog)
            },
            onDismiss = { viewModel.onEvent(TransactionsEvent.DismissExportDialog) }
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListScreenHeader {
            ListHeaderSearchRow(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(TransactionsEvent.SearchQueryChanged(it)) },
                placeholder = stringResource(R.string.search_transactions_placeholder)
            ) {
                ClearChainActionIconButton(
                    icon               = Icons.Default.FileDownload,
                    contentDescription = stringResource(R.string.export_csv),
                    onClick            = { viewModel.onEvent(TransactionsEvent.ShowExportDialog) }
                )
                BadgedBox(
                    badge = {
                        if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                    }
                ) {
                    ClearChainActionIconButton(
                        icon               = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.advanced_filters),
                        onClick            = { viewModel.onEvent(TransactionsEvent.ShowFilterSheet) }
                    )
                }
            }

            FilterChipsRow(
                filters = listOf(
                    FilterChipData(null,        stringResource(R.string.filter_all)),
                    FilterChipData("PENDING",   stringResource(R.string.status_pending)),
                    FilterChipData("APPROVED",  stringResource(R.string.status_approved)),
                    FilterChipData("READY",     stringResource(R.string.status_ready)),
                    FilterChipData("COMPLETED", stringResource(R.string.status_completed))
                ),
                selectedFilter = state.selectedStatus,
                onFilterSelected = { viewModel.onEvent(TransactionsEvent.StatusFilterChanged(it)) }
            )
            }

            ResultsCountAndSort(
                count          = state.filteredTransactions.size,
                itemName       = "transaction",
                selectedSort   = state.selectedSort,
                onSortSelected = { viewModel.onEvent(TransactionsEvent.SortOptionChanged(it)) },
                sortOptions    = state.availableSortOptions
            )

            when {
                state.isLoading && state.allTransactions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.allTransactions.isEmpty() -> {
                    EmptyState(
                        icon     = Icons.Default.Receipt,
                        title    = stringResource(R.string.admin_transactions_empty),
                        subtitle = stringResource(R.string.transactions_empty_subtitle)
                    )
                }
                state.filteredTransactions.isEmpty() -> {
                    EmptyState(
                        icon     = Icons.Default.FilterAlt,
                        title    = stringResource(R.string.empty_no_transactions_filter),
                        subtitle = stringResource(R.string.empty_try_filters)
                    )
                }
                else -> {
                    HapticPullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh    = { viewModel.onEvent(TransactionsEvent.RefreshTransactions) }
                    ) {
                        LazyColumn(
                            contentPadding      = ScreenPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.filteredTransactions, key = { it.id }) { transaction ->
                                TransactionCard(
                                    transaction  = transaction,
                                    isFlagged    = transaction.id in state.flaggedIds,
                                    onViewDetail = { onNavigateToRequestDetail(transaction.id) }
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Transactions filter bottom sheet
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsFilterSheet(
    state:     TransactionsState,
    onEvent:   (TransactionsEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.advanced_filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (state.activeFilterCount > 0) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.action_clear_all),
                        onClick = { onEvent(TransactionsEvent.DatePresetSelected(null)) }
                    )
                }
            }

            FilterSection(title = stringResource(R.string.filter_date_range)) {
                DateRangeFilterRow(state = state, onEvent = onEvent)
            }

            ClearChainButton(
                text = stringResource(R.string.action_apply_filters),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Date range filter row
// -----------------------------------------------------------------------------

@Composable
private fun DateRangeFilterRow(
    state:   TransactionsState,
    onEvent: (TransactionsEvent) -> Unit
) {
    val presets = listOf(
        null    to stringResource(R.string.preset_all_time),
        "TODAY" to stringResource(R.string.preset_today),
        "WEEK"  to stringResource(R.string.preset_this_week),
        "MONTH" to stringResource(R.string.preset_this_month),
        "CUSTOM" to stringResource(R.string.preset_custom)
    )
    Row(
        modifier              = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.DateRange, null,
            modifier = Modifier.size(16.dp),
            tint     = MaterialTheme.colorScheme.onSurfaceVariant
        )
        presets.forEach { (key, label) ->
            if (key == "CUSTOM") {
                FilterChip(
                    selected = state.selectedDatePreset == "CUSTOM",
                    onClick  = {
                        if (state.selectedDatePreset == "CUSTOM") {
                            onEvent(TransactionsEvent.DatePresetSelected(null))
                        } else {
                            onEvent(TransactionsEvent.ShowDatePicker(forStart = true))
                        }
                    },
                    label    = {
                        val label2 = when {
                            state.selectedDatePreset == "CUSTOM" && state.filterStartDate != null && state.filterEndDate != null ->
                                "${state.filterStartDate!!.takeLast(5)} \u2013 ${state.filterEndDate!!.takeLast(5)}"
                            state.selectedDatePreset == "CUSTOM" && state.filterStartDate != null ->
                                "From ${state.filterStartDate!!.takeLast(5)}"
                            else -> label
                        }
                        Text(label2)
                    },
                    leadingIcon = if (state.selectedDatePreset == "CUSTOM") ({
                        Icon(Icons.Default.EditCalendar, null, Modifier.size(14.dp))
                    }) else null
                )
            } else {
                FilterChip(
                    selected = state.selectedDatePreset == key,
                    onClick  = { onEvent(TransactionsEvent.DatePresetSelected(key)) },
                    label    = { Text(label) }
                )
            }
        }
    }
    // When custom is active, show start/end pickers
    if (state.selectedDatePreset == "CUSTOM") {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            ClearChainOutlinedButton(
                text = state.filterStartDate ?: stringResource(R.string.label_from),
                onClick  = { onEvent(TransactionsEvent.ShowDatePicker(forStart = true)) },
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarToday
            )
            Text("\u2013", style = MaterialTheme.typography.bodyMedium)
            ClearChainOutlinedButton(
                text = state.filterEndDate ?: stringResource(R.string.label_to),
                onClick  = { onEvent(TransactionsEvent.ShowDatePicker(forStart = false)) },
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarToday
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Date picker dialog (used for custom range)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerForTransaction(
    isStart:  Boolean,
    initial:  String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMs = remember(initial) {
        runCatching {
            val ld  = java.time.LocalDate.parse(initial ?: throw Exception())
            ld.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton    = {
            ClearChainOutlinedButton(
                text = stringResource(R.string.ok),
                onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val date = java.time.Instant.ofEpochMilli(ms)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                        onConfirm(date)
                    }
                },
                enabled = pickerState.selectedDateMillis != null
            )
        },
        dismissButton    = {
            ClearChainOutlinedButton(text = stringResource(R.string.cancel), onClick = onDismiss)
        }
    ) {
        DatePicker(
            state    = pickerState,
            headline = { Text(stringResource(if (isStart) R.string.label_select_start_date else R.string.label_select_end_date),
                modifier = Modifier.padding(horizontal = 24.dp)) }
        )
    }
}

// -----------------------------------------------------------------------------
// Transaction card
// -----------------------------------------------------------------------------

@Composable
private fun TransactionCard(
    transaction:  PickupRequest,
    isFlagged:    Boolean = false,
    onViewDetail: () -> Unit
) {
    ClearChainCard(
        onClick = onViewDetail,
        border  = if (isFlagged) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // -- Header: who gave to whom + status ----------------------------
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Text(
                    text       = stringResource(
                        R.string.label_org_transfer,
                        transaction.groceryName,
                        transaction.ngoName
                    ),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PickupStatusBadge(transaction.status)
                    if (isFlagged) {
                        StatusBadge(
                            label           = stringResource(R.string.label_overdue),
                            backgroundColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor    = MaterialTheme.colorScheme.onErrorContainer,
                            icon            = Icons.Default.Warning
                        )
                    }
                }
            }

            // -- What was moved -----------------------------------------------
            if (transaction.items.isNotEmpty()) {
                RequestItemsPreview(transaction)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            RequestDetailRow(
                icon = Icons.Default.CalendarToday,
                text = stringResource(
                    R.string.label_submitted_on_at,
                    DateTimeUtils.formatDate(transaction.createdAt),
                    DateTimeUtils.formatTime(transaction.createdAt)
                )
            )
            RequestDetailRow(
                icon = Icons.Default.AccessTime,
                text = stringResource(
                    R.string.label_pickup_on_at,
                    DateTimeUtils.formatDate(transaction.pickupDate),
                    transaction.pickupTime
                )
            )

            // Earliest expiry across the request's items (null on older records)
            ExpiryDetailRow(transaction.listingExpiryDate)

            val handlingParts = buildList {
                if (transaction.requiresRefrigeration) add(stringResource(R.string.note_needs_refrigeration))
                if (transaction.isFragile)             add(stringResource(R.string.note_fragile_items))
                if (transaction.isHeavy)               add(stringResource(R.string.note_heavy_load))
                transaction.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (handlingParts.isNotEmpty()) {
                RequestDetailRow(
                    icon = Icons.AutoMirrored.Filled.StickyNote2,
                    text = handlingParts.joinToString(" \u00B7 ")
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Export dialog
// -----------------------------------------------------------------------------

@Composable
private fun ExportDialog(
    csvText:  String,
    onShare:  () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onShare,
        icon = Icons.Default.FileDownload,
        title = stringResource(R.string.label_export_transactions),
        message = stringResource(R.string.msg_export_transactions),
        confirmLabel = stringResource(R.string.share),
        confirmIcon = Icons.Default.Share,
        dismissLabel = stringResource(R.string.cancel)
    ) {
        Text(
            stringResource(R.string.label_csv_preview_rows, csvText.lines().size - 1),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            color  = MaterialTheme.colorScheme.surfaceVariant,
            shape  = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Text(
                text     = csvText.lines().take(6).joinToString("\n"),
                style    = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp)
            )
        }
    }
}
