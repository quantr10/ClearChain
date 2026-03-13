using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using ClearChain.API.Services;
using ClearChain.API.DTOs.ImageAnalysis;

namespace ClearChain.API.Controllers;

/// <summary>
/// AI-powered food image analysis for auto-filling listing forms
/// </summary>
[ApiController]
[Route("api/[controller]")]
public class ImageAnalysisController : ControllerBase
{
    private readonly IImageAnalysisService _analysisService;
    private readonly IStorageService _storageService; // ✅ ADD THIS
    private readonly ILogger<ImageAnalysisController> _logger;

    // Allowed image formats
    private static readonly string[] AllowedExtensions = { ".jpg", ".jpeg", ".png", ".gif", ".bmp" };
    private const long MaxFileSize = 4 * 1024 * 1024; // 4 MB (Azure limit)

    public ImageAnalysisController(
        IImageAnalysisService analysisService,
        IStorageService storageService,
        ILogger<ImageAnalysisController> logger)
    {
        _analysisService = analysisService;
        _storageService = storageService;
        _logger = logger;
    }

    /// <summary>
    /// Analyze food image with AI and get auto-fill data for CreateListingRequest
    /// </summary>
    /// <remarks>
    /// POST /api/imageanalysis/analyze
    /// 
    /// Upload image from camera or album
    /// Returns: Title, Category, ExpiryDate, Notes, ImageUrl
    /// 
    /// Flow:
    /// 1. Upload to Supabase storage
    /// 2. Analyze with Azure Computer Vision
    /// 3. Return auto-fill data
    /// 4. Save analysis to database
    /// 
    /// Example Response:
    /// {
    ///   "success": true,
    ///   "message": "Image analyzed successfully",
    ///   "data": {
    ///     "title": "Fresh Bread",
    ///     "category": "BAKERY",
    ///     "expiryDate": "2026-03-15",
    ///     "notes": "Good quality, fresh, bright",
    ///     "imageUrl": "https://supabase.../image.jpg",
    ///     "confidence": 0.92,
    ///     "freshnessScore": 85,
    ///     "qualityGrade": "A"
    ///   }
    /// }
    /// </remarks>
    [HttpPost("analyze")]
    [Authorize]
    [ProducesResponseType(typeof(AnalyzeImageResponse), 200)]
    [ProducesResponseType(400)]
    [ProducesResponseType(401)]
    [ProducesResponseType(500)]
    public async Task<ActionResult<AnalyzeImageResponse>> AnalyzeImage(IFormFile image)
    {
        try
        {
            // Get grocery ID from JWT
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId) || !Guid.TryParse(userId, out var groceryId))
            {
                return Unauthorized(new AnalyzeImageResponse
                {
                    Success = false,
                    Message = "User not authenticated"
                });
            }

            // Validate image file
            if (image == null || image.Length == 0)
            {
                return BadRequest(new AnalyzeImageResponse
                {
                    Success = false,
                    Message = "No image file provided"
                });
            }

            // Check file size
            if (image.Length > MaxFileSize)
            {
                return BadRequest(new AnalyzeImageResponse
                {
                    Success = false,
                    Message = $"Image too large. Maximum size is {MaxFileSize / 1024 / 1024} MB"
                });
            }

            // Check file extension
            var extension = Path.GetExtension(image.FileName).ToLowerInvariant();
            if (!AllowedExtensions.Contains(extension))
            {
                return BadRequest(new AnalyzeImageResponse
                {
                    Success = false,
                    Message = $"Invalid file type. Allowed: {string.Join(", ", AllowedExtensions)}"
                });
            }

            _logger.LogInformation($"🔍 Analyzing image for grocery {groceryId}: {image.FileName}");

            // Analyze image with AI
            var result = await _analysisService.AnalyzeFoodImageAsync(image, groceryId);

            _logger.LogInformation($"✅ Analysis complete: {result.Title} ({result.Category}) - Grade {result.QualityGrade}");

