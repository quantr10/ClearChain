package com.clearchain.app.presentation.admin.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    onNavigateToRequestDetail: (String) -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
            state = state,
            onEvent = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(TransactionsEvent.HideFilterSheet) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        icon = Icons.Default.FileDownload,
                        contentDescription = stringResource(R.string.export_csv),
                        onClick = { viewModel.onEvent(TransactionsEvent.ExportCsv) }
                    )
                    BadgedBox(
                        badge = {
                            if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                        }
                    ) {
                        ClearChainActionIconButton(
                            icon = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.advanced_filters),
                            onClick = { viewModel.onEvent(TransactionsEvent.ShowFilterSheet) }
                        )
                    }
                }

                FilterChipsRow(
                    filters = listOf(
                        FilterChipData(null, stringResource(R.string.filter_all)),
                        FilterChipData("PENDING", stringResource(R.string.status_pending)),
                        FilterChipData("APPROVED", stringResource(R.string.status_approved)),
                        FilterChipData("READY", stringResource(R.string.status_ready)),
                        FilterChipData("COMPLETED", stringResource(R.string.status_completed))
                    ),
                    selectedFilter = state.selectedStatus,
                    onFilterSelected = { viewModel.onEvent(TransactionsEvent.StatusFilterChanged(it)) }
                )
            }

            ResultsCountAndSort(
                count = state.filteredTransactions.size,
                itemName = "transaction",
                selectedSort = state.selectedSort,
                onSortSelected = { viewModel.onEvent(TransactionsEvent.SortOptionChanged(it)) },
                sortOptions = state.availableSortOptions
            )

            when {
                state.isLoading && state.allTransactions.isEmpty() -> LoadingState()
                state.allTransactions.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Receipt,
                        title = stringResource(R.string.admin_transactions_empty),
                        subtitle = stringResource(R.string.transactions_empty_subtitle)
                    )
                }
                state.filteredTransactions.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.FilterAlt,
                        title = stringResource(R.string.empty_no_transactions_filter),
                        subtitle = stringResource(R.string.empty_try_filters)
                    )
                }
                else -> {
                    HapticPullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.onEvent(TransactionsEvent.RefreshTransactions) }
                    ) {
                        LazyColumn(
                            contentPadding = ScreenPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.filteredTransactions, key = { it.id }) { transaction ->
                                TransactionCard(
                                    transaction = transaction,
                                    isFlagged = transaction.id in state.flaggedIds,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionsFilterSheet(
    state: TransactionsState,
    onEvent: (TransactionsEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        FilterSheetContent {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.advanced_filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (state.activeFilterCount > 0) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.action_clear_all),
                        onClick = { onEvent(TransactionsEvent.ClearAdvancedFilters) }
                    )
                }
            }

            // Dispute — first, since a disputed pickup is what an admin most often hunts for
            FilterSection(title = stringResource(R.string.filter_dispute)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // No "All" chip: leaving both off shows everything, like the NGO "Saved only" toggle.
                    DisputeFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = filter in state.filterDisputes,
                            onClick = { onEvent(TransactionsEvent.ToggleDisputeFilter(filter)) },
                            label = { Text(stringResource(filter.labelRes), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Food category
            FilterSection(title = stringResource(R.string.filter_category)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.filterCategory == null,
                        onClick = { onEvent(TransactionsEvent.FilterCategoryChanged(null)) },
                        label = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelSmall) }
                    )
                    FoodCategory.entries.forEach { category ->
                        FilterChip(
                            selected = state.filterCategory == category.name,
                            onClick = {
                                onEvent(
                                    TransactionsEvent.FilterCategoryChanged(
                                        if (state.filterCategory == category.name) null else category.name
                                    )
                                )
                            },
                            label = { Text(stringResource(category.labelResId), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Pickup date
            FilterSection(title = stringResource(R.string.filter_pickup_date)) {
                DateFilterField(
                    date = state.filterPickupDate,
                    onDateChanged = { onEvent(TransactionsEvent.FilterPickupDateChanged(it)) }
                )
            }

            ExpiresByFilterSection(
                expiresBy = state.filterExpiresBy,
                onExpiresByChanged = { onEvent(TransactionsEvent.FilterExpiresByChanged(it)) }
            )

            ClearChainButton(
                text = stringResource(R.string.action_apply_filters),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Transaction card
// -----------------------------------------------------------------------------

@Composable
private fun TransactionCard(
    transaction: PickupRequest,
    isFlagged: Boolean = false,
    onViewDetail: () -> Unit
) {
    ClearChainCard(
        onClick = onViewDetail,
        border = if (isFlagged) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // -- Header: who gave to whom + status ----------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = stringResource(
                        R.string.label_org_transfer,
                        transaction.groceryName,
                        transaction.ngoName
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PickupStatusBadge(transaction.status)
                    if (isFlagged) {
                        StatusBadge(
                            label = stringResource(R.string.label_overdue),
                            backgroundColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            icon = Icons.Default.Warning
                        )
                    }
                }
            }

            // -- What was moved -----------------------------------------------
            if (transaction.items.isNotEmpty()) {
                RequestItemsPreview(transaction)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            CompactDetailRow(
                icon = Icons.Default.CalendarToday,
                text = stringResource(
                    R.string.label_submitted_on_at,
                    DateTimeUtils.formatDate(transaction.createdAt),
                    DateTimeUtils.formatTime(transaction.createdAt)
                ),
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CompactDetailRow(
                icon = Icons.Default.AccessTime,
                text = stringResource(
                    R.string.label_pickup_on_at,
                    DateTimeUtils.formatDate(transaction.pickupDate),
                    transaction.pickupTime
                ),
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Earliest expiry across the request's items (null on older records)
            ExpiryDetailRow(transaction.listingExpiryDate)

            val handlingParts = buildList {
                if (transaction.requiresRefrigeration) add(stringResource(R.string.note_needs_refrigeration))
                if (transaction.isFragile) add(stringResource(R.string.note_fragile_items))
                if (transaction.isHeavy) add(stringResource(R.string.note_heavy_load))
                transaction.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (handlingParts.isNotEmpty()) {
                CompactDetailRow(
                    icon = Icons.AutoMirrored.Filled.StickyNote2,
                    text = handlingParts.joinToString(" \u00B7 "),
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
