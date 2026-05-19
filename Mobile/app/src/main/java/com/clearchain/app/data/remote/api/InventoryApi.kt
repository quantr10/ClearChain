package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.InventoryItemResponse
import com.clearchain.app.data.remote.dto.InventoryListResponse
import com.clearchain.app.data.remote.dto.UpdateInventoryItemRequest
import okhttp3.MultipartBody
import retrofit2.http.*

interface InventoryApi {

    @GET("inventory/my")
    suspend fun getMyInventory(
        @Query("status") status: String? = null
    ): InventoryListResponse

    @PUT("inventory/{id}/distribute")
    suspend fun distributeItem(
        @Path("id") id: String
    ): InventoryItemResponse

    @POST("inventory/update-expired")
    suspend fun updateExpiredItems(): InventoryItemResponse

    @GET("inventory/{id}")
    suspend fun getInventoryItemById(@Path("id") id: String): InventoryItemResponse

    @PUT("inventory/{id}")
    suspend fun updateInventoryItem(
        @Path("id") id: String,
        @Body request: UpdateInventoryItemRequest
    ): InventoryItemResponse

    @Multipart
    @POST("inventory/{id}/photo")
    suspend fun uploadInventoryPhoto(
        @Path("id") id: String,
        @Part photo: MultipartBody.Part
    ): InventoryItemResponse
}