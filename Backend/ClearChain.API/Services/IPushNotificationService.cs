using ClearChain.API.DTOs.PickupRequests;

namespace ClearChain.API.Services;

public interface IPushNotificationService
{
    // Existing methods
    Task SendPickupApprovedNotification(Guid userId, PickupRequestData request);
    Task SendPickupReadyNotification(Guid userId, PickupRequestData request);
    Task SendPickupCompletedNotification(Guid userId, PickupRequestData request);
    Task SendPickupRejectedNotification(Guid userId, PickupRequestData request);
    Task SendInventoryAddedNotification(Guid userId, string productName, int quantity, string unit);
    
    // ✅ NEW: Grocery-side notifications
    Task SendPickupRequestCreatedNotification(Guid groceryId, PickupRequestData request);
    Task SendPickupRequestCancelledNotification(Guid groceryId, PickupRequestData request);
    Task SendPickupConfirmedNotification(Guid groceryId, PickupRequestData request);
}