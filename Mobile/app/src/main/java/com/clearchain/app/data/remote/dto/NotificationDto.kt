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
    val totalPages: Int = 1,
    /** How long the server keeps a notification — the window this list was drawn from. */
    val retentionDays: Int = 30
)

/**
 * Server timestamps arrive in .NET's round-trip ("o") format; Room stores epoch millis.
 * Falls back to "now" so a notification with an unreadable timestamp still reaches the
 * inbox rather than being dropped.
 */
private fun String?.toEpochMillis(): Long? {
    if (this.isNullOrBlank()) return null
    return runCatching { java.time.Instant.parse(this).toEpochMilli() }
        .recoverCatching { java.time.OffsetDateTime.parse(this).toInstant().toEpochMilli() }
        .recoverCatching { java.time.LocalDateTime.parse(this).toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }
        .getOrNull()
}

fun NotificationData.toEntity() = com.clearchain.app.data.local.entity.NotificationEntity(
    id = id,
    type = type,
    title = title,
    body = body,
    relatedId = relatedId,
    relatedType = relatedType,
    isRead = isRead,
    createdAt = createdAt.toEpochMillis() ?: System.currentTimeMillis(),
    readAt = readAt.toEpochMillis()
)
