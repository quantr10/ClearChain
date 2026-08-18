namespace ClearChain.Domain.Entities;

public class Cart
{
    public Guid Id { get; set; }
    public Guid NgoId { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public Organization? Ngo { get; set; }
    public ICollection<CartItem> Items { get; set; } = new List<CartItem>();
}
