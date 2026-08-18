namespace ClearChain.Domain.Entities;

public class CartItem
{
    public Guid Id { get; set; }
    public Guid CartId { get; set; }
    public Guid ListingId { get; set; }
    public Guid GroceryId { get; set; }
    public int RequestedQuantity { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public Cart? Cart { get; set; }
    public ClearanceListing? Listing { get; set; }
    public Organization? Grocery { get; set; }
}
