// ═══════════════════════════════════════════════════════════════════════════════
// Color.kt — Unified semantic color palette
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand Colors ────────────────────────────────────────────────────────────
val BrandGreen      = Color(0xFF16A34A)
val BrandGreenLight = Color(0xFFDCFCE7)
val BrandGreenDark  = Color(0xFF166534)

val BrandTeal       = Color(0xFF0D9488)
val BrandTealLight  = Color(0xFFF0FDFA)
val BrandTealDark   = Color(0xFF0F766E)

// ── Status Colors ───────────────────────────────────────────────────────────
object StatusColors {
    // Available / Open / Active
    val Available           = Color(0xFF16A34A)
    val AvailableBg         = Color(0xFFDCFCE7)
    val AvailableOnBg       = Color(0xFF166534)

    // Pending
    val Pending             = Color(0xFFF59E0B)
    val PendingBg           = Color(0xFFFEF3C7)
    val PendingOnBg         = Color(0xFF92400E)

    // Approved
    val Approved            = Color(0xFF3B82F6)
    val ApprovedBg          = Color(0xFFDBEAFE)
    val ApprovedOnBg        = Color(0xFF1E40AF)

    // Ready
    val Ready               = Color(0xFF8B5CF6)
    val ReadyBg             = Color(0xFFEDE9FE)
    val ReadyOnBg           = Color(0xFF5B21B6)

    // Completed
    val Completed           = Color(0xFF0D9488)
    val CompletedBg         = Color(0xFFCCFBF1)
    val CompletedOnBg       = Color(0xFF115E59)

    // Rejected / Error
    val Rejected            = Color(0xFFEF4444)
    val RejectedBg          = Color(0xFFFEE2E2)
    val RejectedOnBg        = Color(0xFF991B1B)

    // Reserved
    val Reserved            = Color(0xFFF97316)
    val ReservedBg          = Color(0xFFFFF7ED)
    val ReservedOnBg        = Color(0xFF9A3412)

    // Expired
    val Expired             = Color(0xFF6B7280)
    val ExpiredBg           = Color(0xFFF3F4F6)
    val ExpiredOnBg         = Color(0xFF374151)

    // Distributed
    val Distributed         = Color(0xFF06B6D4)
    val DistributedBg       = Color(0xFFCFFAFE)
    val DistributedOnBg     = Color(0xFF155E75)
}

// ── Category Colors ─────────────────────────────────────────────────────────
object CategoryColors {
    val Fruits     = Color(0xFFEF4444)
    val Vegetables = Color(0xFF22C55E)
    val Dairy      = Color(0xFF3B82F6)
    val Bakery     = Color(0xFFF59E0B)
    val Meat       = Color(0xFFDC2626)
    val Seafood    = Color(0xFF06B6D4)
    val Packaged   = Color(0xFF8B5CF6)
    val Beverages  = Color(0xFFF97316)
    val Other      = Color(0xFF6B7280)
}

// ── Neutral Colors ──────────────────────────────────────────────────────────
val Gray50  = Color(0xFFF9FAFB)
val Gray100 = Color(0xFFF3F4F6)
val Gray200 = Color(0xFFE5E7EB)
val Gray300 = Color(0xFFD1D5DB)
val Gray400 = Color(0xFF9CA3AF)
val Gray500 = Color(0xFF6B7280)
val Gray600 = Color(0xFF4B5563)
val Gray700 = Color(0xFF374151)
val Gray800 = Color(0xFF1F2937)
val Gray900 = Color(0xFF111827)

// ── Light Theme Palette ─────────────────────────────────────────────────────
val LightPrimary            = BrandTeal
val LightOnPrimary          = Color.White
val LightPrimaryContainer   = BrandTealLight
val LightOnPrimaryContainer = BrandTealDark
val LightSecondary          = Color(0xFF3B82F6)
val LightTertiary           = Color(0xFF8B5CF6)
val LightBackground         = Color(0xFFFAFAFA)
val LightSurface            = Color.White
val LightSurfaceVariant     = Gray100
val LightOnBackground       = Gray900
val LightOnSurface          = Gray900
val LightOnSurfaceVariant   = Gray500
val LightError              = Color(0xFFDC2626)
val LightErrorContainer     = Color(0xFFFEE2E2)
val LightOnErrorContainer   = Color(0xFF991B1B)

// ── Dark Theme Palette ──────────────────────────────────────────────────────
val DarkPrimary             = Color(0xFF2DD4BF)
val DarkOnPrimary           = Color(0xFF003733)
val DarkPrimaryContainer    = Color(0xFF0F766E)
val DarkOnPrimaryContainer  = Color(0xFFA7F3D0)
val DarkSecondary           = Color(0xFF60A5FA)
val DarkTertiary            = Color(0xFFA78BFA)
val DarkBackground          = Color(0xFF0F172A)
val DarkSurface             = Color(0xFF1E293B)
val DarkSurfaceVariant      = Color(0xFF334155)
val DarkOnBackground        = Color(0xFFF1F5F9)
val DarkOnSurface           = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant    = Color(0xFF94A3B8)
val DarkError               = Color(0xFFFCA5A5)
val DarkErrorContainer      = Color(0xFF7F1D1D)
val DarkOnErrorContainer    = Color(0xFFFECACA)