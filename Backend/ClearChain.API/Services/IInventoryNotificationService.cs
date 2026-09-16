using ClearChain.API.DTOs.Inventory;

namespace ClearChain.API.Services;

public interface IInventoryNotificationService
{
    Task NotifyInventoryItemAddedAsync(InventoryItemData item);
    Task NotifyInventoryItemDistributedAsync(string itemId, Guid ngoId);
    Task NotifyInventoryItemExpiredAsync(InventoryItemData item);
    Task NotifyInventoryUpdatedAsync(InventoryItemData item);
}
