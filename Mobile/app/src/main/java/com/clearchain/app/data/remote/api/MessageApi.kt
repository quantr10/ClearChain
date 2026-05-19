package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.MessageListResponse
import com.clearchain.app.data.remote.dto.MessageThreadsResponse
import com.clearchain.app.data.remote.dto.SendMessageRequest
import retrofit2.http.*

interface MessageApi {

    @GET("messages/pickup/{requestId}")
    suspend fun getMessages(@Path("requestId") requestId: String): MessageListResponse

    @POST("messages/pickup/{requestId}")
    suspend fun sendMessage(
        @Path("requestId") requestId: String,
        @Body request: SendMessageRequest
    ): MessageListResponse

    @GET("messages/threads")
    suspend fun getThreads(): MessageThreadsResponse
}
