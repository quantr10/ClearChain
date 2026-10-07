package com.clearchain.app.presentation.ngo.cart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.CartGroupData
import com.clearchain.app.data.remote.dto.CartItemData
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.UiEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

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
                                        onGroceryClick = { groceryId -> onNavigate(Screen.PublicProfile.createRoute(groceryId)) },
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
    onGroceryClick: (String) -> Unit,
    onRequestPickup: () -> Unit
) {
    ClearChainCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGroceryClick(group.groceryId) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvatarImage(
                    imageUrl = group.groceryProfilePictureUrl,
                    name = group.groceryName,
                    size = 38
                )
                Text(
                    group.groceryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${group.items.size} item${if (group.items.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
    val expiryColor = cartItemExpiryColor(item.expiryDate)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    style = MaterialTheme.typography.bodyMedium,
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
                        tint = expiryColor
                    )
                    Text(
                        text = stringResource(
                            R.string.label_expires_date,
                            item.expiryDate?.let { DateTimeUtils.formatDate(it) } ?: "N/A"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = expiryColor,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            ClearChainQuantityStepper(
                quantity = item.requestedQuantity,
                unit = item.unit,
                canIncrement = item.requestedQuantity.toDouble() < item.maxQuantity,
                enabled = item.isValid,
                onDecrement = { onEvent(CartEvent.DecrementItem(item.id, item.requestedQuantity)) },
                onIncrement = { onEvent(CartEvent.IncrementItem(item.id, item.requestedQuantity)) },
                expanded = false,
                buttonSize = 24.dp
            )
            ClearChainActionIconButton(
                icon = Icons.Default.Delete,
                contentDescription = stringResource(R.string.cart_remove),
                onClick = { onEvent(CartEvent.RemoveItem(item.id)) },
                tint = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun cartItemExpiryColor(expiryDate: String?): Color {
    val expiry = expiryDate?.takeIf { it.isNotBlank() } ?: return MaterialTheme.colorScheme.onSurfaceVariant
    val daysUntilExpiry = remember(expiry) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiry)!!
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            TimeUnit.MILLISECONDS.toDays(date.time - today.time)
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }
    return when {
        daysUntilExpiry <= 0L -> MaterialTheme.colorScheme.error
        daysUntilExpiry <= 3L -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
