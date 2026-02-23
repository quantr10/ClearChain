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
    
    // NEW: ListingGroup tracking fields
    public string? GroupId { get; set; }
    public string SplitReason { get; set; } = "new_listing";
    public string? RelatedRequestId { get; set; }
    public int SplitIndex { get; set; } = 0;
    
    // OPTIONAL: Include group summary (for UI context)
    public ListingGroupSummary? GroupSummary { get; set; }
}

/// <summary>
/// Lightweight group info included in listing responses
/// </summary>
public class ListingGroupSummary
{
    public string GroupId { get; set; } = string.Empty;
    public int OriginalQuantity { get; set; }
    public int TotalReserved { get; set; }
    public int TotalAvailable { get; set; }
    public int ChildListingsCount { get; set; }
}