using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.DTOs.Listings;
using ClearChain.API.DTOs.Inventory;
using ClearChain.API.DTOs.Admin;

namespace ClearChain.API.Services;

/// <summary>
/// The single funnel every user-facing notification goes through. One call does three things:
/// persists a row in the notification inbox, pushes to every device the user has registered,
/// and emits the same notification over SignalR so a connected client updates immediately.
///
/// The inbox row is the durable record — FCM delivery is best-effort and SignalR only reaches
/// clients that happen to be connected, so neither can be the source of truth on its own.
/// </summary>
public interface IPushNotificationService
{
    // ── Pickup Request Notifications ─────────────────────────────────────────
    Task SendPickupRequestCreatedNotification(Guid groceryId, PickupRequestData request);
    Task SendPickupRequestCancelledNotification(Guid groceryId, PickupRequestData request);
    Task SendPickupRejectedNotification(Guid userId, PickupRequestData request);
    Task SendPickupApprovedNotification(Guid userId, PickupRequestData request);
    Task SendPickupReadyNotification(Guid userId, PickupRequestData request);
    Task SendPickupCompletedNotification(Guid userId, PickupRequestData request);
    Task SendInventoryAddedNotification(Guid userId, string productName, int quantity, string unit);

    // ── Listing Notifications ────────────────────────────────────────────────
    Task SendNewListingNotificationToAllNGOs(ListingData listing);
    Task SendListingExpiringSoonNotification(Guid groceryId, ListingData listing);
    Task SendListingExpiredNotification(Guid groceryId, ListingData listing);

    // ── Inventory Notifications ──────────────────────────────────────────────
    Task SendInventoryExpiringSoonNotification(Guid ngoId, InventoryItemData item);
    Task SendInventoryExpiredNotification(Guid ngoId, InventoryItemData item);

    // ── User Onboarding & Admin Notifications ────────────────────────────────
    Task SendWelcomeNotification(OrganizationData organization);
    Task SendNewRegistrationAlertToAdmins(OrganizationData organization);
    Task SendResubmissionAlertToAdmins(OrganizationData organization);
    Task SendVerificationApprovedNotification(Guid organizationId, string organizationType);
    Task SendVerificationRejectedNotification(Guid organizationId, string? reason);

    // ── Generic sender — persists, pushes and broadcasts one notification ────
    Task SendNotificationAsync(Guid userId, string title, string body, Dictionary<string, string> data);

    /// <summary>
    /// Drops one device's token at logout so the next person to use that device doesn't
    /// receive the previous account's notifications.
    /// </summary>
    Task RemoveTokenAsync(Guid userId, string token);
}
