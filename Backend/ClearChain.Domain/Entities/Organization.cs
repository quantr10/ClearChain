namespace ClearChain.Domain.Entities;

public class Organization
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Type { get; set; } = string.Empty; // grocery, ngo, admin
    public string Email { get; set; } = string.Empty;
    public string? PasswordHash { get; set; }
    public string Phone { get; set; } = string.Empty;
    public string Address { get; set; } = string.Empty;
    public string Location { get; set; } = string.Empty;
    public bool Verified { get; set; }
    public string VerificationStatus { get; set; } = "pending";
    public string? Hours { get; set; }
    
    // Google OAuth (Phase 2)
    public string? GoogleId { get; set; }
    public string AuthProvider { get; set; } = "local";
    public string? ProfilePictureUrl { get; set; }
    
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}