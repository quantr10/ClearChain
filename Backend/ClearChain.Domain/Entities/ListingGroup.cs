namespace ClearChain.Domain.Entities;

public class ListingGroup
{
    public Guid Id { get; set; }
    public Guid? OriginalListingId { get; set; }
    public Guid GroceryId { get; set; }

    // Quantity tracking
    public decimal OriginalQuantity { get; set; }
    public decimal TotalReserved { get; set; } = 0;
    public decimal TotalAvailable { get; set; } = 0;
    public decimal TotalCompleted { get; set; } = 0;
    public decimal TotalRemoved { get; set; } = 0;

    public bool IsFullyConsumed { get; set; } = false;

    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public List<ClearanceListing> ChildListings { get; set; } = new();
}
