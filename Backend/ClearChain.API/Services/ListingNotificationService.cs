using Microsoft.AspNetCore.SignalR;
using ClearChain.API.Hubs;
using ClearChain.API.DTOs.Listings;

namespace ClearChain.API.Services;

public class ListingNotificationService : IListingNotificationService
{
    private readonly IHubContext<ListingHub> _hubContext;
    private readonly ILogger<ListingNotificationService> _logger;

    public ListingNotificationService(
        IHubContext<ListingHub> hubContext,
        ILogger<ListingNotificationService> logger)
    {
        _hubContext = hubContext;
        _logger = logger;
    }

    public async Task NotifyListingCreated(ListingData listing)
    {
        try
        {
            // Notify all users browsing listings
            await _hubContext.Clients
                .Group("all_listings")
                .SendAsync("ListingCreated", listing);

            _logger.LogInformation($"Notified new listing created: {listing.Id}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending ListingCreated notification");
        }
    }

    public async Task NotifyListingUpdated(ListingData listing)
    {
        try
        {
            // Notify all users and specific listing room
            await _hubContext.Clients
                .Groups("all_listings", $"listing_{listing.Id}")
                .SendAsync("ListingUpdated", listing);

            _logger.LogInformation($"Notified listing updated: {listing.Id}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending ListingUpdated notification");
        }
    }

    public async Task NotifyListingDeleted(string listingId)
    {
        try
        {
            var notification = new
            {
                ListingId = listingId,
                Timestamp = DateTime.UtcNow
            };

            await _hubContext.Clients
                .Groups("all_listings", $"listing_{listingId}")
                .SendAsync("ListingDeleted", notification);

            _logger.LogInformation($"Notified listing deleted: {listingId}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending ListingDeleted notification");
        }
    }

    public async Task NotifyListingQuantityChanged(ListingData listing, int oldQuantity, int newQuantity)
    {
        try
        {
            var notification = new
            {
                Listing = listing,
                OldQuantity = oldQuantity,
                NewQuantity = newQuantity,
                Timestamp = DateTime.UtcNow
            };

            await _hubContext.Clients
                .Groups("all_listings", $"listing_{listing.Id}")
                .SendAsync("ListingQuantityChanged", notification);

            _logger.LogInformation(
                $"Notified quantity change for listing {listing.Id}: {oldQuantity} → {newQuantity}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending ListingQuantityChanged notification");
        }
    }
}