package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.clearchain.app.util.HapticUtils

/**
 * Drop-in replacement for [PullToRefreshBox] that fires a haptic pulse
 * the moment the user triggers a refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh    = { HapticUtils.refresh(context); onRefresh() },
        modifier     = modifier,
        content      = content
    )
}
