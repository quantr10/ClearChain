using ClearChain.API.DTOs.Listings;

namespace ClearChain.API.Services;

public interface IListingNotificationService
{
    Task NotifyListingCreatedAsync(ListingData listing);
    Task NotifyListingUpdatedAsync(ListingData listing);
    Task NotifyListingDeletedAsync(string listingId);
    Task NotifyListingQuantityChangedAsync(ListingData listing, int oldQuantity, int newQuantity);
}
