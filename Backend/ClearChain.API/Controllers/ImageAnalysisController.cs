using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using ClearChain.API.Common;
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
    private readonly IStorageService _storageService;
    private readonly ILogger<ImageAnalysisController> _logger;

    // Formats accepted by /analyze specifically — this is what's sent to the Azure OpenAI
    // vision model, which reads JPEG, PNG, WEBP and non-animated GIF. It's declared
    // independently from StorageBucketPolicy (used by /upload below) so the model's formats
    // and the "food-images" bucket's config can each change on their own.
    private static readonly string[] AnalyzeAllowedMimeTypes =
        { "image/jpeg", "image/png", "image/webp", "image/gif" };
    // The app compresses photos well below this; the cap keeps request size and image
    // token cost bounded for anything else.
    private const long AnalyzeMaxFileSize = 4 * 1024 * 1024; // 4 MB

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
    /// POST /api/imageanalysis/analyze (multipart: image, optional language "en" or "vi")
    ///
    /// The photo goes to a vision model on Azure OpenAI and nothing is stored: the image is
    /// uploaded separately through /upload when the listing is created. Title, description and
    /// notes come back in the requested language.
    ///
    /// Example Response:
    /// {
    ///   "success": true,
    ///   "message": "Image analyzed successfully. Ready to auto-fill listing form.",
    ///   "data": {
    ///     "title": "Whole wheat bread",
    ///     "description": "Sliced whole wheat sandwich bread, 500 g loaf. Soft and hearty, ideal for toast and sandwiches.",
    ///     "category": "BAKERY",
    ///     "expiryDate": "2026-03-15",
    ///     "notes": "Fresh loaf with an intact crust. Store in an airtight bag at room temperature.",
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
    public async Task<ActionResult<AnalyzeImageResponse>> AnalyzeImage(
        IFormFile image, [FromForm] string? language)
    {
        try
        {
            // Get grocery ID from JWT
            if (!this.TryGetUserId(out var groceryId))
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
            if (image.Length > AnalyzeMaxFileSize)
            {
                return BadRequest(new AnalyzeImageResponse
                {
                    Success = false,
                    Message = $"Image too large. Maximum size is {AnalyzeMaxFileSize / 1024 / 1024} MB"
                });
            }

            // Check content type (not file extension — the extension can lie about what's
            // actually inside the file; the browser/OS-reported MIME type is what matters).
            if (!AnalyzeAllowedMimeTypes.Contains(image.ContentType?.ToLowerInvariant()))
            {
                return BadRequest(new AnalyzeImageResponse
                {
                    Success = false,
                    Message = $"Invalid file type. Allowed: {string.Join(", ", AnalyzeAllowedMimeTypes)}"
                });
            }

            _logger.LogInformation($"🔍 Analyzing image for grocery {groceryId}: {image.FileName}");

            // Analyze image with AI
            // Anything other than Vietnamese falls back to English, the app's default language.
            var outputLanguage = string.Equals(language, "vi", StringComparison.OrdinalIgnoreCase) ? "vi" : "en";
            var result = await _analysisService.AnalyzeFoodImageAsync(image, groceryId, outputLanguage);

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
        if (!this.TryGetUserId(out var groceryId))
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

        // Stored straight to the "food-images" Supabase bucket (unlike /analyze above,
        // which only calls Azure and is bound by Azure's own 4 MB / format limits) —
        // validate against that bucket's actual configured limits.
        if (!StorageBucketPolicy.IsAllowedImage(image.ContentType))
        {
            return BadRequest(new UploadImageResponse
            {
                Success = false,
                Message = $"Only {StorageBucketPolicy.ImageTypesMessage} are accepted"
            });
        }

        if (image.Length > StorageBucketPolicy.ImageMaxBytes)
        {
            return BadRequest(new UploadImageResponse
            {
                Success = false,
                Message = $"Image too large. Maximum size is {StorageBucketPolicy.ImageMaxBytes / 1024 / 1024} MB"
            });
        }

        _logger.LogInformation($"📤 Uploading image for grocery {groceryId}");

        using var stream = image.OpenReadStream();
        var imageUrl = await _storageService.UploadFoodImageAsync(stream, image.FileName, groceryId, image.ContentType);

        return Ok(new UploadImageResponse
        {
            Success = true,
            Message = "Image uploaded successfully",
            ImageUrl = imageUrl
        });
    }
}
