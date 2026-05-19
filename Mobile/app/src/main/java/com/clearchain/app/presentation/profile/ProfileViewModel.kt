package com.clearchain.app.presentation.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.clearchain.app.R
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.DeleteAccountRequest
import com.clearchain.app.domain.model.ActivityItem
import com.clearchain.app.domain.model.OrgStats
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.usecase.auth.ChangePasswordUseCase
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.profile.UpdateProfileUseCase
import com.clearchain.app.util.ImageUtils
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val organizationApi: OrganizationApi,
    private val authApi: AuthApi
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadAll()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.LoadProfile  -> loadProfile()
            ProfileEvent.LoadStats    -> loadStats()
            ProfileEvent.LoadActivity -> loadActivity()
            ProfileEvent.Refresh      -> loadAll()
            ProfileEvent.ClearError   -> _state.update { it.copy(error = null) }

            is ProfileEvent.ChangePassword ->
                changePassword(event.currentPassword, event.newPassword)

            is ProfileEvent.DeleteAccount ->
                deleteAccount(event.password)

            ProfileEvent.StartEdit  -> startEdit()
            ProfileEvent.CancelEdit -> cancelEdit()
            ProfileEvent.SaveProfile -> saveProfile()

            is ProfileEvent.EditNameChanged ->
                _state.update { it.copy(editName = event.name, editNameError = null) }
            is ProfileEvent.EditPhoneChanged ->
                _state.update { it.copy(editPhone = event.phone, editPhoneError = null) }
            is ProfileEvent.EditAddressChanged ->
                _state.update { it.copy(editAddress = event.address, editAddressError = null) }
            is ProfileEvent.EditLocationChanged ->
                _state.update { it.copy(editLocation = event.location, editLocationError = null) }
            is ProfileEvent.EditOpenTimeChanged ->
                _state.update { it.copy(editOpenTime = event.time) }
            is ProfileEvent.EditCloseTimeChanged ->
                _state.update { it.copy(editCloseTime = event.time) }
            is ProfileEvent.EditContactPersonChanged ->
                _state.update { it.copy(editContactPerson = event.contactPerson, editContactPersonError = null) }
            is ProfileEvent.EditPickupInstructionsChanged ->
                _state.update { it.copy(editPickupInstructions = event.instructions) }
            is ProfileEvent.EditDescriptionChanged ->
                _state.update { it.copy(editDescription = event.description) }
            is ProfileEvent.EditLocationCoordsChanged ->
                _state.update { it.copy(editLat = event.lat, editLng = event.lng) }

            is ProfileEvent.AvatarSelected -> uploadAvatar(event.uri)
        }
    }

    // ── Data loading ───────────────────────────────────────────────────────────

    private fun loadAll() {
        viewModelScope.launch {
            // Load profile, stats, and activity concurrently
            val profileJob  = async { runCatching { loadProfileSuspend() } }
            val statsJob    = async { runCatching { loadStatsSuspend() } }
            val activityJob = async { runCatching { loadActivitySuspend() } }
            profileJob.await()
            statsJob.await()
            activityJob.await()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { loadProfileSuspend() }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private suspend fun loadProfileSuspend() {
        _state.update { it.copy(isLoading = true, error = null) }
        val user = getCurrentUserUseCase().first()
        _state.update { it.copy(user = user, isLoading = false) }
    }

    private fun loadStats() {
        viewModelScope.launch {
            runCatching { loadStatsSuspend() }
        }
    }

    private suspend fun loadStatsSuspend() {
        _state.update { it.copy(isLoadingStats = true) }
        val dto = organizationApi.getMyStats().data
        val stats = OrgStats(
            inStock        = dto.inStock,
            activeRequests = dto.activeRequests,
            distributed    = dto.distributed,
            availableFood  = dto.availableFood,
            totalCompleted = dto.totalCompleted,
            activeListings = dto.activeListings,
            pendingRequests = dto.pendingRequests,
            completed      = dto.completed,
            foodSaved      = dto.foodSaved,
            totalListings  = dto.totalListings
        )
        _state.update { it.copy(stats = stats, isLoadingStats = false) }
    }

    private fun loadActivity() {
        viewModelScope.launch {
            runCatching { loadActivitySuspend() }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingActivity = false, activityError = e.message) }
                }
        }
    }

    private suspend fun loadActivitySuspend() {
        _state.update { it.copy(isLoadingActivity = true, activityError = null) }
        val items = organizationApi.getMyActivity().data.map { dto ->
            ActivityItem(
                id        = dto.id,
                type      = dto.type,
                title     = dto.title,
                subtitle  = dto.subtitle,
                timestamp = dto.timestamp,
                relatedId = dto.relatedId
            )
        }
        _state.update { it.copy(activity = items, isLoadingActivity = false) }
    }

    // ── Edit profile ───────────────────────────────────────────────────────────

    private fun startEdit() {
        val user = _state.value.user ?: return
        _state.update {
            it.copy(
                isEditing             = true,
                editName              = user.name,
                editPhone             = user.phone,
                editAddress           = user.address,
                editLocation          = user.location,
                editOpenTime          = user.hours?.substringBefore(" - ", "") ?: "",
                editCloseTime         = user.hours?.substringAfter(" - ", "") ?: "",
                editContactPerson     = user.contactPerson ?: "",
                editPickupInstructions = user.pickupInstructions ?: "",
                editDescription       = user.description ?: "",
                editLat               = user.latitude,
                editLng               = user.longitude,
                editNameError         = null,
                editPhoneError        = null,
                editContactPersonError = null,
                error                 = null
            )
        }
    }

    private fun cancelEdit() {
        _state.update {
            it.copy(
                isEditing              = false,
                editNameError          = null,
                editPhoneError         = null,
                editContactPersonError = null,
                error                  = null
            )
        }
    }

    private fun saveProfile() {
        if (!validateProfileInputs()) return
        val s = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isSavingProfile = true, error = null) }

            val result = updateProfileUseCase(
                name                = s.editName,
                phone               = s.editPhone,
                address             = s.editAddress,
                location            = s.editLocation,
                hours               = if (s.editOpenTime.isNotBlank() && s.editCloseTime.isNotBlank())
                                          "${s.editOpenTime} - ${s.editCloseTime}" else null,
                latitude            = s.editLat,
                longitude           = s.editLng,
                contactPerson       = s.editContactPerson.ifBlank { null },
                pickupInstructions  = s.editPickupInstructions.ifBlank { null },
                description         = s.editDescription.ifBlank { null }
            )

            result.fold(
                onSuccess = {
                    loadProfile()
                    _state.update { it.copy(isSavingProfile = false, isEditing = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.snack_profile_updated)))
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isSavingProfile = false, error = e.message ?: getApplication<Application>().getString(R.string.error_update_profile_failed))
                    }
                }
            )
        }
    }

    private fun validateProfileInputs(): Boolean {
        val s = _state.value
        var valid = true
        if (s.editName.isBlank()) {
            _state.update { it.copy(editNameError = getApplication<Application>().getString(R.string.error_name_required)) }; valid = false
        } else if (s.editName.length < 3) {
            _state.update { it.copy(editNameError = getApplication<Application>().getString(R.string.error_name_min_chars)) }; valid = false
        }
        if (s.editPhone.isNotBlank() && !ValidationUtils.isValidPhone(s.editPhone)) {
            _state.update { it.copy(editPhoneError = getApplication<Application>().getString(R.string.error_phone_invalid)) }; valid = false
        }
        if ((s.user?.type == OrganizationType.NGO || s.user?.type == OrganizationType.GROCERY)
            && s.editContactPerson.isBlank()) {
            _state.update { it.copy(editContactPersonError = getApplication<Application>().getString(R.string.error_contact_person_required)) }
            valid = false
        }
        return valid
    }

    // ── Change password ────────────────────────────────────────────────────────

    private fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _state.update { it.copy(isChangingPassword = true, error = null) }
            changePasswordUseCase(currentPassword, newPassword).fold(
                onSuccess = {
                    _state.update { it.copy(isChangingPassword = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.snack_password_changed)))
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isChangingPassword = false, error = e.message ?: getApplication<Application>().getString(R.string.error_change_password_failed))
                    }
                }
            )
        }
    }

    private fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, avatarUploadError = null) }
            try {
                val context = getApplication<Application>()
                val bytes = ImageUtils.compressToBytes(context, uri)
                val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val fileName = "avatar_${System.currentTimeMillis()}.jpg"
                val part = MultipartBody.Part.createFormData("avatar", fileName, requestBody)
                organizationApi.uploadAvatar(part)
                loadProfile()
                _uiEvent.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.snack_avatar_updated)))
            } catch (e: Exception) {
                _state.update { it.copy(avatarUploadError = e.message ?: "Failed to upload avatar") }
            } finally {
                _state.update { it.copy(isUploadingAvatar = false) }
            }
        }
    }

    private fun deleteAccount(password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isDeletingAccount = true, error = null) }
            try {
                authApi.deleteAccount(DeleteAccountRequest(password))
                _state.update { it.copy(isDeletingAccount = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.snack_account_deleted)))
                _uiEvent.send(UiEvent.Navigate("logout"))
            } catch (e: Exception) {
                _state.update { it.copy(isDeletingAccount = false) }
                val msg = runCatching {
                    val body = (e as? retrofit2.HttpException)
                        ?.response()?.errorBody()?.string()
                    org.json.JSONObject(body ?: "").optString("message").takeIf { it.isNotBlank() }
                }.getOrNull() ?: e.message ?: getApplication<Application>().getString(R.string.error_delete_account_failed)
                _uiEvent.send(UiEvent.ShowSnackbar(msg))
            }
        }
    }
}
