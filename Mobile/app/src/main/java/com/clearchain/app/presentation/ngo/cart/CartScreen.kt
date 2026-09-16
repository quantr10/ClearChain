package com.clearchain.app.presentation.ngo.cart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.ScreenPadding
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.CartGroupData
import com.clearchain.app.data.remote.dto.CartItemData
import com.clearchain.app.presentation.components.ClearChainActionIconButton
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.presentation.components.ClearChainQuantityStepper
import com.clearchain.app.presentation.components.EmptyState
import com.clearchain.app.presentation.components.AvatarImage
import com.clearchain.app.presentation.components.ProductThumbnail
import com.clearchain.app.presentation.components.ListHeaderSearchRow
import com.clearchain.app.presentation.components.ListScreenHeader
import com.clearchain.app.presentation.components.ResultsCountAndSort
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent

@Composable
fun CartScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: CartViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val visibleGroups = remember(state.groups, state.searchQuery, state.selectedSort) {
        state.groups
            .filterCartGroups(state.searchQuery)
            .sortCartGroups(state.selectedSort.value)
    }
    val visibleItemCount = visibleGroups.sumOf { it.items.size }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Navigate -> onNavigate(event.route)
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                    ListScreenHeader {
                    ListHeaderSearchRow(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.onEvent(CartEvent.SearchQueryChanged(it)) },
                        placeholder = stringResource(R.string.hint_search_cart)
                    )

                    ResultsCountAndSort(
                        count = visibleItemCount,
                        itemName = "listing",
                        selectedSort = state.selectedSort,
                        onSortSelected = { viewModel.onEvent(CartEvent.SortOptionChanged(it)) },
                        sortOptions = state.availableSortOptions
                    )
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        when {
                            state.error != null && state.groups.all { it.items.isEmpty() } -> {
                                EmptyState(
                                    icon = Icons.Default.ErrorOutline,
                                    title = stringResource(R.string.error_generic),
                                    subtitle = state.error,
                                    actionLabel = stringResource(R.string.retry),
                                    onAction = { viewModel.onEvent(CartEvent.LoadCart) }
                                )
                            }
                            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                            visibleGroups.isEmpty() -> {
                                EmptyState(
                                    icon = Icons.Default.ShoppingCart,
                                    title = stringResource(R.string.cart_empty_title),
                                    subtitle = stringResource(R.string.cart_empty_subtitle)
                                )
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = ScreenPadding,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(visibleGroups, key = { it.groceryId }) { group ->
                                        CartGroupCard(
                                            group = group,
                                            state = state,
                                            onEvent = viewModel::onEvent,
                                            onListingClick = { listingId -> onNavigate(Screen.ListingDetail.createRoute(listingId)) },
                                            onRequestPickup = { onNavigate(Screen.CartPickup.createRoute(group.groceryId)) }
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

@Composable
private fun CartGroupCard(
    group: CartGroupData,
    state: CartState,
    onEvent: (CartEvent) -> Unit,
    onListingClick: (String) -> Unit,
    onRequestPickup: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvatarImage(
                    imageUrl = group.groceryProfilePictureUrl,
                    name = group.groceryName,
                    size = 32
                )
                Text(
                    group.groceryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            group.items.forEachIndexed { index, item ->
                CartItemRow(
                    item = item,
                    showDivider = index != group.items.lastIndex,
                    onEvent = onEvent,
                    onListingClick = { onListingClick(item.listingId) }
                )
            }
            ClearChainButton(
                text = stringResource(R.string.request_pickup),
                onClick = onRequestPickup,
                enabled = group.canCheckout && !state.isSubmitting,
                icon = Icons.Default.LocalShipping
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItemData,
    showDivider: Boolean,
    onEvent: (CartEvent) -> Unit,
    onListingClick: () -> Unit
) {
    val textColor = if (item.isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onListingClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProductThumbnail(
                    imageUrl = item.imageUrl,
                    contentDescription = item.title,
                    size = 48.dp
                )
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.label_expires_date, item.expiryDate ?: "N/A"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    item.invalidReason?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            ClearChainActionIconButton(
                icon = Icons.Default.Delete,
                contentDescription = stringResource(R.string.cart_remove),
                onClick = { onEvent(CartEvent.RemoveItem(item.id)) },
                tint = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        }
        ClearChainQuantityStepper(
            quantity = item.requestedQuantity,
            unit = item.unit,
            canIncrement = item.requestedQuantity.toDouble() < item.maxQuantity,
            enabled = item.isValid,
            onDecrement = { onEvent(CartEvent.DecrementItem(item.id, item.requestedQuantity)) },
            onIncrement = { onEvent(CartEvent.IncrementItem(item.id, item.requestedQuantity)) }
        )
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

private fun List<CartGroupData>.filterCartGroups(query: String): List<CartGroupData> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return mapNotNull { group ->
        val groceryMatches = group.groceryName.contains(normalized, ignoreCase = true)
        val matchingItems = group.items.filter {
            groceryMatches ||
                it.title.contains(normalized, ignoreCase = true) ||
                it.category.contains(normalized, ignoreCase = true)
        }
        if (matchingItems.isEmpty()) null else group.copy(items = matchingItems)
    }
}

private fun List<CartGroupData>.sortCartGroups(sortValue: String): List<CartGroupData> {
    return when (sortValue) {
        "name_desc" -> sortedByDescending { it.groceryName.lowercase() }
        "expiry_asc" -> sortedBy { it.earliestExpiryDate ?: "9999-12-31" }
        "expiry_desc" -> sortedByDescending { it.earliestExpiryDate ?: "" }
        else -> sortedBy { it.groceryName.lowercase() }
    }
}
