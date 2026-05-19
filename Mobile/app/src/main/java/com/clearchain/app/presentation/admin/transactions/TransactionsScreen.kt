package com.clearchain.app.presentation.admin.transactions

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
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
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(TransactionsEvent.ShowExportDialog) }) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_export))
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    query         = state.searchQuery,
                    onQueryChange = { viewModel.onEvent(TransactionsEvent.SearchQueryChanged(it)) },
                    placeholder   = stringResource(R.string.search_transactions_placeholder),
                    modifier      = Modifier.weight(1f)
                )
                BadgedBox(
                    badge = {
                        if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                    }
                ) {
                    IconButton(onClick = { viewModel.onEvent(TransactionsEvent.ShowFilterSheet) }) {
                        Icon(Icons.Default.Tune, stringResource(R.string.advanced_filters))
                    }
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
                onFilterSelected = { viewModel.onEvent(TransactionsEvent.StatusFilterChanged(it)) },
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // ── Aggregate stats header ──────────────────────────────────────
            if (state.allTransactions.isNotEmpty()) {
                TransactionStatsHeader(state = state)
            }

            Text(
                text  = "${state.filteredTransactions.size} transaction${if (state.filteredTransactions.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.filteredTransactions, key = { it.id }) { transaction ->
                                ExpandableTransactionCard(
                                    transaction = transaction,
                                    isExpanded  = state.expandedTransactionId == transaction.id,
                                    isFlagged   = transaction.id in state.flaggedIds,
                                    onToggle    = { viewModel.onEvent(TransactionsEvent.ToggleExpanded(transaction.id)) },
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

// ─────────────────────────────────────────────────────────────────────────────
// Transactions filter bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    TextButton(onClick = { onEvent(TransactionsEvent.DatePresetSelected(null)) }) {
                        Text(stringResource(R.string.action_clear_all))
                    }
                }
            }

            FilterSection(title = stringResource(R.string.filter_date_range)) {
                DateRangeFilterRow(state = state, onEvent = onEvent)
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_apply_filters))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Date range filter row
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// Aggregate stats header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransactionStatsHeader(state: TransactionsState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape  = RoundedCornerShape(12.dp),
        color  = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            StatPill(
                label = stringResource(R.string.stat_total),
                value = state.allTransactions.size.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatPill(
                label = stringResource(R.string.status_completed),
                value = state.totalCompleted.toString(),
                color = BrandGreen
            )
            StatPill(
                label = stringResource(R.string.status_pending),
                value = state.totalPending.toString(),
                color = MaterialTheme.colorScheme.secondary
            )
            if (state.flaggedCount > 0) {
                StatPill(
                    label = stringResource(R.string.stat_overdue),
                    value = state.flaggedCount.toString(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

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
                                "${state.filterStartDate!!.takeLast(5)} – ${state.filterEndDate!!.takeLast(5)}"
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
            OutlinedButton(
                onClick  = { onEvent(TransactionsEvent.ShowDatePicker(forStart = true)) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(state.filterStartDate ?: stringResource(R.string.label_from), style = MaterialTheme.typography.labelMedium)
            }
            Text("–", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick  = { onEvent(TransactionsEvent.ShowDatePicker(forStart = false)) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(state.filterEndDate ?: stringResource(R.string.label_to), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Date picker dialog (used for custom range)
// ─────────────────────────────────────────────────────────────────────────────

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
            TextButton(
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
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton    = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    ) {
        DatePicker(
            state    = pickerState,
            headline = { Text(stringResource(if (isStart) R.string.label_select_start_date else R.string.label_select_end_date),
                modifier = Modifier.padding(horizontal = 24.dp)) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Expandable transaction card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpandableTransactionCard(
    transaction:  PickupRequest,
    isExpanded:   Boolean,
    isFlagged:    Boolean = false,
    onToggle:     () -> Unit,
    onViewDetail: () -> Unit
) {
    val statusColor = when (transaction.status) {
        PickupRequestStatus.COMPLETED -> BrandGreen
        PickupRequestStatus.APPROVED  -> MaterialTheme.colorScheme.primary
        PickupRequestStatus.READY     -> MaterialTheme.colorScheme.tertiary
        PickupRequestStatus.PENDING   -> MaterialTheme.colorScheme.secondary
        PickupRequestStatus.CANCELLED,
        PickupRequestStatus.REJECTED  -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFlagged) 3.dp else 1.dp),
        colors    = if (isFlagged)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
        else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header row (always visible) ──────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            transaction.listingTitle,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (isFlagged) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    stringResource(R.string.label_overdue),
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = MaterialTheme.colorScheme.onError,
                                    modifier = androidx.compose.ui.Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "${transaction.groceryName} → ${transaction.ngoName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(
                        label           = stringResource(transaction.status.labelResId),
                        backgroundColor = statusColor.copy(alpha = 0.15f),
                        contentColor    = statusColor
                    )
                    Icon(
                        imageVector  = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                        tint         = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier     = Modifier.size(20.dp)
                    )
                }
            }

            // ── Expanded details ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(
                    modifier            = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(4.dp))
                    InfoRow(Icons.Default.Category,       stringResource(R.string.label_category),  transaction.listingCategory)
                    InfoRow(Icons.Default.Numbers,         stringResource(R.string.listing_quantity), stringResource(R.string.label_quantity_units, transaction.requestedQuantity))
                    InfoRow(Icons.Default.CalendarToday,  stringResource(R.string.label_pickup_short), stringResource(R.string.label_pickup_at, transaction.pickupDate, transaction.pickupTime))
                    InfoRow(Icons.Default.Schedule,       stringResource(R.string.label_submitted), transaction.createdAt.take(10))
                    if (!transaction.notes.isNullOrBlank()) {
                        InfoRow(Icons.Default.Notes, stringResource(R.string.label_notes), transaction.notes)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onViewDetail) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_view_details))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Export dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExportDialog(
    csvText:  String,
    onShare:  () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.FileDownload, null) },
        title = { Text(stringResource(R.string.label_export_transactions)) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "CSV preview (${csvText.lines().size - 1} rows):",
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
                Text(
                    "Tap Share to open in another app (Notes, Files, email, etc.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onShare) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
