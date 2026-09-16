using System.Security.Claims;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.EntityFrameworkCore;

namespace ClearChain.API.Middleware;

/// <summary>
/// Blocks actions that create platform activity (listings, pickup requests, checkout)
/// for organizations that have not been approved by an admin yet.
/// Read-only endpoints and profile/document management stay accessible so a pending
/// org can still complete its submission while it waits.
/// Admin accounts always pass.
/// </summary>
[AttributeUsage(AttributeTargets.Method | AttributeTargets.Class)]
public sealed class RequireVerifiedOrganizationAttribute : Attribute, IAsyncActionFilter
{
    public async Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
    {
        var userIdValue = context.HttpContext.User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                       ?? context.HttpContext.User.FindFirst("sub")?.Value;

        if (!Guid.TryParse(userIdValue, out var userId))
        {
            context.Result = new UnauthorizedObjectResult(new { message = "User not authenticated" });
            return;
        }

        var db = context.HttpContext.RequestServices.GetRequiredService<ApplicationDbContext>();
        var org = await db.Organizations
            .AsNoTracking()
            .Where(o => o.Id == userId)
            .Select(o => new { o.Type, o.VerificationStatus })
            .FirstOrDefaultAsync();

        if (org == null)
        {
            context.Result = new NotFoundObjectResult(new { message = "Organization not found" });
            return;
        }

        var isApproved = string.Equals(org.VerificationStatus, "approved", StringComparison.OrdinalIgnoreCase);
        var isAdmin = string.Equals(org.Type, "admin", StringComparison.OrdinalIgnoreCase);

        if (!isApproved && !isAdmin)
        {
            context.Result = new ObjectResult(new
            {
                message = "Your organization is pending admin review. This action will be available once your account is approved.",
                verificationStatus = org.VerificationStatus
            })
            {
                StatusCode = StatusCodes.Status403Forbidden
            };
            return;
        }

        await next();
    }
}
