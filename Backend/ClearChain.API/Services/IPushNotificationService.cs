using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.DTOs.Listings;
using ClearChain.API.DTOs.Inventory;
using ClearChain.API.DTOs.Admin;

namespace ClearChain.API.Services;

public interface IPushNotificationService
{
    // ═══════════════════════════════════════════════════════════════════════════
    // Pickup Request Notifications
    // ═══════════════════════════════════════════════════════════════════════════
    Task SendPickupRequestCreatedNotification(Guid groceryId, PickupRequestData request);
    Task SendPickupRequestCancelledNotification(Guid groceryId, PickupRequestData request);
    Task SendPickupRejectedNotification(Guid userId, PickupRequestData request);
    Task SendPickupConfirmedNotification(Guid groceryId, PickupRequestData request);
    Task SendPickupApprovedNotification(Guid userId, PickupRequestData request);
    Task SendPickupReadyNotification(Guid userId, PickupRequestData request);
    Task SendPickupCompletedNotification(Guid userId, PickupRequestData request);
    Task SendInventoryAddedNotification(Guid userId, string productName, int quantity, string unit);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Listing Notifications
    // ═══════════════════════════════════════════════════════════════════════════
    Task SendNewListingNotificationToAllNGOs(ListingData listing);
    Task SendListingExpiringSoonNotification(Guid groceryId, ListingData listing);
    Task SendListingExpiredNotification(Guid groceryId, ListingData listing);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Inventory Notifications
    // ═══════════════════════════════════════════════════════════════════════════
    Task SendInventoryExpiringSoonNotification(Guid ngoId, InventoryItemData item);
    Task SendInventoryExpiredNotification(Guid ngoId, InventoryItemData item);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // User Onboarding & Admin Notifications
    // ═══════════════════════════════════════════════════════════════════════════
    Task SendWelcomeNotification(OrganizationData organization);    
    Task SendNewRegistrationAlertToAdmins(OrganizationData organization);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Generic notification sender
    // ═══════════════════════════════════════════════════════════════════════════
    Task SendNotification(Guid userId, string title, string body, Dictionary<string, string> data);
}