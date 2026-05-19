using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Auth;

public class ResendVerificationRequest
{
    [Required, EmailAddress]
    public string Email { get; set; } = string.Empty;
}
