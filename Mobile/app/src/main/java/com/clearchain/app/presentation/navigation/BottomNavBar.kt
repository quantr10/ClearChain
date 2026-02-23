package com.clearchain.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.clearchain.app.domain.model.OrganizationType

@Composable
fun BottomNavBar(
    navController: NavController,
    userType: OrganizationType
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = when (userType) {
        OrganizationType.NGO -> getNgoNavigationItems()
        OrganizationType.GROCERY -> getGroceryNavigationItems()
        OrganizationType.ADMIN -> getAdminNavigationItems()
    }

    NavigationBar {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination to avoid building up a large stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// NGO Navigation Items
// ═══════════════════════════════════════════════════════════════════════════════

private fun getNgoNavigationItems(): List<NavigationItem> {
    return listOf(
        NavigationItem(
            route = Screen.NgoDashboard.route,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            label = "Home"
        ),
        NavigationItem(
            route = Screen.Inventory.route,
            selectedIcon = Icons.Filled.Inventory,
            unselectedIcon = Icons.Outlined.Inventory2,
            label = "Inventory"
        ),
        NavigationItem(
            route = Screen.BrowseListings.route,
            selectedIcon = Icons.Filled.RestaurantMenu,
            unselectedIcon = Icons.Outlined.RestaurantMenu,
            label = "Browse"
        ),
        NavigationItem(
            route = Screen.Deliveries.route,
            selectedIcon = Icons.Filled.LocalShipping,
            unselectedIcon = Icons.Outlined.LocalShipping,
            label = "My Requests"
        ),
        NavigationItem(
            route = Screen.Profile.route,
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            label = "Profile"
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Grocery Navigation Items
// ═══════════════════════════════════════════════════════════════════════════════

private fun getGroceryNavigationItems(): List<NavigationItem> {
    return listOf(
        NavigationItem(
            route = Screen.GroceryDashboard.route,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            label = "Home"
        ),
        NavigationItem(
            route = Screen.MyListings.route,
            selectedIcon = Icons.Filled.List,
            unselectedIcon = Icons.Outlined.List,
            label = "Listings"
        ),
        NavigationItem(
            route = Screen.CreateListing.route,
            selectedIcon = Icons.Filled.Add,
            unselectedIcon = Icons.Outlined.Add,
            label = "Add"
        ),
        NavigationItem(
            route = Screen.PickupRequests.route,
            selectedIcon = Icons.Filled.LocalShipping,
            unselectedIcon = Icons.Outlined.LocalShipping,
            label = "Requests"
        ),
        NavigationItem(
            route = Screen.Profile.route,
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            label = "Profile"
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Admin Navigation Items
// ═══════════════════════════════════════════════════════════════════════════════

private fun getAdminNavigationItems(): List<NavigationItem> {
    return listOf(
        NavigationItem(
            route = Screen.AdminDashboard.route,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            label = "Home"
        ),
        NavigationItem(
            route = Screen.Verification.route,
            selectedIcon = Icons.Filled.VerifiedUser,
            unselectedIcon = Icons.Outlined.VerifiedUser,
            label = "Verify"
        ),
        NavigationItem(
            route = Screen.Transactions.route,
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History,
            label = "History"
        ),
        NavigationItem(
            route = Screen.Profile.route,
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            label = "Profile"
        )
    )
}