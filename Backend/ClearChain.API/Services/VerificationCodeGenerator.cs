using System.Security.Cryptography;

namespace ClearChain.API.Services;

/// <summary>
/// Cryptographically random 6-digit codes for email verification. Random.Shared is
/// predictable enough (a seeded PRNG) that it shouldn't gate account access, even for a
/// short-lived code.
/// </summary>
internal static class VerificationCodeGenerator
{
    public static string Generate() => (100000 + RandomNumberGenerator.GetInt32(900000)).ToString();
}
