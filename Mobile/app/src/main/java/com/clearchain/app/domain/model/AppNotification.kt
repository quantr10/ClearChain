package com.clearchain.app.domain.model

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val relatedId: String?,
    val relatedType: String?,
    val isRead: Boolean,
    val createdAt: String,
    val readAt: String?
)
