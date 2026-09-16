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
    public string? State { get; set; }
    public string? ZipCode { get; set; }

    public bool Verified { get; set; }
    public string VerificationStatus { get; set; } = "pending";
    public string? VerificationNotes { get; set; }   // Admin's note when approving/rejecting (e.g. rejection reason)
    public string? Hours { get; set; }

    public string AuthProvider { get; set; } = "local";
    public string? ProfilePictureUrl { get; set; }

    public double? Latitude { get; set; }
    public double? Longitude { get; set; }
    public string? ContactPerson { get; set; }       // NGO only
    public string? PickupInstructions { get; set; }   // Grocery only
    public string? Description { get; set; }          // Both roles

    // ── Auth security ────────────────────────────────────────────────────────
    public int FailedLoginCount { get; set; } = 0;
    public DateTime? LockoutUntil { get; set; }
    public bool IsDeleted { get; set; } = false;
    public DateTime? DeletedAt { get; set; }

    // ── Email verification ───────────────────────────────────────────────────
    public bool EmailVerified { get; set; } = false;
    public string? EmailVerificationToken { get; set; }
    public DateTime? EmailVerificationTokenExpiry { get; set; }

    // ── Onboarding document ──────────────────────────────────────────────────
    public string? DocumentUrl { get; set; }        // Verification doc (business reg / charity cert)
    public string? DocumentMimeType { get; set; }

    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}
