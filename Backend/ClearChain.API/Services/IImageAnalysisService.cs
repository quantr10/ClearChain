using ClearChain.API.DTOs.ImageAnalysis;

namespace ClearChain.API.Services;

public interface IImageAnalysisService
{
    /// <summary>
    /// Analyzes a food image and returns auto-fill data; nothing is stored. Title and notes are
    /// written in <paramref name="language"/> ("en" or "vi").
    /// </summary>
    Task<FoodAnalysisData> AnalyzeFoodImageAsync(IFormFile image, Guid groceryId, string language);

}
