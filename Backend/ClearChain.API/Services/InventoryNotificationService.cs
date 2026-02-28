using Microsoft.AspNetCore.SignalR;
using ClearChain.API.Hubs;
using ClearChain.API.DTOs.Inventory;

namespace ClearChain.API.Services;

public class InventoryNotificationService : IInventoryNotificationService
{
    private readonly IHubContext<InventoryHub> _hubContext;
    private readonly ILogger<InventoryNotificationService> _logger;

    public InventoryNotificationService(
        IHubContext<InventoryHub> hubContext,
        ILogger<InventoryNotificationService> logger)
    {
        _hubContext = hubContext;
        _logger = logger;
    }

    public async Task NotifyInventoryItemAdded(InventoryItemData item)
    {
        try
        {
            // Notify NGO that owns this inventory
            await _hubContext.Clients
                .Group($"user_{item.NgoId}")
                .SendAsync("InventoryItemAdded", item);

            _logger.LogInformation($"Notified inventory item added: {item.Id}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending InventoryItemAdded notification");
        }
    }

    public async Task NotifyInventoryItemDistributed(string itemId, Guid ngoId)
    {
        try
        {
            var notification = new
            {
                ItemId = itemId,
                Timestamp = DateTime.UtcNow
            };

            await _hubContext.Clients
                .Group($"user_{ngoId}")
                .SendAsync("InventoryItemDistributed", notification);

            _logger.LogInformation($"Notified inventory item distributed: {itemId}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending InventoryItemDistributed notification");
        }
    }

    public async Task NotifyInventoryItemExpired(InventoryItemData item)
    {
        try
        {
            await _hubContext.Clients
                .Group($"user_{item.NgoId}")
                .SendAsync("InventoryItemExpired", item);

            _logger.LogInformation($"Notified inventory item expired: {item.Id}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending InventoryItemExpired notification");
        }
    }

    public async Task NotifyInventoryUpdated(InventoryItemData item)
    {
        try
        {
            await _hubContext.Clients
                .Groups($"user_{item.NgoId}", $"item_{item.Id}")
                .SendAsync("InventoryItemUpdated", item);

            _logger.LogInformation($"Notified inventory item updated: {item.Id}");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending InventoryItemUpdated notification");
        }
    }
}