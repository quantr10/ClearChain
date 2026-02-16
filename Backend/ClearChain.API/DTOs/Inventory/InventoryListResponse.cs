namespace ClearChain.API.DTOs.Inventory;

public class InventoryListResponse
{
    public string Message { get; set; } = string.Empty;
    public List<InventoryItemData> Data { get; set; } = new();
}