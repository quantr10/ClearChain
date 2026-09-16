package com.clearchain.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.rememberNavController
import com.clearchain.app.data.local.SessionManager
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.presentation.components.ReconnectingBanner
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.navigation.BottomNavBar
import com.clearchain.app.presentation.navigation.NavGraph
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.ClearChainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var signalRService: SignalRService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Notification permission granted")
        } else {
            Log.d("MainActivity", "⚠️ Notification permission denied")
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Apply stored locale before the Activity inflates any resources
        val lang = newBase.getSharedPreferences("settings_sync", Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Handle notification deep link FIRST
        handleNotificationIntent(intent)
        
        enableEdgeToEdge()
        
        // Request notification permission
        requestNotificationPermission()
        
        setContent {
            ClearChainTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    var showBottomBar by remember { mutableStateOf(false) }
                    var userType by remember { mutableStateOf<OrganizationType?>(null) }

                    // TokenAuthenticator fires this when a token refresh itself fails (refresh
                    // token expired/revoked too) — it has no UI access of its own from a
                    // background OkHttp thread, so it just signals here and this bounces the
                    // user back to Login instead of leaving them stuck on a dead session.
                    LaunchedEffect(Unit) {
                        sessionManager.sessionExpired.collect {
                            showBottomBar = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.msg_session_expired),
                                Toast.LENGTH_LONG
                            ).show()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                    // Real-time now backs most screens, so a dropped connection has to be
                    // visible — otherwise stale numbers look like current ones. The
                    // snackbarHost slot puts it above the nav bar with insets already handled.
                    val connectionState by signalRService.connectionState.collectAsState()

                    Scaffold(
                        snackbarHost = { ReconnectingBanner(connectionState) },
                        bottomBar = {
                            if (showBottomBar && userType != null) {
                                BottomNavBar(
                                    navController = navController,
                                    userType = userType!!
                                )
                            }
                        }
                    ) { paddingValues ->
                        NavGraph(
                            navController = navController,
                            modifier = Modifier
                                .padding(paddingValues)
                                .consumeWindowInsets(paddingValues),
                            onShowBottomBar = { show, type ->
                                showBottomBar = show
                                // Profile sub-screens share the current user's nav bar.
                                // Keep the last resolved type when a sub-screen has no type of its own.
                                if (type != null || !show) {
                                    userType = type
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ✅ Handle new intents when app is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)

        // A verification decision can flip which screen the current session should be on
        // (e.g. an org sitting on PendingReview/Settings gets approved). The running
        // NavGraph won't re-run its Splash gate on its own, so force a clean restart —
        // recreate() re-enters onCreate and NavGraph starts at Splash again, which re-checks
        // /auth/me and routes correctly.
        val notificationType = intent.getStringExtra("type")
        if (notificationType == "verification_approved" || notificationType == "verification_rejected") {
            recreate()
        }
    }

    // ✅ Handle notification deep link
    private fun handleNotificationIntent(intent: Intent) {
        val screen = intent.getStringExtra("screen")
        val requestId = intent.getStringExtra("requestId")
        
        Log.d("MainActivity", "📱 Intent received - screen: $screen, requestId: $requestId")
        
        // Store deep link data to be handled after login check
        if (screen != null) {
            getSharedPreferences("deeplink", MODE_PRIVATE)
                .edit()
                .putString("pending_screen", screen)
                .apply {
                    if (requestId != null) {
                        putString("pending_request_id", requestId)
                    }
                }
                .apply()
            
            Log.d("MainActivity", "✅ Deep link saved: screen=$screen, requestId=$requestId")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "✅ Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("MainActivity", "⚠️ User previously denied notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("MainActivity", "✅ Notification permission not required (Android < 13)")
        }
    }
}
