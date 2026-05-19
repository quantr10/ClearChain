package com.clearchain.app.presentation.admin.statistics

import android.app.Application
import android.content.ContentValues
import com.clearchain.app.R
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.AdminStats
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    application: Application,
    private val adminApi: AdminApi,
    private val signalRService: SignalRService
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadStatistics()
        loadDetailedStats("all")
        loadOrgLocations()
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch { signalRService.connect() }
        viewModelScope.launch { signalRService.pickupRequestCreated.collect { loadStatistics() } }
        viewModelScope.launch { signalRService.transactionCompleted.collect { loadStatistics() } }
        viewModelScope.launch { signalRService.listingCreated.collect { loadStatistics() } }
        viewModelScope.launch { signalRService.newOrganizationRegistered.collect { loadStatistics() } }
        viewModelScope.launch { signalRService.pickupRequestCancelled.collect { loadStatistics() } }
        viewModelScope.launch { signalRService.listingDeleted.collect { loadStatistics() } }
    }

    private fun loadDetailedStats(preset: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingDetailed = true) }
            try {
                val response = adminApi.getDetailedStatistics(preset = preset)
                _state.update { it.copy(detailedStats = response.data, isLoadingDetailed = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingDetailed = false) }
            }
        }
        // Load previous period for comparison (skip for "all" — no meaningful previous)
        val previousPreset = previousPresetFor(preset)
        if (previousPreset != null) {
            viewModelScope.launch {
                _state.update { it.copy(isLoadingPrevious = true) }
                try {
                    val response = adminApi.getDetailedStatistics(preset = previousPreset)
                    _state.update { it.copy(previousDetailedStats = response.data, isLoadingPrevious = false) }
                } catch (e: Exception) {
                    _state.update { it.copy(previousDetailedStats = null, isLoadingPrevious = false) }
                }
            }
        } else {
            _state.update { it.copy(previousDetailedStats = null) }
        }
    }

    private fun previousPresetFor(preset: String): String? = when (preset) {
        "today" -> "yesterday"
        "week"  -> "last_week"
        "month" -> "last_month"
        else    -> null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            signalRService.disconnect()
        }
    }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            StatisticsEvent.LoadStatistics -> loadStatistics()
            StatisticsEvent.RefreshStatistics -> refreshStatistics()
            StatisticsEvent.ClearError ->
                _state.update { it.copy(error = null) }
            StatisticsEvent.ExportPdf -> exportPdf()

            is StatisticsEvent.PresetChanged -> {
                _state.update { it.copy(selectedPreset = event.preset) }
                loadDetailedStats(event.preset)
            }
        }
    }

    private fun loadOrgLocations() {
        viewModelScope.launch {
            try {
                val response = adminApi.getAllOrganizations()
                val orgs = response.data.map { it.toDomain() }
                    .filter { it.latitude != null && it.longitude != null }
                _state.update { it.copy(orgLocations = orgs) }
            } catch (_: Exception) {}
        }
    }

    private fun exportPdf() {
        val stats = _state.value.stats ?: run {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.snack_no_data_export))) }
            return
        }
        val detailed = _state.value.detailedStats
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val doc = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = doc.startPage(pageInfo)
                    val canvas = page.canvas
                    val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
                    val bodyPaint = Paint().apply { textSize = 12f }
                    val smallPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.GRAY }

                    val ctx2 = getApplication<Application>()
                    var y = 50f
                    canvas.drawText(ctx2.getString(R.string.pdf_stats_title), 40f, y, titlePaint); y += 30f
                    val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    canvas.drawText(ctx2.getString(R.string.pdf_stats_generated, now, _state.value.selectedPreset), 40f, y, smallPaint); y += 30f

                    fun line(label: String, value: String) {
                        canvas.drawText("$label:", 40f, y, bodyPaint)
                        canvas.drawText(value, 250f, y, bodyPaint)
                        y += 20f
                    }

                    canvas.drawText(ctx2.getString(R.string.pdf_platform_overview), 40f, y, titlePaint.apply { textSize = 14f }); y += 20f
                    line(ctx2.getString(R.string.label_total_organizations), "${stats.totalOrganizations}")
                    line(ctx2.getString(R.string.label_grocery_stores), "${stats.totalGroceries}")
                    line(ctx2.getString(R.string.label_ngos), "${stats.totalNgos}")
                    line(ctx2.getString(R.string.label_verified), "${stats.verifiedOrganizations}")
                    line(ctx2.getString(R.string.label_total_listings), "${stats.totalListings}")
                    line(ctx2.getString(R.string.label_active_listings), "${stats.activeListings}")
                    line(ctx2.getString(R.string.label_total_requests), "${stats.totalPickupRequests}")
                    line(ctx2.getString(R.string.label_completed_requests), "${stats.completedRequests}")
                    line(ctx2.getString(R.string.label_pending_requests), "${stats.pendingRequests}")
                    line(ctx2.getString(R.string.label_total_food_saved_kg), "${stats.totalFoodSaved.toInt()}")

                    detailed?.impact?.let { impact ->
                        y += 10f
                        canvas.drawText(ctx2.getString(R.string.label_impact), 40f, y, titlePaint.apply { textSize = 14f }); y += 20f
                        line(ctx2.getString(R.string.label_kg_saved), "${impact.kgSaved}")
                        line(ctx2.getString(R.string.label_meals_equivalent), "${impact.mealsEquivalent}")
                        line(ctx2.getString(R.string.label_co2_saved_kg), "${impact.co2Saved}")
                        line(ctx2.getString(R.string.label_beneficiaries), "${impact.totalBeneficiaries}")
                    }

                    doc.finishPage(page)

                    val fileName = "clearchain_stats_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"
                    val ctx = getApplication<Application>()
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

                    if (uri != null) {
                        // Share via intent
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(shareIntent, "Share Statistics PDF")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(chooser)
                        _uiEvent.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.snack_pdf_saved)))
                    } else {
                        _uiEvent.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.snack_pdf_failed)))
                    }
                } catch (e: Exception) {
                    _uiEvent.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.snack_export_failed, e.message ?: "")))
                }
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val statsResponse = adminApi.getStatistics()
                val stats = statsResponse.data.let {
                    AdminStats(
                        totalOrganizations = it.totalOrganizations,
                        totalGroceries = it.totalGroceries,
                        totalNgos = it.totalNgos,
                        verifiedOrganizations = it.verifiedOrganizations,
                        unverifiedOrganizations = it.unverifiedOrganizations,
                        totalListings = it.totalListings,
                        activeListings = it.activeListings,
                        reservedListings = it.reservedListings,
                        totalPickupRequests = it.totalPickupRequests,
                        pendingRequests = it.pendingRequests,
                        approvedRequests = it.approvedRequests,
                        readyRequests = it.readyRequests,
                        rejectedRequests = it.rejectedRequests,
                        completedRequests = it.completedRequests,
                        cancelledRequests = it.cancelledRequests,
                        totalFoodSaved = it.totalFoodSaved
                    )
                }

                _state.update {
                    it.copy(
                        stats = stats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: getApplication<Application>().getString(R.string.error_load_statistics),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun refreshStatistics() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            try {
                val statsResponse = adminApi.getStatistics()
                val stats = statsResponse.data.let {
                    AdminStats(
                        totalOrganizations = it.totalOrganizations,
                        totalGroceries = it.totalGroceries,
                        totalNgos = it.totalNgos,
                        verifiedOrganizations = it.verifiedOrganizations,
                        unverifiedOrganizations = it.unverifiedOrganizations,
                        totalListings = it.totalListings,
                        activeListings = it.activeListings,
                        reservedListings = it.reservedListings,
                        totalPickupRequests = it.totalPickupRequests,
                        pendingRequests = it.pendingRequests,
                        approvedRequests = it.approvedRequests,
                        readyRequests = it.readyRequests,
                        rejectedRequests = it.rejectedRequests,
                        completedRequests = it.completedRequests,
                        cancelledRequests = it.cancelledRequests,
                        totalFoodSaved = it.totalFoodSaved
                    )
                }

                _state.update {
                    it.copy(
                        stats = stats,
                        isRefreshing = false
                    )
                }
                _uiEvent.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.snack_stats_refreshed)))
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: getApplication<Application>().getString(R.string.error_refresh_statistics),
                        isRefreshing = false
                    )
                }
            }
        }
    }
}