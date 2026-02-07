namespace ClearChain.Domain.Entities;

public class PickupRequest
{
    public Guid Id { get; set; }
    public Guid NgoId { get; set; }
    public Guid GroceryId { get; set; }
    public DateTime PickupDate { get; set; }
    public string Status { get; set; } = "pending";
    
    public DateTime RequestedAt { get; set; }
    public DateTime? MarkedReadyAt { get; set; }
    public DateTime? MarkedPickedUpAt { get; set; }
    public DateTime? ConfirmedReceivedAt { get; set; }
    
    public string? ProofPhotoUrl { get; set; }
    
    // Navigation properties
    public Organization? Ngo { get; set; }
    public Organization? Grocery { get; set; }
    public List<ClearanceListing> Listings { get; set; } = new();
}