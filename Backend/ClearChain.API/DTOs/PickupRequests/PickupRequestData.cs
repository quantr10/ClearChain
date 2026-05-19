namespace ClearChain.API.DTOs.PickupRequests;

public class PickupRequestData
{
    public string Id { get; set; } = string.Empty;
    public string ListingId { get; set; } = string.Empty;
    public string NgoId { get; set; } = string.Empty;
    public string NgoName { get; set; } = string.Empty;
    public string GroceryId { get; set; } = string.Empty;
    public string GroceryName { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public int RequestedQuantity { get; set; }
    public string PickupDate { get; set; } = string.Empty;
    public string PickupTime { get; set; } = string.Empty;
    public string? Notes { get; set; }
    public string ListingTitle { get; set; } = string.Empty;
    public string ListingCategory { get; set; } = string.Empty;
    public string? ListingExpiryDate { get; set; }
    public string ListingUnit { get; set; } = string.Empty;
    public string CreatedAt { get; set; } = string.Empty;
    public string? MarkedReadyAt { get; set; }
    public string? MarkedPickedUpAt { get; set; }
    public string? ConfirmedReceivedAt { get; set; }
    public string? ProofPhotoUrl { get; set; }
    public double? NgoPickupRate { get; set; }
    public int NgoTotalCompleted { get; set; }
    public string? VehicleType { get; set; }
    public bool RequiresRefrigeration { get; set; } = false;
    public bool IsFragile { get; set; } = false;
    public bool IsHeavy { get; set; } = false;
    public string? ListingDescription { get; set; }
}