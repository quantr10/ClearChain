namespace ClearChain.API.DTOs.Inventory;

public class InventoryItemResponse
{
    public string Message { get; set; } = string.Empty;
    public InventoryItemData Data { get; set; } = new();
}