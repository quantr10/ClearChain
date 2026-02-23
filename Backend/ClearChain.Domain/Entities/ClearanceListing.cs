namespace ClearChain.Domain.Entities;

public class ClearanceListing
{
    public Guid Id { get; set; }
    public Guid GroceryId { get; set; }
    
    // NEW: Link to ListingGroup
    public Guid? GroupId { get; set; }
    
    public string ProductName { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public decimal Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public DateTime? ExpirationDate { get; set; }
    public DateTime ClearanceDeadline { get; set; }
    public string? Notes { get; set; }
    public string? PhotoUrl { get; set; }
    
    // Status: "open" (AVAILABLE) or "reserved" (RESERVED)
    public string Status { get; set; } = "open";
    
    // NEW: Tracking split information
    public string SplitReason { get; set; } = "new_listing";  // new_listing, partial_request, merge, cancel_restore
    public Guid? RelatedRequestId { get; set; }  // If RESERVED
    public Guid? SplitFromListingId { get; set; }  // Parent listing ID if split
    public int SplitIndex { get; set; } = 0;  // Order in group
    
    // DEPRECATED (keep for backward compatibility)
    public Guid? PickupRequestId { get; set; }
    
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public TimeSpan? PickupTimeStart { get; set; }
    public TimeSpan? PickupTimeEnd { get; set; }
    
    // Navigation properties
    public Organization? Grocery { get; set; }
    public ListingGroup? Group { get; set; }
}