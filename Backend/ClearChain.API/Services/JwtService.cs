using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using ClearChain.Domain.Entities;

namespace ClearChain.API.Services;

public interface IJwtService
{
    string GenerateAccessToken(Organization user);
    string GenerateRefreshToken();
    ClaimsPrincipal? ValidateToken(string token);
    Guid? GetUserIdFromToken(string token);
}

public class JwtService : IJwtService
{
    private readonly SymmetricSecurityKey _securityKey;
    private readonly SigningCredentials _credentials;
    private readonly int _expiryMinutes;
    private readonly string _issuer;
    private readonly string _audience;

    public JwtService(IConfiguration configuration)
    {
        var secret = configuration["JWT_SECRET_KEY"]
            ?? throw new InvalidOperationException("JWT_SECRET_KEY is not configured");
        _securityKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secret));
        _credentials = new SigningCredentials(_securityKey, SecurityAlgorithms.HmacSha256);
        _expiryMinutes = int.Parse(configuration["JWT_EXPIRY_MINUTES"] ?? "60");
        _issuer = configuration["JWT_ISSUER"] ?? "";
        _audience = configuration["JWT_AUDIENCE"] ?? "";
    }

    public string GenerateAccessToken(Organization user)
    {
        var claims = new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
            new Claim(JwtRegisteredClaimNames.Email, user.Email),
            new Claim(JwtRegisteredClaimNames.Name, user.Name),
            new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new Claim(ClaimTypes.Role, user.Type),
            new Claim("type", user.Type),
            new Claim("verified", user.Verified.ToString()),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
            new Claim(JwtRegisteredClaimNames.Iat, DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString())
        };

        var token = new JwtSecurityToken(
            issuer: _issuer,
            audience: _audience,
            claims: claims,
            expires: DateTime.UtcNow.AddMinutes(_expiryMinutes),
            signingCredentials: _credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public string GenerateRefreshToken()
    {
        var randomNumber = new byte[64];
        using var rng = RandomNumberGenerator.Create();
        rng.GetBytes(randomNumber);
        return Convert.ToBase64String(randomNumber);
    }

    public ClaimsPrincipal? ValidateToken(string token)
    {
        var tokenHandler = new JwtSecurityTokenHandler();
        try
        {
            var principal = tokenHandler.ValidateToken(token, new TokenValidationParameters
            {
                ValidateIssuer = true,
                ValidateAudience = true,
                ValidateLifetime = true,
                ValidateIssuerSigningKey = true,
                ValidIssuer = _issuer,
                ValidAudience = _audience,
                IssuerSigningKey = _securityKey,
                ClockSkew = TimeSpan.Zero,
                RoleClaimType = ClaimTypes.Role
            }, out _);

            return principal;
        }
        catch
        {
            return null;
        }
    }

    public Guid? GetUserIdFromToken(string token)
    {
        var principal = ValidateToken(token);
        var userIdClaim = principal?.FindFirst(ClaimTypes.NameIdentifier) 
                       ?? principal?.FindFirst(JwtRegisteredClaimNames.Sub);
        
        if (userIdClaim != null && Guid.TryParse(userIdClaim.Value, out var userId))
        {
            return userId;
        }
        return null;
    }
}