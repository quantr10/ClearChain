namespace ClearChain.Domain.Entities;

public class Notification
{
    public Guid Id { get; set; }
    public Guid RecipientId { get; set; }       // Organization receiving this notification
    public string Type { get; set; } = string.Empty;   // new_listing, request_update, expiry_reminder, etc.
    public string Title { get; set; } = string.Empty;
    public string Body { get; set; } = string.Empty;
    public string? RelatedId { get; set; }       // ID of related entity (listing, request, etc.)
    public string? RelatedType { get; set; }     // "listing", "pickup_request", "inventory"
    public bool IsRead { get; set; } = false;
    public DateTime CreatedAt { get; set; }
    public DateTime? ReadAt { get; set; }

    public Organization? Recipient { get; set; }
}
