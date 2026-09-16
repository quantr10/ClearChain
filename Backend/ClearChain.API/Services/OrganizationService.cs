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
    private readonly IPushNotificationService _pushNotificationService;

    public OrganizationService(
        ApplicationDbContext context,
        ILogger<OrganizationService> logger,
        IPushNotificationService pushNotificationService)
    {
        _context = context;
        _logger = logger;
        _pushNotificationService = pushNotificationService;
    }

    public async Task<List<OrganizationDto>> GetPendingVerificationsAsync(string? type = null)
    {
        var query = _context.Organizations
            .Where(o => !o.Verified && o.VerificationStatus == "pending");

        if (!string.IsNullOrEmpty(type))
            query = query.Where(o => o.Type == type.ToLower());

        var organizations = await query.OrderBy(o => o.CreatedAt).ToListAsync();
        return organizations.Select(MapToDto).ToList();
    }

    public async Task<List<OrganizationDto>> GetVerifiedOrganizationsAsync(string? type = null)
    {
        var query = _context.Organizations
            .Where(o => o.Verified && o.VerificationStatus == "approved");

        if (!string.IsNullOrEmpty(type))
            query = query.Where(o => o.Type == type.ToLower());

        var organizations = await query.OrderBy(o => o.Name).ToListAsync();
        return organizations.Select(MapToDto).ToList();
    }

    public async Task<(bool Success, string Message)> VerifyOrganizationAsync(
        Guid organizationId, string action, string? notes = null)
    {
        var organization = await _context.Organizations.FindAsync(organizationId);
        if (organization == null)
            return (false, "Organization not found");
        if (organization.Verified)
            return (false, "Organization already verified");

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
            return (false, "Invalid action. Must be 'approved' or 'rejected'");

        organization.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
        return (true, $"Organization {action} successfully");
    }

    public async Task<OrganizationDto?> GetOrganizationByIdAsync(Guid id)
    {
        var organization = await _context.Organizations.FindAsync(id);
        return organization == null ? null : MapToDto(organization);
    }

    // ═══ UPDATED: handles new fields (Part 1) ═══
    public async Task<(bool Success, string Message)> UpdateProfileAsync(
        Guid userId, UpdateProfileRequest request)
    {
        var user = await _context.Organizations.FindAsync(userId);
        if (user == null)
            return (false, "User not found");

        // Update only provided fields (existing)
        if (!string.IsNullOrEmpty(request.Name)) user.Name = request.Name;
        if (!string.IsNullOrEmpty(request.Phone)) user.Phone = request.Phone;
        if (!string.IsNullOrEmpty(request.Address)) user.Address = request.Address;
        if (!string.IsNullOrEmpty(request.Location)) user.Location = request.Location;
        if (request.State != null) user.State = request.State;
        if (request.ZipCode != null) user.ZipCode = request.ZipCode;
        if (request.Hours != null) user.Hours = request.Hours;

        // ═══ NEW FIELDS (Part 1) ═══
        if (request.Latitude.HasValue) user.Latitude = request.Latitude;
        if (request.Longitude.HasValue) user.Longitude = request.Longitude;
        if (request.ContactPerson != null) user.ContactPerson = request.ContactPerson;
        if (request.PickupInstructions != null) user.PickupInstructions = request.PickupInstructions;
        if (request.Description != null) user.Description = request.Description;

        var resubmitted = ResubmitIfRejected(user);

        user.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        if (resubmitted)
            await NotifyResubmissionAsync(user);

        return (true, resubmitted ? "Profile updated and resubmitted for review" : "Profile updated successfully");
    }

    /// <summary>
    /// A rejected org that edits its own profile or re-uploads a document is treated as
    /// having addressed the feedback — flip it back to "pending" so it reappears in the
    /// admin's review queue instead of staying stuck as rejected forever.
    /// Does not save changes; caller is expected to call SaveChangesAsync.
    /// </summary>
    public static bool ResubmitIfRejected(Organization org)
    {
        if (org.VerificationStatus != "rejected")
            return false;

        org.VerificationStatus = "pending";
        org.VerificationNotes = null;
        return true;
    }

    private async Task NotifyResubmissionAsync(Organization org)
    {
        try
        {
            await _pushNotificationService.SendResubmissionAlertToAdmins(new DTOs.Admin.OrganizationData
            {
                Id = org.Id.ToString(),
                Name = org.Name,
                Email = org.Email,
                Type = org.Type,
                Phone = org.Phone ?? "",
                Address = org.Address ?? "",
                Location = org.Location ?? "",
                Verified = org.Verified,
                VerificationStatus = org.VerificationStatus,
                CreatedAt = org.CreatedAt.ToString("o")
            });
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to send resubmission alert for {OrgId}", org.Id);
        }
    }

    // ═══ UPDATED: includes new fields (Part 1) ═══
    private static OrganizationDto MapToDto(Organization org)
    {
        return new OrganizationDto
        {
            Id = org.Id,
            Name = org.Name,
            Type = org.Type,
            Email = org.Email,
            Phone = org.Phone ?? "",
            Address = org.Address ?? "",
            Location = org.Location ?? "",
            State = org.State ?? "",
            ZipCode = org.ZipCode ?? "",
            Verified = org.Verified,
            VerificationStatus = org.VerificationStatus,
            VerificationNotes = org.VerificationNotes,
            Hours = org.Hours,
            ProfilePictureUrl = org.ProfilePictureUrl,
            CreatedAt = org.CreatedAt.ToString("o"),
            DocumentUrl = org.DocumentUrl,
            // NEW
            Latitude = org.Latitude,
            Longitude = org.Longitude,
            ContactPerson = org.ContactPerson,
            PickupInstructions = org.PickupInstructions,
            Description = org.Description
        };
    }
}
