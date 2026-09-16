package com.clearchain.app.presentation.ngo.browselistings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.CartItemData
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.presentation.ngo.locationpicker.LocationPickerScreen
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.UiEvent
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowseListingsScreen(
    navController: NavController,
    viewModel: BrowseListingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocationSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLocationSet, state.isCheckingLocation) {
        if (!state.isCheckingLocation && !state.isLocationSet) {
            showLocationSheet = true
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Navigate    -> navController.navigate(event.route)
                else -> Unit
            }
        }
    }

    if (state.showFilterSheet) {
        AdvancedFilterSheet(
            state     = state,
            onEvent   = { viewModel.onEvent(it) },
            onDismiss = { viewModel.onEvent(BrowseListingsEvent.HideFilterSheet) }
        )
    }

    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (state.isLocationSet) showLocationSheet = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
            ) {
                Text(
                    text = stringResource(R.string.location_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                )
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    LocationPickerScreen(
                        onLocationSelected = { showLocationSheet = false },
                        onDismiss = if (state.isLocationSet) {
                            { showLocationSheet = false }
                        } else {
                            null
                        },
                        showTopBar = false
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {

                        // -- Static header --------------------------------------------------

                        ListScreenHeader {
                        // Row 1: Search + location pin + filter
                        ListHeaderSearchRow(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onEvent(BrowseListingsEvent.SearchQueryChanged(it)) },
                            placeholder = stringResource(R.string.hint_search_by_name_grocery)
                        ) {
                            if (state.isLocationSet) {
                                ClearChainActionIconButton(
                                    icon               = Icons.Default.Place,
                                    contentDescription = stringResource(R.string.cd_change_location),
                                    onClick            = { showLocationSheet = true },
                                    tint               = MaterialTheme.colorScheme.primary
                                )
                            }
                            BadgedBox(
                                badge = {
                                    if (state.cartItemCount > 0) Badge { Text(state.cartItemCount.toString()) }
                                }
                            ) {
                                ClearChainActionIconButton(
                                    icon               = Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    onClick            = { viewModel.onEvent(BrowseListingsEvent.OpenCart) },
                                    tint               = MaterialTheme.colorScheme.primary
                                )
                            }
                            BadgedBox(
                                badge = {
                                    if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                                }
                            ) {
                                ClearChainActionIconButton(
                                    icon               = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.advanced_filters),
                                    onClick            = { viewModel.onEvent(BrowseListingsEvent.ShowFilterSheet) }
                                )
                            }
                        }

                        // Row 2: List | Map tab switcher
                        FilterChipsRow(
                            tabs = listOf(
                                false to stringResource(R.string.tab_list_view),
                                true  to stringResource(R.string.tab_map_view)
                            ),
                            selectedTab = state.showMapView,
                            onTabSelected = { mapView ->
                                if (mapView != state.showMapView) viewModel.onEvent(BrowseListingsEvent.ToggleMapView)
                            }
                        )

                        // Row 3: Sort (list mode only)
                        if (!state.showMapView) {
                            ResultsCountAndSort(
                                count          = state.filteredListings.size,
                                itemName       = "listing",
                                selectedSort   = state.selectedSort,
                                onSortSelected = { viewModel.onEvent(BrowseListingsEvent.SortOptionChanged(it)) },
                                sortOptions    = state.availableSortOptions
                            )
                        }
                        }

                        // -- Content --------------------------------------------------------
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            when {
                                state.isLoading && state.allListings.isEmpty() ->
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                                state.error != null && state.allListings.isEmpty() ->
                                    EmptyState(
                                        icon        = Icons.Default.ErrorOutline,
                                        title       = stringResource(R.string.error_generic),
                                        subtitle    = state.error,
                                        actionLabel = stringResource(R.string.retry),
                                        onAction    = { viewModel.onEvent(BrowseListingsEvent.LoadListings) }
                                    )

                                state.showMapView ->
                                    GroceryMapView(state = state, viewModel = viewModel, navController = navController)

                                else ->
                                    ListingsListView(state = state, viewModel = viewModel, navController = navController)
                            }
                        }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// List view
// -----------------------------------------------------------------------------

@Composable
private fun ListingsListView(
    state:        BrowseListingsState,
    viewModel:    BrowseListingsViewModel,
    navController: NavController
) {
    when {
        state.filteredListings.isEmpty() -> EmptyState(
            icon     = if (state.showFavoritesOnly) Icons.Default.FavoriteBorder
                       else if (state.allListings.isEmpty()) Icons.Default.SearchOff
                       else Icons.Default.FilterAlt,
            title    = if (state.showFavoritesOnly) stringResource(R.string.empty_no_saved_listings)
                       else if (state.allListings.isEmpty()) stringResource(R.string.empty_no_nearby_listings)
                       else stringResource(R.string.empty_no_listings_browse_filter),
            subtitle = if (state.showFavoritesOnly) stringResource(R.string.empty_no_saved_subtitle)
                       else if (state.allListings.isEmpty()) stringResource(R.string.empty_no_nearby_subtitle)
                       else stringResource(R.string.empty_try_filters)
        )

        else -> HapticPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh    = { viewModel.onEvent(BrowseListingsEvent.RefreshListings) }
        ) {
            LazyColumn(
                contentPadding      = ScreenPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = state.filteredListings, key = { it.id }) { listing ->
                    val isFavorited = listing.id in state.favoritedIds
                    val cartItem = state.cartItemsByListingId[listing.id]
                    ListingCard(
                        listing             = listing,
                        showGroceryInfo     = true,
                        modifier            = Modifier.clickable {
                            navController.navigate(Screen.ListingDetail.createRoute(listing.id))
                        },
                        topRightAction      = {
                            IconButton(
                                onClick  = { viewModel.onEvent(BrowseListingsEvent.ToggleFavorite(listing.id)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector        = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorited) stringResource(R.string.cd_remove_from_saved) else stringResource(R.string.cd_save_listing),
                                    tint               = if (isFavorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        },
                        onGroceryAvatarClick = {
                            navController.navigate(Screen.PublicProfile.createRoute(listing.groceryId))
                        },
                        primaryAction       = {
                            ListingCartAction(
                                listing = listing,
                                cartItem = cartItem,
                                enabled = !state.isUpdatingCart,
                                onAddToCart = { viewModel.onEvent(BrowseListingsEvent.AddToCart(it)) },
                                onIncrementCartItem = { viewModel.onEvent(BrowseListingsEvent.IncrementCartItem(it)) },
                                onDecrementCartItem = { viewModel.onEvent(BrowseListingsEvent.DecrementCartItem(it)) }
                            )
                        }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ListingCartAction(
    listing: Listing,
    cartItem: CartItemData?,
    enabled: Boolean,
    onAddToCart: (String) -> Unit,
    onIncrementCartItem: (String) -> Unit,
    onDecrementCartItem: (String) -> Unit
) {
    if (cartItem == null || cartItem.requestedQuantity <= 0) {
        ClearChainButton(
            text = stringResource(R.string.cart_add_to_cart),
            onClick = { onAddToCart(listing.id) },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.ShoppingCart,
            enabled = enabled
        )
    } else {
        ClearChainQuantityStepper(
            quantity = cartItem.requestedQuantity,
            unit = listing.unit,
            canIncrement = cartItem.requestedQuantity < listing.quantity,
            enabled = enabled,
            onDecrement = { onDecrementCartItem(listing.id) },
            onIncrement = { onIncrementCartItem(listing.id) }
        )
    }
}
// -----------------------------------------------------------------------------
// Map view
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryMapView(
    state:         BrowseListingsState,
    viewModel:     BrowseListingsViewModel,
    navController: NavController
) {
    val prefLat  = state.userLat  ?: 10.8231
    val prefLng  = state.userLng  ?: 106.6297
    val prefPos  = remember(prefLat, prefLng) { LatLng(prefLat, prefLng) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(prefPos, 12f)
    }
    val scope = rememberCoroutineScope()

    // Animate to updated preference location
    LaunchedEffect(state.userLat, state.userLng) {
        if (state.userLat != null && state.userLng != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(state.userLat, state.userLng), 12f)
            )
        }
    }

    val grouped = remember(state.mapFilteredListings) {
        state.mapFilteredListings
            .filter { it.groceryLatitude != null && it.groceryLongitude != null }
            .groupBy { "${it.groceryLatitude}_${it.groceryLongitude}" }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier            = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings          = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            properties          = MapProperties()
        ) {
            // Preference location pin (distinct blue marker)
            if (state.userLat != null && state.userLng != null) {
                Marker(
                    state   = MarkerState(position = prefPos),
                    title   = state.locationDisplayName.ifBlank { "My Location" },
                    snippet = stringResource(R.string.label_n_listings_nearby, state.filteredListings.size),
                    icon    = com.google.android.gms.maps.model.BitmapDescriptorFactory
                                  .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
                )
            }

            // Grocery pins with avatar (initial letter) + count badge
            grouped.forEach { (key, group) ->
                val lat         = group.first().groceryLatitude ?: return@forEach
                val lng         = group.first().groceryLongitude ?: return@forEach
                val pos         = LatLng(lat, lng)
                val count       = group.size
                val groceryName = group.first().groceryName

                // A marker is rasterised once per key set, so an AsyncImage inside it
                // would snapshot before the avatar arrives. Load it here instead and
                // key the marker on the result so the pin is redrawn once it lands.
                val avatarPainter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(group.first().groceryProfilePictureUrl)
                        .size(AVATAR_PIN_PX)
                        // The marker is drawn onto a software canvas, which rejects
                        // Coil's default hardware bitmaps outright.
                        .allowHardware(false)
                        .build()
                )
                val avatarReady = avatarPainter.state is AsyncImagePainter.State.Success

                MarkerComposable(
                    keys    = arrayOf(pos.latitude, pos.longitude, count, avatarReady),
                    state   = MarkerState(position = pos),
                    onClick = { viewModel.onEvent(BrowseListingsEvent.GroceryPinTapped(key)); true }
                ) {
                    GroceryPinContent(
                        name   = groceryName,
                        avatar = avatarPainter.takeIf { avatarReady },
                        count  = count
                    )
                }
            }
        }

        // Listing count badge (top-right)
        Surface(
            modifier        = Modifier.align(Alignment.TopEnd).padding(12.dp),
            shape           = RoundedCornerShape(12.dp),
            color           = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.RestaurantMenu, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    text      = "${grouped.values.sumOf { it.size }} listing${if (grouped.values.sumOf { it.size } != 1) "s" else ""}",
                    style     = MaterialTheme.typography.labelMedium,
                    color     = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // "Return to preference location" FAB (bottom-left)
        SmallFloatingActionButton(
            onClick  = {
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(prefPos, 12f))
                }
            },
            modifier          = Modifier.align(Alignment.BottomStart).padding(16.dp).navigationBarsPadding(),
            containerColor    = MaterialTheme.colorScheme.secondaryContainer,
            contentColor      = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(Icons.Default.MyLocation, stringResource(R.string.cd_return_to_location), Modifier.size(20.dp))
        }

        if (state.isLoadingMapListings) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    // Bottom sheet for tapped grocery pin
    state.selectedGroceryKey?.let { key ->
        val listings = grouped[key] ?: emptyList()
        if (listings.isNotEmpty()) {
            GroceryPinSheet(
                listings           = listings,
                favoritedIds       = state.favoritedIds,
                cartItemsByListingId = state.cartItemsByListingId,
                isUpdatingCart     = state.isUpdatingCart,
                onNavigateToDetail = { navController.navigate(Screen.ListingDetail.createRoute(it)) },
                onNavigateToProfile = { navController.navigate(Screen.PublicProfile.createRoute(it)) },
                onAddToCart        = { viewModel.onEvent(BrowseListingsEvent.AddToCart(it)) },
                onIncrementCartItem = { viewModel.onEvent(BrowseListingsEvent.IncrementCartItem(it)) },
                onDecrementCartItem = { viewModel.onEvent(BrowseListingsEvent.DecrementCartItem(it)) },
                onToggleFavorite   = { viewModel.onEvent(BrowseListingsEvent.ToggleFavorite(it)) },
                onDismiss          = { viewModel.onEvent(BrowseListingsEvent.DismissGrocerySheet) }
            )
        }
    }
}

// Custom grocery pin composable content
private const val AVATAR_PIN_PX = 132

@Composable
private fun GroceryPinContent(name: String, avatar: Painter?, count: Int) {
    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.padding(4.dp)) {
        Surface(
            modifier        = Modifier.size(44.dp),
            shape           = CircleShape,
            color           = MaterialTheme.colorScheme.primaryContainer,
            border          = BorderStroke(2.dp, Color.White),
            shadowElevation = 4.dp
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (avatar != null) {
                    Image(
                        painter            = avatar,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Text(
                        text       = name.take(1).uppercase(),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        if (count > 1) {
            Surface(
                modifier = Modifier
                    .size(18.dp)
                    .offset(x = 4.dp, y = (-4).dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text  = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

// Bottom sheet showing listings for a tapped grocery pin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryPinSheet(
    listings:            List<Listing>,
    favoritedIds:        Set<String>,
    cartItemsByListingId: Map<String, CartItemData>,
    isUpdatingCart:      Boolean,
    onNavigateToDetail:  (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onAddToCart:         (String) -> Unit,
    onIncrementCartItem: (String) -> Unit,
    onDecrementCartItem: (String) -> Unit,
    onToggleFavorite:    (String) -> Unit,
    onDismiss:           () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    listings.first().groceryName,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (listings.size > 1) {
                    Text(
                        "${listings.size} listings",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (listings.size == 1) {
                val listing     = listings.first()
                val isFavorited = listing.id in favoritedIds
                val cartItem    = cartItemsByListingId[listing.id]
                ListingCard(
                    listing              = listing,
                    showGroceryInfo      = false,
                    modifier             = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { onNavigateToDetail(listing.id) },
                    topRightAction       = {
                        IconButton(
                            onClick  = { onToggleFavorite(listing.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector        = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint               = if (isFavorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    },
                    onGroceryAvatarClick = { onNavigateToProfile(listing.groceryId) },
                    primaryAction        = {
                        ListingCartAction(
                            listing = listing,
                            cartItem = cartItem,
                            enabled = !isUpdatingCart,
                            onAddToCart = onAddToCart,
                            onIncrementCartItem = onIncrementCartItem,
                            onDecrementCartItem = onDecrementCartItem
                        )
                    }
                )
            } else {
                val pagerState = rememberPagerState { listings.size }
                HorizontalPager(
                    state          = pagerState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing    = 16.dp,
                    modifier       = Modifier.fillMaxWidth()
                ) { page ->
                    val listing     = listings[page]
                    val isFavorited = listing.id in favoritedIds
                    val cartItem    = cartItemsByListingId[listing.id]
                    ListingCard(
                        listing              = listing,
                        showGroceryInfo      = false,
                        modifier             = Modifier.clickable { onNavigateToDetail(listing.id) },
                        topRightAction       = {
                            IconButton(
                                onClick  = { onToggleFavorite(listing.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector        = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint               = if (isFavorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        },
                        onGroceryAvatarClick = { onNavigateToProfile(listing.groceryId) },
                        primaryAction        = {
                            ListingCartAction(
                                listing = listing,
                                cartItem = cartItem,
                                enabled = !isUpdatingCart,
                                onAddToCart = onAddToCart,
                                onIncrementCartItem = onIncrementCartItem,
                                onDecrementCartItem = onDecrementCartItem
                            )
                        }
                    )
                }

                // Page indicator dots
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(listings.size) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (selected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Advanced Filter Bottom Sheet
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AdvancedFilterSheet(
    state:     BrowseListingsState,
    onEvent:   (BrowseListingsEvent) -> Unit,
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
            // Header
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
                        onClick = { onEvent(BrowseListingsEvent.ClearAdvancedFilters) }
                    )
                }
            }

            // Saved listings
            FilterSection(title = stringResource(R.string.saved_listings)) {
                FilterChip(
                    selected = state.showFavoritesOnly,
                    onClick  = { onEvent(BrowseListingsEvent.ToggleFavoritesOnly) },
                    label    = { Text(stringResource(R.string.cd_saved_only), style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            // Category
            FilterSection(title = stringResource(R.string.filter_category)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick  = { onEvent(BrowseListingsEvent.CategoryFilterChanged(null)) },
                        label    = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelSmall) }
                    )
                    FoodCategory.entries.forEach { category ->
                        FilterChip(
                            selected = state.selectedCategory == category.name,
                            onClick  = {
                                onEvent(BrowseListingsEvent.CategoryFilterChanged(
                                    if (state.selectedCategory == category.name) null else category.name
                                ))
                            },
                            label    = { Text(stringResource(category.labelResId), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Quantity range
            FilterSection(title = stringResource(R.string.filter_quantity_range)) {
                val qtyLabel = when {
                    state.filterMinQuantity > 0 && state.filterMaxQuantity != null ->
                        "${state.filterMinQuantity}-${state.filterMaxQuantity} units"
                    state.filterMinQuantity > 0 -> "Min ${state.filterMinQuantity} units"
                    state.filterMaxQuantity != null -> "Up to ${state.filterMaxQuantity} units"
                    else -> stringResource(R.string.filter_any)
                }
                Text(qtyLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                RangeSlider(
                    value         = state.filterMinQuantity.toFloat()..
                                    (state.filterMaxQuantity?.toFloat() ?: 200f),
                    onValueChange = { range ->
                        onEvent(BrowseListingsEvent.FilterMinQuantityChanged(range.start.toInt()))
                        onEvent(BrowseListingsEvent.FilterMaxQuantityChanged(
                            if (range.endInclusive >= 200f) null else range.endInclusive.toInt()))
                    },
                    valueRange = 0f..200f, steps = 19
                )
            }

            // Expiring within
            FilterSection(title = stringResource(R.string.filter_expiry_within)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null to stringResource(R.string.filter_any), 1 to "Today", 3 to "3 days", 7 to "1 week", 14 to "2 weeks")
                        .forEach { (days, label) ->
                            FilterChip(
                                selected = state.filterMaxExpiryDays == days,
                                onClick  = { onEvent(BrowseListingsEvent.FilterMaxExpiryDaysChanged(days)) },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                }
            }

            // Minimum freshness
            FilterSection(title = stringResource(R.string.filter_min_freshness)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to stringResource(R.string.filter_any), 1 to "1+ day", 2 to "2+ days", 3 to "3+ days", 7 to "7+ days")
                        .forEach { (days, label) ->
                            FilterChip(
                                selected = state.filterMinExpiryDays == days,
                                onClick  = { onEvent(BrowseListingsEvent.FilterMinExpiryDaysChanged(days)) },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                }
            }

            // Max distance
            FilterSection(title = stringResource(R.string.filter_max_distance)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null to stringResource(R.string.filter_default), 2 to "2 km", 5 to "5 km", 10 to "10 km", 20 to "20 km")
                        .forEach { (km, label) ->
                            FilterChip(
                                selected = state.filterMaxDistanceKm == km,
                                onClick  = { onEvent(BrowseListingsEvent.FilterMaxDistanceChanged(km)) },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
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
