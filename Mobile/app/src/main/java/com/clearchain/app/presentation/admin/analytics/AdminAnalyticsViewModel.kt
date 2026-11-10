package com.clearchain.app.presentation.admin.analytics

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.AdminDetailedStatsData
import com.clearchain.app.data.remote.dto.StatsLeader
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.presentation.dispute.NgoDisputeReason
import com.clearchain.app.util.DownloadsExport
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AdminAnalyticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val adminApi: AdminApi,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(
        AdminAnalyticsState(
            focusedSection = AnalyticsSection.fromKey(savedStateHandle[SECTION_ARG])
        )
    )
    val state: StateFlow<AdminAnalyticsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        load(isRefresh = false)
        observeLiveUpdates()
    }

    /**
     * Anything that moves food, membership or trust changes the figures, so the screen
     * follows along.
     */
    private fun observeLiveUpdates() {
        viewModelScope.launch {
            merge(
                signalRService.pickupRequestCreated,
                signalRService.transactionCompleted,
                signalRService.listingCreated,
                signalRService.listingDeleted,
                signalRService.pickupRequestCancelled,
                signalRService.newOrganizationRegistered,
                signalRService.newDisputeCreated
            ).collect { load(isRefresh = false, silent = true) }
        }
    }

    fun onEvent(event: AdminAnalyticsEvent) {
        when (event) {
            AdminAnalyticsEvent.Load -> load(isRefresh = false)
            AdminAnalyticsEvent.Refresh -> load(isRefresh = true)
            AdminAnalyticsEvent.ExportPdf -> exportPdf()
            AdminAnalyticsEvent.FocusConsumed -> _state.update { it.copy(focusedSection = null) }
        }
    }

    /** Always the whole history: the screen has no period to pick. */
    private fun load(isRefresh: Boolean, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _state.update {
                    it.copy(
                        // Only a cold start gets the spinner; a reload keeps the figures on screen.
                        isLoading = !isRefresh && it.data == null,
                        isRefreshing = isRefresh,
                        error = null
                    )
                }
            }

            try {
                val loaded = adminApi.getDetailedStatistics().data

                _state.update {
                    it.copy(
                        data = loaded ?: it.data,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                if (isRefresh) {
                    _uiEvent.send(UiEvent.ShowSnackbar(string(R.string.snack_stats_refreshed)))
                }
            } catch (e: Exception) {
                val msg = e.message ?: string(R.string.error_load_statistics)
                if (_state.value.data == null) {
                    _state.update { it.copy(error = msg, isLoading = false, isRefreshing = false) }
                } else {
                    _state.update { it.copy(isLoading = false, isRefreshing = false) }
                    if (!silent) _uiEvent.send(UiEvent.ShowSnackbar(msg))
                }
            }
        }
    }

    private fun string(resId: Int, vararg args: Any): String =
        context.getString(resId, *args)

    // ── PDF export ───────────────────────────────────────────────────────────

    private fun exportPdf() {
        val data = _state.value.data ?: run {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(string(R.string.snack_no_data_export))) }
            return
        }
        if (_state.value.isExporting) return

        _state.update { it.copy(isExporting = true) }

        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                runCatching { writePdf(data) }.getOrNull()
            }
            _state.update { it.copy(isExporting = false) }

            if (uri == null) {
                _uiEvent.send(UiEvent.ShowSnackbar(string(R.string.snack_pdf_failed)))
                return@launch
            }

            runCatching {
                DownloadsExport.share(context, uri, "application/pdf", string(R.string.pdf_stats_title))
            }
            _uiEvent.send(UiEvent.ShowSnackbar(string(R.string.snack_pdf_saved)))
        }
    }

    /**
     * Writes the report the screen shows, in the same order, breaking to a new page when
     * a section would run off the bottom.
     */
    private fun writePdf(data: AdminDetailedStatsData): Uri? {
        val doc = PdfDocument()

        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
        }
        val headingPaint = Paint().apply {
            textSize = 13f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply { textSize = 11f }
        val mutedPaint = Paint().apply {
            textSize = 9f
            color = android.graphics.Color.GRAY
        }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
        var canvas = page.canvas
        var y = 52f

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            canvas = page.canvas
            y = 52f
        }

        fun heading(text: String) {
            if (y > PAGE_H - 120) newPage()
            y += 14f
            canvas.drawText(text, MARGIN, y, headingPaint)
            y += 16f
        }

        /** Cuts [text] with an ellipsis so a long name can't run into the value column. */
        fun fit(text: String, width: Float): String {
            if (bodyPaint.measureText(text) <= width) return text
            val kept = bodyPaint.breakText(text, true, width - bodyPaint.measureText("…"), null)
            return text.take(kept) + "…"
        }

        fun line(label: String, value: String) {
            if (y > PAGE_H - 60) newPage()
            canvas.drawText(fit(label, VALUE_X - MARGIN - 8f), MARGIN, y, bodyPaint)
            canvas.drawText(value, VALUE_X, y, bodyPaint)
            y += 15f
        }

        fun note(text: String) {
            if (y > PAGE_H - 60) newPage()
            canvas.drawText(text, MARGIN, y, mutedPaint)
            y += 13f
        }

        fun pct(value: Double?) = value?.let { "${(it * 100).roundToInt()}%" } ?: "-"
        fun rating(value: Double?) = value?.let { formatRating(it) } ?: "-"

        canvas.drawText(string(R.string.pdf_stats_title), MARGIN, y, titlePaint)
        y += 22f
        note(
            string(
                R.string.pdf_stats_generated,
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                string(R.string.preset_all_time)
            )
        )

        // ── Request status ──
        val f = data.funnel
        heading(string(R.string.section_request_status_breakdown))
        line(string(R.string.status_pending), "${f.pending}")
        line(string(R.string.status_approved), "${f.approved}")
        line(string(R.string.status_ready), "${f.ready}")
        line(string(R.string.status_completed), "${f.completed}")
        line(string(R.string.status_cancelled), "${f.cancelled}")
        line(string(R.string.status_rejected), "${f.rejected}")

        // ── Leaderboards ──
        val unknown = string(R.string.label_unknown_item)
        listOf(
            string(R.string.org_type_groceries) to data.leaderboards.groceries,
            string(R.string.org_type_ngos) to data.leaderboards.ngos
        ).forEach { (side, board) ->
            listOf<Triple<Int, List<StatsLeader>, (StatsLeader) -> String>>(
                Triple(R.string.leaderboard_metric_pickups, board.byPickups, { l -> "${l.completedPickups}" }),
                Triple(R.string.leaderboard_metric_rating, board.byRating, { l -> "${rating(l.averageRating)} (${l.ratingCount})" })
            ).forEach { (metricRes, leaders, value) ->
                if (leaders.isEmpty()) return@forEach
                heading(string(R.string.pdf_leaderboard_heading, side, string(metricRes)))
                leaders.forEach { l -> line("${l.rank}. ${l.name.ifBlank { unknown }}", value(l)) }
            }
        }
        note(string(R.string.leaderboard_rating_note, data.leaderboards.minRatingsToRank))

        // ── Disputes ──
        val q = data.quality
        heading(string(R.string.section_quality))
        line(string(R.string.quality_disputes), "${q.disputesOpened}")
        line(string(R.string.pdf_dispute_rate), pct(q.disputeRate))
        q.disputeReasons.forEach { reason ->
            val label = NgoDisputeReason.fromKey(reason.key)?.let { string(it.labelRes) } ?: reason.key
            line("  $label", "${reason.count}")
        }
        if (q.disputesOpened > 0) {
            val outcomes = q.disputeOutcomes
            line(string(R.string.dispute_status_open), "${outcomes.open}")
            line(string(R.string.dispute_outcome_ngo), "${outcomes.resolvedForNgo}")
            line(string(R.string.dispute_outcome_grocery), "${outcomes.resolvedForGrocery}")
            line(string(R.string.dispute_status_dismissed), "${outcomes.dismissed}")
        }
        if (q.disputesResolved > 0) {
            note(string(R.string.quality_resolution_time, formatDuration(q.medianHoursToResolve), q.disputesResolved))
        }

        // ── Organizations ──
        val o = data.organizations
        heading(string(R.string.section_organizations))
        line(string(R.string.org_type_groceries), "${o.groceries}")
        line(string(R.string.org_type_ngos), "${o.ngos}")
        line(string(R.string.stat_verified), "${o.verified}")
        line(string(R.string.stat_awaiting_verification), "${o.pendingVerification}")

        doc.finishPage(page)

        val fileName = "clearchain_stats_${DownloadsExport.timestamp()}.pdf"
        val uri = DownloadsExport.save(context, fileName, "application/pdf") { doc.writeTo(it) }
        doc.close()
        return uri
    }

    private companion object {
        const val SECTION_ARG = "section"
        const val PAGE_W = 595 // A4 at 72dpi
        const val PAGE_H = 842
        const val MARGIN = 40f
        const val VALUE_X = 360f
    }
}
