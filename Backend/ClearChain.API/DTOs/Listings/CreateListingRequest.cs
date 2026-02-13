namespace ClearChain.API.DTOs.Listings;

public class CreateListingRequest
{
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public int Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public string ExpiryDate { get; set; } = string.Empty;
    public string PickupTimeStart { get; set; } = string.Empty;
    public string PickupTimeEnd { get; set; } = string.Empty;
    public string? ImageUrl { get; set; }
}