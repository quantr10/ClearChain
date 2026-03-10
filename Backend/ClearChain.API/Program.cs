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

// ═══════════════════════════════════════════════════════════════════════════
// Hangfire Configuration (Background Jobs)
// ═══════════════════════════════════════════════════════════════════════════
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

// ❌ REMOVED: Background service (replaced by Hangfire)
// builder.Services.AddHostedService<NotificationSchedulerService>();

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

// ═══════════════════════════════════════════════════════════════════════════
// Hangfire Dashboard (Admin UI for monitoring jobs)
// Access: https://your-domain/hangfire
// ═══════════════════════════════════════════════════════════════════════════
app.UseHangfireDashboard("/hangfire", new DashboardOptions
{
    AppPath = "/",
    DashboardTitle = "ClearChain Background Jobs",
    // TODO: Add authorization filter in production
    // Authorization = new[] { new HangfireAuthorizationFilter() }
});

// ═══════════════════════════════════════════════════════════════════════════
// Schedule Recurring Jobs (Daily at 2:00 AM UTC)
// Cron format: "minute hour day month day-of-week"
// "0 2 * * *" = Every day at 2:00 AM UTC
// ═══════════════════════════════════════════════════════════════════════════
RecurringJob.AddOrUpdate<NotificationJobs>(
    "check-expiring-listings",
    job => job.CheckExpiringListings(),
    "0 2 * * *",  // Daily at 2 AM UTC
    new RecurringJobOptions
    {
        TimeZone = TimeZoneInfo.Utc
    });

RecurringJob.AddOrUpdate<NotificationJobs>(
    "check-expired-listings",
    job => job.CheckExpiredListings(),
    "0 2 * * *",  // Daily at 2 AM UTC
    new RecurringJobOptions
    {
        TimeZone = TimeZoneInfo.Utc
    });

RecurringJob.AddOrUpdate<NotificationJobs>(
    "check-expiring-inventory",
    job => job.CheckExpiringInventory(),
    "0 2 * * *",  // Daily at 2 AM UTC
    new RecurringJobOptions
    {
        TimeZone = TimeZoneInfo.Utc
    });

RecurringJob.AddOrUpdate<NotificationJobs>(
    "check-expired-inventory",
    job => job.CheckExpiredInventory(),
    "0 2 * * *",  // Daily at 2 AM UTC
    new RecurringJobOptions
    {
        TimeZone = TimeZoneInfo.Utc
    });

app.UseCors("ClearChainPolicy");

app.UseHttpsRedirection();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.MapHub<PickupRequestHub>("/hubs/pickuprequests");
app.MapHub<ListingHub>("/hubs/listings");
app.MapHub<InventoryHub>("/hubs/inventory");
app.MapHub<AdminHub>("/hubs/admin");

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
        new { name = "AdminHub", endpoint = "/hubs/admin" }
    },
    authentication = "JWT Bearer Token (via query string or header)",
    timestamp = DateTime.UtcNow
}).WithTags("Health");

app.MapGet("/api/health/jobs", () => new
{
    status = "active",
    jobs = new[]
    {
        new { name = "check-expiring-listings", schedule = "Daily 2:00 AM UTC", description = "Notify groceries about listings expiring tomorrow" },
        new { name = "check-expired-listings", schedule = "Daily 2:00 AM UTC", description = "Mark expired listings and notify" },
        new { name = "check-expiring-inventory", schedule = "Daily 2:00 AM UTC", description = "Notify NGOs about inventory expiring in 2 days" },
        new { name = "check-expired-inventory", schedule = "Daily 2:00 AM UTC", description = "Mark expired inventory and notify" }
    },
    dashboard = "/hangfire",
    timestamp = DateTime.UtcNow
}).WithTags("Health");

app.Run();