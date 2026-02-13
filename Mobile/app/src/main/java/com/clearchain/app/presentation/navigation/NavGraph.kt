package com.clearchain.app.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.clearchain.app.presentation.auth.login.LoginScreen
import com.clearchain.app.presentation.auth.register.RegisterScreen
import com.clearchain.app.presentation.grocery.GroceryDashboardScreen
import com.clearchain.app.presentation.grocery.createlisting.CreateListingScreen
import com.clearchain.app.presentation.grocery.mylistings.MyListingsScreen
import com.clearchain.app.presentation.splash.SplashScreen
import com.clearchain.app.presentation.ngo.NgoDashboardScreen
import com.clearchain.app.presentation.ngo.browselistings.BrowseListingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash
        composable(route = Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        // Auth screens
        composable(route = Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        // Grocery Dashboard
        composable(route = Screen.GroceryDashboard.route) {
            GroceryDashboardScreen(navController = navController)
        }

        // Create Listing
        composable(route = Screen.CreateListing.route) {
            CreateListingScreen(navController = navController)
        }

        // My Listings
        composable(route = Screen.MyListings.route) {
            MyListingsScreen(navController = navController)
        }

        // Pickup Requests (Placeholder for now)
        composable(route = Screen.PickupRequests.route) {
            PlaceholderScreen(
                title = "Pickup Requests",
                message = "Coming soon!",
                navController = navController
            )
        }

        // NGO Dashboard (Placeholder)
        composable(route = Screen.NgoDashboard.route) {
            PlaceholderDashboard(
                title = "NGO Dashboard",
                userType = "NGO",
                navController = navController
            )
        }

        // Admin Dashboard (Placeholder)
        composable(route = Screen.AdminDashboard.route) {
            PlaceholderDashboard(
                title = "Admin Dashboard",
                userType = "Admin",
                navController = navController
            )
        }

        // NGO Dashboard
        composable(route = Screen.NgoDashboard.route) {
            NgoDashboardScreen(navController = navController)
        }

// Browse Listings
        composable(route = Screen.BrowseListings.route) {
            BrowseListingsScreen(navController = navController)
        }

// Request Pickup (Placeholder for now)
        composable(route = "request_pickup/{listingId}") { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            PlaceholderScreen(
                title = "Request Pickup",
                message = "Coming in next step!",
                navController = navController
            )
        }

// My Requests/Deliveries (Placeholder)
        composable(route = Screen.Deliveries.route) {
            PlaceholderScreen(
                title = "My Requests",
                message = "Coming soon!",
                navController = navController
            )
        }

// Inventory (Placeholder)
        composable(route = Screen.Inventory.route) {
            PlaceholderScreen(
                title = "Inventory",
                message = "Coming soon!",
                navController = navController
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
    navController: NavHostController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,  // FIXED
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🚧",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderDashboard(
    title: String,
    userType: String,
    navController: NavHostController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You're logged in as $userType",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "✅ Authentication Working!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dashboard features coming soon!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}