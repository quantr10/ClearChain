using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Auth;

public class RegisterRequest
{
    [Required(ErrorMessage = "Organization name is required")]
    [StringLength(200, MinimumLength = 3)]
    public string Name { get; set; } = string.Empty;

    [Required(ErrorMessage = "Organization type is required")]
    [RegularExpression("^(grocery|ngo|admin)$", ErrorMessage = "Type must be 'grocery', 'ngo', or 'admin'")]
    public string Type { get; set; } = string.Empty;

    [Required(ErrorMessage = "Email is required")]
    [EmailAddress(ErrorMessage = "Invalid email format")]
    public string Email { get; set; } = string.Empty;

    [Required(ErrorMessage = "Password is required")]
    [StringLength(100, MinimumLength = 8, ErrorMessage = "Password must be at least 8 characters")]
    [RegularExpression(@"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$", 
        ErrorMessage = "Password must contain uppercase, lowercase, and number")]
    public string Password { get; set; } = string.Empty;

    public string? FcmToken { get; set; }
}