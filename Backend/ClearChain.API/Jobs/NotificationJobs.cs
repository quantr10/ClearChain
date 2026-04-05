using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.API.DTOs.Listings;
using ClearChain.API.DTOs.Inventory;
using ClearChain.API.Services;
using ClearChain.Domain.Entities;
using ClearChain.Domain.Enums;

namespace ClearChain.API.Jobs;

/// <summary>
/// Hangfire background jobs for scheduled notifications
/// Migrated from NotificationSchedulerService (BackgroundService)
/// </summary>
public class NotificationJobs
{
    private readonly ApplicationDbContext _context;
    private readonly IPushNotificationService _pushService;
    private readonly ILogger<NotificationJobs> _logger;

    public NotificationJobs(
        ApplicationDbContext context,
        IPushNotificationService pushService,
        ILogger<NotificationJobs> logger)
    {
        _context = context;
        _pushService = pushService;
        _logger = logger;
    }

    /// <summary>
    /// Job #1: Check listings expiring tomorrow and send notifications
    /// Schedule: Daily at 2:00 AM UTC
    /// Cron: "0 2 * * *"
    /// </summary>
    public async Task CheckExpiringListings()
    {
        try
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

            _logger.LogInformation($"🔔 [Hangfire] Found {expiringListings.Count} listings expiring tomorrow");

            foreach (var listing in expiringListings)
            {
                var listingData = new ListingData
                {
                    Id = listing.Id.ToString(),
                    GroceryId = listing.GroceryId.ToString(),
                    GroceryName = listing.Grocery?.Name ?? "",
                    Title = listing.ProductName,
                    Category = listing.Category,
                    Quantity = (int)listing.Quantity,
                    Unit = listing.Unit,
                    ExpiryDate = listing.ExpirationDate?.ToString("yyyy-MM-dd") ?? "",
                    Status = listing.Status.ToString().ToLower(),
                    Location = listing.Grocery?.Location ?? ""
                };

                await _pushService.SendListingExpiringSoonNotification(
                    listing.GroceryId,
                    listingData
                );
            }

            _logger.LogInformation($"✅ [Hangfire] Sent {expiringListings.Count} listing expiry notifications");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ [Hangfire] Error checking expiring listings");
            throw; // Re-throw to let Hangfire handle retry
        }
    }

    /// <summary>
    /// Job #2: Check expired listings, update status, and send notifications
    /// Schedule: Daily at 2:00 AM UTC
    /// Cron: "0 2 * * *"
    /// </summary>
    public async Task CheckExpiredListings()
    {
        try
        {
            var today = DateTime.UtcNow.Date;

            var expiredListings = await _context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .Where(l => l.Status == ListingStatus.Open &&
                           l.ExpirationDate.HasValue &&
                           l.ExpirationDate.Value.Date < today)
                .ToListAsync();

            _logger.LogInformation($"🔔 [Hangfire] Found {expiredListings.Count} expired listings");

            foreach (var listing in expiredListings)
            {
                // Update status to expired
                listing.Status = ListingStatus.Expired;
                listing.UpdatedAt = DateTime.UtcNow;

                var listingData = new ListingData
                {
                    Id = listing.Id.ToString(),
                    GroceryId = listing.GroceryId.ToString(),
                    GroceryName = listing.Grocery?.Name ?? "",
                    Title = listing.ProductName,
                    Category = listing.Category,
                    Quantity = (int)listing.Quantity,
                    Unit = listing.Unit,
                    ExpiryDate = listing.ExpirationDate?.ToString("yyyy-MM-dd") ?? "",
                    Status = "expired",
                    Location = listing.Grocery?.Location ?? ""
                };

                await _pushService.SendListingExpiredNotification(
                    listing.GroceryId,
                    listingData
                );
            }

            if (expiredListings.Any())
            {
                await _context.SaveChangesAsync();
            }

            _logger.LogInformation($"✅ [Hangfire] Marked {expiredListings.Count} listings as expired and sent notifications");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ [Hangfire] Error checking expired listings");
            throw;
        }
    }

    /// <summary>
    /// Job #3: Check inventory expiring in 2 days and send notifications
    /// Schedule: Daily at 2:00 AM UTC
    /// Cron: "0 2 * * *"
    /// </summary>
    public async Task CheckExpiringInventory()
    {
        try
        {
            var twoDaysFromNow = DateTime.UtcNow.Date.AddDays(2);
            var threeDaysFromNow = twoDaysFromNow.AddDays(1);

            var expiringItems = await _context.Inventories
                .Where(i => i.Status == InventoryStatus.Active &&
                           i.ExpiryDate.Date >= twoDaysFromNow &&
                           i.ExpiryDate.Date < threeDaysFromNow)
                .ToListAsync();

            _logger.LogInformation($"🔔 [Hangfire] Found {expiringItems.Count} inventory items expiring in 2 days");

            foreach (var item in expiringItems)
            {
                var itemData = new InventoryItemData
                {
                    Id = item.Id.ToString(),
                    NgoId = item.NgoId.ToString(),
                    ProductName = item.ProductName,
                    Category = item.Category,
                    Quantity = item.Quantity,
                    Unit = item.Unit,
                    ExpiryDate = item.ExpiryDate.ToString("yyyy-MM-dd"),
                    Status = item.Status.ToString().ToLower(),
                    ReceivedAt = item.ReceivedAt.ToString("o"),
                    PickupRequestId = item.PickupRequestId.ToString()
                };

                await _pushService.SendInventoryExpiringSoonNotification(
                    item.NgoId,
                    itemData
                );
            }

            _logger.LogInformation($"✅ [Hangfire] Sent {expiringItems.Count} inventory expiry warnings");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ [Hangfire] Error checking expiring inventory");
            throw;
        }
    }

    /// <summary>
    /// Job #4: Check expired inventory, update status, and send notifications
    /// Schedule: Daily at 2:00 AM UTC
    /// Cron: "0 2 * * *"
    /// </summary>
    public async Task CheckExpiredInventory()
    {
        try
        {
            var today = DateTime.UtcNow.Date;

            var expiredItems = await _context.Inventories
                .Where(i => i.Status == InventoryStatus.Active &&
                           i.ExpiryDate.Date < today)
                .ToListAsync();

            _logger.LogInformation($"🔔 [Hangfire] Found {expiredItems.Count} expired inventory items");

            foreach (var item in expiredItems)
            {
                item.Status = InventoryStatus.Expired;
                item.UpdatedAt = DateTime.UtcNow;

                var itemData = new InventoryItemData
                {
                    Id = item.Id.ToString(),
                    NgoId = item.NgoId.ToString(),
                    ProductName = item.ProductName,
                    Category = item.Category,
                    Quantity = item.Quantity,
                    Unit = item.Unit,
                    ExpiryDate = item.ExpiryDate.ToString("yyyy-MM-dd"),
                    Status = "expired",
                    ReceivedAt = item.ReceivedAt.ToString("o"),
                    PickupRequestId = item.PickupRequestId.ToString()
                };

                await _pushService.SendInventoryExpiredNotification(
                    item.NgoId,
                    itemData
                );
            }

            if (expiredItems.Any())
            {
                await _context.SaveChangesAsync();
            }

            _logger.LogInformation($"✅ [Hangfire] Marked {expiredItems.Count} items as expired and sent notifications");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ [Hangfire] Error checking expired inventory");
            throw;
        }
    }

    /// <summary>
    /// Job #5: Delete refresh tokens that are revoked or expired (older than 30 days)
    /// Prevents unbounded table growth.
    /// Schedule: Daily at 3:00 AM UTC
    /// </summary>
    public async Task CleanupExpiredRefreshTokens()
    {
        try
        {
            var cutoff = DateTime.UtcNow.AddDays(-30);

            var deleted = await _context.RefreshTokens
                .Where(rt => rt.IsRevoked || rt.ExpiresAt < DateTime.UtcNow)
                .Where(rt => rt.CreatedAt < cutoff)
                .ExecuteDeleteAsync();

            _logger.LogInformation($"✅ [Hangfire] Deleted {deleted} expired/revoked refresh tokens");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ [Hangfire] Error cleaning up refresh tokens");
            throw;
        }
    }
}