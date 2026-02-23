package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.CreateListingRequest
import com.clearchain.app.data.remote.dto.ListingResponse
import com.clearchain.app.data.remote.dto.ListingsResponse
import com.clearchain.app.data.remote.dto.UpdateListingQuantityRequest

import retrofit2.http.*

interface ListingApi {

    @POST("listings")
    suspend fun createListing(
        @Body request: CreateListingRequest
    ): ListingResponse

    @GET("listings/grocery/my")
    suspend fun getMyListings(): ListingsResponse

    @GET("listings/{id}")
    suspend fun getListingById(
        @Path("id") id: String
    ): ListingResponse

    @GET("listings")
    suspend fun getAllListings(
        @Query("status") status: String? = null,
        @Query("category") category: String? = null
    ): ListingsResponse

    @PUT("listings/{id}")
    suspend fun updateListing(
        @Path("id") id: String,
        @Body request: CreateListingRequest
    ): ListingResponse

    @DELETE("listings/{id}")
    suspend fun deleteListing(
        @Path("id") id: String
    ): ListingResponse

    // data/remote/api/ListingApi.kt - Add this method:
    @PUT("listings/{id}/quantity")
    suspend fun updateListingQuantity(
        @Path("id") listingId: String,
        @Body request: UpdateListingQuantityRequest
    ): ListingResponse
}