using Supabase.Storage;

namespace ClearChain.API.Services;

public interface IStorageService
{
    Task<string> UploadPickupProofAsync(Stream fileStream, string fileName);
    Task<bool> DeleteFileAsync(string fileUrl);
}

public class SupabaseStorageService : IStorageService
{
    private readonly IConfiguration _configuration;
    private readonly ILogger<SupabaseStorageService> _logger;
    private readonly string _supabaseUrl;
    private readonly string _supabaseKey;
    private const string PICKUP_PROOFS_BUCKET = "pickup-proofs";

    public SupabaseStorageService(
        IConfiguration configuration,
        ILogger<SupabaseStorageService> logger)
    {
        _configuration = configuration;
        _logger = logger;

        _supabaseUrl = _configuration["SUPABASE_URL"] 
            ?? throw new InvalidOperationException("SUPABASE_URL not configured");
        _supabaseKey = _configuration["SUPABASE_SERVICE_KEY"] 
            ?? throw new InvalidOperationException("SUPABASE_SERVICE_KEY not configured");
    }

    public async Task<string> UploadPickupProofAsync(Stream fileStream, string fileName)
    {
        try
        {
            // Generate unique filename
            var timestamp = DateTime.UtcNow.ToString("yyyyMMddHHmmss");
            var extension = Path.GetExtension(fileName);
            var uniqueFileName = $"{timestamp}_{Guid.NewGuid()}{extension}";

            // Convert stream to byte array
            using var memoryStream = new MemoryStream();
            await fileStream.CopyToAsync(memoryStream);
            var fileBytes = memoryStream.ToArray();

            _logger.LogInformation($"Uploading file: {uniqueFileName}, Size: {fileBytes.Length} bytes");

            // ✅ FIX: Use correct Supabase Storage upload method
            using var httpClient = new HttpClient();
            httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {_supabaseKey}");
            httpClient.DefaultRequestHeaders.Add("apikey", _supabaseKey);

            var uploadUrl = $"{_supabaseUrl}/storage/v1/object/{PICKUP_PROOFS_BUCKET}/{uniqueFileName}";
            
            using var content = new ByteArrayContent(fileBytes);
            content.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("image/jpeg");
            
            var response = await httpClient.PostAsync(uploadUrl, content);
            
            if (!response.IsSuccessStatusCode)
            {
                var errorContent = await response.Content.ReadAsStringAsync();
                _logger.LogError($"Supabase upload failed: {errorContent}");
                throw new Exception($"Upload failed: {errorContent}");
            }

            // Get public URL
            var publicUrl = $"{_supabaseUrl}/storage/v1/object/public/{PICKUP_PROOFS_BUCKET}/{uniqueFileName}";

            _logger.LogInformation($"Upload successful. URL: {publicUrl}");
            return publicUrl;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, $"Error uploading pickup proof: {fileName}");
            throw new Exception($"Failed to upload photo: {ex.Message}", ex);
        }
    }

    public async Task<bool> DeleteFileAsync(string fileUrl)
    {
        try
        {
            // Extract filename from URL
            var uri = new Uri(fileUrl);
            var pathSegments = uri.AbsolutePath.Split('/');
            var fileName = pathSegments[^1]; // Last segment

            _logger.LogInformation($"Deleting file: {fileName}");

            using var httpClient = new HttpClient();
            httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {_supabaseKey}");
            httpClient.DefaultRequestHeaders.Add("apikey", _supabaseKey);

            var deleteUrl = $"{_supabaseUrl}/storage/v1/object/{PICKUP_PROOFS_BUCKET}/{fileName}";
            
            var response = await httpClient.DeleteAsync(deleteUrl);

            if (!response.IsSuccessStatusCode)
            {
                var errorContent = await response.Content.ReadAsStringAsync();
                _logger.LogError($"Supabase delete failed: {errorContent}");
                return false;
            }

            _logger.LogInformation($"File deleted successfully: {fileName}");
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, $"Error deleting file from URL: {fileUrl}");
            return false;
        }
    }
}