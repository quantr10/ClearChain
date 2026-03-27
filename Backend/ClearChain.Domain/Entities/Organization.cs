namespace ClearChain.Domain.Entities;

public class Organization
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Type { get; set; } = string.Empty; // grocery, ngo, admin
    public string Email { get; set; } = string.Empty;
    public string? PasswordHash { get; set; }
    
    public string? Phone { get; set; }
    public string? Address { get; set; }
    public string? Location { get; set; }
    
    public bool Verified { get; set; }
    public string VerificationStatus { get; set; } = "pending";
    public string? Hours { get; set; }
    
    // Google OAuth (Phase 2)
    public string? GoogleId { get; set; }
    public string AuthProvider { get; set; } = "local";
    public string? ProfilePictureUrl { get; set; }
    
    // ═══ NEW FIELDS (Part 1) ═══
    public double? Latitude { get; set; }
    public double? Longitude { get; set; }
    public string? ContactPerson { get; set; }       // NGO only
    public string? PickupInstructions { get; set; }   // Grocery only
    public string? Description { get; set; }          // Both roles
    
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}