package com.clearchain.app.domain.model

data class Message(
    val id: String,
    val pickupRequestId: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val content: String,
    val isRead: Boolean,
    val sentAt: String,
    val readAt: String?
)

data class MessageThread(
    val pickupRequestId: String,
    val lastMessage: Message,
    val unreadCount: Int
)
