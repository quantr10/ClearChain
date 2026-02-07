namespace ClearChain.Domain.Entities;

public class DistributedItem
{
    public Guid Id { get; set; }
    public Guid ListingId { get; set; }
    public Guid NgoId { get; set; }
    public string ProductName { get; set; } = string.Empty;
    public decimal Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public string GroceryName { get; set; } = string.Empty;
    public DateTime PickupDate { get; set; }
    public DateTime DistributedAt { get; set; }
    public DateTime? ExpirationDate { get; set; }
}