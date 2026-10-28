namespace ClearChain.Domain.Entities;

public class Review
{
    public Guid Id { get; set; }
    public Guid PickupRequestId { get; set; }
    public Guid ReviewerId { get; set; }       // Either party on the pickup
    public Guid ReviewedId { get; set; }       // The other party
    public int Rating { get; set; }            // 1–5
    public string? Comment { get; set; }
    public DateTime CreatedAt { get; set; }

    public PickupRequest? PickupRequest { get; set; }
}
