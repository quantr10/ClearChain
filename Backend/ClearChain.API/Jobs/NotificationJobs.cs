using Hangfire;
using Microsoft.EntityFrameworkCore;
using ClearChain.API.Common;
using ClearChain.Infrastructure.Data;
using ClearChain.API.DTOs.Listings;
using ClearChain.API.DTOs.Inventory;
using ClearChain.API.Services;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;

namespace ClearChain.API.Jobs;

/// <summary>
/// The platform's scheduled work. Two rules hold across every job here:
///
/// 1. A job that changes state must also announce it. Marking a listing expired in the
///    database is invisible to anyone with the app open unless the same job emits the SignalR
///    event, so state changes and broadcasts always travel together.
/// 2. A job that fails raises a system alert before rethrowing, so admins see it on the
///    dashboard rather than only in the server log.
///
/// Concurrency is disabled per job because they mutate shared rows and Hangfire will happily
/// start a second run while the first is still going after a retry or a server restart.
/// </summary>
public class NotificationJobs
{
    /// <summary>Devices that haven't checked in for this long are assumed uninstalled.</summary>
    private const int StaleTokenDays = 60;

    /// <summary>How far past its pickup date a still-pending request is given up on.</summary>
    private const int StaleRequestGraceDays = 2;

    private readonly ApplicationDbContext _context;
    private readonly IPushNotificationService _pushService;
    private readonly IListingNotificationService _listingNotificationService;
    private readonly IInventoryNotificationService _inventoryNotificationService;
    private readonly IAdminNotificationService _adminNotificationService;
    private readonly IPickupRequestService _pickupRequestService;
    private readonly ILogger<NotificationJobs> _logger;

