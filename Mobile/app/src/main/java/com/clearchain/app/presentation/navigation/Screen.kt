package com.clearchain.app.presentation.navigation

sealed class Screen(val route: String) {
    // Auth
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")

    // ═══ NEW (Part 1) ═══
    object Onboarding : Screen("onboarding")

    // Grocery
    object GroceryDashboard : Screen("grocery_dashboard")
    object CreateListing : Screen("create_listing")
    object MyListings : Screen("my_listings")
    object PickupRequests : Screen("pickup_requests")

    // NGO
    object NgoDashboard : Screen("ngo_dashboard")
    object BrowseListings : Screen("browse_listings")
    object Cart : Screen("cart")
    object Deliveries : Screen("deliveries")
    object Inventory : Screen("inventory")

    // Admin
    object AdminDashboard : Screen("admin_dashboard")
    object Verification : Screen("admin/verification")
    object Transactions : Screen("admin/transactions")

    // Shared
    object Profile : Screen("profile")
}