package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.InventoryItemResponse
import com.clearchain.app.data.remote.dto.InventoryListResponse
import com.clearchain.app.data.remote.dto.UpdateExpiredItemsResponse
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
    suspend fun updateExpiredItems(): UpdateExpiredItemsResponse

    @GET("inventory/{id}")
    suspend fun getInventoryItemById(@Path("id") id: String): InventoryItemResponse
}
