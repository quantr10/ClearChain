package com.clearchain.app.presentation.navigation

sealed class Screen(val route: String) {

    // ── Auth ──────────────────────────────────────────────
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Onboarding : Screen("onboarding")
    object EmailVerification : Screen("email_verification/{email}") {
        fun createRoute(email: String) = "email_verification/${java.net.URLEncoder.encode(email, "UTF-8")}"
    }

    // ── Grocery ───────────────────────────────────────────
    object GroceryDashboard : Screen("grocery_dashboard")
    object CreateListing : Screen("create_listing")
    object MyListings : Screen("my_listings")
    object PickupRequests : Screen("pickup_requests")

    // ── NGO ───────────────────────────────────────────────
    object NgoDashboard : Screen("ngo_dashboard")
    object BrowseListings : Screen("browse_listings")
    object MyRequests : Screen("my_requests")
    object Inventory : Screen("inventory")
    object LocationPicker : Screen("location_picker")
    object LocationPickerEdit : Screen("location_picker_edit")

    // ── Admin ─────────────────────────────────────────────
    object AdminDashboard : Screen("admin_dashboard")
    object Verification : Screen("admin/verification")
    object Transactions : Screen("admin/transactions")
    object AdminStatistics : Screen("admin/statistics")

    // ── Detail screens (parameterized) ────────────────────
    object ListingDetail : Screen("listing_detail/{listingId}") {
        fun createRoute(listingId: String) = "listing_detail/$listingId"
    }
    object RequestPickup : Screen("request_pickup/{listingId}") {
        fun createRoute(listingId: String) = "request_pickup/$listingId"
    }
    object RequestDetail : Screen("request_detail/{requestId}") {
        fun createRoute(requestId: String) = "request_detail/$requestId"
    }
    object InventoryDetail : Screen("inventory_detail/{itemId}") {
        fun createRoute(itemId: String) = "inventory_detail/$itemId"
    }

    // ── Shared ────────────────────────────────────────────
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object NotificationInbox : Screen("notifications")
    object Analytics : Screen("analytics")
    object Help : Screen("help")

    object PublicProfile : Screen("public_profile/{orgId}") {
        fun createRoute(orgId: String) = "public_profile/$orgId"
    }
    object Dispute : Screen("dispute/{pickupRequestId}") {
        fun createRoute(pickupRequestId: String) = "dispute/$pickupRequestId"
    }
}
