package com.clearchain.app.presentation.grocery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearchain.app.ui.theme.ScreenPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.UpcomingPickupData
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.components.buildDailyActivityCounts
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.presentation.ngo.ActivityFeedList
import com.clearchain.app.presentation.ngo.ActivityHistorySheet
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryDashboardScreen(
    navController: NavController,
    viewModel: GroceryDashboardViewModel = hiltViewModel()
) {
    val state             by viewModel.state.collectAsState()
    var showActivitySheet by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        HapticPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh    = viewModel::refresh,
            modifier     = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardWelcomeHeader(
                    userName       = state.userName,
                    subtitle       = stringResource(R.string.grocery_dashboard_subtitle),
                    roleLabel      = stringResource(R.string.role_grocery),
                    profilePictureUrl = state.profilePictureUrl,
                    gradientColors = listOf(BrandTeal, BrandGreen),
                    onProfileClick = { navController.navigate(Screen.AccountDetail.route) },
                    onNotificationsClick = { navController.navigate(Screen.NotificationInbox.route) }
                )

                Column(
                    modifier            = Modifier.padding(ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Impact ─────────────────────────────────────────────
                    // First card on the page: what the store has actually rescued leads,
                    // before today's workload.
                    state.stats?.let { stats ->
                        DashboardSection(
                            title      = stringResource(R.string.analytics_impact),
                            titleStyle = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp)
                        ) {
                            ImpactSummaryRow(
                                kgSaved       = stats.foodSaved,
                                mealsEstimate = stats.mealsEstimate,
                                co2EstimateKg = stats.co2EstimateKg
                            )
                        }
                    }

                    // ── Weekly Pickups ─────────────────────────────────────
                    // The same card the NGO home shows: both sides of a hand-over count
                    // the same completed pickup.
                    DashboardSection(title = "") {
                        WeeklyGoalCard(
                            completed = state.weeklyCompleted,
                            goal      = state.weeklyGoal,
                            progress  = state.weeklyProgress
                        )
                    }

                    // ── Today's Pickups ────────────────────────────────────
                    val upcomingPickups = state.todaySummary?.upcomingPickups.orEmpty()
                    if (upcomingPickups.isNotEmpty()) {
                        DashboardSection(title = "") {
                            GroceryUpcomingPickupsTimeline(
                                pickups   = upcomingPickups,
                                onViewAll = { navController.navigate(Screen.PickupRequests.route) }
                            )
                        }
                    }

                    // ── Activity Trend + Recent Activity ──────────────────
                    val sparklineData = buildDailyActivityCounts(state.activities)
                    DashboardSection(title = "") {
                        ActivitySparklineCard(
                            title = stringResource(R.string.label_actions_this_week),
                            data  = sparklineData
                        )
                        if (state.activities.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            ActivityFeedList(activities = state.activities.take(5))
                            if (state.activities.size > 5) {
                                Spacer(Modifier.height(8.dp))
                                ClearChainButton(
                                    text = stringResource(R.string.action_view_more),
                                    onClick  = { showActivitySheet = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // ── Quick actions ──────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardActionCard(
                            icon     = Icons.Default.AddCircle,
                            title    = stringResource(R.string.action_create_listing),
                            subtitle = stringResource(R.string.action_create_listing_subtitle),
                            onClick  = { navController.navigate(Screen.CreateListing.route) }
                        )
                        DashboardActionCard(
                            icon     = Icons.Default.List,
                            title    = stringResource(R.string.action_my_listings),
                            subtitle = stringResource(R.string.action_my_listings_subtitle),
                            onClick  = { navController.navigate(Screen.MyListings.route) }
                        )
                        DashboardActionCard(
                            icon     = Icons.Default.LocalShipping,
                            title    = stringResource(R.string.action_pickup_requests),
                            subtitle = stringResource(R.string.action_pickup_requests_subtitle),
                            onClick  = { navController.navigate(Screen.PickupRequests.route) }
                        )
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

// ── Today's Pickups timeline (Grocery side — shows incoming NGO pickups) ───────

@Composable
private fun GroceryUpcomingPickupsTimeline(
    pickups:   List<UpcomingPickupData>,
    onViewAll: () -> Unit
) {
    // Rendered straight onto the DashboardSection that hosts it — a Card here would
    // put a second rounded, elevated surface inside the section's own.
    Column(modifier = Modifier.fillMaxWidth()) {
        pickups.take(3).forEachIndexed { index, pickup ->
            GroceryPickupTimelineItem(
                pickup = pickup,
                isLast = index == minOf(pickups.size, 3) - 1
            )
        }
        if (pickups.size > 3) {
            ClearChainOutlinedButton(
                text = stringResource(R.string.view_all_pickups, pickups.size),
                onClick  = onViewAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun GroceryPickupTimelineItem(pickup: UpcomingPickupData, isLast: Boolean) {
    val statusColor = when (pickup.status.uppercase()) {
        "APPROVED" -> BrandGreen
        "READY"    -> MaterialTheme.colorScheme.tertiary
        else       -> MaterialTheme.colorScheme.outline
    }
    val approvedLabel = stringResource(R.string.status_approved)
    val readyLabel    = stringResource(R.string.status_ready)
    val statusLabel = when (pickup.status.uppercase()) {
        "APPROVED" -> approvedLabel
        "READY"    -> readyLabel
        else       -> pickup.status
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                text       = pickup.listingTitle,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = pickup.ngoName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = pickup.pickupTime,
                style      = MaterialTheme.typography.labelMedium,
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
