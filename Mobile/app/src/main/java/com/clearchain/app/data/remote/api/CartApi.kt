package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.*
import retrofit2.http.*

interface CartApi {
    @GET("cart")
    suspend fun getCart(): CartResponse

    @POST("cart/items")
    suspend fun addItem(@Body request: AddCartItemRequest): CartResponse

    @PUT("cart/items/{itemId}")
    suspend fun updateItem(
        @Path("itemId") itemId: String,
        @Body request: UpdateCartItemRequest
    ): CartResponse

    @POST("cart/checkout")
    suspend fun checkout(@Body request: CheckoutCartGroupRequest): PickupRequestResponse
}
