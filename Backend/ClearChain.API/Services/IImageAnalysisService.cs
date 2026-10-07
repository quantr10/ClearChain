using ClearChain.API.DTOs.ImageAnalysis;

namespace ClearChain.API.Services;

public interface IImageAnalysisService
{
    /// <summary>
    /// Analyze a food image and return auto-fill data (NO DB SAVE)
    /// </summary>
    Task<FoodAnalysisData> AnalyzeFoodImageAsync(IFormFile image, Guid groceryId);

}
