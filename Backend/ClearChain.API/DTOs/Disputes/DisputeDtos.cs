using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Disputes;

/// <summary>
/// Contact details for one side of a dispute — everything an admin needs to reach out and
/// resolve it by phone or email, since there is no in-app negotiation flow between the two
/// organizations.
/// </summary>
public class DisputePartyContact
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string? Phone { get; set; }
    public string? ProfilePictureUrl { get; set; }
}

public class DisputeListingItem
{
    public string Title { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public int Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public string? ExpiryDate { get; set; }
    public string? PhotoUrl { get; set; }
}

public class DisputeListItemData
{
    public string Id { get; set; } = string.Empty;
    public string PickupRequestId { get; set; } = string.Empty;
    public string ListingTitle { get; set; } = string.Empty;
    public string PickupDate { get; set; } = string.Empty;
    public List<DisputeListingItem> Items { get; set; } = new();
    public DisputePartyContact Ngo { get; set; } = new();
    public DisputePartyContact Grocery { get; set; } = new();
    public string Reason { get; set; } = string.Empty;
    public string? NgoStatement { get; set; }
    public string? GroceryStatement { get; set; }
    public string? PhotoEvidenceUrl { get; set; }
    public string Status { get; set; } = string.Empty;
    public string? AdminResolution { get; set; }
    public string CreatedAt { get; set; } = string.Empty;
}

public class DisputeListResponse
{
    public string Message { get; set; } = string.Empty;
    public List<DisputeListItemData> Data { get; set; } = new();
}

public class DisputeResponse
{
    public string Message { get; set; } = string.Empty;
    public DisputeListItemData Data { get; set; } = new();
}

/// <summary>
/// What an NGO sees of its own dispute. The grocery's contact details stay admin-only, and so does
/// the grocery's statement; the NGO sees the status and the admin's resolution note.
/// </summary>
public class MyDisputeData
{
    public string Id { get; set; } = string.Empty;
    public string PickupRequestId { get; set; } = string.Empty;
    public string Reason { get; set; } = string.Empty;
    public string? NgoStatement { get; set; }
    public string? PhotoEvidenceUrl { get; set; }
    public string Status { get; set; } = string.Empty;
    public string? AdminResolution { get; set; }
    public string CreatedAt { get; set; } = string.Empty;
}

public class MyDisputeListResponse
{
    public string Message { get; set; } = string.Empty;
    public List<MyDisputeData> Data { get; set; } = new();
}

/// <summary>
/// Closes out a dispute the admin handled outside the app (phone/email with both parties).
/// GroceryStatement is optional here — filled in by the admin after contacting the grocery,
/// since the grocery has no in-app way to submit one itself.
/// </summary>
public class ResolveDisputeRequest
{
    [Required]
    [RegularExpression("^(resolved_ngo|resolved_grocery|dismissed)$",
        ErrorMessage = "Status must be 'resolved_ngo', 'resolved_grocery', or 'dismissed'")]
    public string Status { get; set; } = string.Empty;

    public string? GroceryStatement { get; set; }

    [Required(ErrorMessage = "A resolution note is required")]
    [StringLength(2000, MinimumLength = 1)]
    public string AdminResolution { get; set; } = string.Empty;
}
