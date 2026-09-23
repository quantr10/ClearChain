namespace ClearChain.API.Services;

public interface IStorageService
{
    Task<string> UploadPickupProofAsync(Stream fileStream, string fileName, string contentType);
    Task<string> UploadFoodImageAsync(Stream fileStream, string fileName, Guid groceryId, string contentType);
    Task<string> UploadFileAsync(Stream fileStream, string fileName, string contentType, string bucket);
}

public class SupabaseStorageService : IStorageService
{
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly IConfiguration _configuration;
    private readonly ILogger<SupabaseStorageService> _logger;
    private readonly string _supabaseUrl;
    private readonly string _supabaseKey;
    private const string PICKUP_PROOFS_BUCKET = "pickup-proofs";
    private const string FOOD_IMAGES_BUCKET = "food-images";

    public SupabaseStorageService(
        IHttpClientFactory httpClientFactory,
        IConfiguration configuration,
        ILogger<SupabaseStorageService> logger)
    {
        _httpClientFactory = httpClientFactory;
        _configuration = configuration;
        _logger = logger;

        _supabaseUrl = _configuration["SUPABASE_URL"]
            ?? throw new InvalidOperationException("SUPABASE_URL not configured");
        _supabaseKey = _configuration["SUPABASE_SERVICE_KEY"]
            ?? throw new InvalidOperationException("SUPABASE_SERVICE_KEY not configured");
    }

    public async Task<string> UploadPickupProofAsync(Stream fileStream, string fileName, string contentType)
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

            var httpClient = _httpClientFactory.CreateClient();
            httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {_supabaseKey}");
            httpClient.DefaultRequestHeaders.Add("apikey", _supabaseKey);

            var uploadUrl = $"{_supabaseUrl}/storage/v1/object/{PICKUP_PROOFS_BUCKET}/{uniqueFileName}";

            using var content = new ByteArrayContent(fileBytes);
            content.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue(
                string.IsNullOrEmpty(contentType) ? "application/octet-stream" : contentType);

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
    public async Task<string> UploadFoodImageAsync(Stream fileStream, string fileName, Guid groceryId, string contentType)
    {
        try
        {
            // Generate unique filename with grocery ID for organization
            var timestamp = DateTime.UtcNow.ToString("yyyyMMddHHmmss");
            var extension = Path.GetExtension(fileName);
            var uniqueFileName = $"{groceryId}/{timestamp}_{Guid.NewGuid()}{extension}";

            // Convert stream to byte array
            using var memoryStream = new MemoryStream();
            await fileStream.CopyToAsync(memoryStream);
            var fileBytes = memoryStream.ToArray();

            _logger.LogInformation($"Uploading food image: {uniqueFileName}, Size: {fileBytes.Length} bytes");

            var httpClient = _httpClientFactory.CreateClient();
            httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {_supabaseKey}");
            httpClient.DefaultRequestHeaders.Add("apikey", _supabaseKey);

            var uploadUrl = $"{_supabaseUrl}/storage/v1/object/{FOOD_IMAGES_BUCKET}/{uniqueFileName}";

            using var content = new ByteArrayContent(fileBytes);
            content.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue(
                string.IsNullOrEmpty(contentType) ? "application/octet-stream" : contentType);

            var response = await httpClient.PostAsync(uploadUrl, content);

            if (!response.IsSuccessStatusCode)
            {
                var errorContent = await response.Content.ReadAsStringAsync();
                _logger.LogError($"Supabase upload failed: {errorContent}");
                throw new Exception($"Upload failed: {errorContent}");
            }

            // Get public URL
            var publicUrl = $"{_supabaseUrl}/storage/v1/object/public/{FOOD_IMAGES_BUCKET}/{uniqueFileName}";

            _logger.LogInformation($"Food image upload successful. URL: {publicUrl}");
            return publicUrl;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, $"Error uploading food image: {fileName}");
            throw new Exception($"Failed to upload food image: {ex.Message}", ex);
        }
    }
    public async Task<string> UploadFileAsync(Stream fileStream, string fileName, string contentType, string bucket)
    {
        try
        {
            var timestamp = DateTime.UtcNow.ToString("yyyyMMddHHmmss");
            var extension = Path.GetExtension(fileName);
            var uniqueFileName = $"{timestamp}_{Guid.NewGuid()}{extension}";

            using var memoryStream = new MemoryStream();
            await fileStream.CopyToAsync(memoryStream);
            var fileBytes = memoryStream.ToArray();

            var httpClient = _httpClientFactory.CreateClient();
            httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {_supabaseKey}");
            httpClient.DefaultRequestHeaders.Add("apikey", _supabaseKey);

            var uploadUrl = $"{_supabaseUrl}/storage/v1/object/{bucket}/{uniqueFileName}";

            using var content = new ByteArrayContent(fileBytes);
            content.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue(
                string.IsNullOrEmpty(contentType) ? "application/octet-stream" : contentType);

            var response = await httpClient.PostAsync(uploadUrl, content);

            if (!response.IsSuccessStatusCode)
            {
                var errorContent = await response.Content.ReadAsStringAsync();
                throw new Exception($"Upload failed: {errorContent}");
            }

            return $"{_supabaseUrl}/storage/v1/object/public/{bucket}/{uniqueFileName}";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error uploading file {FileName} to bucket {Bucket}", fileName, bucket);
            throw new Exception($"Failed to upload file: {ex.Message}", ex);
        }
    }
}
