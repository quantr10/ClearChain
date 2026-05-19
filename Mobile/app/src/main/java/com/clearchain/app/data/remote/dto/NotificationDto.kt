package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val relatedId: String? = null,
    val relatedType: String? = null,
    val isRead: Boolean = false,
    val createdAt: String,
    val readAt: String? = null
)

@Serializable
data class NotificationListResponse(
    val message: String = "",
    val data: List<NotificationData> = emptyList(),
    val unreadCount: Int = 0,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int = 1
)

@Serializable
data class UnreadCountResponse(
    val unreadCount: Int = 0
)
