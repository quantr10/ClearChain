using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Google.Apis.Auth.OAuth2;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.DTOs.Listings;
using ClearChain.API.DTOs.Admin;
using ClearChain.API.DTOs.Inventory;
using ClearChain.API.DTOs.Notifications;
using ClearChain.API.Hubs;
using ClearChain.Domain.Entities;

// FirebaseAdmin.Messaging also defines a Notification; alias the entity so the two never blur.
using NotificationRow = ClearChain.Domain.Entities.Notification;

namespace ClearChain.API.Services;

/// <summary>
/// Every user-facing notification goes through <see cref="SendNotificationAsync"/>, which persists
/// an inbox row, broadcasts it over SignalR and pushes it to the user's devices.
///
/// The three legs fail independently on purpose: a device with a stale FCM token, or a client
/// that isn't connected, must not stop the notification from being recorded. The inbox row is
/// what the app reads on next launch, so it is written first and its id travels with the push
/// and the broadcast to let the client dedupe the same notification arriving twice.
/// </summary>
public class PushNotificationService : IPushNotificationService
{
    /// <summary>FCM rejects multicasts larger than this.</summary>
    private const int MulticastBatchSize = 500;

    private readonly ApplicationDbContext _context;
    private readonly IHubContext<NotificationHub> _hubContext;
    private readonly ILogger<PushNotificationService> _logger;

    private static readonly object InitLock = new();
    private static bool _firebaseInitialized;

    public PushNotificationService(
        ApplicationDbContext context,
        IHubContext<NotificationHub> hubContext,
        ILogger<PushNotificationService> logger)
    {
        _context = context;
        _hubContext = hubContext;
        _logger = logger;

        InitializeFirebase();
    }

