package com.clearchain.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.presentation.admin.analytics.AdminAnalyticsScreen
import com.clearchain.app.presentation.admin.dashboard.AdminDashboardScreen
import com.clearchain.app.presentation.admin.transactions.TransactionsScreen
import com.clearchain.app.presentation.admin.verification.VerificationQueueScreen
import com.clearchain.app.presentation.analytics.AnalyticsScreen
import com.clearchain.app.presentation.auth.login.LoginScreen
import com.clearchain.app.presentation.auth.register.RegisterScreen
import com.clearchain.app.presentation.auth.verify.EmailVerificationScreen
import com.clearchain.app.presentation.dispute.DisputeScreen
import com.clearchain.app.presentation.grocery.GroceryDashboardScreen
import com.clearchain.app.presentation.grocery.createlisting.CreateListingScreen
import com.clearchain.app.presentation.grocery.editlisting.EditListingScreen
import com.clearchain.app.presentation.grocery.managerequests.ManageRequestsScreen
import com.clearchain.app.presentation.grocery.mylistings.MyListingsScreen
import com.clearchain.app.presentation.help.HelpScreen
import com.clearchain.app.presentation.ngo.NgoDashboardScreen
import com.clearchain.app.presentation.ngo.browselistings.BrowseListingsScreen
import com.clearchain.app.presentation.ngo.cart.CartPickupScreen
import com.clearchain.app.presentation.ngo.cart.CartScreen
import com.clearchain.app.presentation.ngo.inventory.InventoryScreen
import com.clearchain.app.presentation.ngo.inventorydetail.InventoryDetailScreen
import com.clearchain.app.presentation.ngo.listingdetail.ListingDetailScreen
import com.clearchain.app.presentation.ngo.locationpicker.LocationPickerScreen
import com.clearchain.app.presentation.ngo.myrequests.MyRequestsScreen
import com.clearchain.app.presentation.notifications.NotificationInboxScreen
import com.clearchain.app.presentation.onboarding.OnboardingScreen
import com.clearchain.app.presentation.pendingreview.PendingReviewScreen
import com.clearchain.app.presentation.profile.AccountDetailScreen
import com.clearchain.app.presentation.profile.ProfileScreen
import com.clearchain.app.presentation.publicprofile.PublicProfileScreen
import com.clearchain.app.presentation.settings.SettingsScreen
import com.clearchain.app.presentation.shared.requestdetail.RequestDetailScreen
import com.clearchain.app.presentation.splash.SplashScreen
import kotlinx.coroutines.flow.first

/**
 * Defense-in-depth: Splash/Login already keep a pending/rejected org off the dashboard,
 * but if the cached user is somehow stale (killed mid-transition, etc.) this bounces them
 * back to PendingReview instead of leaving a gated org on a fully-functional dashboard.
 */
