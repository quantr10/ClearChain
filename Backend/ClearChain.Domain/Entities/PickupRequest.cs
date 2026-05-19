using ClearChain.Domain.Enums;

namespace ClearChain.Domain.Entities;

public class PickupRequest
{
    public Guid Id { get; set; }
    public Guid NgoId { get; set; }
    public Guid GroceryId { get; set; }
    public Guid? ListingId { get; set; }
    public DateTime PickupDate { get; set; }
    public PickupRequestStatus Status { get; set; } = PickupRequestStatus.Pending;
    
    public DateTime RequestedAt { get; set; }
    public DateTime? MarkedReadyAt { get; set; }
    public DateTime? MarkedPickedUpAt { get; set; }
    public DateTime? ConfirmedReceivedAt { get; set; }
    public string? ProofPhotoUrl { get; set; }
    public int? RequestedQuantity { get; set; }
    public string? PickupTime { get; set; }
    public string? Notes { get; set; }
    public string ListingTitle { get; set; } = string.Empty;
    public string ListingCategory { get; set; } = string.Empty;
    public string? ListingExpiryDate { get; set; }   // yyyy-MM-dd, snapshot at request creation
    public string ListingUnit { get; set; } = string.Empty;

    public string? CancellationReason { get; set; }  // set when Status == Cancelled
    public string? VehicleType { get; set; }          // van, car, bike, other
    public string? LicensePlate { get; set; }
    public bool RequiresRefrigeration { get; set; } = false;
    public bool IsFragile { get; set; } = false;
    public bool IsHeavy { get; set; } = false;

    public Organization? Ngo { get; set; }
    public Organization? Grocery { get; set; }
}