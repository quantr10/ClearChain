package com.clearchain.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.ui.theme.*

/** How far the label size rule steps down between the sizes it tries. */
private const val LABEL_SIZE_STEP_SP = 0.5f

/** The most lines a bar label may take before every label in the bar gets smaller. */
private const val LABEL_MAX_LINES = 2

/**
 * The tab bar along the bottom of every top-level screen: five tabs for each role.
 *
 * Every label in the bar shares one size: the user's `label-small` while each label fits in two
 * lines of its tab, otherwise the largest size at which they all do, never below the size the
 * label has at font scale 1.0. Labels wrap between words and are never cut off.
 */
@Composable
fun BottomNavBar(
    navController: NavController,
    userType: OrganizationType
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val profileRoutes = setOf(
        Screen.Profile.route,
        Screen.AccountDetail.route,
        Screen.Analytics.route,
        Screen.Help.route
    )

    val items = when (userType) {
        OrganizationType.NGO -> getNgoNavigationItems()
        OrganizationType.GROCERY -> getGroceryNavigationItems()
        OrganizationType.ADMIN -> getAdminNavigationItems()
    }
    val labels = items.map { stringResource(it.labelResId) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(thickness = BorderWidth, color = MaterialTheme.colorScheme.outlineVariant)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val labelSize = rememberBarLabelSize(labels, maxWidth / items.size - NavTabGap * 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = BottomBarHeight)
                    .height(IntrinsicSize.Min)
                    .selectableGroup()
                    .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = items.size) }
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = currentRoute == item.route ||
                        (item.route == Screen.Profile.route && currentRoute in profileRoutes)
                    BarTab(
                        item = item,
                        label = labels[index],
                        labelSize = labelSize,
                        selected = isSelected,
                        index = index,
                        onClick = {
                            if (currentRoute != item.route) navController.navigateToTab(item.navRoute)
                        }
                    )
                }
            }
        }
        // The bar's surface runs on under the system navigation bar.
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** Opens a bottom-bar destination the way its tab does: one copy, with each tab's state saved and restored. */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** One tab: the icon in its indicator pill, with the label below. */
@Composable
private fun RowScope.BarTab(
    item: NavigationItem,
    label: String,
    labelSize: TextUnit,
    selected: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .semantics {
                collectionItemInfo = CollectionItemInfo(
                    rowIndex = 0,
                    rowSpan = 1,
                    columnIndex = index,
                    columnSpan = 1
                )
            }
            .padding(vertical = ScreenVPadding, horizontal = NavTabGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NavTabGap)
    ) {
        Box(
            modifier = Modifier
                .size(width = NavIndicatorWidth, height = NavIndicatorHeight)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.background(colors.primary.copy(alpha = Alpha.TileTint).compositeOver(colors.surface))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                modifier = Modifier.size(ControlIconSize),
                tint = if (selected) colors.primary else colors.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = labelSize,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The one label size for the whole bar. Tries the user's scaled `label-small` first, then steps
 * down until every label fits [labelWidth]: no single word wider than the tab, and the whole
 * label in at most [LABEL_MAX_LINES] lines. The floor is the size at font scale 1.0; if even
 * that doesn't fit, the labels use it and wrap further.
 */
@Composable
private fun rememberBarLabelSize(labels: List<String>, labelWidth: Dp): TextUnit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = MaterialTheme.typography.labelSmall
    return remember(labels, labelWidth, density, base) {
        val widthPx = with(density) { labelWidth.roundToPx() }.coerceAtLeast(1)
        val floor = with(density) { base.fontSize.value.dp.toSp() }.value
        val measured = base.copy(fontWeight = FontWeight.SemiBold)

        fun fits(size: Float): Boolean = labels.all { label ->
            val style = measured.copy(fontSize = size.sp)
            val wordsFit = label.split(' ').filter { it.isNotBlank() }.all { word ->
                measurer.measure(word, style, softWrap = false, maxLines = 1).size.width <= widthPx
            }
            wordsFit && measurer.measure(
                label,
                style,
                constraints = Constraints(maxWidth = widthPx)
            ).lineCount <= LABEL_MAX_LINES
        }

        var size = base.fontSize.value
        while (size > floor && !fits(size)) {
            size = (size - LABEL_SIZE_STEP_SP).coerceAtLeast(floor)
        }
        size.sp
    }
}

private fun getNgoNavigationItems(): List<NavigationItem> = listOf(
    NavigationItem(Screen.NgoDashboard.route, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    NavigationItem(Screen.Inventory.route, Icons.Filled.Inventory, Icons.Outlined.Inventory2, R.string.nav_inventory),
    NavigationItem(Screen.BrowseListings.route, Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu, R.string.nav_browse),
    NavigationItem(Screen.MyRequests.route, Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, R.string.nav_requests),
    NavigationItem(Screen.Profile.route, Icons.Filled.Person, Icons.Outlined.Person, R.string.nav_profile)
)

private fun getGroceryNavigationItems(): List<NavigationItem> = listOf(
    NavigationItem(Screen.GroceryDashboard.route, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    NavigationItem(Screen.MyListings.route, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List, R.string.nav_listings),
    NavigationItem(Screen.CreateListing.route, Icons.Filled.AddCircle, Icons.Outlined.AddCircle, R.string.nav_add),
    NavigationItem(Screen.PickupRequests.route, Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, R.string.nav_requests),
    NavigationItem(Screen.Profile.route, Icons.Filled.Person, Icons.Outlined.Person, R.string.nav_profile)
)

private fun getAdminNavigationItems(): List<NavigationItem> = listOf(
    NavigationItem(Screen.AdminDashboard.route, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    NavigationItem(Screen.Verification.route, Icons.Filled.VerifiedUser, Icons.Outlined.VerifiedUser, R.string.nav_verify),
    NavigationItem(
        route = Screen.AdminStatistics.route,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        labelResId = R.string.nav_stats,
        // The tab opens the screen at the top; the section argument is for the deep links
        // the admin home uses.
        navRoute = Screen.AdminStatistics.BASE
    ),
    NavigationItem(Screen.Transactions.route, Icons.Filled.History, Icons.Outlined.History, R.string.nav_history),
    NavigationItem(Screen.Profile.route, Icons.Filled.Person, Icons.Outlined.Person, R.string.nav_profile)
)
