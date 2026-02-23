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
import com.clearchain.app.presentation.ngo.myrequests.MyRequestsScreen
import com.clearchain.app.presentation.ngo.requestpickup.RequestPickupScreen
import com.clearchain.app.presentation.profile.ProfileScreen
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
        // ═══════════════════════════════════════════════════════════════════════════
        // Auth Screens (No Bottom Bar)
        // ═══════════════════════════════════════════════════════════════════════════
        
        composable(route = Screen.Splash.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            SplashScreen(navController = navController)
        }

        composable(route = Screen.Login.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            LoginScreen(navController = navController)
        }

        composable(route = Screen.Register.route) {
            LaunchedEffect(Unit) { onShowBottomBar(false, null) }
            RegisterScreen(navController = navController)
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // Grocery Screens (Show Bottom Bar)
        // ═══════════════════════════════════════════════════════════════════════════
        
        composable(route = Screen.GroceryDashboard.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            GroceryDashboardScreen(navController = navController)
        }

        composable(route = Screen.CreateListing.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            CreateListingScreen(navController = navController)
        }

        composable(route = Screen.MyListings.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            MyListingsScreen(navController = navController)
        }

        composable(route = Screen.PickupRequests.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.GROCERY) }
            ManageRequestsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // NGO Screens (Show Bottom Bar)
        // ═══════════════════════════════════════════════════════════════════════════
        
        composable(route = Screen.NgoDashboard.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            NgoDashboardScreen(navController = navController)
        }

        composable(route = Screen.BrowseListings.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            BrowseListingsScreen(navController = navController)
        }

        composable(route = Screen.Deliveries.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            MyRequestsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(route = Screen.Inventory.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            InventoryScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = "request_pickup/{listingId}",
            arguments = listOf(
                navArgument("listingId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.NGO) }
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            RequestPickupScreen(
                listingId = listingId,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // Admin Screens (Show Bottom Bar)
        // ═══════════════════════════════════════════════════════════════════════════
        
        composable(route = Screen.AdminDashboard.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            AdminDashboardScreen(navController = navController)
        }

        composable(route = Screen.Verification.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            VerificationQueueScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(route = Screen.Transactions.route) {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            TransactionsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(route = "admin/statistics") {
            LaunchedEffect(Unit) { onShowBottomBar(true, OrganizationType.ADMIN) }
            StatisticsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // Shared Screens (Show Bottom Bar)
        // ═══════════════════════════════════════════════════════════════════════════
        
        composable(route = Screen.Profile.route) {
            // Get user type from viewModel
            val getCurrentUserUseCase: GetCurrentUserUseCase = hiltViewModel<com.clearchain.app.presentation.profile.ProfileViewModel>().getCurrentUserUseCase
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