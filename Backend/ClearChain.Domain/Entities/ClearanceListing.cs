namespace ClearChain.Domain.Entities;

public class ClearanceListing
{
    public Guid Id { get; set; }
    public Guid GroceryId { get; set; }
    public string ProductName { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public decimal Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public DateTime? ExpirationDate { get; set; }
    public DateTime ClearanceDeadline { get; set; }
    public string? Notes { get; set; }
    public string? PhotoUrl { get; set; }
    public string Status { get; set; } = "open";
    public Guid? PickupRequestId { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    
    // Navigation properties
    public Organization? Grocery { get; set; }
}