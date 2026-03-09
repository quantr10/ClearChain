using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.DependencyInjection;
using ClearChain.Infrastructure.Data;
using ClearChain.API.DTOs.Listings;
using ClearChain.API.DTOs.Inventory;
using Microsoft.EntityFrameworkCore;

namespace ClearChain.API.Services;

public class NotificationSchedulerService : BackgroundService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<NotificationSchedulerService> _logger;
    private readonly TimeSpan _checkInterval = TimeSpan.FromHours(24);

    public NotificationSchedulerService(
        IServiceProvider serviceProvider,
        ILogger<NotificationSchedulerService> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("🕐 NotificationSchedulerService started");

        // Wait until 2 AM for first run
        await WaitUntilTargetTime(stoppingToken);

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                _logger.LogInformation("🔔 Running daily notification checks...");

                using (var scope = _serviceProvider.CreateScope())
                {
                    var context = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
                    var pushService = scope.ServiceProvider.GetRequiredService<IPushNotificationService>();

                    await CheckExpiringListings(context, pushService);
                    await CheckExpiredListings(context, pushService);
                    await CheckExpiringInventory(context, pushService);
                    await CheckExpiredInventory(context, pushService);
                }

                _logger.LogInformation("✅ Daily notification checks completed");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "❌ Error in notification scheduler");
            }

            await Task.Delay(_checkInterval, stoppingToken);
        }
    }

private async Task WaitUntilTargetTime(CancellationToken stoppingToken)
{
    var now = DateTime.UtcNow;
    var today2AM = DateTime.UtcNow.Date.AddHours(2);
    var tomorrow2AM = DateTime.UtcNow.Date.AddDays(1).AddHours(2);

    DateTime targetTime;
    
    // If current time is before 2 AM today, wait until 2 AM today
    // Otherwise, wait until 2 AM tomorrow
    if (now.Hour < 2)
    {
        targetTime = today2AM;
    }
    else
    {
        targetTime = tomorrow2AM;
    }

    var delay = targetTime - now;
    
    // Safety check: If delay is negative (shouldn't happen with fix above), default to 1 minute
    if (delay.TotalMilliseconds < 0)
    {
        _logger.LogWarning($"⚠️ Calculated delay was negative ({delay.TotalHours:F1} hours). Defaulting to 1 minute delay.");
        delay = TimeSpan.FromMinutes(1);
    }

    _logger.LogInformation($"⏰ Scheduler will start at {targetTime:yyyy-MM-dd HH:mm:ss} UTC (in {delay.TotalHours:F1} hours)");

    await Task.Delay(delay, stoppingToken);
}
    private async Task CheckExpiringListings(ApplicationDbContext context, IPushNotificationService pushService)
    {
        try
        {
            var tomorrow = DateTime.UtcNow.Date.AddDays(1);
            var dayAfterTomorrow = tomorrow.AddDays(1);

            var expiringListings = await context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .Where(l => l.Status == "open" &&
                           l.ExpirationDate.HasValue &&
                           l.ExpirationDate.Value.Date >= tomorrow &&
                           l.ExpirationDate.Value.Date < dayAfterTomorrow)
                .ToListAsync();

            _logger.LogInformation($"Found {expiringListings.Count} listings expiring tomorrow");

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
                    Status = listing.Status,
                    Location = listing.Grocery?.Location ?? ""
                };

                await pushService.SendListingExpiringSoonNotification(
                    listing.GroceryId,
                    listingData
                );
            }

            _logger.LogInformation($"✅ Sent {expiringListings.Count} listing expiry notifications");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking expiring listings");
        }
    }

    private async Task CheckExpiredListings(ApplicationDbContext context, IPushNotificationService pushService)
    {
        try
        {
            var today = DateTime.UtcNow.Date;

            var expiredListings = await context.ClearanceListings
                .Include(l => l.Grocery)
                .Include(l => l.Group)
                .Where(l => l.Status == "open" &&
                           l.ExpirationDate.HasValue &&
                           l.ExpirationDate.Value.Date < today)
                .ToListAsync();

            _logger.LogInformation($"Found {expiredListings.Count} expired listings");

            foreach (var listing in expiredListings)
            {
                // Update status to expired
                listing.Status = "expired";
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

                await pushService.SendListingExpiredNotification(
                    listing.GroceryId,
                    listingData
                );
            }

            if (expiredListings.Any())
            {
                await context.SaveChangesAsync();
            }

            _logger.LogInformation($"✅ Marked {expiredListings.Count} listings as expired and sent notifications");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking expired listings");
        }
    }

    private async Task CheckExpiringInventory(ApplicationDbContext context, IPushNotificationService pushService)
    {
        try
        {
            var twoDaysFromNow = DateTime.UtcNow.Date.AddDays(2);
            var threeDaysFromNow = twoDaysFromNow.AddDays(1);

            var expiringItems = await context.Inventories
                .Where(i => i.Status == "active" &&
                           i.ExpiryDate.Date >= twoDaysFromNow &&
                           i.ExpiryDate.Date < threeDaysFromNow)
                .ToListAsync();

            _logger.LogInformation($"Found {expiringItems.Count} inventory items expiring in 2 days");

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
                    Status = item.Status,
                    ReceivedAt = item.ReceivedAt.ToString("o"),
                    PickupRequestId = item.PickupRequestId.ToString()
                };

                await pushService.SendInventoryExpiringSoonNotification(
                    item.NgoId,
                    itemData
                );
            }

            _logger.LogInformation($"✅ Sent {expiringItems.Count} inventory expiry warnings");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking expiring inventory");
        }
    }

    private async Task CheckExpiredInventory(ApplicationDbContext context, IPushNotificationService pushService)
    {
        try
        {
            var today = DateTime.UtcNow.Date;

            var expiredItems = await context.Inventories
                .Where(i => i.Status == "active" &&
                           i.ExpiryDate.Date < today)
                .ToListAsync();

            _logger.LogInformation($"Found {expiredItems.Count} expired inventory items");

            foreach (var item in expiredItems)
            {
                item.Status = "expired";
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

                await pushService.SendInventoryExpiredNotification(
                    item.NgoId,
                    itemData
                );
            }

            if (expiredItems.Any())
            {
                await context.SaveChangesAsync();
            }

            _logger.LogInformation($"✅ Marked {expiredItems.Count} items as expired and sent notifications");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking expired inventory");
        }
    }
}