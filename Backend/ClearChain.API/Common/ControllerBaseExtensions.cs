using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;

namespace ClearChain.API.Common;

/// <summary>
/// Helpers shared by the API controllers.
/// </summary>
public static class ControllerBaseExtensions
{
    /// <summary>
    /// Reads the caller's organization id from the JWT subject claim.
    /// </summary>
    /// <remarks>
    /// Returns false rather than throwing when the claim is missing or malformed: an
    /// endpoint reached without a usable identity answers 401, and that is a caller
    /// problem, not a server fault for the error middleware to turn into a 500.
    /// </remarks>
    public static bool TryGetUserId(this ControllerBase controller, out Guid userId)
    {
        var value = controller.User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.TryParse(value, out userId);
    }
}
