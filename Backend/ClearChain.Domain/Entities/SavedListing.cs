namespace ClearChain.Domain.Entities;

public class SavedListing
{
    public Guid Id { get; set; }
    public Guid NgoId { get; set; }
    public Guid ListingId { get; set; }
    public DateTime SavedAt { get; set; }

    public Organization? Ngo { get; set; }
    public ClearanceListing? Listing { get; set; }
}
