package com.clearchain.app.domain.repository

import com.clearchain.app.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeAll(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markAsRead(id: String)
    suspend fun markAllAsRead()
    suspend fun insert(notification: AppNotification)
    suspend fun clearAll()
}
