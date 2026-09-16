namespace ClearChain.API.DTOs.Cart;

public class CartItemData
{
    public string Id { get; set; } = string.Empty;
    public string ListingId { get; set; } = string.Empty;
    public string GroceryId { get; set; } = string.Empty;
    public string GroceryName { get; set; } = string.Empty;
    public string? GroceryProfilePictureUrl { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public string Unit { get; set; } = string.Empty;
    public int RequestedQuantity { get; set; }
    public decimal MaxQuantity { get; set; }
    public string? ExpiryDate { get; set; }
    public string? ImageUrl { get; set; }
    public string? PickupTimeStart { get; set; }
    public string? PickupTimeEnd { get; set; }
    public string Status { get; set; } = string.Empty;
    public bool IsValid { get; set; }
    public string? InvalidReason { get; set; }
}

public class CartGroupData
{
    public string GroceryId { get; set; } = string.Empty;
    public string GroceryName { get; set; } = string.Empty;
    public string? GroceryProfilePictureUrl { get; set; }
    public List<CartItemData> Items { get; set; } = new();
    public string? EarliestExpiryDate { get; set; }
    public string? PickupTimeStart { get; set; }
    public string? PickupTimeEnd { get; set; }
    public bool CanCheckout => Items.Count > 0 && Items.All(i => i.IsValid);
}

public class CartResponse
{
    public string Message { get; set; } = string.Empty;
    public List<CartGroupData> Data { get; set; } = new();
}

public class AddCartItemRequest
{
    public string ListingId { get; set; } = string.Empty;
    public int Quantity { get; set; } = 1;
}

public class UpdateCartItemRequest
{
    public int Quantity { get; set; }
}

public class CheckoutCartGroupRequest
{
    public string GroceryId { get; set; } = string.Empty;
    public string PickupDate { get; set; } = string.Empty;
    public string PickupTime { get; set; } = string.Empty;
    public string? Notes { get; set; }
    public bool RequiresRefrigeration { get; set; } = false;
    public bool IsFragile { get; set; } = false;
    public bool IsHeavy { get; set; } = false;
}
