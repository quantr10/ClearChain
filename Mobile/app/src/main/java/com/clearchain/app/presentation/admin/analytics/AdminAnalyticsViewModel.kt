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
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.util.DownloadsExport
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.time.LocalDate
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
        loadActiveDates()
        observeLiveUpdates()
    }

    /** Anything that moves food or membership changes the figures, so the screen follows along. */
    private fun observeLiveUpdates() {
        viewModelScope.launch {
            merge(
                signalRService.pickupRequestCreated,
                signalRService.transactionCompleted,
                signalRService.listingCreated,
                signalRService.listingDeleted,
                signalRService.pickupRequestCancelled,
                signalRService.newOrganizationRegistered
            ).collect {
                load(isRefresh = false, silent = true)
                loadActiveDates()
            }
        }
    }

    fun onEvent(event: AdminAnalyticsEvent) {
        when (event) {
            AdminAnalyticsEvent.Load -> load(isRefresh = false)
            AdminAnalyticsEvent.Refresh -> {
                load(isRefresh = true)
                loadActiveDates()
            }
            AdminAnalyticsEvent.ExportPdf -> exportPdf()
            AdminAnalyticsEvent.FocusConsumed -> _state.update { it.copy(focusedSection = null) }
            AdminAnalyticsEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = true) }
            AdminAnalyticsEvent.HideFilterSheet -> _state.update { it.copy(showFilterSheet = false) }
            is AdminAnalyticsEvent.PeriodTypeSelected -> setPeriod(AnalyticsPeriod(event.type))
            AdminAnalyticsEvent.PreviousPeriod -> {
                val current = _state.value
                if (current.period.canGoBack(current.firstDataDate)) {
                    setPeriod(current.period.copy(offset = current.period.offset + 1))
                }
            }
            AdminAnalyticsEvent.NextPeriod -> {
                val period = _state.value.period
                if (period.canGoForward) setPeriod(period.copy(offset = period.offset - 1))
            }
            AdminAnalyticsEvent.ClearFilters -> setPeriod(AnalyticsPeriod())
        }
    }

    private fun setPeriod(period: AnalyticsPeriod) {
        if (period == _state.value.period) return
        _state.update { it.copy(period = period) }
        load(isRefresh = false)
    }

    private fun loadActiveDates() {
        viewModelScope.launch {
            // Only bounds how far back the period can step; without it stepping back is
            // simply unbounded, so a failure here stays quiet.
            runCatching { adminApi.getStatisticsActiveDates().data }
                .onSuccess { dates ->
                    val first = dates.minOrNull()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    _state.update { it.copy(firstDataDate = first) }
                }
        }
    }

    private fun load(isRefresh: Boolean, silent: Boolean = false) {
        val range = _state.value.requestRange
        viewModelScope.launch {
            if (!silent) {
                _state.update {
                    it.copy(
                        // A range change keeps the old figures on screen rather than blanking
                        // the page; only a cold start gets the spinner.
                        isLoading = !isRefresh && it.data == null,
                        isRefreshing = isRefresh,
                        error = null
                    )
                }
            }

            try {
                val loaded = if (range == null) {
                    adminApi.getDetailedStatistics(preset = "all").data
                } else {
                    adminApi.getDetailedStatistics(from = range.first, to = range.second, preset = "custom").data
                }

                // A range change raced against this response would show the wrong figures
                // under the new range, so a stale answer is dropped.
                if (_state.value.requestRange != range) return@launch

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

        val periodLabel = _state.value.period.description(context.resources)
        _state.update { it.copy(isExporting = true) }

        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                runCatching { writePdf(data, periodLabel) }.getOrNull()
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
    private fun writePdf(data: AdminDetailedStatsData, periodLabel: String): Uri? {
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

        fun line(label: String, value: String) {
            if (y > PAGE_H - 60) newPage()
            canvas.drawText(label, MARGIN, y, bodyPaint)
            canvas.drawText(value, 340f, y, bodyPaint)
            y += 15f
        }

        fun note(text: String) {
            if (y > PAGE_H - 60) newPage()
            canvas.drawText(text, MARGIN, y, mutedPaint)
            y += 13f
        }

        fun pct(value: Double) = "${(value * 100).roundToInt()}%"

        canvas.drawText(string(R.string.pdf_stats_title), MARGIN, y, titlePaint)
        y += 22f
        note(
            string(
                R.string.pdf_stats_generated,
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                periodLabel
            )
        )

        val b = data.backlog
        heading(string(R.string.section_live_backlog))
        note(string(R.string.backlog_note))
        line(string(R.string.backlog_open_listings), "${b.openListings}")
        line(string(R.string.backlog_expiring_24h), "${b.expiringWithin24h}")
        line(string(R.string.backlog_pending_requests), "${b.pendingRequests}")
        line(string(R.string.backlog_ready_requests), "${b.readyRequests}")
        line(string(R.string.backlog_pending_verifications), "${b.pendingVerifications}")
        line(string(R.string.backlog_open_disputes), "${b.openDisputes}")

        val q = data.quality
        heading(string(R.string.section_quality))
        line(string(R.string.quality_avg_rating), q.averageRating?.toString() ?: "-")
        line(string(R.string.quality_reviews), "${q.reviewCount}")
        line(string(R.string.quality_review_coverage_label), pct(q.reviewCoverage))
        line(string(R.string.quality_disputes), "${q.disputesOpened}")

        if (data.leaderboards.topGroceries.isNotEmpty()) {
            heading(string(R.string.chart_top_groceries))
            data.leaderboards.topGroceries.forEach { g ->
                line(g.name.ifBlank { "-" }, "${g.completedPickups}")
            }
        }

        if (data.leaderboards.topNgos.isNotEmpty()) {
            heading(string(R.string.chart_top_ngos))
            data.leaderboards.topNgos.forEach { n ->
                line(n.name.ifBlank { "-" }, "${n.completedPickups}")
            }
        }

        val o = data.organizations
        heading(string(R.string.section_organizations))
        line(string(R.string.org_type_groceries), "${o.groceries}")
        line(string(R.string.org_type_ngos), "${o.ngos}")
        line(string(R.string.stat_verified), "${o.verified}")
        line(string(R.string.status_pending), "${o.pendingVerification}")

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
    }
}
