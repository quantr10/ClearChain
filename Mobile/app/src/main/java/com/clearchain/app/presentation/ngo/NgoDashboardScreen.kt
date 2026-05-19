package com.clearchain.app.presentation.ngo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.UpcomingPickupData
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.components.buildDailyActivityCounts
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.clearchain.app.data.remote.dto.ActivityItemData
import androidx.compose.foundation.BorderStroke
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
            onRefresh    = { viewModel.refresh() },
            modifier     = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardWelcomeHeader(
                    userName       = state.userName,
                    subtitle       = stringResource(R.string.ngo_dashboard_subtitle),
                    roleLabel      = stringResource(R.string.role_ngo),
                    gradientColors = listOf(BrandGreen, BrandTeal),
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                )

                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // ── Today's summary ────────────────────────────────────
                    state.todaySummary?.let { summary ->
                        DashboardSection(title = stringResource(R.string.section_todays_summary)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TodaySummaryChip(
                                    label    = stringResource(R.string.chip_requests_created),
                                    value    = summary.requestsCreatedToday.toString(),
                                    color    = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                TodaySummaryChip(
                                    label    = stringResource(R.string.chip_pickups),
                                    value    = summary.pickupsToday.toString(),
                                    color    = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                                TodaySummaryChip(
                                    label    = stringResource(R.string.chip_distributed),
                                    value    = summary.distributedToday.toString(),
                                    color    = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // ── Nearby Listings Mini-Map ───────────────────────────
                    val mapListings = state.availableListings.filter {
                        it.groceryLatitude != null && it.groceryLongitude != null
                    }
                    val userLat = state.userLatitude
                    val userLng = state.userLongitude
                    if (userLat != null && userLng != null) {
                        DashboardSection(title = stringResource(R.string.section_nearby_listings)) {
                            NearbyListingsMiniMap(
                                userLat         = userLat,
                                userLng         = userLng,
                                listings        = mapListings,
                                totalAvailable  = state.stats?.availableFood ?: mapListings.size,
                                onViewAll       = { navController.navigate(Screen.BrowseListings.route) }
                            )
                        }
                    }

                    // ── Impact Tracker ─────────────────────────────────────
                    DashboardSection(title = stringResource(R.string.section_your_impact)) {
                        ImpactTrackerCard(impact = state.impact)
                    }

                    // ── Weekly Goal ────────────────────────────────────────
                    DashboardSection(title = stringResource(R.string.section_weekly_goal)) {
                        WeeklyGoalCard(
                            completed = state.weeklyCompleted,
                            goal      = state.weeklyGoal,
                            progress  = state.weeklyProgress
                        )
                    }

                    // ── Upcoming Pickups ───────────────────────────────────
                    val upcoming = state.todaySummary?.upcomingPickups.orEmpty()
                    if (upcoming.isNotEmpty()) {
                        DashboardSection(title = stringResource(R.string.section_todays_pickups)) {
                            UpcomingPickupsTimeline(
                                pickups   = upcoming,
                                onViewAll = { navController.navigate(Screen.MyRequests.route) }
                            )
                        }
                    }

                    // ── Activity Trend + Recent Activity ──────────────────
                    if (state.activities.isNotEmpty()) {
                        val sparklineData = buildDailyActivityCounts(state.activities)
                        DashboardSection(title = stringResource(R.string.section_activity_trend)) {
                            ActivitySparklineCard(
                                title = stringResource(R.string.label_actions_this_week),
                                data  = sparklineData
                            )
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.activities.take(5).forEach { item ->
                                    ActivityFeedItem(item = item)
                                }
                            }
                            if (state.activities.size > 5) {
                                TextButton(
                                    onClick  = { showActivitySheet = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_view_more))
                                }
                            }
                        }
                    }

                    // ── Quick Actions ──────────────────────────────────────
                    DashboardSection(title = stringResource(R.string.section_quick_actions)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashboardActionCard(
                                icon     = Icons.Default.RestaurantMenu,
                                title    = stringResource(R.string.action_browse_food),
                                subtitle = stringResource(R.string.action_browse_food_subtitle),
                                onClick  = { navController.navigate(Screen.BrowseListings.route) }
                            )
                            DashboardActionCard(
                                icon     = Icons.Default.LocalShipping,
                                title    = stringResource(R.string.action_my_requests),
                                subtitle = stringResource(R.string.action_my_requests_subtitle),
                                onClick  = { navController.navigate(Screen.MyRequests.route) }
                            )
                            DashboardActionCard(
                                icon     = Icons.Default.Inventory,
                                title    = stringResource(R.string.action_inventory),
                                subtitle = stringResource(R.string.action_inventory_subtitle),
                                onClick  = { navController.navigate(Screen.Inventory.route) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // ── Activity bottom sheet ──────────────────────────────────────────────
    if (showActivitySheet) {
        ActivityHistorySheet(
            activities = state.activities,
            onDismiss  = { showActivitySheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity History Bottom Sheet (shared between NGO and Grocery)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityHistorySheet(
    activities: List<ActivityItemData>,
    onDismiss:  () -> Unit
) {
    var showAll by remember { mutableStateOf(false) }
    val displayed = if (showAll) activities else activities.take(20)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.section_activity_trend),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.close))
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayed) { item ->
                    ActivityFeedItem(item = item)
                }
                if (!showAll && activities.size > 20) {
                    item {
                        TextButton(
                            onClick  = { showAll = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_view_more))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Impact Tracker Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImpactTrackerCard(impact: ImpactMetrics) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ImpactMetricItem(
                icon  = Icons.Default.Scale,
                value = impact.kgSaved,
                unit  = "kg",
                label = stringResource(R.string.impact_food_saved),
                color = BrandGreen
            )
            VerticalDivider(modifier = Modifier.height(64.dp))
            ImpactMetricItem(
                icon  = Icons.Default.Restaurant,
                value = impact.mealsProvided,
                unit  = "",
                label = stringResource(R.string.impact_meals),
                color = MaterialTheme.colorScheme.tertiary
            )
            VerticalDivider(modifier = Modifier.height(64.dp))
            ImpactMetricItem(
                icon  = Icons.Default.EnergySavingsLeaf,
                value = impact.co2AvoidedKg,
                unit  = "kg",
                label = stringResource(R.string.impact_co2),
                color = BrandTeal
            )
        }
    }
}

@Composable
private fun ImpactMetricItem(
    icon:  ImageVector,
    value: Int,
    unit:  String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier          = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment  = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedCounter(
                count = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            if (unit.isNotEmpty()) {
                Text(
                    text  = " $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Weekly Goal Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyGoalCard(completed: Int, goal: Int, progress: Float) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint   = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text  = stringResource(R.string.weekly_pickups),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text  = "$completed / $goal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress          = { progress },
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                trackColor        = MaterialTheme.colorScheme.surfaceVariant,
                color             = if (progress >= 1f) BrandGreen else MaterialTheme.colorScheme.primary
            )
            Text(
                text  = if (progress >= 1f) stringResource(R.string.goal_reached) else stringResource(R.string.goal_remaining, goal - completed),
                style = MaterialTheme.typography.bodySmall,
                color = if (progress >= 1f) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Upcoming Pickups Timeline
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UpcomingPickupsTimeline(
    pickups:   List<UpcomingPickupData>,
    onViewAll: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            pickups.take(3).forEachIndexed { index, pickup ->
                PickupTimelineItem(pickup = pickup, isLast = index == minOf(pickups.size, 3) - 1)
            }
            if (pickups.size > 3) {
                TextButton(
                    onClick  = onViewAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.view_all_pickups, pickups.size))
                }
            }
        }
    }
}

@Composable
private fun PickupTimelineItem(pickup: UpcomingPickupData, isLast: Boolean) {
    val statusColor = when (pickup.status.uppercase()) {
        "APPROVED" -> BrandGreen
        "READY"    -> MaterialTheme.colorScheme.tertiary
        else       -> MaterialTheme.colorScheme.outline
    }
    val approvedLabel = stringResource(R.string.status_approved)
    val readyLabel    = stringResource(R.string.status_ready)
    val pendingLabel  = stringResource(R.string.status_pending)
    val statusLabel = when (pickup.status.uppercase()) {
        "APPROVED" -> approvedLabel
        "READY"    -> readyLabel
        "PENDING"  -> pendingLabel
        else       -> pickup.status
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
                modifier         = Modifier
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
                text  = pickup.listingTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = pickup.groceryName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = pickup.pickupTime,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text     = statusLabel,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = statusColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (!isLast) {
        HorizontalDivider(modifier = Modifier.padding(start = 38.dp), thickness = 0.5.dp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nearby Listings Mini-Map
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NearbyListingsMiniMap(
    userLat:        Double,
    userLng:        Double,
    listings:       List<Listing>,
    totalAvailable: Int,
    onViewAll:      () -> Unit,
    modifier:       Modifier = Modifier
) {
    val center = LatLng(userLat, userLng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 12f)
    }

    Card(
        shape    = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            GoogleMap(
                modifier            = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings          = MapUiSettings(
                    zoomControlsEnabled    = false,
                    myLocationButtonEnabled = false,
                    scrollGesturesEnabled  = false,
                    zoomGesturesEnabled    = false,
                    rotationGesturesEnabled = false,
                    tiltGesturesEnabled    = false
                ),
                properties = MapProperties()
            ) {
                // User location marker (azure)
                Marker(
                    state = MarkerState(position = center),
                    icon  = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    title = stringResource(R.string.label_your_location)
                )

                // Grocery pins grouped by location
                val groceryPins = remember(listings) {
                    listings.groupBy { LatLng(it.groceryLatitude!!, it.groceryLongitude!!) }
                        .mapValues { (_, list) -> list.first().groceryName to list.size }
                }
                groceryPins.forEach { (pos, nameCount) ->
                    val (name, count) = nameCount
                    MarkerComposable(
                        keys  = arrayOf(pos.latitude, pos.longitude, count),
                        state = MarkerState(position = pos)
                    ) {
                        GroceryPinContent(name = name, count = count)
                    }
                }
            }

            // Count badge — uses the exact total from backend stats
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                shape    = RoundedCornerShape(10.dp),
                color    = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.RestaurantMenu, null,
                        Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        stringResource(R.string.label_n_available, totalAvailable),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // "View all" row
        TextButton(
            onClick  = onViewAll,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(stringResource(R.string.action_browse_all_listings))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowForward, null, Modifier.size(14.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Today Summary Chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodaySummaryChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color    = color.copy(alpha = 0.12f),
        shape    = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = color
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity Feed Item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun ActivityFeedItem(item: ActivityItemData) {
    val icon = when (item.type) {
        "listing_created"    -> Icons.Default.AddCircle
        "pickup_request"     -> Icons.Default.LocalShipping
        "pickup_approved"    -> Icons.Default.CheckCircle
        "pickup_completed"   -> Icons.Default.Done
        "pickup_cancelled"   -> Icons.Default.Cancel
        "inventory_received" -> Icons.Default.Inventory
        else                 -> Icons.Default.Info
    }
    val iconColor = when (item.type) {
        "listing_created"  -> MaterialTheme.colorScheme.primary
        "pickup_completed" -> MaterialTheme.colorScheme.primary
        "pickup_cancelled" -> MaterialTheme.colorScheme.error
        "pickup_approved"  -> MaterialTheme.colorScheme.tertiary
        else               -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val timeLabel = remember(item.timestamp) {
        try {
            OffsetDateTime.parse(item.timestamp)
                .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
        } catch (_: Exception) { "" }
    }
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Box(
            modifier         = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (timeLabel.isNotEmpty()) {
            Text(timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grocery Pin Content (for map markers)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GroceryPinContent(name: String, count: Int) {
    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.padding(4.dp)) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape    = CircleShape,
            color    = MaterialTheme.colorScheme.primaryContainer,
            border   = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text       = name.take(1).uppercase(),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                        text     = count.toString(),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onError,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
