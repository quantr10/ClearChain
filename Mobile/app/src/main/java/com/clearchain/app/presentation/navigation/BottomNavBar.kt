package com.clearchain.app.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType

@Composable
fun BottomNavBar(
    navController: NavController,
    userType: OrganizationType
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = when (userType) {
        OrganizationType.NGO     -> getNgoNavigationItems()
        OrganizationType.GROCERY -> getGroceryNavigationItems()
        OrganizationType.ADMIN   -> getAdminNavigationItems()
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val iconSize by animateDpAsState(
                targetValue = if (isSelected) 26.dp else 24.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "icon_size"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.labelResId),
                        modifier = Modifier.size(iconSize)
                    )
                },
                label = { Text(stringResource(item.labelResId), style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
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
    NavigationItem(Screen.MyListings.route, Icons.Filled.List, Icons.Outlined.List, R.string.nav_listings),
    NavigationItem(Screen.CreateListing.route, Icons.Filled.AddCircle, Icons.Outlined.AddCircle, R.string.nav_add),
    NavigationItem(Screen.PickupRequests.route, Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, R.string.nav_requests),
    NavigationItem(Screen.Profile.route, Icons.Filled.Person, Icons.Outlined.Person, R.string.nav_profile)
)

private fun getAdminNavigationItems(): List<NavigationItem> = listOf(
    NavigationItem(Screen.AdminDashboard.route, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    NavigationItem(Screen.Verification.route, Icons.Filled.VerifiedUser, Icons.Outlined.VerifiedUser, R.string.nav_verify),
    NavigationItem(Screen.Transactions.route, Icons.Filled.History, Icons.Outlined.History, R.string.nav_history),
    NavigationItem(Screen.AdminStatistics.route, Icons.Filled.BarChart, Icons.Outlined.BarChart, R.string.nav_stats),
    NavigationItem(Screen.Profile.route, Icons.Filled.Person, Icons.Outlined.Person, R.string.nav_profile)
)
