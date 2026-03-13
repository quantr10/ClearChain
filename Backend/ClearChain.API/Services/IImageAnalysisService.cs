using ClearChain.API.DTOs.ImageAnalysis;

namespace ClearChain.API.Services;

public interface IImageAnalysisService
{
    /// <summary>
    /// Analyze a food image and return auto-fill data (NO DB SAVE)
    /// </summary>
    Task<FoodAnalysisData> AnalyzeFoodImageAsync(IFormFile image, Guid groceryId);

    /// <summary>
    /// Save analysis to database AFTER listing created successfully
    /// </summary>
    Task SaveAnalysisAsync(FoodAnalysisData data, Guid groceryId);

    /// <summary>
    /// Get analysis history for a grocery
    /// </summary>
    Task<List<FoodAnalysisData>> GetAnalysisHistoryAsync(Guid groceryId, int limit = 10);
}