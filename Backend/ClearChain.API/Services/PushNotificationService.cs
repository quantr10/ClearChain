using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Google.Apis.Auth.OAuth2;
using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.API.DTOs.PickupRequests;

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
            // Check if firebase-adminsdk.json exists
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

    // ✅ Helper method for quick notifications (already exists but adding for clarity)
    private async Task SendNotification(
        Guid userId,
        string title,
        string body,
        Dictionary<string, string> data)
    {
        try
        {
            // Get FCM token from database
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

            // Build Firebase message
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

            // Send notification
            var response = await FirebaseMessaging.DefaultInstance.SendAsync(message);
            _logger.LogInformation($"✅ Push notification sent to user {userId}: {response}");
        }
        catch (FirebaseMessagingException ex)
        {
            _logger.LogError($"❌ Firebase error sending notification to user {userId}: {ex.Message}");

            // Remove invalid token
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
        { "screen", "browse_listings" }  // Redirect to browse for alternatives
    });
    }

    // ✅ NEW: Notify Grocery when NGO creates request
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

    // ✅ NEW: Notify Grocery when request is ready to be picked up (confirmation)
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

    // ✅ NEW: Notify Grocery when NGO cancels
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
}