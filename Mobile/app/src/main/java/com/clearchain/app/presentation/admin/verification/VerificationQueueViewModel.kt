package com.clearchain.app.presentation.admin.verification

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.RejectOrganizationBody
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class VerificationQueueViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adminApi: AdminApi,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationQueueState())
    val state: StateFlow<VerificationQueueState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadOrganizations()
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch {
            signalRService.newOrganizationRegistered.collect { notification ->
                loadOrganizations()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_registration, notification.type, notification.name)))
            }
        }
    }

    fun onEvent(event: VerificationQueueEvent) {
        when (event) {
            VerificationQueueEvent.LoadOrganizations -> loadOrganizations()
            VerificationQueueEvent.RefreshOrganizations -> refreshOrganizations()
            VerificationQueueEvent.ClearError -> _state.update { it.copy(error = null) }

            is VerificationQueueEvent.SearchQueryChanged ->
                _state.update { it.copy(searchQuery = event.query) }
            is VerificationQueueEvent.StatusFilterChanged ->
                _state.update { it.copy(selectedStatus = event.status) }
            is VerificationQueueEvent.SortOptionChanged ->
                _state.update { it.copy(selectedSort = event.option) }

            // Advanced filter sheet
            VerificationQueueEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = true) }
            VerificationQueueEvent.HideFilterSheet -> _state.update { it.copy(showFilterSheet = false) }
            is VerificationQueueEvent.FilterOrgTypeChanged ->
                _state.update { it.copy(filterOrgType = event.type) }
            VerificationQueueEvent.ClearAdvancedFilters ->
                _state.update { it.copy(filterOrgType = null) }

            // Approval checklist
            is VerificationQueueEvent.ShowChecklist ->
                _state.update { it.copy(showChecklistForId = event.orgId, checkedItems = emptySet()) }
            VerificationQueueEvent.DismissChecklist ->
                _state.update { it.copy(showChecklistForId = null) }
            is VerificationQueueEvent.ToggleChecklistItem ->
                _state.update {
                    val updated = if (event.index in it.checkedItems) {
                        it.checkedItems - event.index
                    } else {
                        it.checkedItems + event.index
                    }
                    it.copy(checkedItems = updated)
                }
            VerificationQueueEvent.ConfirmApprove -> approveOrganization()

            // Rejection dialog
            is VerificationQueueEvent.ShowRejectDialog ->
                _state.update { it.copy(showRejectDialogForId = event.orgId, rejectionReason = "") }
            VerificationQueueEvent.DismissRejectDialog ->
                _state.update { it.copy(showRejectDialogForId = null) }
            is VerificationQueueEvent.RejectionReasonChanged ->
                _state.update { it.copy(rejectionReason = event.reason) }
            is VerificationQueueEvent.SelectRejectionTemplate ->
                _state.update { it.copy(rejectionReason = event.reason) }
            VerificationQueueEvent.ConfirmReject -> rejectOrganization()

            // Batch selection
            VerificationQueueEvent.ToggleBatchMode ->
                _state.update { it.copy(isBatchMode = !it.isBatchMode, selectedOrgIds = emptySet()) }
            is VerificationQueueEvent.ToggleOrgSelection -> {
                _state.update {
                    val updated = if (event.orgId in it.selectedOrgIds) {
                        it.selectedOrgIds - event.orgId
                    } else {
                        it.selectedOrgIds + event.orgId
                    }
                    it.copy(selectedOrgIds = updated, isBatchMode = updated.isNotEmpty())
                }
            }
            VerificationQueueEvent.SelectAllVisible ->
                _state.update {
                    val selected = it.filteredOrgs.map { o -> o.id }.toSet()
                    it.copy(selectedOrgIds = selected, isBatchMode = selected.isNotEmpty())
                }
            VerificationQueueEvent.ClearSelection ->
                _state.update { it.copy(selectedOrgIds = emptySet(), isBatchMode = false) }
            VerificationQueueEvent.BatchApprove -> batchApprove()
            VerificationQueueEvent.BatchReject -> batchReject()

            VerificationQueueEvent.ExportCsv -> exportCsv()
        }
    }

    private fun exportCsv() {
        val current = _state.value
        val orgs = current.filteredOrgs
        if (orgs.isEmpty()) {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_no_orgs_export))) }
            return
        }
        val tag = current.selectedStatus?.lowercase() ?: "all"
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val sb = StringBuilder()
                    sb.appendLine("Name,Type,Status,Email,Phone,Location,Contact Person,Created At")
                    orgs.forEach { org ->
                        fun esc(s: String) = if (s.contains(',') || s.contains('"')) "\"${s.replace("\"", "\"\"")}\"" else s
                        sb.appendLine(
                            "${esc(org.name)},${org.type.name},${org.verificationStatus.name}," +
                                "${esc(org.email)},${esc(org.phone)},${esc(org.location)}," +
                                "${esc(org.contactPerson ?: "")},${org.createdAt.take(10)}"
                        )
                    }
                    val csv = sb.toString()
                    val fileName = "clearchain_organizations_${tag}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"

                    val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.also { u ->
                            context.contentResolver.openOutputStream(u)?.use { it.write(csv.toByteArray()) }
                        }
                    } else {
                        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                        FileOutputStream(file).use { it.write(csv.toByteArray()) }
                        Uri.fromFile(file)
                    }

                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(shareIntent, "Share Organizations CSV")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_csv_saved)))
                    } else {
                        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_csv_failed)))
                    }
                } catch (e: Exception) {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_export_failed, e.message ?: "")))
                }
            }
        }
    }

    private fun batchApprove() {
        val ids = _state.value.selectedOrgIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, isBatchMode = false, selectedOrgIds = emptySet()) }
            var successCount = 0
            ids.forEach { orgId ->
                try {
                    adminApi.verifyOrganization(orgId)
                    successCount++
                } catch (_: Exception) {}
            }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_approved_orgs, successCount, ids.size)))
            loadOrganizations()
            _state.update { it.copy(isProcessing = false) }
        }
    }

    private fun batchReject() {
        val ids = _state.value.selectedOrgIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, isBatchMode = false, selectedOrgIds = emptySet()) }
            var successCount = 0
            ids.forEach { orgId ->
                try {
                    adminApi.unverifyOrganization(orgId, RejectOrganizationBody())
                    successCount++
                } catch (_: Exception) {}
            }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_rejected_orgs, successCount, ids.size)))
            loadOrganizations()
            _state.update { it.copy(isProcessing = false) }
        }
    }

    private fun approveOrganization() {
        val orgId = _state.value.showChecklistForId ?: return
        viewModelScope.launch {
            _state.update { it.copy(showChecklistForId = null, isProcessing = true) }
            try {
                adminApi.verifyOrganization(orgId)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_org_approved)))
                loadOrganizations()
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_approve_failed)))
            }
            _state.update { it.copy(isProcessing = false) }
        }
    }

    private fun rejectOrganization() {
        val orgId = _state.value.showRejectDialogForId ?: return
        val reason = _state.value.rejectionReason.trim().ifBlank { null }
        viewModelScope.launch {
            _state.update { it.copy(showRejectDialogForId = null, isProcessing = true) }
            try {
                adminApi.unverifyOrganization(orgId, RejectOrganizationBody(reason))
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_org_rejected)))
                loadOrganizations()
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_reject_failed)))
            }
            _state.update { it.copy(isProcessing = false) }
        }
    }

    private fun loadOrganizations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = adminApi.getAllOrganizations()
                val organizations = response.data
                    .map { dto -> dto.toDomain() }
                    .filter { it.type != OrganizationType.ADMIN }
                _state.update { it.copy(organizations = organizations, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: context.getString(R.string.error_load_orgs), isLoading = false) }
            }
        }
    }

    private fun refreshOrganizations() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                val response = adminApi.getAllOrganizations()
                val organizations = response.data
                    .map { dto -> dto.toDomain() }
                    .filter { it.type != OrganizationType.ADMIN }
                _state.update { it.copy(organizations = organizations, isRefreshing = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_orgs_refreshed)))
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: context.getString(R.string.error_refresh_failed), isRefreshing = false) }
            }
        }
    }
}
