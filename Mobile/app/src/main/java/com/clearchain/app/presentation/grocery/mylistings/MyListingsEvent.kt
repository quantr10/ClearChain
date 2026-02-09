package com.clearchain.app.presentation.grocery.mylistings

sealed class MyListingsEvent {
    object LoadListings : MyListingsEvent()
    object RefreshListings : MyListingsEvent()
    data class DeleteListing(val listingId: String) : MyListingsEvent()
    object ClearError : MyListingsEvent()
}