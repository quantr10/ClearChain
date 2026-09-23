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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.clearchain.app.data.local.SessionManager
import com.clearchain.app.data.local.SettingsStore
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.navigation.BottomNavBar
import com.clearchain.app.presentation.navigation.NavGraph
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.ClearChainTheme
import com.clearchain.app.util.LocaleUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var settingsStore: SettingsStore

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "✅ Notification permission granted")
        } else {
            Log.d(TAG, "⚠️ Notification permission denied")
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Apply the stored locale before the Activity inflates any resources. Read straight from
        // SharedPreferences because DataStore is suspend-only and this runs before onCreate.
        val lang = newBase.getSharedPreferences(SettingsStore.SYNC_PREFS, Context.MODE_PRIVATE)
            .getString("language", SettingsStore.LANG_EN) ?: SettingsStore.LANG_EN
        super.attachBaseContext(LocaleUtils.wrap(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Handle notification deep link FIRST
        handleNotificationIntent(intent)

        enableEdgeToEdge()

        // Request notification permission
        requestNotificationPermission()

        // The mirror is seeded here so an install that picked a theme before the mirror
        // existed keeps it, rather than falling back to "system" on this launch.
        lifecycleScope.launch { settingsStore.primeSyncedTheme() }

        setContent {
            // syncedTheme() is the value already on disk, so the very first frame paints the
            // chosen palette; the flow then keeps it live when the setting changes.
            val storedTheme = remember { settingsStore.syncedTheme() }
            val theme by settingsStore.theme.collectAsState(initial = storedTheme)

            ClearChainTheme(
                darkTheme = when (theme) {
                    SettingsStore.THEME_LIGHT -> false
                    SettingsStore.THEME_DARK -> true
                    else -> isSystemInDarkTheme()
                }
            ) {
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

                    Scaffold(
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

        Log.d(TAG, "📱 Intent received - screen: $screen, requestId: $requestId")

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

            Log.d(TAG, "✅ Deep link saved: screen=$screen, requestId=$requestId")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "✅ Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d(TAG, "⚠️ User previously denied notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(TAG, "✅ Notification permission not required (Android < 13)")
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
