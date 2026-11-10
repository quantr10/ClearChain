using ClearChain.API.Common;
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
/// The answer is cached so this doesn't add a database round trip to every request. The deletion
/// itself writes the cache through <see cref="MarkDeleted"/>, so the instance that served it
/// refuses the account at once; any other instance catches up within <see cref="_cacheFor"/>.
/// </summary>
public static class DeletedAccountTokenCheck
{
    private static readonly TimeSpan _cacheFor = TimeSpan.FromMinutes(1);

    private static string CacheKey(Guid userId) => $"account_deleted_{userId}";

    public static async Task RejectIfDeletedAsync(TokenValidatedContext context)
    {
        if (context.Principal is not { } principal || !principal.TryGetUserId(out var userId)) return;

        var services = context.HttpContext.RequestServices;
        var isDeleted = await services.GetRequiredService<IMemoryCache>().GetOrCreateAsync(
            CacheKey(userId),
            entry =>
            {
                entry.AbsoluteExpirationRelativeToNow = _cacheFor;
                return services.GetRequiredService<ApplicationDbContext>().Organizations
                    .AsNoTracking()
                    .AnyAsync(o => o.Id == userId && o.IsDeleted);
            });

        if (isDeleted) context.Fail("This account has been deleted");
    }

    /// <summary>Records a deletion once it is saved, replacing any cached "not deleted" answer.</summary>
    public static void MarkDeleted(IMemoryCache cache, Guid userId) =>
        cache.Set(CacheKey(userId), true, _cacheFor);
}
