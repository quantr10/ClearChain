package com.clearchain.app.presentation.navigation

sealed class Screen(val route: String) {
    // ═══════════════════════════════════════════════════════════════════════════
    // Auth Screens
    // ═══════════════════════════════════════════════════════════════════════════
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")

    // ═══════════════════════════════════════════════════════════════════════════
    // Grocery Screens
    // ═══════════════════════════════════════════════════════════════════════════
    object GroceryDashboard : Screen("grocery_dashboard")
    object CreateListing : Screen("create_listing")
    object MyListings : Screen("my_listings")
    object PickupRequests : Screen("pickup_requests")  // ✅ FIXED: Added this

    // ═══════════════════════════════════════════════════════════════════════════
    // NGO Screens
    // ═══════════════════════════════════════════════════════════════════════════
    object NgoDashboard : Screen("ngo_dashboard")
    object BrowseListings : Screen("browse_listings")
    object Cart : Screen("cart")
    object Deliveries : Screen("deliveries")  // ✅ FIXED: This is My Requests
    object Inventory : Screen("inventory")

    // ═══════════════════════════════════════════════════════════════════════════
    // Admin Screens
    // ═══════════════════════════════════════════════════════════════════════════
    object AdminDashboard : Screen("admin_dashboard")
    object Verification : Screen("admin/verification")  // ✅ FIXED: Added this
    object Transactions : Screen("admin/transactions")
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Shared Screens
    // ═══════════════════════════════════════════════════════════════════════════
    object Profile : Screen("profile")
}