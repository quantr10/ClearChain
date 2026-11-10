using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace ClearChain.API.Common;

/// <summary>Helpers shared by the API controllers.</summary>
public static class ControllerBaseExtensions
{
    /// <summary>Reads the caller's organization id from the JWT subject claim.</summary>
    /// <remarks>
    /// Returns false rather than throwing when the claim is missing or malformed: an
    /// endpoint reached without a usable identity answers 401, and that is a caller
    /// problem, not a server fault for the error middleware to turn into a 500.
    /// </remarks>
    public static bool TryGetUserId(this ControllerBase controller, out Guid userId) =>
        controller.User.TryGetUserId(out userId);

    /// <summary>
    /// The organization id in a validated token: the subject claim, under the name the JWT
    /// handler maps it to or as the raw "sub" when it isn't mapped.
    /// </summary>
    public static bool TryGetUserId(this ClaimsPrincipal principal, out Guid userId)
    {
        var value = principal.FindFirst(ClaimTypes.NameIdentifier)?.Value
                 ?? principal.FindFirst("sub")?.Value;
        return Guid.TryParse(value, out userId);
    }
}
