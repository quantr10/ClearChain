using System.ComponentModel.DataAnnotations;

namespace ClearChain.API.DTOs.Auth;

public class LogoutRequest
{
    [Required]
    public string RefreshToken { get; set; } = string.Empty;

    /// <summary>
    /// This device's FCM token, so logout can unregister it — without this, the device
    /// kept receiving the departing account's push notifications until the 60-day
    /// stale-token sweep or another login on the same device overwrote it.
    /// </summary>
    public string? FcmToken { get; set; }
}