    private void InitializeFirebase()
    {
        if (_firebaseInitialized) return;

        lock (InitLock)
        {
            if (_firebaseInitialized) return;

            try
            {
                // An app created by an earlier instance still counts as initialized — the old
                // code only set the flag on the branch that created it, so every later instance
                // fell through and silently skipped every push.
                if (FirebaseApp.DefaultInstance != null)
                {
                    _firebaseInitialized = true;
                    return;
                }

                var credentialPath = Path.Combine(Directory.GetCurrentDirectory(), "firebase-adminsdk.json");

                if (!File.Exists(credentialPath))
                {
                    _logger.LogWarning("⚠️ firebase-adminsdk.json not found. Push delivery disabled — " +
                                       "notifications are still recorded and delivered over SignalR.");
                    return;
                }

                FirebaseApp.Create(new AppOptions
                {
                    Credential = GoogleCredential.FromFile(credentialPath)
                });

                _firebaseInitialized = true;
                _logger.LogInformation("✅ Firebase Admin SDK initialized successfully");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "❌ Failed to initialize Firebase Admin SDK");
            }
        }
    }

    // ── Pickup Request Notifications ─────────────────────────────────────────

    public Task SendPickupRequestCreatedNotification(Guid groceryId, PickupRequestData request) =>
        SendNotificationAsync(
            groceryId,
            "📝 New Pickup Request",
            $"{request.NgoName} requested {request.RequestedQuantity} of {request.ListingTitle}",
            new Dictionary<string, string>
            {
                { "type", "pickup_request_created" },
                { "requestId", request.Id },
                { "screen", "grocery_requests" }
            });

    public Task SendPickupApprovedNotification(Guid userId, PickupRequestData request) =>
        SendNotificationAsync(
            userId,
            "🎉 Pickup Request Approved!",
            $"Your request for {request.ListingTitle} has been approved by {request.GroceryName}",
            new Dictionary<string, string>
            {
                { "type", "pickup_approved" },
                { "requestId", request.Id },
                { "screen", "my_requests" }
            });

    public Task SendPickupReadyNotification(Guid userId, PickupRequestData request) =>
        SendNotificationAsync(
            userId,
            "✅ Food Ready!",
            $"{request.ListingTitle} is ready at {request.GroceryName}",
            new Dictionary<string, string>
            {
                { "type", "pickup_ready" },
                { "requestId", request.Id },
                { "screen", "my_requests" }
            });

    public Task SendPickupCompletedNotification(Guid userId, PickupRequestData request) =>
        SendNotificationAsync(
            userId,
            "🎊 Pickup Completed!",
            $"Thank you! {request.RequestedQuantity} {request.ListingTitle} marked as received",
            new Dictionary<string, string>
            {
                { "type", "pickup_completed" },
                { "requestId", request.Id },
                { "screen", "inventory" }
            });

    public Task SendPickupRejectedNotification(Guid userId, PickupRequestData request) =>
        SendNotificationAsync(
            userId,
            "❌ Pickup Request Rejected",
            $"Your request for {request.ListingTitle} was declined by {request.GroceryName}",
            new Dictionary<string, string>
            {
                { "type", "pickup_rejected" },
                { "requestId", request.Id },
                { "screen", "browse_listings" }
            });

    public Task SendPickupRequestCancelledNotification(Guid groceryId, PickupRequestData request) =>
        SendNotificationAsync(
            groceryId,
            "❌ Pickup Request Cancelled",
            $"{request.NgoName} cancelled pickup for {request.ListingTitle}",
            new Dictionary<string, string>
            {
                { "type", "pickup_cancelled" },
                { "requestId", request.Id },
                { "screen", "grocery_requests" }
            });

    public Task SendInventoryAddedNotification(Guid userId, string productName, int quantity, string unit) =>
        SendNotificationAsync(
            userId,
            "📦 New Inventory Added!",
            $"{quantity} {unit} of {productName} added to your inventory",
            new Dictionary<string, string>
            {
                { "type", "inventory_added" },
                { "screen", "inventory" }
            });

    // ── Listing Notifications ────────────────────────────────────────────────

    public Task SendNewListingNotificationToAllNGOs(ListingData listing) =>
        SendToOrganizationType(
            "ngo",
            "🆕 New Food Available!",
            $"{listing.GroceryName} posted {listing.Quantity} {listing.Unit} {listing.Title}",
            new Dictionary<string, string>
            {
                { "type", "new_listing" },
                { "listingId", listing.Id },
                { "groceryId", listing.GroceryId },
                { "screen", "browse_listings" }
            });

    public Task SendListingExpiringSoonNotification(Guid groceryId, ListingData listing) =>
        SendNotificationAsync(
            groceryId,
            "⚠️ Listing Expiring Soon",
            $"{listing.Title} expires in {DaysUntil(listing.ExpiryDate)} day(s)! Consider clearance.",
            new Dictionary<string, string>
            {
                { "type", "listing_expiring_soon" },
                { "listingId", listing.Id },
                { "screen", "my_listings" }
            });

    public Task SendListingExpiredNotification(Guid groceryId, ListingData listing) =>
        SendNotificationAsync(
            groceryId,
            "🚨 Listing Expired",
            $"{listing.Title} has expired. Please update or remove this listing.",
            new Dictionary<string, string>
            {
                { "type", "listing_expired" },
                { "listingId", listing.Id },
                { "screen", "my_listings" }
            });

    // ── Inventory Notifications ──────────────────────────────────────────────

    public Task SendInventoryExpiringSoonNotification(Guid ngoId, InventoryItemData item) =>
        SendNotificationAsync(
            ngoId,
            "⚠️ Food Expiring Soon",
            $"{item.ProductName} ({item.Quantity} {item.Unit}) expires in {DaysUntil(item.ExpiryDate)} day(s)!",
            new Dictionary<string, string>
            {
                { "type", "inventory_expiring_soon" },
                { "inventoryId", item.Id },
                { "screen", "inventory" }
            });

    public Task SendInventoryExpiredNotification(Guid ngoId, InventoryItemData item) =>
        SendNotificationAsync(
            ngoId,
            "🚨 Expired Food Alert",
            $"{item.ProductName} has expired. Please remove from inventory.",
            new Dictionary<string, string>
            {
                { "type", "inventory_expired" },
                { "inventoryId", item.Id },
                { "screen", "inventory" }
            });

    // ── User Onboarding & Admin Notifications ────────────────────────────────

    public Task SendWelcomeNotification(OrganizationData organization)
    {
        if (!Guid.TryParse(organization.Id, out var organizationId))
        {
            _logger.LogWarning("Welcome notification skipped — unparseable organization id {Id}", organization.Id);
            return Task.CompletedTask;
        }

        var actionText = organization.Type.Equals("ngo", StringComparison.OrdinalIgnoreCase)
            ? "Start browsing available food items!"
            : "Start posting surplus food items!";

        return SendNotificationAsync(
            organizationId,
            "👋 Welcome to ClearChain!",
            $"Hi {organization.Name}! Thank you for joining ClearChain. {actionText}",
            new Dictionary<string, string>
            {
                { "type", "welcome" },
                { "organizationType", organization.Type },
                { "screen", DashboardFor(organization.Type) }
            });
    }

    public Task SendNewRegistrationAlertToAdmins(OrganizationData organization) =>
        SendToOrganizationType(
            "admin",
            "📢 New Registration",
            $"{organization.Name} ({organization.Type}) needs verification",
            new Dictionary<string, string>
            {
                { "type", "new_registration" },
                { "organizationId", organization.Id },
                { "organizationType", organization.Type },
                { "screen", "admin_verification" }
            });

    public Task SendResubmissionAlertToAdmins(OrganizationData organization) =>
        SendToOrganizationType(
            "admin",
            "🔄 Resubmission for review",
            $"{organization.Name} ({organization.Type}) updated their profile after rejection — needs re-review",
            new Dictionary<string, string>
            {
                { "type", "organization_resubmitted" },
                { "organizationId", organization.Id },
                { "organizationType", organization.Type },
                { "screen", "admin_verification" }
            });

    public Task SendVerificationApprovedNotification(Guid organizationId, string organizationType) =>
        SendNotificationAsync(
            organizationId,
            "✅ Your organization has been approved!",
            "Welcome aboard — your ClearChain account is now fully active.",
            new Dictionary<string, string>
            {
                { "type", "verification_approved" },
                { "organizationType", organizationType },
                { "screen", DashboardFor(organizationType) }
            });

    public Task SendVerificationRejectedNotification(Guid organizationId, string? reason) =>
        SendNotificationAsync(
            organizationId,
            "⚠️ Verification not approved",
            string.IsNullOrWhiteSpace(reason)
                ? "Your verification was not approved. Please review your profile and documents, then resubmit."
                : $"Your verification was not approved: {reason}",
            new Dictionary<string, string>
            {
                { "type", "verification_rejected" },
                { "screen", "pending_review" }
            });

    // ── The funnel ───────────────────────────────────────────────────────────

    public async Task SendNotificationAsync(
        Guid userId,
        string title,
        string body,
        Dictionary<string, string> data)
    {
        var dto = await PersistAsync(userId, title, body, data);
        if (dto == null) return;

        await BroadcastAsync(userId, dto);
        await PushToUserAsync(userId, title, body, data, dto.Id);
    }

    /// <summary>
    /// Fans one notification out to every organization of a given type (all NGOs, all admins).
    /// Inbox rows are written in a single batch, then the push goes out as one multicast per
    /// 500 tokens rather than a send per recipient.
    /// </summary>
    private async Task SendToOrganizationType(
        string organizationType,
        string title,
        string body,
        Dictionary<string, string> data)
    {
        // Unverified organizations are blocked from the screens these notifications point at
        // (see RequireVerifiedOrganizationAttribute), so notifying them is noise they can't act
        // on — and it would fill their inbox before they are even approved.
        var recipientIds = await _context.Organizations
            .Where(o => o.Type.ToLower() == organizationType.ToLower() && o.Verified && !o.IsDeleted)
            .Select(o => o.Id)
            .ToListAsync();

        if (recipientIds.Count == 0)
        {
            _logger.LogInformation("No verified {Type} organizations to notify", organizationType);
            return;
        }

        var (relatedId, relatedType) = DeriveRelation(data);
        var type = data.GetValueOrDefault("type", "general");
        var now = DateTime.UtcNow;

        var rows = recipientIds.Select(id => new NotificationRow
        {
            Id = Guid.NewGuid(),
            RecipientId = id,
            Type = type,
            Title = title,
            Body = body,
            RelatedId = relatedId,
            RelatedType = relatedType,
            IsRead = false,
            CreatedAt = now
        }).ToList();

        try
        {
            _context.Notifications.AddRange(rows);
            await _context.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to persist {Count} {Type} notifications", rows.Count, organizationType);
            return;
        }

        foreach (var row in rows)
        {
            await BroadcastAsync(row.RecipientId, MapToDto(row));
        }

        var tokens = await _context.FCMTokens
            .Where(t => recipientIds.Contains(t.OrganizationId))
            .Select(t => t.Token)
            .Distinct()
            .ToListAsync();

        // The inbox id is per-recipient here, so the multicast carries none. A client that
        // receives both this push and its SignalR twin dedupes on type + relatedId instead.
        await PushToTokensAsync(tokens, title, body, data, notificationId: null);
    }

    private async Task<NotificationDto?> PersistAsync(
        Guid userId,
        string title,
        string body,
        IReadOnlyDictionary<string, string> data)
    {
        try
        {
            var (relatedId, relatedType) = DeriveRelation(data);

            var notification = new NotificationRow
            {
                Id = Guid.NewGuid(),
                RecipientId = userId,
                Type = data.GetValueOrDefault("type", "general"),
                Title = title,
                Body = body,
                RelatedId = relatedId,
                RelatedType = relatedType,
                IsRead = false,
                CreatedAt = DateTime.UtcNow
            };

            _context.Notifications.Add(notification);
            await _context.SaveChangesAsync();

            return MapToDto(notification);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to persist notification for user {UserId}", userId);
            return null;
        }
    }

    /// <summary>
    /// Pushes the notification itself and nothing more. The client's unread badge counts its
    /// own cached rows, so it updates the moment this row is inserted — a separate unread-count
    /// event would only repeat what the client already knows.
    /// </summary>
    private async Task BroadcastAsync(Guid userId, NotificationDto dto)
    {
        try
        {
            await _hubContext.Clients
                .Group($"user_{userId}")
                .SendAsync("NotificationReceived", dto);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to broadcast notification to user {UserId}", userId);
        }
    }

    private async Task PushToUserAsync(
        Guid userId,
        string title,
        string body,
        IReadOnlyDictionary<string, string> data,
        string notificationId)
    {
        var tokens = await _context.FCMTokens
            .Where(t => t.OrganizationId == userId)
            .Select(t => t.Token)
            .Distinct()
            .ToListAsync();

        if (tokens.Count == 0)
        {
            // Not an error: the user may simply have no device registered yet. The inbox row
            // is already written, so nothing is lost.
            _logger.LogInformation("No FCM token registered for user {UserId} — inbox only", userId);
            return;
        }

        await PushToTokensAsync(tokens, title, body, data, notificationId);
    }

    private async Task PushToTokensAsync(
        List<string> tokens,
        string title,
        string body,
        IReadOnlyDictionary<string, string> data,
        string? notificationId)
    {
        if (!_firebaseInitialized || tokens.Count == 0) return;

        // Data-only. A message carrying a `notification` block is drawn by the system when the
        // app is backgrounded and never reaches onMessageReceived, so the app could not record
        // it in the inbox. Sending data-only hands every message to the client, which builds
        // the system notification itself.
        var payload = new Dictionary<string, string>(data)
        {
            ["title"] = title,
            ["body"] = body
        };
        if (notificationId != null) payload["notificationId"] = notificationId;

        for (var offset = 0; offset < tokens.Count; offset += MulticastBatchSize)
        {
            var batch = tokens.Skip(offset).Take(MulticastBatchSize).ToList();

            try
            {
                var message = new MulticastMessage
                {
                    Tokens = batch,
                    Data = payload,
                    Android = new AndroidConfig
                    {
                        Priority = Priority.High
                    }
                };

                var response = await FirebaseMessaging.DefaultInstance.SendEachForMulticastAsync(message);

                if (response.FailureCount > 0)
                {
                    await PruneRejectedTokensAsync(batch, response);
                }

                _logger.LogInformation(
                    "✅ Push delivered to {Success}/{Total} devices", response.SuccessCount, batch.Count);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "❌ Push batch failed for {Count} devices", batch.Count);
            }
        }
    }

    /// <summary>
    /// Deletes only the tokens FCM actually rejected. The old code wiped every token the user
    /// had as soon as one came back unregistered, which logged out their other devices too.
    /// </summary>
    private async Task PruneRejectedTokensAsync(List<string> batch, BatchResponse response)
    {
        var dead = new List<string>();

        for (var i = 0; i < response.Responses.Count && i < batch.Count; i++)
        {
            var result = response.Responses[i];
            if (result.IsSuccess) continue;

            var code = result.Exception?.MessagingErrorCode;
            if (code == MessagingErrorCode.Unregistered || code == MessagingErrorCode.InvalidArgument)
            {
                dead.Add(batch[i]);
            }
        }

        if (dead.Count == 0) return;

        try
        {
            await _context.FCMTokens
                .Where(t => dead.Contains(t.Token))
                .ExecuteDeleteAsync();

            _logger.LogInformation("🗑️ Removed {Count} rejected FCM token(s)", dead.Count);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to prune rejected FCM tokens");
        }
    }

    public async Task RemoveTokenAsync(Guid userId, string token)
    {
        try
        {
            await _context.FCMTokens
                .Where(t => t.OrganizationId == userId && t.Token == token)
                .ExecuteDeleteAsync();

            _logger.LogInformation("🗑️ Unregistered device token for user {UserId}", userId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to remove FCM token for user {UserId}", userId);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static NotificationDto MapToDto(NotificationRow n) => new()
    {
        Id = n.Id.ToString(),
        Type = n.Type,
        Title = n.Title,
        Body = n.Body,
        RelatedId = n.RelatedId,
        RelatedType = n.RelatedType,
        IsRead = n.IsRead,
        CreatedAt = n.CreatedAt.ToString("o"),
        ReadAt = n.ReadAt?.ToString("o")
    };

    /// <summary>
    /// Maps the push payload's entity key onto the inbox row's relation columns, so a
    /// notification opened from the inbox deep-links the same way as one opened from the tray.
    /// </summary>
    private static (string? RelatedId, string? RelatedType) DeriveRelation(
        IReadOnlyDictionary<string, string> data)
    {
        if (data.TryGetValue("requestId", out var requestId)) return (requestId, "pickup_request");
        if (data.TryGetValue("listingId", out var listingId)) return (listingId, "listing");
        if (data.TryGetValue("inventoryId", out var inventoryId)) return (inventoryId, "inventory");
        if (data.TryGetValue("organizationId", out var orgId)) return (orgId, "organization");
        return (null, null);
    }

    private static string DashboardFor(string organizationType) => organizationType.ToLower() switch
    {
        "ngo" => "ngo_dashboard",
        "grocery" => "grocery_dashboard",
        _ => "profile"
    };

    private static int DaysUntil(string isoDate) =>
        DateTime.TryParse(isoDate, out var parsed)
            ? Math.Max(0, (parsed.Date - DateTime.UtcNow.Date).Days)
            : 0;
}
