namespace ClearChain.Domain.Entities;

public class Review
{
    public Guid Id { get; set; }
    public Guid PickupRequestId { get; set; }
    public Guid ReviewerId { get; set; }       // NGO who reviews Grocery
    public Guid ReviewedId { get; set; }       // Grocery being reviewed
    public int Rating { get; set; }            // 1–5
    public string? Comment { get; set; }
    public DateTime CreatedAt { get; set; }

    public Organization? Reviewer { get; set; }
    public Organization? Reviewed { get; set; }
    public PickupRequest? PickupRequest { get; set; }
}
