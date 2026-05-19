package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.NotificationListResponse
import com.clearchain.app.data.remote.dto.UnreadCountResponse
import retrofit2.http.*

interface NotificationApi {

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("unreadOnly") unreadOnly: Boolean = false
    ): NotificationListResponse

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @PUT("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: String): Any

    @PUT("notifications/read-all")
    suspend fun markAllAsRead(): Any

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): Any
}
