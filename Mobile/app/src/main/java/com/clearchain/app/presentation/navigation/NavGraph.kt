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
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.presentation.admin.AdminDashboardScreen
import com.clearchain.app.presentation.admin.statistics.StatisticsScreen
import com.clearchain.app.presentation.admin.transactions.TransactionsScreen
import com.clearchain.app.presentation.admin.verification.VerificationQueueScreen
import com.clearchain.app.presentation.auth.login.LoginScreen
import com.clearchain.app.presentation.auth.register.RegisterScreen
import com.clearchain.app.presentation.grocery.GroceryDashboardScreen
import com.clearchain.app.presentation.grocery.createlisting.CreateListingScreen
import com.clearchain.app.presentation.grocery.managerequests.ManageRequestsScreen
import com.clearchain.app.presentation.grocery.mylistings.MyListingsScreen
import com.clearchain.app.presentation.ngo.NgoDashboardScreen
import com.clearchain.app.presentation.ngo.browselistings.BrowseListingsScreen
import com.clearchain.app.presentation.ngo.inventory.InventoryScreen
import com.clearchain.app.presentation.ngo.listingdetail.ListingDetailScreen
import com.clearchain.app.presentation.ngo.inventorydetail.InventoryDetailScreen
import com.clearchain.app.presentation.ngo.myrequests.MyRequestsScreen
import com.clearchain.app.presentation.ngo.requestpickup.RequestPickupScreen
import com.clearchain.app.presentation.ngo.locationpicker.LocationPickerScreen
import com.clearchain.app.presentation.onboarding.OnboardingScreen
import com.clearchain.app.presentation.profile.ProfileScreen
import com.clearchain.app.presentation.shared.requestdetail.RequestDetailScreen
import com.clearchain.app.presentation.splash.SplashScreen
import kotlinx.coroutines.flow.first

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
        // ── Auth ──────────────────────────────────────────

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

        // ── Grocery ───────────────────────────────────────

        composable(Screen.GroceryDashboard.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            GroceryDashboardScreen(navController = navController)
        }

        composable(Screen.CreateListing.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            CreateListingScreen(navController = navController)
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

        // ── NGO ───────────────────────────────────────────

        composable(Screen.NgoDashboard.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            NgoDashboardScreen(navController = navController)
        }

        composable(Screen.BrowseListings.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            BrowseListingsScreen(navController = navController)
        }

        composable(Screen.MyRequests.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            MyRequestsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRequestDetail = { requestId ->
                    navController.navigate(Screen.RequestDetail.createRoute(requestId))
                }
            )
        }

        composable(Screen.Inventory.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            InventoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToItemDetail = { itemId ->
                    navController.navigate(Screen.InventoryDetail.createRoute(itemId))
                }
            )
        }

        composable(Screen.LocationPicker.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            LocationPickerScreen(
                onLocationSelected = {
                    navController.navigate(Screen.BrowseListings.route) {
                        popUpTo(Screen.LocationPicker.route) { inclusive = true }
                    }
                },
                onDismiss = null
            )
        }

        composable(Screen.LocationPickerEdit.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            LocationPickerScreen(
                onLocationSelected = { navController.navigateUp() },
                onDismiss = { navController.navigateUp() }
            )
        }

        // ── Detail screens ────────────────────────────────

        composable(
            route = Screen.ListingDetail.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            ListingDetailScreen(
                listingId = listingId,
                onNavigateBack = { navController.navigateUp() },
                onRequestPickup = { id ->
                    navController.navigate(Screen.RequestPickup.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.RequestPickup.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            RequestPickupScreen(
                listingId = backStackEntry.arguments?.getString("listingId") ?: "",
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.RequestDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RequestDetailScreen(
                requestId = requestId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.InventoryDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            InventoryDetailScreen(
                itemId = itemId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRequestDetail = { requestId ->
                    navController.navigate(Screen.RequestDetail.createRoute(requestId))
                }
            )
        }

        // ── Admin ─────────────────────────────────────────

        composable(Screen.AdminDashboard.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            AdminDashboardScreen(navController = navController)
        }

        composable(Screen.Verification.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            VerificationQueueScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(Screen.Transactions.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            TransactionsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRequestDetail = { requestId ->
                    navController.navigate(Screen.RequestDetail.createRoute(requestId))
                }
            )
        }

        composable(Screen.AdminStatistics.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            StatisticsScreen(onNavigateBack = { navController.navigateUp() })
        }

        // ── Shared ────────────────────────────────────────

        composable(Screen.Profile.route) {
            val getCurrentUserUseCase: GetCurrentUserUseCase =
                hiltViewModel<com.clearchain.app.presentation.profile.ProfileViewModel>().getCurrentUserUseCase
            LaunchedEffect(Unit) {
                val user = getCurrentUserUseCase().first()
                onShowBottomBar(true, user?.type)
            }
            ProfileScreen(
                onNavigateBack = { navController.navigateUp() },
                onLogout = {
                    onShowBottomBar(false, null)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
