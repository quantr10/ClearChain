package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.SavedListingIdsResponse
import com.clearchain.app.data.remote.dto.SavedListingsResponse
import com.clearchain.app.data.remote.dto.SavedListingToggleResponse
import retrofit2.http.*

interface SavedListingApi {

    @POST("savedlistings/{listingId}")
    suspend fun saveListing(@Path("listingId") listingId: String): SavedListingToggleResponse

    @DELETE("savedlistings/{listingId}")
    suspend fun unsaveListing(@Path("listingId") listingId: String): SavedListingToggleResponse

    @GET("savedlistings")
    suspend fun getSavedListings(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): SavedListingsResponse

    @GET("savedlistings/ids")
    suspend fun getSavedListingIds(): SavedListingIdsResponse
}
