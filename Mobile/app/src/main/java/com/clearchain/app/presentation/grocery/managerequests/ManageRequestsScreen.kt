package com.clearchain.app.presentation.grocery.managerequests

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManageRequestsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRequestDetail: (String) -> Unit = {},
    viewModel: ManageRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBulkRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    if (showBulkRejectDialog) {
        AlertDialog(
            onDismissRequest = { showBulkRejectDialog = false },
            title = { Text(stringResource(R.string.manage_reject_title, state.selectedCount)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.manage_reject_optional_reason))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text(stringResource(R.string.manage_reject_reason_label)) },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showBulkRejectDialog = false
                    viewModel.onEvent(ManageRequestsEvent.BulkReject(rejectReason.ifBlank { null }))
                    rejectReason = ""
                }) { Text(stringResource(R.string.action_reject_all), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showBulkRejectDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    BackHandler(state.isSelectionMode) { viewModel.onEvent(ManageRequestsEvent.ToggleSelectionMode) }

    if (state.showFilterSheet) {
        ManageRequestsFilterSheet(
            state     = state,
            onEvent   = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(ManageRequestsEvent.HideFilterSheet) }
        )
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = state.isSelectionMode && state.selectedCount > 0,
                enter   = slideInVertically { it },
                exit    = slideOutVertically { it }
            ) {
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick  = { viewModel.onEvent(ManageRequestsEvent.BulkApprove) },
                            modifier = Modifier.weight(1f),
                            enabled  = !state.isBulkOperating && state.pendingSelectedCount > 0
                        ) {
                            if (state.isBulkOperating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.action_approve_bulk, state.pendingSelectedCount))
                            }
                        }
                        OutlinedButton(
                            onClick  = { showBulkRejectDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled  = !state.isBulkOperating
                        ) {
                            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_reject_bulk, state.selectedCount))
                        }
                    }
                }
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.allRequests.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null && state.allRequests.isEmpty() -> {
                    EmptyState(
                        icon        = Icons.Default.ErrorOutline,
                        title       = stringResource(R.string.msg_failed_load_requests),
                        subtitle    = state.error,
                        actionLabel = stringResource(R.string.retry),
                        onAction    = { viewModel.onEvent(ManageRequestsEvent.LoadRequests) }
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchBar(
                                query       = state.searchQuery,
                                onQueryChange = { viewModel.onEvent(ManageRequestsEvent.SearchQueryChanged(it)) },
                                placeholder = stringResource(R.string.hint_search_by_item_ngo),
                                modifier    = Modifier.weight(1f)
                            )
                            BadgedBox(
                                badge = {
                                    if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                                }
                            ) {
                                ClearChainActionIconButton(
                                    icon               = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.advanced_filters),
                                    onClick            = { viewModel.onEvent(ManageRequestsEvent.ShowFilterSheet) }
                                )
                            }
                        }

                        FilterChipsRow(
                            filters        = state.availableStatusFilters,
                            selectedFilter = state.selectedStatus,
                            onFilterSelected = { viewModel.onEvent(ManageRequestsEvent.StatusFilterChanged(it)) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        if (state.isSelectionMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    if (state.allSelected) viewModel.onEvent(ManageRequestsEvent.DeselectAll)
                                    else viewModel.onEvent(ManageRequestsEvent.SelectAll)
                                }) {
                                    Text(if (state.allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text  = "${state.filteredRequests.size} request${if (state.filteredRequests.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            SortDropdown(
                                selectedSort   = state.selectedSort,
                                onSortSelected = { viewModel.onEvent(ManageRequestsEvent.SortOptionChanged(it)) },
                                sortOptions    = state.availableSortOptions
                            )
                        }

                        state.error?.let {
                            ErrorBanner(
                                message   = it,
                                onDismiss = { viewModel.onEvent(ManageRequestsEvent.ClearError) },
                                modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        when {
                            state.filteredRequests.isEmpty() -> {
                                EmptyState(
                                    icon     = if (state.allRequests.isEmpty()) Icons.Default.Inbox else Icons.Default.FilterAlt,
                                    title    = if (state.allRequests.isEmpty()) stringResource(R.string.empty_no_pickup_requests)
                                               else stringResource(R.string.empty_no_requests_filter),
                                    subtitle = if (state.allRequests.isEmpty()) stringResource(R.string.empty_requests_grocery_subtitle)
                                               else stringResource(R.string.empty_try_filters)
                                )
                            }

                            else -> {
                                HapticPullToRefreshBox(
                                    isRefreshing = state.isRefreshing,
                                    onRefresh    = { viewModel.onEvent(ManageRequestsEvent.RefreshRequests) }
                                ) {
                                    LazyColumn(
                                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(state.filteredRequests, key = { it.id }) { request ->
                                            val isSelected = request.id in state.selectedIds

                                            Box {
                                                RequestCard(
                                                    request  = request,
                                                    modifier = Modifier.combinedClickable(
                                                        onClick = {
                                                            if (state.isSelectionMode) {
                                                                viewModel.onEvent(ManageRequestsEvent.ToggleItemSelection(request.id))
                                                            } else {
                                                                onNavigateToRequestDetail(request.id)
                                                            }
                                                        },
                                                        onLongClick = {
                                                            viewModel.onEvent(ManageRequestsEvent.ToggleSelectionMode)
                                                            viewModel.onEvent(ManageRequestsEvent.ToggleItemSelection(request.id))
                                                        }
                                                    ),
                                                    viewMode = RequestViewMode.GROCERY,
                                                    onApprove = if (!state.isSelectionMode) {
                                                        { viewModel.onEvent(ManageRequestsEvent.ApproveRequest(it)) }
                                                    } else null,
                                                    onReject = if (!state.isSelectionMode) {
                                                        { viewModel.onEvent(ManageRequestsEvent.RejectRequest(it)) }
                                                    } else null,
                                                    onMarkReady = if (!state.isSelectionMode) {
                                                        { viewModel.onEvent(ManageRequestsEvent.MarkReady(it)) }
                                                    } else null
                                                )
                                                if (state.isSelectionMode) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { viewModel.onEvent(ManageRequestsEvent.ToggleItemSelection(request.id)) },
                                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                                    )
                                                }
                                            }
                                        }

                                        item { Spacer(Modifier.height(16.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Advanced filter sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ManageRequestsFilterSheet(
    state: ManageRequestsState,
    onEvent: (ManageRequestsEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.advanced_filters),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.activeFilterCount > 0) {
                    TextButton(onClick = { onEvent(ManageRequestsEvent.ClearAdvancedFilters) }) {
                        Text(stringResource(R.string.action_clear_all))
                    }
                }
            }

            // Listing category
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.filter_listing_category), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.filterCategory == null,
                        onClick  = { onEvent(ManageRequestsEvent.FilterCategoryChanged(null)) },
                        label    = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelSmall) }
                    )
                    FoodCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.filterCategory == cat.name,
                            onClick  = { onEvent(ManageRequestsEvent.FilterCategoryChanged(if (state.filterCategory == cat.name) null else cat.name)) },
                            label    = { Text(stringResource(cat.labelResId), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Pickup date preset
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.filter_pickup_date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Any", "today" to "Today", "this_week" to "This week", "next_30" to "Next 30 days")
                        .forEach { (preset, label) ->
                            FilterChip(
                                selected = state.filterPickupDatePreset == preset,
                                onClick  = { onEvent(ManageRequestsEvent.FilterPickupDatePresetChanged(preset)) },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_apply_filters))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
