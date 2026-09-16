package com.clearchain.app.presentation.ngo

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.ActivityItemData
import com.clearchain.app.data.remote.dto.UpcomingPickupData
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.clearchain.app.ui.theme.ScreenPadding
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NgoDashboardScreen(
    navController: NavController,
    viewModel: NgoDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showActivitySheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        HapticPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardWelcomeHeader(
                    userName = state.userName,
                    subtitle = stringResource(R.string.ngo_dashboard_subtitle),
                    roleLabel = stringResource(R.string.role_ngo),
                    profilePictureUrl = state.profilePictureUrl,
                    gradientColors = listOf(BrandGreen, BrandTeal),
                    onProfileClick = { navController.navigate(Screen.AccountDetail.route) },
                    onNotificationsClick = { navController.navigate(Screen.NotificationInbox.route) }
                )

                Column(
                    modifier = Modifier.padding(ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Impact ───────────────────────────────────────────────
                    // First card on the page: what the organization has actually achieved
                    // leads, before today's workload.
                    DashboardSection(
                        title = stringResource(R.string.analytics_impact),
                        titleStyle = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp)
                    ) {
                        ImpactSummaryRow(
                            kgSaved = state.impact.kgSaved,
                            mealsEstimate = state.impact.mealsProvided,
                            co2EstimateKg = state.impact.co2AvoidedKg
                        )
                    }

                    // ── Nearby Listings Mini-Map ─────────────────────────────
                    val mapListings = state.availableListings.filter {
                        it.groceryLatitude != null && it.groceryLongitude != null
                    }
                    val userLat = state.userLatitude
                    val userLng = state.userLongitude
                    if (userLat != null && userLng != null) {
                        DashboardSection(title = "") {
                            NearbyListingsMiniMap(
                                userLat = userLat,
                                userLng = userLng,
                                listings = mapListings,
                                totalAvailable = state.stats?.availableFood ?: mapListings.size,
                                onViewAll = { navController.navigate(Screen.BrowseListings.route) }
                            )
                        }
                    }

                    // ── Weekly Goal ──────────────────────────────────────────
                    DashboardSection(title = "") {
                        WeeklyGoalCard(
                            completed = state.weeklyCompleted,
                            goal = state.weeklyGoal,
                            progress = state.weeklyProgress
                        )
                    }

                    // ── Upcoming Pickups ─────────────────────────────────────
                    val upcoming = state.todaySummary?.upcomingPickups.orEmpty()
                    if (upcoming.isNotEmpty()) {
                        DashboardSection(title = "") {
                            UpcomingPickupsTimeline(
                                pickups = upcoming,
                                onViewAll = { navController.navigate(Screen.MyRequests.route) }
                            )
                        }
                    }

                    // ── Activity Trend + Recent Activity ─────────────────────
                    val sparklineData = buildDailyActivityCounts(state.activities)
                    DashboardSection(title = "") {
                        ActivitySparklineCard(
                            title = stringResource(R.string.label_actions_this_week),
                            data = sparklineData
                        )
                        if (state.activities.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            ActivityFeedList(activities = state.activities.take(5))
                            if (state.activities.size > 5) {
                                Spacer(Modifier.height(8.dp))
                                ClearChainButton(
                                    text = stringResource(R.string.action_view_more),
                                    onClick = { showActivitySheet = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // ── Quick Actions ────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardActionCard(
                            icon = Icons.Default.RestaurantMenu,
                            title = stringResource(R.string.action_browse_food),
                            subtitle = stringResource(R.string.action_browse_food_subtitle),
                            onClick = { navController.navigate(Screen.BrowseListings.route) }
                        )
                        DashboardActionCard(
                            icon = Icons.Default.LocalShipping,
                            title = stringResource(R.string.action_my_requests),
                            subtitle = stringResource(R.string.action_my_requests_subtitle),
                            onClick = { navController.navigate(Screen.MyRequests.route) }
                        )
                        DashboardActionCard(
                            icon = Icons.Default.Inventory,
                            title = stringResource(R.string.action_inventory),
                            subtitle = stringResource(R.string.action_inventory_subtitle),
                            onClick = { navController.navigate(Screen.Inventory.route) }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // ── Activity bottom sheet ────────────────────────────────────────────────
    if (showActivitySheet) {
        ActivityHistorySheet(
            activities = state.activities,
            onDismiss = { showActivitySheet = false }
        )
    }
}

// ── Activity History Bottom Sheet (shared between NGO and Grocery) ───────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityHistorySheet(
    activities: List<ActivityItemData>,
    onDismiss: () -> Unit
) {
    var showAll by remember { mutableStateOf(false) }
    val displayed = if (showAll) activities else activities.take(20)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.section_activity_trend),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.close))
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(displayed) { index, item ->
                    ActivityFeedItemWithDivider(
                        item = item,
                        showDivider = index < displayed.lastIndex
                    )
                }
                if (!showAll && activities.size > 20) {
                    item {
                        ClearChainButton(
                            text = stringResource(R.string.action_view_more),
                            onClick = { showAll = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ── Upcoming Pickups Timeline ────────────────────────────────────────────────

@Composable
private fun UpcomingPickupsTimeline(
    pickups: List<UpcomingPickupData>,
    onViewAll: () -> Unit
) {
    // Rendered straight onto the DashboardSection that hosts it — a Card here would
    // put a second rounded, elevated surface inside the section's own.
    Column(modifier = Modifier.fillMaxWidth()) {
        pickups.take(3).forEachIndexed { index, pickup ->
            PickupTimelineItem(pickup = pickup, isLast = index == minOf(pickups.size, 3) - 1)
        }
        if (pickups.size > 3) {
            ClearChainOutlinedButton(
                text = stringResource(R.string.view_all_pickups, pickups.size),
                onClick = onViewAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fillMaxWidth = true
            )
        }
    }
}

@Composable
private fun PickupTimelineItem(pickup: UpcomingPickupData, isLast: Boolean) {
    val statusColor = when (pickup.status.uppercase()) {
        "APPROVED" -> BrandGreen
        "READY" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    val approvedLabel = stringResource(R.string.status_approved)
    val readyLabel = stringResource(R.string.status_ready)
    val pendingLabel = stringResource(R.string.status_pending)
    val statusLabel = when (pickup.status.uppercase()) {
        "APPROVED" -> approvedLabel
        "READY" -> readyLabel
        "PENDING" -> pendingLabel
        else -> pickup.status
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pickup.listingTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = pickup.groceryName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = pickup.pickupTime,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (!isLast) {
        HorizontalDivider(modifier = Modifier.padding(start = 38.dp), thickness = 0.5.dp)
    }
}

// ── Nearby Listings Mini-Map ─────────────────────────────────────────────────

@Composable
private fun NearbyListingsMiniMap(
    userLat: Double,
    userLng: Double,
    listings: List<Listing>,
    totalAvailable: Int,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val center = LatLng(userLat, userLng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 12f)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // A clipped Box rather than a Card: the DashboardSection around it already
        // supplies the surface and elevation, so all this needs is rounded corners
        // for the map itself.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    tiltGesturesEnabled = false
                ),
                properties = MapProperties()
            ) {
                // User location marker (azure)
                Marker(
                    state = MarkerState(position = center),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    title = stringResource(R.string.label_your_location)
                )

                // Grocery pins grouped by location
                val groceryPins = remember(listings) {
                    listings.groupBy { LatLng(it.groceryLatitude!!, it.groceryLongitude!!) }
                        .mapValues { (_, list) ->
                            Triple(
                                list.first().groceryName,
                                list.first().groceryProfilePictureUrl,
                                list.size
                            )
                        }
                }
                groceryPins.forEach { (pos, pin) ->
                    val (name, avatarUrl, count) = pin
                    // A marker is rasterised once per key set, so an AsyncImage inside it
                    // would snapshot before the avatar arrives. Load it here instead and
                    // key the marker on the result so the pin is redrawn once it lands.
                    val avatarPainter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .size(AVATAR_PIN_PX)
                            // The marker is drawn onto a software canvas, which rejects
                            // Coil's default hardware bitmaps outright.
                            .allowHardware(false)
                            .build()
                    )
                    val avatarReady = avatarPainter.state is AsyncImagePainter.State.Success
                    MarkerComposable(
                        keys = arrayOf(pos.latitude, pos.longitude, count, avatarReady),
                        state = MarkerState(position = pos)
                    ) {
                        GroceryPinContent(
                            name = name,
                            avatar = avatarPainter.takeIf { avatarReady },
                            count = count
                        )
                    }
                }
            }

            // Count badge — uses the exact total from backend stats
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.RestaurantMenu,
                        null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        stringResource(R.string.label_n_available, totalAvailable),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        ClearChainButton(
            text = stringResource(R.string.action_browse_all_listings),
            onClick = onViewAll,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Activity Feed Item ───────────────────────────────────────────────────────

@Composable
internal fun ActivityFeedItem(item: ActivityItemData) {
    val icon = when (item.type) {
        "listing_created" -> Icons.Default.AddCircle
        "pickup_request" -> Icons.Default.LocalShipping
        "pickup_approved" -> Icons.Default.CheckCircle
        "pickup_completed" -> Icons.Default.Done
        "pickup_cancelled" -> Icons.Default.Cancel
        "inventory_received" -> Icons.Default.Inventory
        else -> Icons.Default.Info
    }
    val iconColor = when (item.type) {
        "listing_created" -> MaterialTheme.colorScheme.primary
        "pickup_completed" -> MaterialTheme.colorScheme.primary
        "pickup_cancelled" -> MaterialTheme.colorScheme.error
        "pickup_approved" -> MaterialTheme.colorScheme.tertiary
        "inventory_received" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconContainerColor = when (item.type) {
        "pickup_cancelled" -> MaterialTheme.colorScheme.errorContainer
        else -> iconColor.copy(alpha = 0.12f)
    }
    val timeLabel = remember(item.timestamp) {
        try {
            OffsetDateTime.parse(item.timestamp)
                .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
        } catch (_: Exception) {
            ""
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = iconContainerColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (timeLabel.isNotEmpty()) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }
    }
}

// ── Grocery Pin Content (for map markers) ────────────────────────────────────

@Composable
internal fun ActivityFeedList(
    activities: List<ActivityItemData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        activities.forEachIndexed { index, item ->
            ActivityFeedItemWithDivider(
                item = item,
                showDivider = index < activities.lastIndex
            )
        }
    }
}

@Composable
private fun ActivityFeedItemWithDivider(
    item: ActivityItemData,
    showDivider: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ActivityFeedItem(item = item)
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 38.dp, top = 8.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private const val AVATAR_PIN_PX = 132

@Composable
private fun GroceryPinContent(name: String, avatar: Painter?, count: Int) {
    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.padding(4.dp)) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(2.dp, Color.White)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (avatar != null) {
                    Image(
                        painter = avatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
