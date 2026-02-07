using Microsoft.EntityFrameworkCore;
using ClearChain.Infrastructure.Data;
using ClearChain.Domain.Entities;
using ClearChain.API.DTOs.Organizations;
using ClearChain.API.DTOs.Auth;

namespace ClearChain.API.Services;

public interface IOrganizationService
{
    Task<List<OrganizationDto>> GetPendingVerificationsAsync(string? type = null);
    Task<List<OrganizationDto>> GetVerifiedOrganizationsAsync(string? type = null);
    Task<(bool Success, string Message)> VerifyOrganizationAsync(Guid organizationId, string action, string? notes = null);
    Task<OrganizationDto?> GetOrganizationByIdAsync(Guid id);
    Task<(bool Success, string Message)> UpdateProfileAsync(Guid userId, UpdateProfileRequest request);
}

public class OrganizationService : IOrganizationService
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<OrganizationService> _logger;

    public OrganizationService(ApplicationDbContext context, ILogger<OrganizationService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<List<OrganizationDto>> GetPendingVerificationsAsync(string? type = null)
    {
        var query = _context.Organizations
            .Where(o => !o.Verified && o.VerificationStatus == "pending");

        if (!string.IsNullOrEmpty(type))
        {
            query = query.Where(o => o.Type == type.ToLower());
        }

        var organizations = await query
            .OrderBy(o => o.CreatedAt)
            .ToListAsync();

        return organizations.Select(MapToDto).ToList();
    }

    public async Task<List<OrganizationDto>> GetVerifiedOrganizationsAsync(string? type = null)
    {
        var query = _context.Organizations
            .Where(o => o.Verified && o.VerificationStatus == "approved");

        if (!string.IsNullOrEmpty(type))
        {
            query = query.Where(o => o.Type == type.ToLower());
        }

        var organizations = await query
            .OrderBy(o => o.Name)
            .ToListAsync();

        return organizations.Select(MapToDto).ToList();
    }

    public async Task<(bool Success, string Message)> VerifyOrganizationAsync(
        Guid organizationId, 
        string action, 
        string? notes = null)
    {
        var organization = await _context.Organizations.FindAsync(organizationId);

        if (organization == null)
        {
            return (false, "Organization not found");
        }

        if (organization.Verified)
        {
            return (false, "Organization already verified");
        }

        if (action.ToLower() == "approved")
        {
            organization.Verified = true;
            organization.VerificationStatus = "approved";
        }
        else if (action.ToLower() == "rejected")
        {
            organization.Verified = false;
            organization.VerificationStatus = "rejected";
        }
        else
        {
            return (false, "Invalid action. Must be 'approved' or 'rejected'");
        }

        organization.UpdatedAt = DateTime.UtcNow;

        // Create audit log
        var auditLog = new AuditLog
        {
            Id = Guid.NewGuid(),
            UserId = organizationId, // In real app, this should be admin's ID
            Action = $"Organization {action}",
            EntityType = "Organization",
            EntityId = organizationId,
            OldValue = null,
            NewValue = action,
            Timestamp = DateTime.UtcNow
        };

        _context.AuditLogs.Add(auditLog);
        await _context.SaveChangesAsync();

        return (true, $"Organization {action} successfully");
    }

    public async Task<OrganizationDto?> GetOrganizationByIdAsync(Guid id)
    {
        var organization = await _context.Organizations.FindAsync(id);
        return organization == null ? null : MapToDto(organization);
    }

    public async Task<(bool Success, string Message)> UpdateProfileAsync(
        Guid userId, 
        UpdateProfileRequest request)
    {
        var user = await _context.Organizations.FindAsync(userId);

        if (user == null)
        {
            return (false, "User not found");
        }

        // Update only provided fields
        if (!string.IsNullOrEmpty(request.Name))
            user.Name = request.Name;

        if (!string.IsNullOrEmpty(request.Phone))
            user.Phone = request.Phone;

        if (!string.IsNullOrEmpty(request.Address))
            user.Address = request.Address;

        if (!string.IsNullOrEmpty(request.Location))
            user.Location = request.Location;

        if (request.Hours != null)
            user.Hours = request.Hours;

        user.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        return (true, "Profile updated successfully");
    }

    private static OrganizationDto MapToDto(Organization org)
    {
        return new OrganizationDto
        {
            Id = org.Id,
            Name = org.Name,
            Type = org.Type,
            Email = org.Email,
            Phone = org.Phone,
            Address = org.Address,
            Location = org.Location,
            Verified = org.Verified,
            VerificationStatus = org.VerificationStatus,
            Hours = org.Hours,
            ProfilePictureUrl = org.ProfilePictureUrl,
            CreatedAt = org.CreatedAt
        };
    }
}