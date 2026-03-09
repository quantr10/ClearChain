using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Google.Apis.Auth.OAuth2;
using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.API.DTOs.PickupRequests;
using ClearChain.API.DTOs.Listings;
using ClearChain.API.DTOs.Admin;
using ClearChain.API.DTOs.Inventory;

namespace ClearChain.API.Services;

public class PushNotificationService : IPushNotificationService
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<PushNotificationService> _logger;
    private static bool _firebaseInitialized = false;

    public PushNotificationService(
        ApplicationDbContext context,
        ILogger<PushNotificationService> logger)
    {
        _context = context;
        _logger = logger;

        InitializeFirebase();
    }

    private void InitializeFirebase()
    {
        if (_firebaseInitialized) return;

        try
        {
            var credentialPath = Path.Combine(Directory.GetCurrentDirectory(), "firebase-adminsdk.json");

            if (!File.Exists(credentialPath))
            {
                _logger.LogWarning("⚠️ firebase-adminsdk.json not found. Push notifications disabled.");
                return;
            }

            if (FirebaseApp.DefaultInstance == null)
            {
                FirebaseApp.Create(new AppOptions
                {
                    Credential = GoogleCredential.FromFile(credentialPath)
                });

                _firebaseInitialized = true;
                _logger.LogInformation("✅ Firebase Admin SDK initialized successfully");
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ Failed to initialize Firebase Admin SDK");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXISTING: Pickup Request Notifications
    // ═══════════════════════════════════════════════════════════════════════════

    public async Task SendPickupApprovedNotification(Guid userId, PickupRequestData request)
    {
        if (!_firebaseInitialized)
        {
            _logger.LogWarning("Firebase not initialized, skipping notification");
            return;
        }

        var title = "🎉 Pickup Request Approved!";
        var body = $"Your request for {request.ListingTitle} has been approved by {request.GroceryName}";

        await SendNotification(userId, title, body, new Dictionary<string, string>
        {
            { "type", "pickup_approved" },
            { "requestId", request.Id },
            { "screen", "my_requests" }
        });
    }

    public async Task SendPickupReadyNotification(Guid userId, PickupRequestData request)
    {
        if (!_firebaseInitialized)
        {
            _logger.LogWarning("Firebase not initialized, skipping notification");
            return;
        }

        var title = "✅ Food Ready for Pickup!";
        var body = $"{request.ListingTitle} is ready for pickup at {request.GroceryName}";

        await SendNotification(userId, title, body, new Dictionary<string, string>
        {
            { "type", "pickup_ready" },
            { "requestId", request.Id },
            { "screen", "my_requests" }
        });
    }

    public async Task SendPickupCompletedNotification(Guid userId, PickupRequestData request)
    {
        if (!_firebaseInitialized)
        {
            _logger.LogWarning("Firebase not initialized, skipping notification");
            return;
        }

        var title = "🎊 Pickup Completed!";
        var body = $"Thank you! {request.RequestedQuantity} {request.ListingTitle} marked as received";

        await SendNotification(userId, title, body, new Dictionary<string, string>
        {
            { "type", "pickup_completed" },
            { "requestId", request.Id },
            { "screen", "inventory" }
        });
    }

    public async Task SendPickupRejectedNotification(Guid userId, PickupRequestData request)
    {
        if (!_firebaseInitialized)
        {
            _logger.LogWarning("Firebase not initialized, skipping notification");
            return;
        }

        var title = "❌ Pickup Request Rejected";
        var body = $"Your request for {request.ListingTitle} was declined by {request.GroceryName}";

        await SendNotification(userId, title, body, new Dictionary<string, string>
        {
            { "type", "pickup_rejected" },
            { "requestId", request.Id },
            { "screen", "browse_listings" }
        });
    }

    public async Task SendInventoryAddedNotification(Guid userId, string productName, int quantity, string unit)
    {
        if (!_firebaseInitialized)
        {
            _logger.LogWarning("Firebase not initialized, skipping notification");
            return;
        }

        var title = "📦 New Inventory Added!";
        var body = $"{quantity} {unit} of {productName} added to your inventory";

        await SendNotification(userId, title, body, new Dictionary<string, string>
        {
            { "type", "inventory_added" },
            { "screen", "inventory" }
        });
    }

    public async Task SendPickupRequestCreatedNotification(Guid groceryId, PickupRequestData request)
    {
        await SendNotification(
            groceryId,
            "📝 New Pickup Request",
            $"{request.NgoName} requested {request.RequestedQuantity} of {request.ListingTitle}",
            new Dictionary<string, string>
            {
                { "type", "pickup_request_created" },
                { "requestId", request.Id },
                { "screen", "grocery_requests" }
            }
        );
    }

    public async Task SendPickupConfirmedNotification(Guid groceryId, PickupRequestData request)
    {
        await SendNotification(
            groceryId,
            "📦 Ready for Pickup Confirmed",
            $"{request.NgoName} confirmed they will pickup {request.ListingTitle}",
            new Dictionary<string, string>
            {
                { "type", "pickup_confirmed" },
                { "requestId", request.Id },
                { "screen", "grocery_requests" }
            }
        );
    }

    public async Task SendPickupRequestCancelledNotification(Guid groceryId, PickupRequestData request)
    {
        await SendNotification(
            groceryId,
            "❌ Pickup Request Cancelled",
            $"{request.NgoName} cancelled pickup for {request.ListingTitle}",
            new Dictionary<string, string>
            {
                { "type", "pickup_cancelled" },
                { "requestId", request.Id },
                { "screen", "grocery_requests" }
            }
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW: Listing Notifications
    // ═══════════════════════════════════════════════════════════════════════════

    public async Task SendNewListingNotificationToAllNGOs(ListingData listing)
    {
        if (!_firebaseInitialized)
        {
            _logger.LogWarning("Firebase not initialized, skipping notification");
            return;
        }

        try
        {
            var ngoTokens = await _context.FCMTokens
                .Include(t => t.Organization)
                .Where(t => t.Organization != null && t.Organization.Type.ToLower() == "ngo")
                .Select(t => t.Token)
                .Distinct()
                .ToListAsync();

            if (!ngoTokens.Any())
            {
                _logger.LogWarning("No NGO FCM tokens found");
                return;
            }

            var title = "🆕 New Food Available!";
            var body = $"{listing.GroceryName} posted {listing.Quantity} {listing.Unit} {listing.Title}";

            var message = new MulticastMessage
            {
                Tokens = ngoTokens,
                Notification = new Notification
                {
                    Title = title,
                    Body = body
                },
                Data = new Dictionary<string, string>
                {
                    { "type", "new_listing" },
                    { "listingId", listing.Id },
                    { "groceryId", listing.GroceryId },
                    { "screen", "browse_listings" }
                },
                Android = new AndroidConfig
                {
                    Priority = Priority.High,
                    Notification = new AndroidNotification
                    {
                        ChannelId = "clearchain_notifications",
                        Sound = "default",
                        Priority = NotificationPriority.MAX
                    }
                }
            };

            var response = await FirebaseMessaging.DefaultInstance.SendEachForMulticastAsync(message);
            _logger.LogInformation($"✅ Broadcast sent to {response.SuccessCount}/{ngoTokens.Count} NGOs. Failures: {response.FailureCount}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error broadcasting new listing to NGOs");
        }
    }

    public async Task SendListingExpiringSoonNotification(Guid groceryId, ListingData listing)
    {
        var expiryDate = DateTime.Parse(listing.ExpiryDate);
        var daysUntilExpiry = (expiryDate - DateTime.UtcNow.Date).Days;

        await SendNotification(
            groceryId,
            "⚠️ Listing Expiring Soon",
            $"{listing.Title} expires in {daysUntilExpiry} day(s)! Consider clearance.",
            new Dictionary<string, string>
            {
                { "type", "listing_expiring_soon" },
                { "listingId", listing.Id },
                { "screen", "my_listings" }
            }
        );
    }
    public async Task SendListingExpiredNotification(Guid groceryId, ListingData listing)
    {
        await SendNotification(
            groceryId,
            "🚨 Listing Expired",
            $"{listing.Title} has expired. Please update or remove this listing.",
            new Dictionary<string, string>
            {
                { "type", "listing_expired" },
                { "listingId", listing.Id },
                { "screen", "my_listings" }
            }
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW: Inventory Notifications
    // ═══════════════════════════════════════════════════════════════════════════

    public async Task SendInventoryExpiringSoonNotification(Guid ngoId, InventoryItemData item)
    {
        var expiryDate = DateTime.Parse(item.ExpiryDate);
        var daysUntilExpiry = (expiryDate - DateTime.UtcNow.Date).Days;

        await SendNotification(
            ngoId,
            "⚠️ Food Expiring Soon",
            $"{item.ProductName} ({item.Quantity} {item.Unit}) expires in {daysUntilExpiry} day(s)!",
            new Dictionary<string, string>
            {
                { "type", "inventory_expiring_soon" },
                { "inventoryId", item.Id },
                { "screen", "inventory" }
            }
        );
    }

    public async Task SendInventoryExpiredNotification(Guid ngoId, InventoryItemData item)
    {
        await SendNotification(
            ngoId,
            "🚨 Expired Food Alert",
            $"{item.ProductName} has expired. Please remove from inventory.",
            new Dictionary<string, string>
            {
                { "type", "inventory_expired" },
                { "inventoryId", item.Id },
                { "screen", "inventory" }
            }
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW: User Onboarding & Admin Notifications
    // ═══════════════════════════════════════════════════════════════════════════

    public async Task SendWelcomeNotification(OrganizationData organization)
    {
        var dashboard = organization.Type.ToLower() switch
        {
            "ngo" => "ngo_dashboard",
            "grocery" => "grocery_dashboard",
            _ => "profile"
        };

        var actionText = organization.Type.ToLower() == "ngo"
            ? "Start browsing available food items!"
            : "Start posting surplus food items!";

        await SendNotification(
            Guid.Parse(organization.Id),
            "👋 Welcome to ClearChain!",
            $"Hi {organization.Name}! Thank you for joining ClearChain. {actionText}",
            new Dictionary<string, string>
            {
                { "type", "welcome" },
                { "organizationType", organization.Type },
                { "screen", dashboard }
            }
        );
    }

    public async Task SendNewRegistrationAlertToAdmins(OrganizationData organization)
    {
        if (!_firebaseInitialized)
        {
            _logger.LogWarning("Firebase not initialized, skipping notification");
            return;
        }

        try
        {
            var adminTokens = await _context.FCMTokens
                .Include(t => t.Organization)
                .Where(t => t.Organization != null && t.Organization.Type.ToLower() == "admin")
                .Select(t => t.Token)
                .Distinct()
                .ToListAsync();

            if (!adminTokens.Any())
            {
                _logger.LogWarning("No admin FCM tokens found");
                return;
            }

            var title = "📢 New Registration";
            var body = $"{organization.Name} ({organization.Type}) needs verification";

            var message = new MulticastMessage
            {
                Tokens = adminTokens,
                Notification = new Notification
                {
                    Title = title,
                    Body = body
                },
                Data = new Dictionary<string, string>
                {
                    { "type", "new_registration" },
                    { "organizationId", organization.Id },
                    { "organizationType", organization.Type },
                    { "screen", "admin_verification" }
                },
                Android = new AndroidConfig
                {
                    Priority = Priority.High,
                    Notification = new AndroidNotification
                    {
                        ChannelId = "clearchain_notifications",
                        Sound = "default",
                        Priority = NotificationPriority.MAX
                    }
                }
            };

            var response = await FirebaseMessaging.DefaultInstance.SendEachForMulticastAsync(message);
            _logger.LogInformation($"✅ Admin alert sent to {response.SuccessCount}/{adminTokens.Count} admins");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending new registration alert to admins");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER: Generic notification sender (PUBLIC for interface)
    // ═══════════════════════════════════════════════════════════════════════════

    public async Task SendNotification(
        Guid userId,
        string title,
        string body,
        Dictionary<string, string> data)
    {
        try
        {
            var token = await _context.FCMTokens
                .Where(t => t.OrganizationId == userId)
                .OrderByDescending(t => t.UpdatedAt)
                .Select(t => t.Token)
                .FirstOrDefaultAsync();

            if (token == null)
            {
                _logger.LogWarning($"⚠️ No FCM token found for user {userId}");
                return;
            }

            var message = new Message
            {
                Token = token,
                Notification = new Notification
                {
                    Title = title,
                    Body = body
                },
                Data = data,
                Android = new AndroidConfig
                {
                    Priority = Priority.High,
                    Notification = new AndroidNotification
                    {
                        ChannelId = "clearchain_notifications",
                        Sound = "default",
                        Priority = NotificationPriority.MAX
                    }
                }
            };

            var response = await FirebaseMessaging.DefaultInstance.SendAsync(message);
            _logger.LogInformation($"✅ Push notification sent to user {userId}: {response}");
        }
        catch (FirebaseMessagingException ex)
        {
            _logger.LogError($"❌ Firebase error sending notification to user {userId}: {ex.Message}");

            if (ex.MessagingErrorCode == MessagingErrorCode.Unregistered ||
                ex.MessagingErrorCode == MessagingErrorCode.InvalidArgument)
            {
                await RemoveInvalidToken(userId);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError($"❌ Error sending notification to user {userId}: {ex.Message}");
        }
    }

    private async Task RemoveInvalidToken(Guid userId)
    {
        try
        {
            var tokens = await _context.FCMTokens
                .Where(t => t.OrganizationId == userId)
                .ToListAsync();

            _context.FCMTokens.RemoveRange(tokens);
            await _context.SaveChangesAsync();

            _logger.LogInformation($"🗑️ Removed invalid FCM tokens for user {userId}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, $"Failed to remove invalid tokens for user {userId}");
        }
    }
}