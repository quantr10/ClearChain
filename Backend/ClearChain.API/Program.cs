using DotNetEnv;
using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;

// Load .env file
var envPath = Path.Combine(Directory.GetCurrentDirectory(), ".env");
Console.WriteLine($"Looking for .env at: {envPath}");
Console.WriteLine($"File exists: {File.Exists(envPath)}");

if (File.Exists(envPath))
{
    Env.Load(envPath);
    Console.WriteLine(".env file loaded successfully");
}
else
{
    Console.WriteLine("WARNING: .env file not found!");
}

var builder = WebApplication.CreateBuilder(args);

// Add environment variables from .env
builder.Configuration.AddEnvironmentVariables();

// DEBUG: Print connection string (remove in production!)
var connString = builder.Configuration["DATABASE_URL"];
Console.WriteLine($"DATABASE_URL loaded: {(string.IsNullOrEmpty(connString) ? "EMPTY/NULL" : "Found (length: " + connString.Length + ")")}");

// Add DbContext
if (string.IsNullOrEmpty(connString))
{
    Console.WriteLine("ERROR: DATABASE_URL is empty or null!");
}
else
{
    builder.Services.AddDbContext<ApplicationDbContext>(options =>
        options.UseNpgsql(connString));
}

// Add services to the container.
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Configure CORS
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

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseCors("ClearChainPolicy");

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

// Test endpoint
app.MapGet("/", () => new
{
    message = "ClearChain API is running!",
    timestamp = DateTime.UtcNow,
    environment = app.Environment.EnvironmentName
});

// Database test endpoint
app.MapGet("/api/health/database", async (ApplicationDbContext db) =>
{
    try
    {
        var count = await db.Organizations.CountAsync();
        return Results.Ok(new
        {
            status = "healthy",
            organizationsCount = count,
            message = "Database connection successful!"
        });
    }
    catch (Exception ex)
    {
        return Results.Problem($"Database error: {ex.Message}");
    }
});

app.Run();