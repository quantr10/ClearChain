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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.UpcomingPickupData
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.components.buildDailyActivityCounts
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.presentation.ngo.ActivityFeedItem
import com.clearchain.app.presentation.ngo.ActivityHistorySheet
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryDashboardScreen(
    navController: NavController,
    viewModel: GroceryDashboardViewModel = hiltViewModel()
) {
    val userName          by viewModel.userName.collectAsState()
    val stats             by viewModel.stats.collectAsState()
    val todaySummary      by viewModel.todaySummary.collectAsState()
    val activities        by viewModel.activities.collectAsState()
    val profileComplete   by viewModel.profileCompleteness.collectAsState()
    val isRefreshing      by viewModel.isRefreshing.collectAsState()
    var showActivitySheet by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        HapticPullToRefreshBox(
            isRefreshing = isRefreshing,
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
                    userName       = userName,
                    subtitle       = stringResource(R.string.grocery_dashboard_subtitle),
                    roleLabel      = stringResource(R.string.role_grocery),
                    gradientColors = listOf(BrandTeal, BrandGreen),
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                )

                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // ── Profile completeness card ──────────────────────────
                    ProfileCompletenessCard(
                        percent         = profileComplete,
                        onCompleteClick = { navController.navigate(Screen.Profile.route) }
                    )

                    // ── Today's summary ────────────────────────────────────
                    todaySummary?.let { summary ->
                        DashboardSection(title = stringResource(R.string.section_todays_summary)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TodaySummaryChip(
                                    label = stringResource(R.string.chip_listing_created),
                                    value = summary.listingsCreatedToday.toString(),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                TodaySummaryChip(
                                    label = stringResource(R.string.chip_requests_received),
                                    value = summary.requestsCreatedToday.toString(),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                                TodaySummaryChip(
                                    label = stringResource(R.string.chip_pickups),
                                    value = summary.pickupsToday.toString(),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // ── Today's Pickups ────────────────────────────────────
                    val upcomingPickups = todaySummary?.upcomingPickups.orEmpty()
                    if (upcomingPickups.isNotEmpty()) {
                        DashboardSection(title = stringResource(R.string.section_todays_pickups)) {
                            GroceryUpcomingPickupsTimeline(
                                pickups   = upcomingPickups,
                                onViewAll = { navController.navigate(Screen.PickupRequests.route) }
                            )
                        }
                    }

                    // ── Activity Trend + Recent Activity ──────────────────
                    if (activities.isNotEmpty()) {
                        val sparklineData = buildDailyActivityCounts(activities)
                        DashboardSection(title = stringResource(R.string.section_activity_trend)) {
                            ActivitySparklineCard(
                                title = stringResource(R.string.label_actions_this_week),
                                data  = sparklineData
                            )
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                activities.take(5).forEach { item ->
                                    ActivityFeedItem(item = item)
                                }
                            }
                            if (activities.size > 5) {
                                TextButton(
                                    onClick  = { showActivitySheet = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_view_more))
                                }
                            }
                        }
                    }

                    // ── Quick actions ──────────────────────────────────────
                    DashboardSection(title = stringResource(R.string.section_quick_actions)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // ── Activity bottom sheet ──────────────────────────────────────────────
    if (showActivitySheet) {
        ActivityHistorySheet(
            activities = activities,
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
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            pickups.take(3).forEachIndexed { index, pickup ->
                GroceryPickupTimelineItem(
                    pickup = pickup,
                    isLast = index == minOf(pickups.size, 3) - 1
                )
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

// ── Profile completeness card ──────────────────────────────────────────────────

@Composable
private fun ProfileCompletenessCard(
    percent: Int,
    onCompleteClick: () -> Unit
) {
    val isComplete = percent >= 100
    val containerColor = if (isComplete)
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val iconTint = if (isComplete)
        MaterialTheme.colorScheme.secondary
    else
        MaterialTheme.colorScheme.primary

    Surface(
        color    = containerColor,
        shape    = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.weight(1f)
            ) {
                Icon(
                    if (isComplete) Icons.Default.VerifiedUser else Icons.Default.AccountCircle,
                    null,
                    tint     = iconTint,
                    modifier = Modifier.size(36.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text       = stringResource(R.string.profile_completeness, percent),
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    LinearProgressIndicator(
                        progress  = { percent / 100f },
                        modifier  = Modifier
                            .fillMaxWidth(0.7f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color      = iconTint,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            if (!isComplete) {
                TextButton(onClick = onCompleteClick) {
                    Text(stringResource(R.string.continue_label), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ── Today summary chip ─────────────────────────────────────────────────────────

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
                text  = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
