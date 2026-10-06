package com.clearchain.app.presentation.dispute

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.clearchain.app.R
import com.clearchain.app.presentation.components.StatusBadge
import com.clearchain.app.ui.theme.StatusColors

/**
 * The reasons an NGO can pick when opening a dispute. [key] is what the backend stores (see
 * DisputeReasons on the server); [labelRes] is what the user reads.
 */
enum class NgoDisputeReason(val key: String, @StringRes val labelRes: Int) {
    POOR_CONDITION("poor_condition", R.string.dispute_reason_poor_condition),
    WRONG_ITEMS("wrong_items", R.string.dispute_reason_wrong_items),
    QUANTITY_MISMATCH("quantity_mismatch", R.string.dispute_reason_quantity),
    EXPIRED("expired", R.string.dispute_reason_expired),
    NOT_AVAILABLE("not_available", R.string.dispute_reason_not_available),
    OTHER("other", R.string.dispute_reason_other);

    companion object {
        /** Null for a stored value that isn't a known key, e.g. text saved before keys existed. */
        fun fromKey(key: String): NgoDisputeReason? = entries.find { it.key == key }
    }
}

private data class DisputeBadgeStyle(val bg: Color, val onBg: Color, @StringRes val labelRes: Int)

/** Open and under-review read as pending, dismissed as expired, and any resolution as available. */
@Composable
fun DisputeStatusBadge(status: String) {
    val style = when (status) {
        "open" -> DisputeBadgeStyle(StatusColors.PendingBg, StatusColors.PendingOnBg, R.string.dispute_status_open)
        "under_review" -> DisputeBadgeStyle(StatusColors.ReservedBg, StatusColors.ReservedOnBg, R.string.dispute_status_under_review)
        "dismissed" -> DisputeBadgeStyle(StatusColors.ExpiredBg, StatusColors.ExpiredOnBg, R.string.dispute_status_dismissed)
        else -> DisputeBadgeStyle(StatusColors.AvailableBg, StatusColors.AvailableOnBg, R.string.dispute_status_resolved)
    }
    StatusBadge(stringResource(style.labelRes), style.bg, style.onBg)
}
