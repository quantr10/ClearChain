using ClearChain.API.DTOs.Inventory;

namespace ClearChain.API.Services;

public interface IInventoryNotificationService
{
    Task NotifyInventoryItemAdded(InventoryItemData item);
    Task NotifyInventoryItemDistributed(string itemId, Guid ngoId);
    Task NotifyInventoryItemExpired(InventoryItemData item);
    Task NotifyInventoryUpdated(InventoryItemData item);
}