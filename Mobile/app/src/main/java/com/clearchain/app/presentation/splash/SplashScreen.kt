package com.clearchain.app.presentation.splash

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Entrance animations
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.55f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logo_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    LaunchedEffect(Unit) { delay(100); visible = true }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == null) return@LaunchedEffect
        delay(1600)

        if (isLoggedIn == true) {
            val currentUser = viewModel.getCurrentUser()
            if (currentUser != null) {
                if (!currentUser.isProfileComplete()) {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }

                // Deep link handling
                val prefs = context.getSharedPreferences("deeplink", Context.MODE_PRIVATE)
                val pendingScreen = prefs.getString("pending_screen", null)
                if (pendingScreen != null) {
                    val route = when (pendingScreen) {
                        "my_requests" -> Screen.MyRequests.route
                        "browse_listings" -> Screen.BrowseListings.route
                        "inventory" -> Screen.Inventory.route
                        else -> null
                    }
                    if (route != null) {
                        prefs.edit().clear().apply()
                        navController.navigate(route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                        return@LaunchedEffect
                    }
                }

                val destination = when (currentUser.type) {
                    OrganizationType.NGO     -> Screen.NgoDashboard.route
                    OrganizationType.GROCERY -> Screen.GroceryDashboard.route
                    OrganizationType.ADMIN   -> Screen.AdminDashboard.route
                }
                navController.navigate(destination) { popUpTo(Screen.Splash.route) { inclusive = true } }
            } else {
                navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
            }
        } else if (isLoggedIn == false) {
            navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = listOf(BrandTeal, BrandGreen))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.scale(scale)
        ) {
            // Logo container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color.White
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer(alpha = alpha)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.splash_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
        }

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .size(28.dp),
            color = Color.White.copy(alpha = 0.55f),
            strokeWidth = 2.5.dp
        )
    }
}
