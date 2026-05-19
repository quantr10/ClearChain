package com.clearchain.app.presentation.notifications

import com.clearchain.app.domain.model.AppNotification

data class NotificationInboxState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0
)
