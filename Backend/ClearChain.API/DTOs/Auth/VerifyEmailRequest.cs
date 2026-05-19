using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Auth;

public class VerifyEmailRequest
{
    [Required, EmailAddress]
    public string Email { get; set; } = string.Empty;

    [Required]
    public string Code { get; set; } = string.Empty;
}
