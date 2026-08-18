namespace ClearChain.Domain.Entities;

public class PickupRequestItem
{
    public Guid Id { get; set; }
    public Guid PickupRequestId { get; set; }
    public Guid? ListingGroupId { get; set; }
    public Guid? OriginalListingId { get; set; }
    public Guid? ReservedListingId { get; set; }
    public int RequestedQuantity { get; set; }
    public string ListingTitle { get; set; } = string.Empty;
    public string ListingCategory { get; set; } = string.Empty;
    public string? ListingExpiryDate { get; set; }
    public string ListingUnit { get; set; } = string.Empty;
    public string? ListingPhotoUrl { get; set; }
    public DateTime CreatedAt { get; set; }

    public PickupRequest? PickupRequest { get; set; }
    public ListingGroup? Group { get; set; }
    public ClearanceListing? OriginalListing { get; set; }
    public ClearanceListing? ReservedListing { get; set; }
}
