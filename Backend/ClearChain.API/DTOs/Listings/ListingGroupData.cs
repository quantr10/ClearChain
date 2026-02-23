namespace ClearChain.API.DTOs.Listings;

/// <summary>
/// DTO for ListingGroup information
/// Used to provide context about splits and tracking
/// </summary>
public class ListingGroupData
{
    public string Id { get; set; } = string.Empty;
    public string OriginalListingId { get; set; } = string.Empty;
    public string GroceryId { get; set; } = string.Empty;
    
    // Product info
    public string ProductName { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public string Unit { get; set; } = string.Empty;
    
    // Quantity tracking
    public int OriginalQuantity { get; set; }
    public int TotalReserved { get; set; }
    public int TotalAvailable { get; set; }
    public int TotalCompleted { get; set; }
    
    public bool IsFullyConsumed { get; set; }
    public string CreatedAt { get; set; } = string.Empty;
}