    public NotificationJobs(
        ApplicationDbContext context,
        IPushNotificationService pushService,
        IListingNotificationService listingNotificationService,
        IInventoryNotificationService inventoryNotificationService,
        IAdminNotificationService adminNotificationService,
        IPickupRequestService pickupRequestService,
        ILogger<NotificationJobs> logger)
    {
        _context = context;
        _pushService = pushService;
        _listingNotificationService = listingNotificationService;
        _inventoryNotificationService = inventoryNotificationService;
        _adminNotificationService = adminNotificationService;
        _pickupRequestService = pickupRequestService;
        _logger = logger;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Job #1 — Listings expiring tomorrow
    // ═══════════════════════════════════════════════════════════════════════════

    [AutomaticRetry(Attempts = 3, DelaysInSeconds = new[] { 60, 300, 900 })]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public async Task CheckExpiringListings()
    {
        await RunGuarded(nameof(CheckExpiringListings), async () =>
        {
            var tomorrow = DateTime.UtcNow.Date.AddDays(1);
            var dayAfterTomorrow = tomorrow.AddDays(1);

            var expiringListings = await _context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .Where(l => l.Status == ListingStatus.Open &&
                            l.ExpirationDate.HasValue &&
                            l.ExpirationDate.Value.Date >= tomorrow &&
                            l.ExpirationDate.Value.Date < dayAfterTomorrow)
                .ToListAsync();

            _logger.LogInformation("🔔 [Hangfire] {Count} listings expiring tomorrow", expiringListings.Count);

            foreach (var listing in expiringListings)
            {
                await _pushService.SendListingExpiringSoonNotification(
                    listing.GroceryId, MapListing(listing));
            }

            _logger.LogInformation("✅ [Hangfire] Sent {Count} listing expiry warnings", expiringListings.Count);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Job #2 — Listings past their expiry date
    // ═══════════════════════════════════════════════════════════════════════════

    [AutomaticRetry(Attempts = 3, DelaysInSeconds = new[] { 60, 300, 900 })]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public async Task CheckExpiredListings()
    {
        await RunGuarded(nameof(CheckExpiredListings), async () =>
        {
            var today = DateTime.UtcNow.Date;

            var expiredListings = await _context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .Where(l => l.Status == ListingStatus.Open &&
                            l.ExpirationDate.HasValue &&
                            l.ExpirationDate.Value.Date < today)
                .ToListAsync();

            _logger.LogInformation("🔔 [Hangfire] {Count} listings to expire", expiredListings.Count);

            if (expiredListings.Count == 0) return;

            foreach (var listing in expiredListings)
            {
                listing.Status = ListingStatus.Expired;
                listing.UpdatedAt = DateTime.UtcNow;
            }

            // Persist before announcing: a client that reacts to the broadcast by refetching
            // must not be able to read the row back in its old state.
            await _context.SaveChangesAsync();

            foreach (var listing in expiredListings)
            {
                var dto = MapListing(listing, statusOverride: "expired");

                // Anyone browsing sees it drop out of the list without a manual refresh.
                await _listingNotificationService.NotifyListingUpdated(dto);
                await _pushService.SendListingExpiredNotification(listing.GroceryId, dto);
            }

            _logger.LogInformation("✅ [Hangfire] Expired {Count} listings", expiredListings.Count);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Job #3 — Inventory expiring in two days
    // ═══════════════════════════════════════════════════════════════════════════

    [AutomaticRetry(Attempts = 3, DelaysInSeconds = new[] { 60, 300, 900 })]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public async Task CheckExpiringInventory()
    {
        await RunGuarded(nameof(CheckExpiringInventory), async () =>
        {
            var twoDaysFromNow = DateTime.UtcNow.Date.AddDays(2);
            var threeDaysFromNow = twoDaysFromNow.AddDays(1);

            var expiringItems = await _context.Inventories
                .Where(i => i.Status == InventoryStatus.Active &&
                            i.ExpiryDate.Date >= twoDaysFromNow &&
                            i.ExpiryDate.Date < threeDaysFromNow)
                .ToListAsync();

            _logger.LogInformation("🔔 [Hangfire] {Count} inventory items expiring in 2 days", expiringItems.Count);

            foreach (var item in expiringItems)
            {
                await _pushService.SendInventoryExpiringSoonNotification(item.NgoId, MapInventory(item));
            }

            _logger.LogInformation("✅ [Hangfire] Sent {Count} inventory expiry warnings", expiringItems.Count);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Job #4 — Inventory past its expiry date
    // ═══════════════════════════════════════════════════════════════════════════

    [AutomaticRetry(Attempts = 3, DelaysInSeconds = new[] { 60, 300, 900 })]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public async Task CheckExpiredInventory()
    {
        await RunGuarded(nameof(CheckExpiredInventory), async () =>
        {
            var today = DateTime.UtcNow.Date;

            var expiredItems = await _context.Inventories
                .Where(i => i.Status == InventoryStatus.Active && i.ExpiryDate.Date < today)
                .ToListAsync();

            _logger.LogInformation("🔔 [Hangfire] {Count} inventory items to expire", expiredItems.Count);

            if (expiredItems.Count == 0) return;

            foreach (var item in expiredItems)
            {
                item.Status = InventoryStatus.Expired;
                item.UpdatedAt = DateTime.UtcNow;
            }

            await _context.SaveChangesAsync();

            foreach (var item in expiredItems)
            {
                var dto = MapInventory(item, statusOverride: "expired");

                await _inventoryNotificationService.NotifyInventoryItemExpired(dto);
                await _pushService.SendInventoryExpiredNotification(item.NgoId, dto);
            }

            _logger.LogInformation("✅ [Hangfire] Expired {Count} inventory items", expiredItems.Count);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Job #5 — Pending requests whose pickup date has come and gone
    // ═══════════════════════════════════════════════════════════════════════════

    /// <summary>
    /// A pending request holds a reserved slice of a listing. Left alone after its pickup date
    /// it keeps that stock off the market forever, so the request is cancelled through the
    /// normal service path — which is what knows how to merge the reservation back into its
    /// group and notify the grocery.
    ///
    /// Only Pending requests are swept. Approved and Ready mean the grocery has set stock
    /// aside and a person needs to decide what happened, not a cron job.
    /// </summary>
    [AutomaticRetry(Attempts = 3, DelaysInSeconds = new[] { 60, 300, 900 })]
    [DisableConcurrentExecution(timeoutInSeconds: 900)]
    public async Task ExpireStalePickupRequests()
    {
        await RunGuarded(nameof(ExpireStalePickupRequests), async () =>
        {
            var cutoff = DateTime.UtcNow.Date.AddDays(-StaleRequestGraceDays);

            var stale = await _context.PickupRequests
                .Where(p => p.Status == PickupRequestStatus.Pending && p.PickupDate.Date < cutoff)
                .Select(p => new { p.Id, p.NgoId })
                .ToListAsync();

            _logger.LogInformation("🔔 [Hangfire] {Count} stale pending requests", stale.Count);

            var cancelled = 0;
            foreach (var request in stale)
            {
                // Cancelling as the NGO — the request never got a response, so the reservation
                // goes back the same way it would if the NGO had withdrawn it themselves.
                var result = await _pickupRequestService.CancelAsync(
                    request.Id,
                    request.NgoId,
                    "Automatically cancelled — the pickup date passed without a response");

                if (result.Success) cancelled++;
                else _logger.LogWarning(
                    "Could not auto-cancel request {Id}: {Message}", request.Id, result.ErrorMessage);
            }

            _logger.LogInformation("✅ [Hangfire] Auto-cancelled {Count} stale requests", cancelled);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Job #6 — Daily platform stats for the admin dashboard
    // ═══════════════════════════════════════════════════════════════════════════

    /// <summary>
    /// Stats are otherwise only pushed when a transaction completes, so an admin watching a
    /// quiet dashboard sees numbers that drift further out of date the longer they look.
    /// </summary>
    [AutomaticRetry(Attempts = 2, DelaysInSeconds = new[] { 60, 300 })]
    [DisableConcurrentExecution(timeoutInSeconds: 300)]
    public async Task BroadcastPlatformStats()
    {
        await RunGuarded(nameof(BroadcastPlatformStats), async () =>
        {
            var today = DateTime.UtcNow.Date;

            await _adminNotificationService.NotifyStatsUpdated(new PlatformStatsNotification
            {
                TotalNGOs       = await _context.Organizations.CountAsync(o => o.Type == "ngo"),
                TotalGroceries  = await _context.Organizations.CountAsync(o => o.Type == "grocery"),
                TotalDonations  = await _context.PickupRequests.CountAsync(),
                ActiveListings  = await _context.ClearanceListings.CountAsync(l => l.Status == ListingStatus.Open),
                PendingRequests = await _context.PickupRequests.CountAsync(r => r.Status == PickupRequestStatus.Pending),
                CompletedToday  = await _context.PickupRequests.CountAsync(r =>
                    r.Status == PickupRequestStatus.Completed && r.RequestedAt.Date == today),
                UpdatedAt = DateTime.UtcNow
            });

            _logger.LogInformation("✅ [Hangfire] Platform stats broadcast to admins");
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Job #7 — Refresh token sweep
    // ═══════════════════════════════════════════════════════════════════════════

    [AutomaticRetry(Attempts = 3, DelaysInSeconds = new[] { 60, 300, 900 })]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public async Task CleanupExpiredRefreshTokens()
    {
        await RunGuarded(nameof(CleanupExpiredRefreshTokens), async () =>
        {
            var cutoff = DateTime.UtcNow.AddDays(-30);

            var deleted = await _context.RefreshTokens
                .Where(rt => rt.IsRevoked || rt.ExpiresAt < DateTime.UtcNow)
                .Where(rt => rt.CreatedAt < cutoff)
                .ExecuteDeleteAsync();

            _logger.LogInformation("✅ [Hangfire] Deleted {Count} expired/revoked refresh tokens", deleted);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Job #8 — Stale device token sweep
    // ═══════════════════════════════════════════════════════════════════════════

    /// <summary>
    /// FCM reports uninstalled devices only when something is actually sent to them, so a user
    /// who stops opening the app leaves tokens behind that are never pushed to and never
    /// pruned. Anything not re-registered in <see cref="StaleTokenDays"/> days is dropped; a
    /// device still in use refreshes its token every launch.
    /// </summary>
    [AutomaticRetry(Attempts = 3, DelaysInSeconds = new[] { 60, 300, 900 })]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public async Task PruneStaleFcmTokens()
    {
        await RunGuarded(nameof(PruneStaleFcmTokens), async () =>
        {
            var cutoff = DateTime.UtcNow.AddDays(-StaleTokenDays);

            var deleted = await _context.FCMTokens
                .Where(t => t.UpdatedAt < cutoff)
                .ExecuteDeleteAsync();

            _logger.LogInformation("✅ [Hangfire] Pruned {Count} stale FCM tokens", deleted);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Job #9 — Notification inbox sweep
    // ═══════════════════════════════════════════════════════════════════════════

    /// <summary>
    /// Every notification the platform sends is now persisted, so this table grows with usage
    /// and needs a ceiling. One age limit applies to all of them, read or not — see
    /// <see cref="NotificationRetentionPolicy"/>.
    /// </summary>
    [AutomaticRetry(Attempts = 3, DelaysInSeconds = new[] { 60, 300, 900 })]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public async Task CleanupOldNotifications()
    {
        await RunGuarded(nameof(CleanupOldNotifications), async () =>
        {
            var cutoff = NotificationRetentionPolicy.Cutoff(DateTime.UtcNow);

            var deleted = await _context.Notifications
                .Where(n => n.CreatedAt < cutoff)
                .ExecuteDeleteAsync();

            _logger.LogInformation(
                "✅ [Hangfire] Deleted {Count} notifications older than {Days} days",
                deleted, NotificationRetentionPolicy.RetentionDays);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /// <summary>
    /// Runs a job body, and on failure raises a system alert to the admin dashboard before
    /// rethrowing so Hangfire still records the failure and applies its retry policy.
    /// </summary>
    private async Task RunGuarded(string jobName, Func<Task> body)
    {
        try
        {
            await body();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ [Hangfire] {Job} failed", jobName);

            try
            {
                await _adminNotificationService.NotifySystemAlert(new SystemAlertNotification
                {
                    Level = "error",
                    Message = $"Scheduled job {jobName} failed",
                    Details = ex.Message,
                    Timestamp = DateTime.UtcNow
                });
            }
            catch (Exception alertEx)
            {
                // An unreachable hub must not mask the job's own failure.
                _logger.LogError(alertEx, "Could not raise system alert for {Job}", jobName);
            }

            throw;
        }
    }

    private static ListingData MapListing(ClearanceListing listing, string? statusOverride = null) => new()
    {
        Id = listing.Id.ToString(),
        GroceryId = listing.GroceryId.ToString(),
        GroceryName = listing.Grocery?.Name ?? "",
        Title = listing.ProductName,
        Category = listing.Category,
        Quantity = (int)listing.Quantity,
        Unit = listing.Unit,
        ExpiryDate = listing.ExpirationDate?.ToString("yyyy-MM-dd") ?? "",
        Status = statusOverride ?? listing.Status.ToString().ToLower(),
        Location = listing.Grocery?.Location ?? ""
    };

    private static InventoryItemData MapInventory(Inventory item, string? statusOverride = null) => new()
    {
        Id = item.Id.ToString(),
        NgoId = item.NgoId.ToString(),
        ProductName = item.ProductName,
        Category = item.Category,
        Quantity = item.Quantity,
        Unit = item.Unit,
        ExpiryDate = item.ExpiryDate.ToString("yyyy-MM-dd"),
        Status = statusOverride ?? item.Status.ToString().ToLower(),
        ReceivedAt = item.ReceivedAt.ToString("o"),
        PickupRequestId = item.PickupRequestId.ToString()
    };
}
