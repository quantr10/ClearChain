package com.clearchain.app.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The app's snackbar: `inverse-on-surface` text on `inverse-surface`, with `shape-extra-small`
 * corners. A long message wraps onto as many lines as it needs rather than being cut off.
 */
@Composable
fun ClearChainSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = MaterialTheme.shapes.extraSmall,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}
