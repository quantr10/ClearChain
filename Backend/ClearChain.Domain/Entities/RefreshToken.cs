namespace ClearChain.Domain.Entities;

public class RefreshToken
{
    public Guid Id { get; set; }
    public Guid OrganizationId { get; set; }
    public string Token { get; set; } = string.Empty;
    public DateTime ExpiresAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? RevokedAt { get; set; }
    public bool IsRevoked { get; set; }
}