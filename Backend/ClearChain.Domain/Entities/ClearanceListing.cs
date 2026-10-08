using ClearChain.Domain.Enums;

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

    public ListingStatus Status { get; set; } = ListingStatus.Open;

    public int ViewCount { get; set; } = 0;

    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public TimeSpan? PickupTimeStart { get; set; }
    public TimeSpan? PickupTimeEnd { get; set; }

    // Navigation properties
    public Organization? Grocery { get; set; }
    public ListingGroup? Group { get; set; }
}
