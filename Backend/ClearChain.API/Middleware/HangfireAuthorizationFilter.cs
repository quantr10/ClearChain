using Hangfire.Dashboard;

namespace ClearChain.API.Middleware;

public class HangfireAuthorizationFilter : IDashboardAuthorizationFilter
{
    public bool Authorize(DashboardContext context)
    {
        var httpContext = context.GetHttpContext();

        // Must be authenticated
        if (!httpContext.User.Identity?.IsAuthenticated ?? true)
            return false;

        // Must be an admin
        return httpContext.User.IsInRole("admin");
    }
}
