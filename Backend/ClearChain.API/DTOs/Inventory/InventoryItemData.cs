namespace ClearChain.API.DTOs.Inventory;

public class InventoryItemData
{
    public string Id { get; set; } = string.Empty;
    public string ProductName { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public decimal Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public string ExpiryDate { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public string ReceivedAt { get; set; } = string.Empty;
    public string? DistributedAt { get; set; }
}