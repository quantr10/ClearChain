namespace ClearChain.API.Common;

/// <summary>
/// File-size and MIME-type limits mirrored from each Supabase Storage bucket's own
/// configuration (Dashboard → Storage → Buckets: avatars, documents, disputes,
/// inventory-photos, pickup-proofs, food-images). Validating against these here first
/// gives a clean 400 with a useful message instead of surfacing Supabase's raw
/// "NoSuchBucket"/storage-policy error, and never rejects something the bucket itself
/// would accept. If a bucket's settings change in Supabase, update this file to match.
/// </summary>
public static class StorageBucketPolicy
{
    private static readonly string[] ImageTypes =
    {
        "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"
    };

    // avatars, disputes, inventory-photos, pickup-proofs, food-images — all 5 MB, images only
    public const long ImageMaxBytes = 5 * 1024 * 1024;
    public static readonly string[] ImageMimeTypes = ImageTypes;

    // documents — 10 MB, images + PDF + Word docs
    public const long DocumentMaxBytes = 10 * 1024 * 1024;
    public static readonly string[] DocumentMimeTypes = ImageTypes
        .Concat(new[]
        {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        })
        .ToArray();

    public static bool IsAllowedImage(string? contentType) =>
        contentType != null && ImageMimeTypes.Contains(contentType.ToLowerInvariant());

    public static bool IsAllowedDocument(string? contentType) =>
        contentType != null && DocumentMimeTypes.Contains(contentType.ToLowerInvariant());

    public static string ImageTypesMessage => "JPEG, PNG, WebP, HEIC, or HEIF images";
    public static string DocumentTypesMessage => "PDF, Word documents, or images (JPEG, PNG, WebP, HEIC, HEIF)";
}
