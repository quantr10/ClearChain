using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Auth;

public class LoginRequest
{
    [Required(ErrorMessage = "Email is required")]
    [EmailAddress(ErrorMessage = "Invalid email format")]
    public string Email { get; set; } = string.Empty;

    [Required(ErrorMessage = "Password is required")]
    public string Password { get; set; } = string.Empty;

    // Optional: FCM token for push notifications
    public string? DeviceToken { get; set; }
}