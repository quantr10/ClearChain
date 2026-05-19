namespace ClearChain.Domain.Entities;

public class Message
{
    public Guid Id { get; set; }
    public Guid PickupRequestId { get; set; }
    public Guid SenderId { get; set; }
    public Guid ReceiverId { get; set; }
    public string Content { get; set; } = string.Empty;
    public bool IsRead { get; set; } = false;
    public DateTime SentAt { get; set; }
    public DateTime? ReadAt { get; set; }

    public Organization? Sender { get; set; }
    public Organization? Receiver { get; set; }
    public PickupRequest? PickupRequest { get; set; }
}