            return Ok(new AnalyzeImageResponse
            {
                Success = true,
                Message = "Image analyzed successfully. Ready to auto-fill listing form.",
                Data = result
            });
        }
        catch (InvalidOperationException ex)
        {
            _logger.LogError(ex, "❌ Image analysis failed");
            return BadRequest(new AnalyzeImageResponse
            {
                Success = false,
                Message = ex.Message
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ Unexpected error during image analysis");
            return StatusCode(500, new AnalyzeImageResponse
            {
                Success = false,
                Message = "An error occurred while analyzing the image. Please try again."
            });
        }
    }

    /// <summary>
    /// Get analysis history for authenticated grocery
    /// </summary>
    /// <remarks>
    /// GET /api/imageanalysis/history?limit=10
    /// 
    /// Returns list of previous AI analyses
    /// </remarks>
    [HttpGet("history")]
    [Authorize]
    [ProducesResponseType(typeof(AnalysisHistoryResponse), 200)]
    [ProducesResponseType(401)]
    public async Task<ActionResult<AnalysisHistoryResponse>> GetHistory([FromQuery] int limit = 10)
    {
        try
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId) || !Guid.TryParse(userId, out var groceryId))
            {
                return Unauthorized(new AnalysisHistoryResponse
                {
                    Success = false,
                    Message = "User not authenticated"
                });
            }

            var analyses = await _analysisService.GetAnalysisHistoryAsync(groceryId, limit);

            return Ok(new AnalysisHistoryResponse
            {
                Success = true,
                Message = "Analysis history retrieved successfully",
                Count = analyses.Count,
                Analyses = analyses
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ Error retrieving analysis history");
            return StatusCode(500, new AnalysisHistoryResponse
            {
                Success = false,
                Message = "An error occurred while retrieving history"
            });
        }
    }

    /// <summary>
    /// Health check for AI service
    /// </summary>
    [HttpGet("health")]
    [AllowAnonymous]
    public IActionResult Health()
    {
        return Ok(new
        {
            status = "healthy",
            service = "Azure Computer Vision AI",
            features = new[]
            {
                "Food category detection (10 categories)",
                "Freshness scoring (0-100)",
                "Quality grading (A/B/C/D)",
                "Expiry date estimation",
                "Auto-fill listing form"
            },
            maxFileSize = $"{MaxFileSize / 1024 / 1024} MB",
            allowedFormats = AllowedExtensions,
            endpoint = "/api/imageanalysis/analyze",
            timestamp = DateTime.UtcNow
        });
    }

    /// <summary>
    /// Save analysis result to database (called after listing created successfully)
    /// </summary>
    [HttpPost("save")]
    [Authorize]
    [ProducesResponseType(200)]
    [ProducesResponseType(400)]
    [ProducesResponseType(401)]
    public async Task<IActionResult> SaveAnalysis([FromBody] FoodAnalysisData analysisData)
    {
        try
        {
            // Get grocery ID from JWT
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrEmpty(userId) || !Guid.TryParse(userId, out var groceryId))
            {
                return Unauthorized(new { success = false, message = "User not authenticated" });
            }

            // Validate data
            if (analysisData == null || string.IsNullOrEmpty(analysisData.ImageUrl))
            {
                return BadRequest(new { success = false, message = "Invalid analysis data" });
            }

            _logger.LogInformation($"💾 Saving AI analysis for grocery {groceryId}: {analysisData.Title}");

            // Save to database
            await _analysisService.SaveAnalysisAsync(analysisData, groceryId);

            return Ok(new
            {
                success = true,
                message = "Analysis saved successfully"
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "❌ Error saving analysis");
            return StatusCode(500, new
            {
                success = false,
                message = "Failed to save analysis"
            });
        }
    }

    /// <summary>
/// Upload food image to Supabase storage (called before creating listing)
/// </summary>
[HttpPost("upload")]
[Authorize]
[ProducesResponseType(typeof(UploadImageResponse), 200)]
[ProducesResponseType(400)]
[ProducesResponseType(401)]
public async Task<ActionResult<UploadImageResponse>> UploadImage(IFormFile image)
{
    try
    {
        var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId) || !Guid.TryParse(userId, out var groceryId))
        {
            return Unauthorized(new UploadImageResponse
            {
                Success = false,
                Message = "User not authenticated"
            });
        }

        if (image == null || image.Length == 0)
        {
            return BadRequest(new UploadImageResponse
            {
                Success = false,
                Message = "No image file provided"
            });
        }

        // Check file size
        if (image.Length > MaxFileSize)
        {
            return BadRequest(new UploadImageResponse
            {
                Success = false,
                Message = $"Image too large. Maximum size is {MaxFileSize / 1024 / 1024} MB"
            });
        }

        _logger.LogInformation($"📤 Uploading image for grocery {groceryId}");

        using var stream = image.OpenReadStream();
        var imageUrl = await _storageService.UploadFoodImageAsync(stream, image.FileName, groceryId);

        return Ok(new UploadImageResponse
        {
            Success = true,
            Message = "Image uploaded successfully",
            ImageUrl = imageUrl
        });
    }
    catch (Exception ex)
    {
        _logger.LogError(ex, "❌ Image upload failed");
        return StatusCode(500, new UploadImageResponse
        {
            Success = false,
            Message = "Failed to upload image"
        });
    }
}
}