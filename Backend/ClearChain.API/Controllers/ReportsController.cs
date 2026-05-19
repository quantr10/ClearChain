using ClearChain.Domain.Entities;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace ClearChain.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ReportsController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public ReportsController(ApplicationDbContext context)
    {
        _context = context;
    }

    // POST api/reports — Report a listing
    [HttpPost]
    public async Task<IActionResult> SubmitReport([FromBody] SubmitReportRequest request)
    {
        if (!TryGetUserId(out var userId)) return Unauthorized();

        var listing = await _context.ClearanceListings.FindAsync(request.ListingId);
        if (listing == null) return NotFound(new { message = "Listing not found" });

        var report = new Report
        {
            Id = Guid.NewGuid(),
            ReporterId = userId,
            ListingId = request.ListingId,
            Reason = request.Reason,
            Details = request.Details,
            Status = "pending",
            CreatedAt = DateTime.UtcNow
        };

        _context.Reports.Add(report);
        await _context.SaveChangesAsync();

        return Ok(new { message = "Report submitted. Our team will review it.", data = new { id = report.Id.ToString() } });
    }

    // GET api/reports — Admin: all reports
    [HttpGet]
    public async Task<IActionResult> GetAllReports(
        [FromQuery] string? status = null,
        [FromQuery] int page = 1, [FromQuery] int pageSize = 20)
    {
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin") return Forbid();

        var clampedPage = Math.Max(1, page);
        var clampedSize = Math.Clamp(pageSize, 1, 50);

        var query = _context.Reports
            .Include(r => r.Reporter)
            .Include(r => r.Listing)
            .AsQueryable();

        if (!string.IsNullOrEmpty(status))
            query = query.Where(r => r.Status == status);

        var total = await query.CountAsync();
        var items = await query
            .OrderByDescending(r => r.CreatedAt)
            .Skip((clampedPage - 1) * clampedSize)
            .Take(clampedSize)
            .ToListAsync();

        return Ok(new
        {
            message = "Reports retrieved",
            data = items.Select(r => new
            {
                id = r.Id.ToString(),
                reporterId = r.ReporterId.ToString(),
                reporterName = r.Reporter?.Name ?? "",
                listingId = r.ListingId?.ToString(),
                listingName = r.Listing?.ProductName ?? "",
                reason = r.Reason,
                details = r.Details,
                status = r.Status,
                adminNote = r.AdminNote,
                createdAt = r.CreatedAt.ToString("o"),
                reviewedAt = r.ReviewedAt?.ToString("o")
            }).ToList(),
            total,
            page = clampedPage,
            pageSize = clampedSize,
            totalPages = (int)Math.Ceiling((double)total / clampedSize)
        });
    }

    // PUT api/reports/{id}/review — Admin reviews a report
    [HttpPut("{id}/review")]
    public async Task<IActionResult> ReviewReport(Guid id, [FromBody] ReviewReportRequest request)
    {
        var userType = User.FindFirst("type")?.Value;
        if (userType != "admin") return Forbid();
        if (!TryGetUserId(out var adminId)) return Unauthorized();

        var report = await _context.Reports.FindAsync(id);
        if (report == null) return NotFound();

        report.Status = request.Status; // "reviewed" or "dismissed"
        report.AdminNote = request.Note;
        report.ReviewedByAdminId = adminId;
        report.ReviewedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(new { message = "Report reviewed" });
    }

    private bool TryGetUserId(out Guid userId)
    {
        var value = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return Guid.TryParse(value, out userId);
    }
}

public class SubmitReportRequest
{
    public Guid ListingId { get; set; }
    public string Reason { get; set; } = string.Empty;
    public string? Details { get; set; }
}

public class ReviewReportRequest
{
    public string Status { get; set; } = string.Empty;
    public string? Note { get; set; }
}
