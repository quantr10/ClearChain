namespace ClearChain.Domain.Entities;

public class Dispute
{
    public Guid Id { get; set; }
    public Guid PickupRequestId { get; set; }
    public Guid InitiatorId { get; set; }      // NGO who opens dispute
    public string Reason { get; set; } = string.Empty;   // wrong_items, wrong_quantity, damaged, other
    public string? NgoStatement { get; set; }
    public string? GroceryStatement { get; set; }
    public string? PhotoEvidenceUrl { get; set; }
    public string Status { get; set; } = "open";    // open, under_review, resolved_ngo, resolved_grocery, dismissed
    public string? AdminResolution { get; set; }
    public Guid? ResolvedByAdminId { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? ResolvedAt { get; set; }

    public Organization? Initiator { get; set; }
    public PickupRequest? PickupRequest { get; set; }
}
