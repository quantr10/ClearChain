package com.clearchain.app.presentation.splash

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(key1 = isLoggedIn) {
        delay(1500)

        if (isLoggedIn == true) {
            val currentUser = viewModel.getCurrentUser()

            if (currentUser != null) {
                Log.d("SplashScreen", "User logged in: ${currentUser.name} (${currentUser.type})")

                // ═══ NEW: isProfileComplete() gate (Part 1) ═══
                if (!currentUser.isProfileComplete()) {
                    Log.d("SplashScreen", "Profile incomplete. Missing: ${currentUser.getMissingFields()}")
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }

                // Check deep link
                val prefs = context.getSharedPreferences("deeplink", Context.MODE_PRIVATE)
                val pendingScreen = prefs.getString("pending_screen", null)
                if (pendingScreen != null) {
                    val route = when (pendingScreen) {
                        "my_requests" -> Screen.Deliveries.route
                        "browse_listings" -> Screen.BrowseListings.route
                        "inventory" -> Screen.Inventory.route
                        else -> null
                    }
                    if (route != null) {
                        prefs.edit().clear().apply()
                        navController.navigate(route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        return@LaunchedEffect
                    }
                }

                // Navigate to dashboard
                val destination = when (currentUser.type) {
                    OrganizationType.NGO -> Screen.NgoDashboard.route
                    OrganizationType.GROCERY -> Screen.GroceryDashboard.route
                    OrganizationType.ADMIN -> Screen.AdminDashboard.route
                }
                navController.navigate(destination) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        } else if (isLoggedIn == false) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🍎", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("ClearChain",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Surplus Food Clearance Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary)
        }
    }
}