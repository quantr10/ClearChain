using System.Security.Claims;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Caching.Memory;

namespace ClearChain.API.Middleware;

/// <summary>
/// Refuses the access token of a deleted account, which would otherwise stay valid until it
/// expires. The app answers the 401 by refreshing, finds its refresh token revoked too, and goes
/// back to Login, so a device still signed in leaves on its next request.
///
/// The answer is cached so this doesn't add a database round trip to every request, which leaves
/// a deleted account reading for up to <see cref="CacheFor"/>. Actions that create platform
/// activity don't wait for that: <see cref="RequireVerifiedOrganizationAttribute"/> asks the
/// database directly.
/// </summary>
public static class DeletedAccountTokenCheck
{
    private static readonly TimeSpan CacheFor = TimeSpan.FromMinutes(1);

    public static async Task RejectIfDeletedAsync(TokenValidatedContext context)
    {
        var userIdValue = context.Principal?.FindFirst(ClaimTypes.NameIdentifier)?.Value
                       ?? context.Principal?.FindFirst("sub")?.Value;
        if (!Guid.TryParse(userIdValue, out var userId)) return;

        var services = context.HttpContext.RequestServices;
        var isDeleted = await services.GetRequiredService<IMemoryCache>().GetOrCreateAsync(
            $"account_deleted_{userId}",
            entry =>
            {
                entry.AbsoluteExpirationRelativeToNow = CacheFor;
                return services.GetRequiredService<ApplicationDbContext>().Organizations
                    .AsNoTracking()
                    .AnyAsync(o => o.Id == userId && o.IsDeleted);
            });

        if (isDeleted) context.Fail("This account has been deleted");
    }
}
