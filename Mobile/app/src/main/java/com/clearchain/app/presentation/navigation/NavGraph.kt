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
        // ═══ Auth (No Bottom Bar) ═══

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

        // ═══ Onboarding (Part 1) — No Bottom Bar ═══

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

        // ═══ Grocery (Show Bottom Bar) ═══

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
                    navController.navigate("request_detail/$requestId")
                }
            )
        }

        // ═══ NGO (Show Bottom Bar) ═══

        composable(Screen.NgoDashboard.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            NgoDashboardScreen(navController = navController)
        }

        composable(Screen.BrowseListings.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            BrowseListingsScreen(navController = navController)
        }

        composable(Screen.Deliveries.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            MyRequestsScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRequestDetail = { requestId ->
                    navController.navigate("request_detail/$requestId")
                }
            )
        }

        composable(Screen.Inventory.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            InventoryScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToItemDetail = { itemId ->
                    navController.navigate("inventory_detail/$itemId")
                }
            )
        }

        composable(
            route = "request_pickup/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            RequestPickupScreen(
                listingId = backStackEntry.arguments?.getString("listingId") ?: "",
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // ═══ Detail Screens (Part 3) ═══

        composable(
            route = "listing_detail/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            ListingDetailScreen(
                listingId = listingId,
                onNavigateBack = { navController.navigateUp() },
                onRequestPickup = { id ->
                    navController.navigate("request_pickup/$id")
                }
            )
        }

        composable(
            route = "request_detail/{requestId}",
            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            RequestDetailScreen(
                requestId = requestId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = "inventory_detail/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            InventoryDetailScreen(
                itemId = itemId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRequestDetail = { requestId ->
                    navController.navigate("request_detail/$requestId")
                }
            )
        }

        // ═══ Admin (Show Bottom Bar) ═══

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
                    navController.navigate("request_detail/$requestId")
                }
            )
        }

        composable("admin/statistics") {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            StatisticsScreen(onNavigateBack = { navController.navigateUp() })
        }

        // ═══ Shared ═══

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