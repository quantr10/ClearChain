package com.clearchain.app.presentation.admin.analytics

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.AdminDetailedStatsData
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class AdminAnalyticsViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val adminApi: AdminApi,
    private val signalRService: SignalRService
) : AndroidViewModel(application) {

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
            ).collect { load(isRefresh = false, silent = true) }
        }
    }

    fun onEvent(event: AdminAnalyticsEvent) {
        when (event) {
            AdminAnalyticsEvent.Load          -> load(isRefresh = false)
            AdminAnalyticsEvent.Refresh       -> load(isRefresh = true)
            AdminAnalyticsEvent.ExportPdf     -> exportPdf()
            AdminAnalyticsEvent.ClearError    -> _state.update { it.copy(error = null) }
            AdminAnalyticsEvent.FocusConsumed -> _state.update { it.copy(focusedSection = null) }
            is AdminAnalyticsEvent.SelectPeriod -> {
                if (event.period != _state.value.period) {
                    _state.update { it.copy(period = event.period) }
                    load(isRefresh = false)
                }
            }
        }
    }

    private fun load(isRefresh: Boolean, silent: Boolean = false) {
        val period = _state.value.period
        viewModelScope.launch {
            if (!silent) {
                _state.update {
                    it.copy(
                        // A period switch keeps the old figures on screen rather than blanking
                        // the page; only a cold start gets the spinner.
                        isLoading    = !isRefresh && it.data == null,
                        isRefreshing = isRefresh,
                        error        = null
                    )
                }
            }

            try {
                val loaded = adminApi.getDetailedStatistics(preset = period.preset).data

                // A period change raced against this response would show the wrong figures
                // under the new chip, so a stale answer is dropped.
                if (_state.value.period != period) return@launch

                _state.update {
                    it.copy(
                        data         = loaded ?: it.data,
                        isLoading    = false,
                        isRefreshing = false
                    )
                }
                if (isRefresh) {
                    _uiEvent.send(UiEvent.ShowSnackbar(string(R.string.snack_stats_refreshed)))
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error        = e.message ?: string(R.string.error_load_statistics),
                        isLoading    = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    private fun string(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    // ── PDF export ───────────────────────────────────────────────────────────

    private fun exportPdf() {
        val data = _state.value.data ?: run {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(string(R.string.snack_no_data_export))) }
            return
        }
        if (_state.value.isExporting) return

        val periodLabel = string(_state.value.period.labelRes)
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

            val ctx = getApplication<Application>()
            runCatching {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(
                    Intent.createChooser(share, string(R.string.pdf_stats_title))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            _uiEvent.send(UiEvent.ShowSnackbar(string(R.string.snack_pdf_saved)))
        }
    }

    /**
     * Writes the report the screen shows, in the same order, breaking to a new page when
     * a section would run off the bottom.
     */
    private fun writePdf(data: AdminDetailedStatsData, periodLabel: String): Uri? {
        val ctx = getApplication<Application>()
        val doc = PdfDocument()

        val titlePaint   = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headingPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint    = Paint().apply { textSize = 11f }
        val mutedPaint   = Paint().apply { textSize = 9f; color = android.graphics.Color.GRAY }

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
        fun hours(value: Double?) = value?.let {
            when {
                it < 1  -> "${(it * 60).roundToInt()}m"
                it < 48 -> "${it.roundToInt()}h"
                else    -> "${(it / 24).roundToInt()}d"
            }
        } ?: "-"

        canvas.drawText(string(R.string.pdf_stats_title), MARGIN, y, titlePaint)
        y += 22f
        note(
            string(
                R.string.pdf_stats_generated,
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                periodLabel
            )
        )

        val t = data.timing
        heading(string(R.string.section_timing))
        line(string(R.string.timing_to_ready), hours(t.medianHoursToReady))
        line(string(R.string.timing_to_pickup), hours(t.medianHoursToPickup))
        line(string(R.string.timing_to_confirm), hours(t.medianHoursToConfirm))
        line(string(R.string.timing_p90), hours(t.p90HoursToPickup))
        line(string(R.string.timing_within_24h), pct(t.completedWithin24hRate))
        note(string(R.string.timing_sample, t.sampleSize))

        val b = data.backlog
        heading(string(R.string.section_live_backlog))
        note(string(R.string.backlog_note))
        line(string(R.string.backlog_open_listings), "${b.openListings}")
        line(string(R.string.backlog_expiring_24h), "${b.expiringWithin24h}")
        line(string(R.string.backlog_pending_requests), "${b.pendingRequests}")
        line(string(R.string.backlog_ready_requests), "${b.readyRequests}")
        line(string(R.string.backlog_pending_verifications), "${b.pendingVerifications}")
        line(string(R.string.backlog_open_disputes), "${b.openDisputes}")
        line(string(R.string.backlog_pending_reports), "${b.pendingReports}")

        val q = data.quality
        heading(string(R.string.section_quality))
        line(string(R.string.quality_avg_rating), q.averageRating?.toString() ?: "-")
        line(string(R.string.quality_reviews), "${q.reviewCount}")
        line(string(R.string.quality_review_coverage_label), pct(q.reviewCoverage))
        line(string(R.string.quality_disputes), "${q.disputesOpened}")
        line(string(R.string.quality_reports), "${q.reportsFiled}")

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

        val fileName = "clearchain_stats_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.also { u ->
                ctx.contentResolver.openOutputStream(u)?.use { doc.writeTo(it) }
            }
        } else {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            FileOutputStream(file).use { doc.writeTo(it) }
            Uri.fromFile(file)
        }
        doc.close()
        return uri
    }

    private companion object {
        const val SECTION_ARG = "section"
        const val PAGE_W = 595   // A4 at 72dpi
        const val PAGE_H = 842
        const val MARGIN = 40f
    }
}
