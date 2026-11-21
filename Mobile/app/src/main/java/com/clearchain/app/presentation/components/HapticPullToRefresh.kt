package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.clearchain.app.util.HapticUtils

/**
 * Pull-to-refresh that fires a haptic pulse the moment the user triggers a refresh.
 *
 * Built on [pullToRefresh] rather than Material's `PullToRefreshBox`, which can't be switched
 * off: [enabled] is false while there is nothing to refresh yet (a first load) or nothing a
 * refresh can fix (an error with its own Retry). [indicatorPadding] moves the indicator clear
 * of anything drawn over the top of the content, such as the status bar on a brand screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indicatorPadding: PaddingValues = PaddingValues(),
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val state = rememberPullToRefreshState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = isRefreshing,
                state = state,
                enabled = enabled
            ) {
                HapticUtils.refresh(context)
                onRefresh()
            }
    ) {
        content()
        PullToRefreshDefaults.Indicator(
            state = state,
            isRefreshing = isRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(indicatorPadding)
        )
    }
}
