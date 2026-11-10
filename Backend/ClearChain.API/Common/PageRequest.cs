namespace ClearChain.API.Common;

/// <summary>A page a caller asked for, clamped to what the endpoint serves.</summary>
public readonly record struct PageRequest(int Page, int Size)
{
    public static PageRequest Clamp(int page, int pageSize, int maxSize) =>
        new(Math.Max(1, page), Math.Clamp(pageSize, 1, maxSize));

    /// <summary>
    /// Rows before this page. A long, so a page far past the end comes back empty instead of
    /// overflowing into a negative offset.
    /// </summary>
    public long Skip => (long)(Page - 1) * Size;

    /// <summary><see cref="Skip"/> capped at <paramref name="total"/>, for the int that Queryable.Skip takes.</summary>
    public int SkipWithin(int total) => (int)Math.Min(Skip, total);

    public int TotalPages(int total) => (int)Math.Ceiling((double)total / Size);
}
