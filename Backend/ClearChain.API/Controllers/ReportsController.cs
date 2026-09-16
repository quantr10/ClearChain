using ClearChain.API.Common;
using ClearChain.Domain.Entities;
using ClearChain.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

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
        if (!this.TryGetUserId(out var userId)) return Unauthorized();

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

}

public class SubmitReportRequest
{
    public Guid ListingId { get; set; }
    public string Reason { get; set; } = string.Empty;
    public string? Details { get; set; }
}