@Composable
private fun VerificationGate(navController: NavHostController) {
    val getCurrentUserUseCase = hiltViewModel<VerificationGateViewModel>().getCurrentUserUseCase
    LaunchedEffect(Unit) {
        val user = getCurrentUserUseCase().first()
        if (user != null && user.requiresVerificationGate()) {
            navController.navigate(Screen.PendingReview.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route,
    onShowBottomBar: (Boolean, OrganizationType?) -> Unit = { _, _ -> }
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ── Auth ─────────────────────────────────────────────────────────────

        composable(Screen.Splash.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            SplashScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            RegisterScreen(navController = navController)
        }

        composable(
            route = Screen.EmailVerification.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            EmailVerificationScreen(navController = navController)
        }

        composable(Screen.Onboarding.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PendingReview.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            PendingReviewScreen(
                onNavigateRoute = { route ->
                    navController.navigate(route) { popUpTo(0) { inclusive = true } }
                },
                onEditProfile = { navController.navigate(Screen.Onboarding.route) },
                onOpenHelp = { navController.navigate(Screen.Help.route) },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // ── Grocery ──────────────────────────────────────────────────────────

        composable(Screen.GroceryDashboard.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            VerificationGate(navController)
            GroceryDashboardScreen(navController = navController)
        }

        composable(Screen.CreateListing.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            CreateListingScreen(navController = navController)
        }

        composable(
            route     = Screen.EditListing.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) { onShowBottomBar(false, OrganizationType.GROCERY) }
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            EditListingScreen(listingId = listingId, navController = navController)
        }

        composable(Screen.MyListings.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            MyListingsScreen(navController = navController)
        }

        composable(Screen.PickupRequests.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            ManageRequestsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRequestDetail = { requestId ->
                    navController.navigate(Screen.RequestDetail.createRoute(requestId))
                }
            )
        }

        // ── NGO ──────────────────────────────────────────────────────────────

        composable(Screen.NgoDashboard.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            VerificationGate(navController)
            NgoDashboardScreen(navController = navController)
        }

        composable(
            route = Screen.BrowseListings.route,
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://browse" })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            BrowseListingsScreen(navController = navController)
        }

        composable(Screen.Cart.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            CartScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = Screen.CartPickup.route,
            arguments = listOf(navArgument("groceryId") { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) { onShowBottomBar(false, OrganizationType.NGO) }
            val groceryId = backStackEntry.arguments?.getString("groceryId") ?: ""
            CartPickupScreen(
                groceryId = groceryId,
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.MyRequests.route,
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://ngo/requests" })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            MyRequestsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRequestDetail = { requestId ->
                    navController.navigate(Screen.RequestDetail.createRoute(requestId))
                },
                onNavigateToRoute = { route -> navController.navigate(route) }
            )
        }

        composable(
            route = Screen.Inventory.route,
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://ngo/inventory" })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            InventoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToItemDetail = { itemId ->
                    navController.navigate(Screen.InventoryDetail.createRoute(itemId))
                }
            )
        }

        composable(Screen.LocationPicker.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, OrganizationType.NGO) }
            LocationPickerScreen(
                onLocationSelected = {
                    navController.navigate(Screen.BrowseListings.route) {
                        popUpTo(Screen.LocationPicker.route) { inclusive = true }
                    }
                },
                onDismiss = null
            )
        }

        // ── Detail screens ───────────────────────────────────────────────────

        composable(
            route = Screen.ListingDetail.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://listing/{listingId}" })
        ) { backStackEntry ->
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            ListingDetailScreen(
                listingId = listingId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToListingDetail = { id ->
                    navController.navigate(Screen.ListingDetail.createRoute(id))
                },
                onNavigateToStoreProfile = { orgId ->
                    navController.navigate(Screen.PublicProfile.createRoute(orgId))
                },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.EditListing.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.RequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://request/{requestId}" })
        ) { backStackEntry ->
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RequestDetailScreen(
                requestId = requestId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToDispute = { id -> navController.navigate(Screen.Dispute.createRoute(id)) },
                onNavigateToPublicProfile = { orgId -> navController.navigate(Screen.PublicProfile.createRoute(orgId)) },
                onNavigateToListing = { listingId -> navController.navigate(Screen.ListingDetail.createRoute(listingId)) }
            )
        }

        composable(
            route = Screen.InventoryDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            InventoryDetailScreen(
                itemId = itemId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRequestDetail = { requestId ->
                    navController.navigate(Screen.RequestDetail.createRoute(requestId))
                },
                onNavigateToPublicProfile = { orgId ->
                    navController.navigate(Screen.PublicProfile.createRoute(orgId))
                },
                onNavigateToListingDetail = { listingId ->
                    navController.navigate(Screen.ListingDetail.createRoute(listingId))
                }
            )
        }

        // ── Admin ────────────────────────────────────────────────────────────

        composable(
            route = Screen.AdminDashboard.route,
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://admin" })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            AdminDashboardScreen(navController = navController)
        }

        composable(
            route = Screen.Verification.route,
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://admin/verification" })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            VerificationQueueScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToPublicProfile = { orgId ->
                    navController.navigate(Screen.PublicProfile.createRoute(orgId))
                }
            )
        }

        composable(
            route = Screen.Transactions.route,
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://admin/transactions" })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            TransactionsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRequestDetail = { requestId ->
                    navController.navigate(Screen.RequestDetail.createRoute(requestId))
                }
            )
        }

        composable(
            route = Screen.AdminStatistics.route,
            arguments = listOf(
                navArgument("section") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            AdminAnalyticsScreen()
        }

        // ── Shared ───────────────────────────────────────────────────────────

        composable(Screen.Analytics.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            AnalyticsScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(Screen.AccountDetail.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            AccountDetailScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(Screen.Help.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            HelpScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.PublicProfile.route,
            arguments = listOf(navArgument("orgId") { type = NavType.StringType })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            PublicProfileScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToListingDetail = { listingId ->
                    navController.navigate(Screen.ListingDetail.createRoute(listingId))
                }
            )
        }

        composable(
            route = Screen.Dispute.route,
            arguments = listOf(navArgument("pickupRequestId") { type = NavType.StringType })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            DisputeScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(Screen.Settings.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            SettingsScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.NotificationInbox.route,
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://notifications" })
        ) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            NotificationInboxScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToDetail = { type, id ->
                    when (type) {
                        "pickup_request" -> navController.navigate(Screen.RequestDetail.createRoute(id))
                        "listing" -> navController.navigate(Screen.ListingDetail.createRoute(id))
                        else -> Unit
                    }
                }
            )
        }

        composable(
            route = Screen.Profile.route,
            deepLinks = listOf(navDeepLink { uriPattern = "clearchain://profile" })
        ) {
            val getCurrentUserUseCase: GetCurrentUserUseCase =
                hiltViewModel<com.clearchain.app.presentation.profile.ProfileViewModel>().getCurrentUserUseCase
            LaunchedEffect(Unit) {
                val user = getCurrentUserUseCase().first()
                onShowBottomBar(true, user?.type)
            }
            ProfileScreen(
                onLogout = {
                    onShowBottomBar(false, null)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToHelp = { navController.navigate(Screen.Help.route) },
                onNavigateToAccountDetail = { navController.navigate(Screen.AccountDetail.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
    }
}
