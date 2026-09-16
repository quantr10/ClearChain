package com.clearchain.app.domain.model

/**
 * The server's notification retention window, reported by the inbox endpoint.
 *
 * The inbox shows everything inside it, so the screen states the window rather than letting an
 * empty-looking list read as "nothing ever happened". The default matches the server's own
 * policy and is only used before the first successful sync.
 */
data class NotificationRetention(
    val retentionDays: Int = 30
)
