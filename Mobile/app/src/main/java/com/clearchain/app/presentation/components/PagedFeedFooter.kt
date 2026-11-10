package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.util.PagedFeed

/**
 * The end of a sheet that pages through a [PagedFeed]: View more while the server has more,
 * a spinner on it while the next page loads, and a line saying so when that failed. The
 * message sits in the sheet itself because a snackbar would be hidden behind it.
 */
@Composable
fun PagedFeedFooter(
    feed: PagedFeed<*>,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (feed.endReached) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (feed.loadMoreFailed) {
            Text(
                text = stringResource(R.string.error_load_more),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        ClearChainOutlinedButton(
            text = stringResource(if (feed.loadMoreFailed) R.string.action_retry else R.string.action_view_more),
            onClick = onLoadMore,
            loading = feed.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
