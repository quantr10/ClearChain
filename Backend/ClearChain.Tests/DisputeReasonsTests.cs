using ClearChain.API.DTOs.Disputes;

namespace ClearChain.Tests;

public class DisputeReasonsTests
{
    [Theory]
    [InlineData("poor_condition")]
    [InlineData("wrong_items")]
    [InlineData("quantity_mismatch")]
    [InlineData("expired")]
    [InlineData("not_available")]
    [InlineData("other")]
    public void Accepts_every_ngo_reason_key(string key)
    {
        Assert.True(DisputeReasons.IsValidNgoReason(key));
    }

    [Theory]
    [InlineData("")]
    [InlineData("Poor food condition")] // a label, not a key
    [InlineData("no_show")]             // a grocery-only reason; NGOs cannot file it
    public void Rejects_values_that_are_not_ngo_keys(string key)
    {
        Assert.False(DisputeReasons.IsValidNgoReason(key));
    }

    [Fact]
    public void Rejects_null()
    {
        Assert.False(DisputeReasons.IsValidNgoReason(null));
    }

    [Fact]
    public void Label_maps_a_key_to_its_english_text()
    {
        Assert.Equal("Quantity mismatch", DisputeReasons.Label("quantity_mismatch"));
    }

    [Fact]
    public void Label_returns_unknown_stored_values_unchanged()
    {
        Assert.Equal("Legacy free text", DisputeReasons.Label("Legacy free text"));
    }
}
