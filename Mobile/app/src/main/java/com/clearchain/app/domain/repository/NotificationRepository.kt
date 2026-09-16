package com.clearchain.app.domain.repository

import com.clearchain.app.domain.model.AppNotification
import com.clearchain.app.domain.model.NotificationRetention
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeAll(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>

    /**
     * Pulls the server's whole retained inbox into the local cache, page by page. Needed
     * because a push can be dropped, throttled, or arrive while the app is force-stopped — the
     * server's copy is the record, Room is only what we can read offline.
     *
     * Returns the retention window the server reported, so the inbox can say what it is showing.
     */
    suspend fun sync(): Result<NotificationRetention>

    suspend fun markAsRead(id: String)
    suspend fun markAllAsRead()
    suspend fun insert(notification: AppNotification)
    suspend fun clearAll()
}
