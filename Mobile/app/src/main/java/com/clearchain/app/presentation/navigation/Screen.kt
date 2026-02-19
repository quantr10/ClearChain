package com.clearchain.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")

    // Grocery screens
    object GroceryDashboard : Screen("grocery_dashboard")
    object CreateListing : Screen("create_listing")
    object MyListings : Screen("my_listings")
    object PickupRequests : Screen("pickup_requests")

    // NGO screens
    object NgoDashboard : Screen("ngo_dashboard")
    object BrowseListings : Screen("browse_listings")
    object Cart : Screen("cart")
    object Deliveries : Screen("deliveries")
    object Inventory : Screen("inventory")

    // Admin screens
    object AdminDashboard : Screen("admin_dashboard")
    object Verification : Screen("verification")
    object Transactions : Screen("transactions")
    
    // Profile (shared by all roles)
    object Profile : Screen("profile")
}