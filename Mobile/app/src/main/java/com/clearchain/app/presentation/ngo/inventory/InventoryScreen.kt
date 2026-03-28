// ═══════════════════════════════════════════════════════════════════════════════
// InventoryScreen.kt — Same pattern as MyListingsScreen/ManageRequestsScreen
// Status chips + Category chips + fullscreen error on first load fail
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.ngo.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

private val statusFilters = listOf(
    FilterChipData(null, "All"),
    FilterChipData("ACTIVE", "Active"),
    FilterChipData("DISTRIBUTED", "Distributed"),
    FilterChipData("EXPIRED", "Expired")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToItemDetail: (String) -> Unit = {},
    viewModel: InventoryViewModel = hiltViewModel()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Inventory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(InventoryEvent.RefreshInventory) }
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                // ── 1. First load → fullscreen spinner ──────────────
                state.isLoading && state.allItems.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // ── 2. First load error → fullscreen error + Retry ──
                state.error != null && state.allItems.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = "Failed to load inventory",
                        subtitle = state.error,
                        actionLabel = "Retry",
                        onAction = { viewModel.onEvent(InventoryEvent.LoadInventory) }
                    )
                }

                // ── 3. Normal → filters always visible + content ────
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Search bar
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onEvent(InventoryEvent.SearchQueryChanged(it)) },
                            placeholder = "Search by product name...",
                            modifier = Modifier.padding(16.dp)
                        )

                        // Status chips (row 1)
                        FilterChipsRow(
                            filters = statusFilters,
                            selectedFilter = state.selectedStatusTab?.name,
                            onFilterSelected = { value ->
                                val status = value?.let { InventoryStatus.valueOf(it) }
                                viewModel.onEvent(InventoryEvent.StatusTabChanged(status))
                            },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Category chips (row 2)
                        FilterChipsRow(
                            filters = state.availableCategoryFilters,
                            selectedFilter = state.selectedCategory,
                            onFilterSelected = { viewModel.onEvent(InventoryEvent.CategoryFilterChanged(it)) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Results count + Sort
                        ResultsCountAndSort(
                            count = state.filteredItems.size,
                            itemName = "item",
                            selectedSort = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(InventoryEvent.SortOptionChanged(it)) },
                            sortOptions = state.availableSortOptions,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Inline error (after data loaded but action failed)
                        state.error?.let {
                            ErrorBanner(
                                message = it,
                                onDismiss = { viewModel.onEvent(InventoryEvent.ClearError) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        // Content area
                        when {
                            state.filteredItems.isEmpty() -> {
                                EmptyState(
                                    icon = if (state.allItems.isEmpty()) Icons.Default.Inventory2 else Icons.Default.FilterAlt,
                                    title = if (state.allItems.isEmpty()) "No items in inventory"
                                    else "No items match your filters",
                                    subtitle = if (state.allItems.isEmpty())
                                        "Items will appear here after pickup confirmations"
                                    else "Try adjusting your search or filters"
                                )
                            }

                            else -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.filteredItems, key = { it.id }) { item ->
                                        InventoryItemCard(
                                            item = item,
                                            modifier = Modifier.clickable {
                                                onNavigateToItemDetail(item.id)
                                            },
                                            onDistribute = { viewModel.onEvent(InventoryEvent.DistributeItem(it)) }
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
    }
}