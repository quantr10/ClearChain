package com.clearchain.app.data.repository

import android.util.Log
import com.clearchain.app.data.local.dao.NotificationDao
import com.clearchain.app.data.local.entity.toDomain
import com.clearchain.app.data.local.entity.toEntity
import com.clearchain.app.data.remote.api.NotificationApi
import com.clearchain.app.data.remote.dto.toEntity
import com.clearchain.app.domain.model.AppNotification
import com.clearchain.app.domain.model.NotificationRetention
import com.clearchain.app.domain.repository.NotificationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The inbox, cached in Room and backed by the server.
 *
 * Reads come from Room so the inbox renders instantly and works offline. Writes go to the
 * server first and are mirrored locally, because read state has to survive a reinstall and be
 * consistent across a user's devices — a local-only inbox showed every notification as unread
 * again on a new phone, and lost anything that arrived while the app was force-stopped.
 */
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val notificationApi: NotificationApi
) : NotificationRepository {

    private companion object {
        const val TAG = "NotificationRepo"

        /** The server caps pageSize at 50, so this is one round trip per 50 notifications. */
        const val SYNC_PAGE_SIZE = 50

        /**
         * A stop so a bad totalPages can't turn a sync into an unbounded request loop. At 50 a
         * page this covers 1,000 notifications in the retention window, far past what any real
         * inbox holds.
         */
        const val MAX_SYNC_PAGES = 20

        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }

    override fun observeAll(): Flow<List<AppNotification>> =
        notificationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeUnreadCount(): Flow<Int> =
        notificationDao.observeUnreadCount()

    override suspend fun sync(): Result<NotificationRetention> = try {
        var page = 1
        var synced = 0
        var retention = NotificationRetention()
        var unreadCount = 0

        // Walks every page rather than stopping at the first: the inbox promises everything the
        // server still retains, and a user who has been away a while can easily have more than
        // one page of it waiting.
        while (page <= MAX_SYNC_PAGES) {
            val response = notificationApi.getNotifications(page = page, pageSize = SYNC_PAGE_SIZE)

            // Upsert rather than replace: a push that landed a moment ago may not be on this
            // page yet, and dropping it would make a notification the user can see vanish.
            response.data.forEach { notificationDao.insert(it.toEntity()) }

            synced += response.data.size
            unreadCount = response.unreadCount
            retention = NotificationRetention(retentionDays = response.retentionDays)

            if (response.data.isEmpty() || page >= response.totalPages) break
            page++
        }

        // Room would otherwise keep rows the server has already swept, so the inbox would show
        // a longer history on an old install than on a fresh one.
        notificationDao.deleteOlderThan(
            System.currentTimeMillis() - retention.retentionDays * MILLIS_PER_DAY
        )

        Log.d(TAG, "Synced $synced notifications ($unreadCount unread) over $page page(s)")
        Result.success(retention)
    } catch (e: Exception) {
        // Offline is normal here — the cached inbox stays on screen.
        Log.w(TAG, "Inbox sync failed: ${e.message}")
        Result.failure(e)
    }

    override suspend fun markAsRead(id: String) {
        // Local first so the badge responds immediately; the server call reconciles behind it.
        notificationDao.markAsRead(id)
        runCatching { notificationApi.markAsRead(id) }
            .onFailure { Log.w(TAG, "Could not mark $id read on server: ${it.message}") }
    }

    override suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
        runCatching { notificationApi.markAllAsRead() }
            .onFailure { Log.w(TAG, "Could not mark all read on server: ${it.message}") }
    }

    override suspend fun insert(notification: AppNotification) =
        notificationDao.insert(notification.toEntity())

    override suspend fun clearAll() {
        notificationDao.clearAll()
        // Server-side too, or the next sync pulls everything the user just cleared back.
        runCatching { notificationApi.deleteAllNotifications() }
            .onFailure { Log.w(TAG, "Could not clear inbox on server: ${it.message}") }
    }
}
