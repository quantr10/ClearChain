package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.clearchain.app.R

/** What a list area shows. */
sealed interface ListAreaState {
    /** Nothing to show yet: the area spinner. Pull is off. */
    data object Loading : ListAreaState

    /** The first load failed: an error [EmptyState] with Retry. Pull is off. */
    data class Error(val title: String, val message: String?, val onRetry: () -> Unit) : ListAreaState

    /** Nothing to list. Pullable. */
    data class Empty(
        val icon: ImageVector,
        val title: String,
        val subtitle: String? = null,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : ListAreaState

    /** The list itself. Pullable. */
    data object Content : ListAreaState
}

/**
 * A list screen's content area: its states and its list share one pull-to-refresh area, so an
 * empty list can be pulled too, and a refresh that empties or fills the list keeps its indicator.
 *
 * Pull is on only for [ListAreaState.Empty] and [ListAreaState.Content]: during a first load
 * there is nothing to refresh yet, and an error has its own Retry. With a null [onRefresh] the
 * area has no pull at all. [content] is the list itself, shown for [ListAreaState.Content].
 */
@Composable
fun ListArea(
    state: ListAreaState,
    isRefreshing: Boolean,
    onRefresh: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (onRefresh != null) {
        HapticPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            enabled = state is ListAreaState.Empty || state is ListAreaState.Content
        ) {
            ListAreaBody(state, content)
        }
    } else {
        Box(modifier.fillMaxSize()) {
            ListAreaBody(state, content)
        }
    }
}

@Composable
private fun ListAreaBody(state: ListAreaState, content: @Composable () -> Unit) {
    if (state is ListAreaState.Content) {
        content()
    } else {
        // One item filling the area, so a pull on it reaches the pull area like a pull on a list.
        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "list_area_state") { ListAreaStateItem(state) }
        }
    }
}

@Composable
private fun LazyItemScope.ListAreaStateItem(state: ListAreaState) {
    val fill = Modifier.fillParentMaxSize()
    when (state) {
        ListAreaState.Loading -> LoadingState(fill)
        is ListAreaState.Error -> EmptyState(
            modifier = fill,
            icon = Icons.Default.ErrorOutline,
            title = state.title,
            subtitle = state.message,
            actionLabel = stringResource(R.string.retry),
            onAction = state.onRetry
        )
        is ListAreaState.Empty -> EmptyState(
            modifier = fill,
            icon = state.icon,
            title = state.title,
            subtitle = state.subtitle,
            actionLabel = state.actionLabel,
            onAction = state.onAction
        )
        ListAreaState.Content -> Unit
    }
}
