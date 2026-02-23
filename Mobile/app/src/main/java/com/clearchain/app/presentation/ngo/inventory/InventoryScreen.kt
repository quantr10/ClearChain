// ═══════════════════════════════════════════════════════════════════════════════
// InventoryScreen.kt - UPDATED WITH STATUS TABS + CATEGORY FILTERS
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.ngo.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.InventoryItem
import com.clearchain.app.domain.model.InventoryStatus
import com.clearchain.app.presentation.components.FilterSection
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ══════════════════════════════════════════════════════
                // STATUS TABS (Above Search Bar)
                // ══════════════════════════════════════════════════════
                StatusTabs(
                    selectedStatus = state.selectedStatusTab,
                    statusCounts = state.getStatusCounts(),
                    onStatusSelected = { status ->
                        viewModel.onEvent(InventoryEvent.StatusTabChanged(status))
                    }
                )

                // ══════════════════════════════════════════════════════
                // FILTER SECTION (Search + Category Chips + Results/Sort)
                // ══════════════════════════════════════════════════════
                FilterSection(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { 
                        viewModel.onEvent(InventoryEvent.SearchQueryChanged(it)) 
                    },
                    searchPlaceholder = "Search by product name...",
                    selectedSort = state.selectedSort,
                    onSortSelected = { 
                        viewModel.onEvent(InventoryEvent.SortOptionChanged(it)) 
                    },
                    sortOptions = state.availableSortOptions,
                    // CATEGORY CHIPS (Food Categories)
                    filterChips = state.availableCategoryFilters,
                    selectedFilter = state.selectedCategory,
                    onFilterSelected = { 
                        viewModel.onEvent(InventoryEvent.CategoryFilterChanged(it)) 
                    },
                    resultsCount = state.filteredItems.size,
                    itemName = "item"
                )

                // Error Message
                state.error?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.onEvent(InventoryEvent.ClearError) }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // ══════════════════════════════════════════════════════
                // CONTENT
                // ══════════════════════════════════════════════════════
                when {
                    state.isLoading && state.allItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.allItems.isEmpty() && !state.isLoading -> {
                        EmptyState(message = "No items in inventory yet")
                    }

                    state.filteredItems.isEmpty() -> {
                        EmptyState(message = "No items match your filters")
                    }

                    else -> {
                        InventoryList(
                            items = state.filteredItems,
                            onDistributeItem = { itemId ->
                                viewModel.onEvent(InventoryEvent.DistributeItem(itemId))
                            }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// NEW: STATUS TABS COMPONENT
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatusTabs(
    selectedStatus: InventoryStatus?,
    statusCounts: Map<InventoryStatus?, Int>,
    onStatusSelected: (InventoryStatus?) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = when (selectedStatus) {
            null -> 0
            InventoryStatus.ACTIVE -> 1
            InventoryStatus.DISTRIBUTED -> 2
            InventoryStatus.EXPIRED -> 3
        },
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        divider = {}
    ) {
        // All Tab
        Tab(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedStatus == null) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${statusCounts[null] ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )

        // Active Tab
        Tab(
            selected = selectedStatus == InventoryStatus.ACTIVE,
            onClick = { onStatusSelected(InventoryStatus.ACTIVE) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedStatus == InventoryStatus.ACTIVE) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${statusCounts[InventoryStatus.ACTIVE] ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == InventoryStatus.ACTIVE) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )

        // Distributed Tab
        Tab(
            selected = selectedStatus == InventoryStatus.DISTRIBUTED,
            onClick = { onStatusSelected(InventoryStatus.DISTRIBUTED) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Distributed",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedStatus == InventoryStatus.DISTRIBUTED) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${statusCounts[InventoryStatus.DISTRIBUTED] ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == InventoryStatus.DISTRIBUTED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )

        // Expired Tab
        Tab(
            selected = selectedStatus == InventoryStatus.EXPIRED,
            onClick = { onStatusSelected(InventoryStatus.EXPIRED) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Expired",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedStatus == InventoryStatus.EXPIRED) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${statusCounts[InventoryStatus.EXPIRED] ?: 0}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStatus == InventoryStatus.EXPIRED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun InventoryList(
    items: List<InventoryItem>,
    onDistributeItem: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            InventoryItemCard(
                item = item,
                onDistributeItem = onDistributeItem
            )
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    onDistributeItem: (String) -> Unit
) {
    var showDistributeDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = item.status)
            }

            HorizontalDivider()

            // Details
            DetailRow(
                icon = Icons.Default.ShoppingCart,
                label = "Quantity",
                value = "${item.quantity} ${item.unit}"
            )

//            DetailRow(
//                icon = Icons.Default.Store,
//                label = "From",
//                value = item.sourceName
//            )

            DetailRow(
                icon = Icons.Default.DateRange,
                label = "Received",
                value = DateTimeUtils.formatDate(item.receivedAt)
            )

            DetailRow(
                icon = Icons.Default.CalendarToday,
                label = "Expires",
                value = DateTimeUtils.formatDate(item.expiryDate),
                isWarning = item.status == InventoryStatus.ACTIVE
            )

            // Distributed date if applicable
            item.distributedAt?.let { distributedAt ->
                DetailRow(
                    icon = Icons.Default.CheckCircle,
                    label = "Distributed",
                    value = DateTimeUtils.formatDate(distributedAt)
                )
            }

            // Action button for ACTIVE items
            if (item.status == InventoryStatus.ACTIVE) {
                HorizontalDivider()
                Button(
                    onClick = { showDistributeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Distributed")
                }
            }

            // Expired warning
            if (item.status == InventoryStatus.EXPIRED) {
                HorizontalDivider()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "This item has expired and should be disposed of properly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    // Distribute Confirmation Dialog
    if (showDistributeDialog) {
        AlertDialog(
            onDismissRequest = { showDistributeDialog = false },
            title = { Text("Mark as Distributed?") },
            text = { 
                Text("Confirm that ${item.productName} has been distributed to beneficiaries?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDistributeItem(item.id)
                        showDistributeDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDistributeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusBadge(status: InventoryStatus) {
    val (backgroundColor, textColor, label) = when (status) {
        InventoryStatus.ACTIVE -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Active"
        )
        InventoryStatus.DISTRIBUTED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Distributed"
        )
        InventoryStatus.EXPIRED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Expired"
        )
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isWarning: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isWarning && label == "Expires") {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isWarning && label == "Expires") {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📦",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}