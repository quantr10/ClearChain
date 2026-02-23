namespace ClearChain.Domain.Entities;

public class ListingGroup
{
    public Guid Id { get; set; }
    public Guid OriginalListingId { get; set; }
    public Guid GroceryId { get; set; }
    
    // Product info (immutable)
    public string ProductName { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public string Unit { get; set; } = string.Empty;
    public string? Notes { get; set; }
    public string? PhotoUrl { get; set; }
    public DateTime? ExpirationDate { get; set; }
    public DateTime ClearanceDeadline { get; set; }
    public TimeSpan? PickupTimeStart { get; set; }
    public TimeSpan? PickupTimeEnd { get; set; }
    
    // Quantity tracking
    public decimal OriginalQuantity { get; set; }
    public decimal TotalReserved { get; set; } = 0;
    public decimal TotalAvailable { get; set; } = 0;
    public decimal TotalCompleted { get; set; } = 0;
    
    public bool IsFullyConsumed { get; set; } = false;
    
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    
    public Organization? Grocery { get; set; }
    public List<ClearanceListing> ChildListings { get; set; } = new();
}