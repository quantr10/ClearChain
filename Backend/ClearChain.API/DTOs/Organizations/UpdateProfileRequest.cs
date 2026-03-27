using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Organizations;

public class UpdateProfileRequest
{
    [StringLength(200, MinimumLength = 3)]
    public string? Name { get; set; }

    [Phone]
    public string? Phone { get; set; }

    public string? Address { get; set; }

    public string? Location { get; set; }

    public string? Hours { get; set; }

    // ═══ NEW FIELDS (Part 1) ═══
    public double? Latitude { get; set; }
    public double? Longitude { get; set; }
    public string? ContactPerson { get; set; }        // NGO only
    public string? PickupInstructions { get; set; }    // Grocery only
    public string? Description { get; set; }           // Both
}