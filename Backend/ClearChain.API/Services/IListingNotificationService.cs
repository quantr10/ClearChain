using ClearChain.API.DTOs.Listings;

namespace ClearChain.API.Services;

public interface IListingNotificationService
{
    Task NotifyListingCreated(ListingData listing);
    Task NotifyListingUpdated(ListingData listing);
    Task NotifyListingDeleted(string listingId);
    Task NotifyListingQuantityChanged(ListingData listing, int oldQuantity, int newQuantity);
}