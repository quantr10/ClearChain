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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.BrandGradient
import com.clearchain.app.presentation.components.LightStatusBarIcons
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.Alpha
import com.clearchain.app.ui.theme.HeaderPadding
import com.clearchain.app.ui.theme.ListGap
import com.clearchain.app.ui.theme.LogoDiscSplashSize
import com.clearchain.app.ui.theme.LogoMarkSplashSize
import com.clearchain.app.ui.theme.OnBrandGradient
import com.clearchain.app.ui.theme.ScreenHPadding
import com.clearchain.app.ui.theme.SpinnerAreaSize
import com.clearchain.app.ui.theme.WordmarkStyle
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // The disc stays where the system splash drew it; the name and tagline fade in under it.
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

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

                if (currentUser.requiresVerificationGate()) {
                    navController.navigate(Screen.PendingReview.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }

                // Deep link handling. Consumed at most once: cleared here regardless of
                // whether it maps to a route below, so a screen value we don't special-case
                // (e.g. a verification push's "grocery_dashboard"/"pending_review" — already
                // fully handled by the checks above) doesn't linger in prefs forever.
                val prefs = context.getSharedPreferences("deeplink", Context.MODE_PRIVATE)
                val pendingScreen = prefs.getString("pending_screen", null)
                val pendingRequestId = prefs.getString("pending_request_id", null)
                if (pendingScreen != null) {
                    prefs.edit { clear() }

                    // A pickup-request push carries the specific request's id, so route straight to it.
                    if ((pendingScreen == "my_requests" || pendingScreen == "grocery_requests") &&
                        pendingRequestId != null
                    ) {
                        navController.navigate(Screen.RequestDetail.createRoute(pendingRequestId)) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                        return@LaunchedEffect
                    }

                    val route = when (pendingScreen) {
                        "my_requests" -> Screen.MyRequests.route
                        "grocery_requests" -> Screen.PickupRequests.route
                        "browse_listings" -> Screen.BrowseListings.route
                        "my_listings" -> Screen.MyListings.route
                        "inventory" -> Screen.Inventory.route
                        else -> null
                    }
                    if (route != null) {
                        navController.navigate(route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                        return@LaunchedEffect
                    }
                }

                val destination = when (currentUser.type) {
                    OrganizationType.NGO -> Screen.NgoDashboard.route
                    OrganizationType.GROCERY -> Screen.GroceryDashboard.route
                    OrganizationType.ADMIN -> Screen.AdminDashboard.route
                }
                navController.navigate(destination) { popUpTo(Screen.Splash.route) { inclusive = true } }
            } else {
                navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
            }
        } else if (isLoggedIn == false) {
            navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
        }
    }

    LightStatusBarIcons()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandGradient)
    ) {
        // Equal space above and below the disc keeps its centre on the screen's centre, where
        // the system splash's splash_mark drew it.
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(LogoDiscSplashSize)
                    .clip(CircleShape)
                    .background(OnBrandGradient.copy(alpha = Alpha.LogoDisc)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    modifier = Modifier.size(LogoMarkSplashSize),
                    tint = OnBrandGradient
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = ScreenHPadding)
                    .padding(top = ListGap)
                    .graphicsLayer(alpha = alpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ListGap)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = WordmarkStyle,
                    color = OnBrandGradient
                )
                Text(
                    text = stringResource(R.string.splash_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBrandGradient.copy(alpha = Alpha.Subtitle),
                    textAlign = TextAlign.Center
                )
            }
        }

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = HeaderPadding)
                .size(SpinnerAreaSize),
            color = OnBrandGradient
        )
    }
}
