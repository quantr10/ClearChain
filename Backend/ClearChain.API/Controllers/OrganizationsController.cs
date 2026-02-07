using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using ClearChain.API.Services;
using ClearChain.API.DTOs.Organizations;
using ClearChain.API.DTOs.Common;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/organizations")]
[Authorize]
public class OrganizationsController : ControllerBase
{
    private readonly IOrganizationService _organizationService;

    public OrganizationsController(IOrganizationService organizationService)
    {
        _organizationService = organizationService;
    }

    /// <summary>
    /// Get pending verification requests (Admin only)
    /// </summary>
    [HttpGet("pending")]
    public async Task<IActionResult> GetPendingVerifications([FromQuery] string? type = null)
    {
        // TODO: Add admin role check in real implementation
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin")
        {
            return Forbid();
        }

        var organizations = await _organizationService.GetPendingVerificationsAsync(type);
        return Ok(ApiResponse<List<DTOs.Auth.OrganizationDto>>.SuccessResponse(
            organizations,
            $"Retrieved {organizations.Count} pending verifications"
        ));
    }

    /// <summary>
    /// Get verified organizations (Admin only)
    /// </summary>
    [HttpGet("verified")]
    public async Task<IActionResult> GetVerifiedOrganizations([FromQuery] string? type = null)
    {
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin")
        {
            return Forbid();
        }

        var organizations = await _organizationService.GetVerifiedOrganizationsAsync(type);
        return Ok(ApiResponse<List<DTOs.Auth.OrganizationDto>>.SuccessResponse(
            organizations,
            $"Retrieved {organizations.Count} verified organizations"
        ));
    }

    /// <summary>
    /// Verify or reject an organization (Admin only)
    /// </summary>
    [HttpPut("{id}/verify")]
    public async Task<IActionResult> VerifyOrganization(
        Guid id,
        [FromBody] VerifyOrganizationRequest request)
    {
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin")
        {
            return Forbid();
        }

        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var (success, message) = await _organizationService.VerifyOrganizationAsync(
            id,
            request.Action,
            request.Notes
        );

        if (!success)
        {
            return BadRequest(ApiResponse<object>.ErrorResponse(message));
        }

        return Ok(ApiResponse<object>.SuccessResponse(null!, message));
    }

    /// <summary>
    /// Get organization by ID
    /// </summary>
    [HttpGet("{id}")]
    public async Task<IActionResult> GetOrganizationById(Guid id)
    {
        var organization = await _organizationService.GetOrganizationByIdAsync(id);

        if (organization == null)
        {
            return NotFound(ApiResponse<object>.ErrorResponse("Organization not found"));
        }

        return Ok(ApiResponse<DTOs.Auth.OrganizationDto>.SuccessResponse(
            organization,
            "Organization retrieved successfully"
        ));
    }

    /// <summary>
    /// Update current user's profile
    /// </summary>
    [HttpPut("profile")]
    public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileRequest request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value
                  ?? User.FindFirst("sub")?.Value;

        if (userId == null || !Guid.TryParse(userId, out var userGuid))
        {
            return Unauthorized();
        }

        var (success, message) = await _organizationService.UpdateProfileAsync(userGuid, request);

        if (!success)
        {
            return BadRequest(ApiResponse<object>.ErrorResponse(message));
        }

        return Ok(ApiResponse<object>.SuccessResponse(null!, message));
    }
}