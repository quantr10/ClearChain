package com.clearchain.app.presentation.notifications

import com.clearchain.app.domain.model.AppNotification
import com.clearchain.app.domain.model.NotificationRetention

data class NotificationInboxState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    /** The server's retention window, so the list can say how far back it goes. */
    val retention: NotificationRetention = NotificationRetention()
)
