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
 *
 * [maxPageSize] is the largest page the endpoint serves, which lets a refresh reload several
 * pages in one request.
 */
class FeedPager<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int,
    private val maxPageSize: Int,
    private val id: (T) -> String,
    private val fetch: suspend (page: Int, pageSize: Int) -> FeedPage<T>
) {
    private val _state = MutableStateFlow(PagedFeed<T>())
    val state: StateFlow<PagedFeed<T>> = _state.asStateFlow()

    private val mutex = Mutex()
    private var loadedPages = 0

    /** Items as deep as the feed has been read, and at least its first page. */
    private val depth get() = maxOf(1, loadedPages) * pageSize

    /**
     * Loads the feed again from the first page, as deep as it had been read, so a sheet paged
     * down doesn't snap back to its first page when the dashboard reloads under it. Returns
     * whether it all arrived; on a failure the feed keeps what it showed before, like every
     * other dashboard section.
     */
    suspend fun refresh(): Boolean = mutex.withLock { reload() }

    /**
     * [refresh] from [first], a first page the caller already fetched at a larger size for its
     * own use. It only goes to the network when [first] doesn't reach as deep as the feed was read.
     */
    suspend fun refreshFrom(first: FeedPage<T>): Boolean = mutex.withLock {
        val wanted = depth
        if (first.items.size < wanted && first.items.size < first.total) return@withLock reload()
        val kept = first.items.take(wanted)
        _state.value = PagedFeed(kept, first.total, endReached = kept.size >= first.total)
        loadedPages = wanted / pageSize
        true
    }

    /** Fetches the next page onto the end, unless one is already on its way or there is none. */
    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.endReached) return
        // Marked before launching, so a second tap can't queue the same page twice.
        _state.update { it.copy(isLoading = true) }
        scope.launch {
            mutex.withLock {
                attempt(isNextPage = true) {
                    val next = loadedPages + 1
                    val result = fetch(next, pageSize)
                    _state.value = PagedFeed(
                        merge(_state.value.items, result.items),
                        result.total,
                        endReached = next >= result.totalPages || result.items.isEmpty()
                    )
                    loadedPages = next
                }
            }
        }
    }

    /** The body of [refresh]; the caller holds [mutex]. */
    private suspend fun reload(): Boolean = attempt(isNextPage = false) {
        val wanted = depth
        // Every request asks for the same size, so each one starts where the one before it ended.
        val size = minOf(wanted, maxOf(1, maxPageSize / pageSize) * pageSize)
        var items = emptyList<T>()
        var page = 0
        var last: FeedPage<T>
        do {
            last = fetch(++page, size)
            items = merge(items, last.items)
        } while (items.size < wanted && page < last.totalPages && last.items.isNotEmpty())

        val endReached = items.size <= wanted && (page >= last.totalPages || last.items.isEmpty())
        _state.value = PagedFeed(items.take(wanted), last.total, endReached = endReached)
        loadedPages = minOf(page * size, wanted) / pageSize
    }

    /**
     * A page boundary shifts when something newer arrives in between, so a page can repeat
     * the last item of the one before it.
     */
    private fun merge(onto: List<T>, page: List<T>): List<T> = (onto + page).distinctBy(id)

    private suspend fun attempt(isNextPage: Boolean, load: suspend () -> Unit): Boolean {
        _state.update { it.copy(isLoading = true, loadMoreFailed = false) }
        return try {
            load()
            true
        } catch (_: Exception) {
            _state.update { it.copy(isLoading = false, loadMoreFailed = isNextPage) }
            false
        }
    }
}
