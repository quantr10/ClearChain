using ClearChain.API.DTOs.ImageAnalysis;

namespace ClearChain.API.Services;

public interface IImageAnalysisService
{
    /// <summary>
    /// Analyze a food image and return auto-fill data (NO DB SAVE). Title and notes are
    /// written in <paramref name="language"/> ("en" or "vi").
    /// </summary>
    Task<FoodAnalysisData> AnalyzeFoodImageAsync(IFormFile image, Guid groceryId, string language);

}
