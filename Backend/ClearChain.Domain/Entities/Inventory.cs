namespace ClearChain.Domain.Entities;

public class Inventory
{
    public Guid Id { get; set; }
    public Guid NgoId { get; set; }
    public Guid PickupRequestId { get; set; }
    public string ProductName { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public decimal Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public DateTime ExpiryDate { get; set; }
    public string Status { get; set; } = "active"; // active, distributed, expired
    public DateTime ReceivedAt { get; set; }
    public DateTime? DistributedAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}