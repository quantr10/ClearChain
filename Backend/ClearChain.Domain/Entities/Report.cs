namespace ClearChain.Domain.Entities;

public class Report
{
    public Guid Id { get; set; }
    public Guid ReporterId { get; set; }
    public Guid? ListingId { get; set; }         // What is being reported
    public string Reason { get; set; } = string.Empty;   // incorrect_info, inappropriate, expired_listed_fresh, other
    public string? Details { get; set; }
    public string Status { get; set; } = "pending";      // pending, reviewed, dismissed
    public string? AdminNote { get; set; }
    public Guid? ReviewedByAdminId { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? ReviewedAt { get; set; }

    public Organization? Reporter { get; set; }
    public ClearanceListing? Listing { get; set; }
}
