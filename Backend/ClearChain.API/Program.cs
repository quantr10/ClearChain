using DotNetEnv;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using System.Text;
using System.Security.Claims;
using ClearChain.Infrastructure.Data;
using ClearChain.API.Services;
using ClearChain.API.Middleware;
using ClearChain.API.Hubs;
using ClearChain.API.Jobs;
using Microsoft.AspNetCore.SignalR;
using Hangfire;
using Hangfire.PostgreSql;

Env.Load();

var builder = WebApplication.CreateBuilder(args);

builder.Configuration.AddEnvironmentVariables();

var connectionString = builder.Configuration["DATABASE_URL"];
builder.Services.AddDbContext<ApplicationDbContext>(options =>
    options.UseNpgsql(connectionString));

// ── Hangfire Configuration (Background Jobs) ─────────────────────────────────
builder.Services.AddHangfire(config => config
    .SetDataCompatibilityLevel(CompatibilityLevel.Version_180)
    .UseSimpleAssemblyNameTypeSerializer()
    .UseRecommendedSerializerSettings()
    .UsePostgreSqlStorage(options =>
        options.UseNpgsqlConnection(connectionString)));

// Add Hangfire server with custom options
builder.Services.AddHangfireServer(options =>
{
    options.WorkerCount = 5; // Number of concurrent jobs
    options.ServerName = "ClearChain-Notifications";
});

// Register NotificationJobs for dependency injection
builder.Services.AddScoped<NotificationJobs>();

// Add Services
builder.Services.AddScoped<IJwtService, JwtService>();
builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IOrganizationService, OrganizationService>();
builder.Services.AddScoped<IStorageService, SupabaseStorageService>();
builder.Services.AddScoped<IPickupNotificationService, PickupNotificationService>();
builder.Services.AddScoped<IListingNotificationService, ListingNotificationService>();
builder.Services.AddScoped<IInventoryNotificationService, InventoryNotificationService>();
builder.Services.AddScoped<IAdminNotificationService, AdminNotificationService>();
builder.Services.AddScoped<IPushNotificationService, PushNotificationService>();
builder.Services.AddScoped<IImageAnalysisService, AzureVisionService>();
builder.Services.AddScoped<IPickupRequestService, PickupRequestService>();
builder.Services.AddScoped<ICartService, CartService>();
builder.Services.AddScoped<IEmailService, EmailService>();

// Add SignalR with custom user ID provider
builder.Services.AddSignalR();
builder.Services.AddSingleton<IUserIdProvider, CustomUserIdProvider>();

// Configure JWT Authentication
var jwtSecretKey = builder.Configuration["JWT_SECRET_KEY"];
var jwtIssuer = builder.Configuration["JWT_ISSUER"];
var jwtAudience = builder.Configuration["JWT_AUDIENCE"];

builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuer = true,
        ValidateAudience = true,
        ValidateLifetime = true,
        ValidateIssuerSigningKey = true,
        ValidIssuer = jwtIssuer,
        ValidAudience = jwtAudience,
        IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSecretKey!)),
        ClockSkew = TimeSpan.Zero,
        RoleClaimType = ClaimTypes.Role
    };

    options.Events = new JwtBearerEvents
    {
        OnMessageReceived = context =>
        {
            var accessToken = context.Request.Query["access_token"];
            var path = context.HttpContext.Request.Path;

            if (!string.IsNullOrEmpty(accessToken) && path.StartsWithSegments("/hubs"))
            {
                context.Token = accessToken;
            }

            return Task.CompletedTask;
        }
    };
});

builder.Services.AddAuthorization();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();

builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "ClearChain API",
        Version = "v1",
        Description = "Surplus food clearance platform API"
    });

    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Enter 'Bearer' [space] and then your token"
    });

    c.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

builder.Services.AddCors(options =>
{
    options.AddPolicy("ClearChainPolicy", policy =>
    {
        var allowedOrigins = builder.Configuration["ALLOWED_ORIGINS"]?.Split(',')
            ?? new[] { "http://localhost:3000" };

        policy.WithOrigins(allowedOrigins)
              .AllowAnyMethod()
              .AllowAnyHeader()
              .AllowCredentials();
    });
});

var app = builder.Build();

app.UseMiddleware<ErrorHandlingMiddleware>();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(c =>
    {
        c.SwaggerEndpoint("/swagger/v1/swagger.json", "ClearChain API v1");
        c.RoutePrefix = "swagger";
    });
}

// ── Hangfire Dashboard — restricted to admin role ────────────────────────────
// Access: https://your-domain/hangfire
app.UseHangfireDashboard("/hangfire", new DashboardOptions
{
    AppPath = "/",
    DashboardTitle = "ClearChain Background Jobs",
    Authorization = new[] { new HangfireAuthorizationFilter() }
});

