package com.clearchain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.navigation.BottomNavBar
import com.clearchain.app.presentation.navigation.NavGraph
import com.clearchain.app.ui.theme.ClearChainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClearChainTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // State to track if bottom bar should be shown
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
}