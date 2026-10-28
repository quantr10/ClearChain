package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A [Scaffold] for screens with a floating action button, whose snackbar sits at the bottom and
 * covers the FAB until it goes away. Material 3's Scaffold lifts the snackbar above the FAB
 * instead, which leaves it floating over the middle of the screen.
 */
@Composable
fun FabScaffold(
    snackbarHostState: SnackbarHostState,
    floatingActionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable (PaddingValues) -> Unit
) {
    // The Scaffold's bottom padding clears the bottom bar and system bars, which is where its own
    // snackbar would sit if there were no FAB.
    var bottomPadding by remember { mutableStateOf(0.dp) }

    Box(modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            containerColor = containerColor
        ) { padding ->
            bottomPadding = padding.calculateBottomPadding()
            content(padding)
        }

        // Placed after the Scaffold so it draws on top of the FAB.
        SnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
        )
    }
}