// ── Scheduled Jobs — staggered 5 min apart to avoid DB contention ────────────
RecurringJob.AddOrUpdate<NotificationJobs>(
    "check-expiring-listings",
    job => job.CheckExpiringListings(),
    "0 0 * * *",   // 00:00 UTC
    new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

RecurringJob.AddOrUpdate<NotificationJobs>(
    "check-expired-listings",
    job => job.CheckExpiredListings(),
    "5 0 * * *",   // 00:05 UTC
    new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

RecurringJob.AddOrUpdate<NotificationJobs>(
    "check-expiring-inventory",
    job => job.CheckExpiringInventory(),
    "10 0 * * *",  // 00:10 UTC
    new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

RecurringJob.AddOrUpdate<NotificationJobs>(
    "check-expired-inventory",
    job => job.CheckExpiredInventory(),
    "15 0 * * *",  // 00:15 UTC
    new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

RecurringJob.AddOrUpdate<NotificationJobs>(
    "expire-stale-pickup-requests",
    job => job.ExpireStalePickupRequests(),
    "20 0 * * *",  // 00:20 UTC — after the listing sweeps, so released stock lands on fresh rows
    new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

// ── Housekeeping — separate window from the notification jobs ────────────────
RecurringJob.AddOrUpdate<NotificationJobs>(
    "cleanup-refresh-tokens",
    job => job.CleanupExpiredRefreshTokens(),
    "0 1 * * *",   // 01:00 UTC
    new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

RecurringJob.AddOrUpdate<NotificationJobs>(
    "cleanup-old-notifications",
    job => job.CleanupOldNotifications(),
    "10 1 * * *",  // 01:10 UTC
    new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

RecurringJob.AddOrUpdate<NotificationJobs>(
    "prune-stale-fcm-tokens",
    job => job.PruneStaleFcmTokens(),
    "20 1 * * 0",  // Sundays 01:20 UTC — slow-moving data, no need for a daily pass
    new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

// ── Admin dashboard keep-alive ───────────────────────────────────────────────
RecurringJob.AddOrUpdate<NotificationJobs>(
    "broadcast-platform-stats",
    job => job.BroadcastPlatformStats(),
    "0 * * * *",   // hourly
    new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

app.UseCors("ClearChainPolicy");

app.UseHttpsRedirection();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.MapHub<PickupRequestHub>("/hubs/pickuprequests");
app.MapHub<ListingHub>("/hubs/listings");
app.MapHub<InventoryHub>("/hubs/inventory");
app.MapHub<AdminHub>("/hubs/admin");
app.MapHub<NotificationHub>("/hubs/notifications");

app.MapGet("/", () => new
{
    message = "ClearChain API is running!",
    timestamp = DateTime.UtcNow,
    environment = app.Environment.EnvironmentName,
    version = "1.0.0",
    hangfire = "✅ Background jobs active"
}).WithTags("Health");

app.MapGet("/api/health/signalr", () => new
{
    status = "configured",
    hubs = new[]
    {
        new { name = "PickupRequestHub", endpoint = "/hubs/pickuprequests" },
        new { name = "ListingHub", endpoint = "/hubs/listings" },
        new { name = "InventoryHub", endpoint = "/hubs/inventory" },
        new { name = "AdminHub", endpoint = "/hubs/admin" },
        new { name = "NotificationHub", endpoint = "/hubs/notifications" }
    },
    authentication = "JWT Bearer Token (via query string or header)",
    timestamp = DateTime.UtcNow
}).WithTags("Health");

app.MapGet("/api/health/jobs", () => new
{
    status = "active",
    jobs = new[]
    {
        new { name = "check-expiring-listings",       schedule = "Daily 00:00 UTC",  description = "Warn groceries about listings expiring tomorrow" },
        new { name = "check-expired-listings",        schedule = "Daily 00:05 UTC",  description = "Mark listings expired, broadcast and notify" },
        new { name = "check-expiring-inventory",      schedule = "Daily 00:10 UTC",  description = "Warn NGOs about inventory expiring in 2 days" },
        new { name = "check-expired-inventory",       schedule = "Daily 00:15 UTC",  description = "Mark inventory expired, broadcast and notify" },
        new { name = "expire-stale-pickup-requests",  schedule = "Daily 00:20 UTC",  description = "Cancel pending requests past their pickup date and release the reserved stock" },
        new { name = "cleanup-refresh-tokens",        schedule = "Daily 01:00 UTC",  description = "Delete revoked/expired refresh tokens older than 30 days" },
        new { name = "cleanup-old-notifications",     schedule = "Daily 01:10 UTC",  description = "Sweep read notifications after 30 days, everything after 90" },
        new { name = "prune-stale-fcm-tokens",        schedule = "Sundays 01:20 UTC", description = "Drop device tokens not re-registered in 60 days" },
        new { name = "broadcast-platform-stats",      schedule = "Hourly",            description = "Push fresh platform stats to the admin dashboard" }
    },
    dashboard = "/hangfire",
    timestamp = DateTime.UtcNow
}).WithTags("Health");

app.Run();
