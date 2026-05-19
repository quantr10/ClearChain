package com.clearchain.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageData(
    val id: String,
    val pickupRequestId: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val content: String,
    val isRead: Boolean = false,
    val sentAt: String,
    val readAt: String? = null
)

@Serializable
data class MessageListResponse(
    val message: String = "",
    val data: List<MessageData> = emptyList()
)

@Serializable
data class MessageThreadData(
    val pickupRequestId: String,
    val lastMessage: MessageData,
    val unreadCount: Int = 0
)

@Serializable
data class MessageThreadsResponse(
    val message: String = "",
    val data: List<MessageThreadData> = emptyList()
)

@Serializable
data class SendMessageRequest(
    val content: String
)
