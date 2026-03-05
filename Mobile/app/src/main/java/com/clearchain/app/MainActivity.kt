package com.clearchain.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.navigation.BottomNavBar
import com.clearchain.app.presentation.navigation.NavGraph
import com.clearchain.app.ui.theme.ClearChainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Notification permission granted")
        } else {
            Log.d("MainActivity", "⚠️ Notification permission denied")
        }
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
                    
                    var showBottomBar by remember { mutableStateOf(false) }
                    var userType by remember { mutableStateOf<OrganizationType?>(null) }
                    
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
                            modifier = Modifier.padding(paddingValues),
                            onShowBottomBar = { show, type ->
                                showBottomBar = show
                                userType = type
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