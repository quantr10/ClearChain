package com.clearchain.app.presentation.grocery.managerequests

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.clearchain.app.ui.theme.ScreenPadding
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

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    if (state.showFilterSheet) {
        ManageRequestsFilterSheet(
            state = state,
            onEvent = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(ManageRequestsEvent.HideFilterSheet) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ListScreenHeader {
                    ListHeaderSearchRow(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.onEvent(ManageRequestsEvent.SearchQueryChanged(it)) },
                        placeholder = stringResource(R.string.hint_search_by_item_ngo)
                    ) {
                        BadgedBox(
                            badge = {
                                if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                            }
                        ) {
                            ClearChainActionIconButton(
                                icon = Icons.Default.Tune,
                                contentDescription = stringResource(R.string.advanced_filters),
                                onClick = { viewModel.onEvent(ManageRequestsEvent.ShowFilterSheet) }
                            )
                        }
                    }

                    FilterChipsRow(
                        filters = state.availableStatusFilters,
                        selectedFilter = state.selectedStatus,
                        onFilterSelected = { viewModel.onEvent(ManageRequestsEvent.StatusFilterChanged(it)) }
                    )

                    ResultsCountAndSort(
                        count = state.filteredRequests.size,
                        itemName = "request",
                        selectedSort = state.selectedSort,
                        onSortSelected = { viewModel.onEvent(ManageRequestsEvent.SortOptionChanged(it)) },
                        sortOptions = state.availableSortOptions
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when {
                        state.isLoading && state.allRequests.isEmpty() -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        state.error != null && state.allRequests.isEmpty() -> {
                            EmptyState(
                                icon = Icons.Default.ErrorOutline,
                                title = stringResource(R.string.error_generic),
                                subtitle = state.error,
                                actionLabel = stringResource(R.string.retry),
                                onAction = { viewModel.onEvent(ManageRequestsEvent.LoadRequests) }
                            )
                        }

                        state.filteredRequests.isEmpty() -> {
                            EmptyState(
                                icon = if (state.allRequests.isEmpty()) Icons.Default.Inbox else Icons.Default.FilterAlt,
                                title = if (state.allRequests.isEmpty()) {
                                    stringResource(R.string.empty_no_pickup_requests)
                                } else {
                                    stringResource(R.string.empty_no_requests_filter)
                                },
                                subtitle = if (state.allRequests.isEmpty()) {
                                    stringResource(R.string.empty_requests_grocery_subtitle)
                                } else {
                                    stringResource(R.string.empty_try_filters)
                                }
                            )
                        }

                        else -> {
                            HapticPullToRefreshBox(
                                isRefreshing = state.isRefreshing,
                                onRefresh = { viewModel.onEvent(ManageRequestsEvent.RefreshRequests) }
                            ) {
                                LazyColumn(
                                    contentPadding = ScreenPadding,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(state.filteredRequests, key = { it.id }) { request ->
                                        Box {
                                            RequestCard(
                                                request = request,
                                                modifier = Modifier.clickable { onNavigateToRequestDetail(request.id) },
                                                viewMode = RequestViewMode.GROCERY,
                                                onApprove = { viewModel.onEvent(ManageRequestsEvent.ApproveRequest(it)) },
                                                onReject = { viewModel.onEvent(ManageRequestsEvent.RejectRequest(it)) },
                                                onMarkReady = { viewModel.onEvent(ManageRequestsEvent.MarkReady(it)) }
                                            )
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

// ── Advanced filter sheet ────────────────────────────────────────────────────

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        onClick = { onEvent(ManageRequestsEvent.ClearAdvancedFilters) }
                    )
                }
            }

            // Listing category
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.filter_listing_category), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.filterCategory == null,
                        onClick = { onEvent(ManageRequestsEvent.FilterCategoryChanged(null)) },
                        label = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelSmall) }
                    )
                    FoodCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.filterCategory == cat.name,
                            onClick = { onEvent(ManageRequestsEvent.FilterCategoryChanged(if (state.filterCategory == cat.name) null else cat.name)) },
                            label = { Text(stringResource(cat.labelResId), style = MaterialTheme.typography.labelSmall) }
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
                                onClick = { onEvent(ManageRequestsEvent.FilterPickupDatePresetChanged(preset)) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                }
            }

            ClearChainButton(
                text = stringResource(R.string.action_apply_filters),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
