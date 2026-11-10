package com.clearchain.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A server-paged feed, newest first, as far as it has been loaded. */
data class PagedFeed<T>(
    val items: List<T> = emptyList(),
    /** Everything the server has across all pages; [items] may hold only the first few. */
    val total: Int = 0,
    val isLoading: Boolean = false,
    /** The last next page didn't arrive. What was loaded before it stays. */
    val loadMoreFailed: Boolean = false,
    val endReached: Boolean = false
)

/** One page as the server sent it. */
data class FeedPage<T>(
    val items: List<T>,
    val total: Int,
    val totalPages: Int
)

/**
 * Walks a paged endpoint into a [PagedFeed]: [refresh] reloads it from the first page, and
 * [loadMore] adds the next one. A dashboard card shows the first few items and its sheet the
 * rest, so both read the same feed.
 *
 * Loads run one at a time. A refresh that arrives while a next page is in flight waits for it
 * and then replaces it, instead of a late page landing after the new first page.
 */
class FeedPager<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int,
    private val id: (T) -> String,
    private val fetch: suspend (page: Int, pageSize: Int) -> FeedPage<T>
) {
    private val _state = MutableStateFlow(PagedFeed<T>())
    val state: StateFlow<PagedFeed<T>> = _state.asStateFlow()

    private val mutex = Mutex()
    private var loadedPages = 0

    /**
     * Loads the feed again from the first page, as deep as it had been read, so a sheet paged
     * down doesn't snap back to its first page when the dashboard reloads under it. Returns
     * whether it all arrived; on a failure the feed keeps what it showed before, like every
     * other dashboard section.
     */
    suspend fun refresh(): Boolean = mutex.withLock {
        load(pages = 1..maxOf(1, loadedPages), onto = emptyList())
    }

    /** Fetches the next page onto the end, unless one is already on its way or there is none. */
    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.endReached) return
        // Marked before launching, so a second tap can't queue the same page twice.
        _state.update { it.copy(isLoading = true) }
        scope.launch {
            mutex.withLock { load(pages = loadedPages + 1..loadedPages + 1, onto = _state.value.items) }
        }
    }

    private suspend fun load(pages: IntRange, onto: List<T>): Boolean {
        _state.update { it.copy(isLoading = true, loadMoreFailed = false) }
        return try {
            var items = onto
            var total = 0
            var lastPage = pages.first
            var endReached = false
            for (page in pages) {
                val result = fetch(page, pageSize)
                // A page boundary shifts when something newer arrives in between, so a page
                // can repeat the last item of the one before it.
                items = (items + result.items).distinctBy(id)
                total = result.total
                lastPage = page
                endReached = page >= result.totalPages || result.items.isEmpty()
                if (endReached) break
            }
            _state.value = PagedFeed(items, total, endReached = endReached)
            loadedPages = lastPage
            true
        } catch (_: Exception) {
            _state.update { it.copy(isLoading = false, loadMoreFailed = pages.first > 1) }
            false
        }
    }
}
