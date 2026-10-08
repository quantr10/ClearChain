using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Auth;

public class ForgotPasswordRequest
{
    [Required, EmailAddress]
    public string Email { get; set; } = string.Empty;
}
