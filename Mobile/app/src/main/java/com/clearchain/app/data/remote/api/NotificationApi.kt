package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.NotificationListResponse
import retrofit2.http.*

interface NotificationApi {

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("unreadOnly") unreadOnly: Boolean = false
    ): NotificationListResponse

    @PUT("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: String): Any

    @PUT("notifications/read-all")
    suspend fun markAllAsRead(): Any

    /** Clears the whole inbox server-side — one call rather than a delete per row. */
    @DELETE("notifications")
    suspend fun deleteAllNotifications(): Any
}
