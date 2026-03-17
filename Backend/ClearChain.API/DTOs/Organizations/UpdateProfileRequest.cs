using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Organizations;

public class UpdateProfileRequest
{
    [StringLength(200, MinimumLength = 3)]
    public string? Name { get; set; }

    // ✅ ADDED: Fields removed from RegisterRequest
    [Phone]
    public string? Phone { get; set; }

    public string? Address { get; set; }

    public string? Location { get; set; }

    public string? Hours { get; set; }
    
    // ❌ NOT INCLUDED: Type (should not be changeable after registration)
}