namespace ClearChain.Domain.Entities;

public class FCMToken
{
    public Guid Id { get; set; }
    public Guid OrganizationId { get; set; }
    public string Token { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    
    // Navigation property
    public Organization? Organization { get; set; }
}