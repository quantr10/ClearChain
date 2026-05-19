package com.clearchain.app.data.repository

import com.clearchain.app.data.local.dao.NotificationDao
import com.clearchain.app.data.local.entity.toEntity
import com.clearchain.app.data.local.entity.toDomain
import com.clearchain.app.domain.model.AppNotification
import com.clearchain.app.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun observeAll(): Flow<List<AppNotification>> =
        notificationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeUnreadCount(): Flow<Int> =
        notificationDao.observeUnreadCount()

    override suspend fun markAsRead(id: String) =
        notificationDao.markAsRead(id)

    override suspend fun markAllAsRead() =
        notificationDao.markAllAsRead()

    override suspend fun insert(notification: AppNotification) =
        notificationDao.insert(notification.toEntity())

    override suspend fun clearAll() =
        notificationDao.clearAll()
}
