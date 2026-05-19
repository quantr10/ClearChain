using ClearChain.Domain.Enums;

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
    public InventoryStatus Status { get; set; } = InventoryStatus.Active;
    public string? Notes { get; set; }
    public string? PhotoUrl { get; set; }
    public int BeneficiaryCount { get; set; } = 0;   // How many people received this item when distributed
    public bool IsManuallyAdded { get; set; } = false;  // true = not from a pickup request
    public string? SourcePickupRequestId { get; set; }  // original request link for traceability

    public DateTime ReceivedAt { get; set; }
    public DateTime? DistributedAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}