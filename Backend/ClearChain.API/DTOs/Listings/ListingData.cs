namespace ClearChain.API.DTOs.Listings;

public class ListingData
{
    public string Id { get; set; } = string.Empty;
    public string GroceryId { get; set; } = string.Empty;
    public string GroceryName { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public int Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public string ExpiryDate { get; set; } = string.Empty;
    public string PickupTimeStart { get; set; } = string.Empty;
    public string PickupTimeEnd { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public string? ImageUrl { get; set; }
    public string Location { get; set; } = string.Empty;
    public string CreatedAt { get; set; } = string.Empty;
}