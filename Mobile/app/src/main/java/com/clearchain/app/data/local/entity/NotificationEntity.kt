package com.clearchain.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clearchain.app.domain.model.AppNotification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val body: String,
    val relatedId: String? = null,
    val relatedType: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null
)

fun NotificationEntity.toDomain() = AppNotification(
    id = id,
    type = type,
    title = title,
    body = body,
    relatedId = relatedId,
    relatedType = relatedType,
    isRead = isRead,
    createdAt = createdAt.toString(),
    readAt = readAt?.toString()
)

fun AppNotification.toEntity() = NotificationEntity(
    id = id,
    type = type,
    title = title,
    body = body,
    relatedId = relatedId,
    relatedType = relatedType,
    isRead = isRead,
    createdAt = createdAt.toLongOrNull() ?: System.currentTimeMillis(),
    readAt = readAt?.toLongOrNull()
)
