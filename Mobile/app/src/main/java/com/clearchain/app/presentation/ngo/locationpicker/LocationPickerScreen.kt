package com.clearchain.app.presentation.ngo.locationpicker

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.local.LocationPreferenceStore
import com.clearchain.app.domain.model.LocationPreference
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class PlaceSuggestion(
    val name: String, val fullAddress: String,
    val latitude: Double, val longitude: Double
)

data class LocationPickerState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val displayName: String = "",
    val radiusKm: Int = 10,
    val searchQuery: String = "",
    val searchSuggestions: List<PlaceSuggestion> = emptyList(),
    val showSuggestions: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingGps: Boolean = false,
    val isReverseGeocoding: Boolean = false,
    val isInitializing: Boolean = true,    // True until first position resolved
    val needsGpsAutoDetect: Boolean = false, // True if no saved/profile location
    val error: String? = null,
    val profileLat: Double? = null,
    val profileLng: Double? = null,
    val profileCity: String? = null,
    val hasProfileLocation: Boolean = false,
    val cameraMoveId: Int = 0
)

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    private val locationPreferenceStore: LocationPreferenceStore,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(LocationPickerState())
    val state: StateFlow<LocationPickerState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            // 1. Load profile location (for "Profile" button)
            val user = getCurrentUserUseCase().first()
            if (user != null && user.latitude != null && user.longitude != null) {
                _state.update {
                    it.copy(
                        profileLat = user.latitude, profileLng = user.longitude,
                        profileCity = user.location.ifBlank { user.address },
                        hasProfileLocation = true
                    )
                }
            }

            // 2. Check saved preference
            val savedPref = locationPreferenceStore.locationPreference.first()

            when {
                // Priority 1: Saved preference exists → use it
                savedPref != null -> {
                    _state.update {
                        it.copy(
                            latitude = savedPref.latitude,
                            longitude = savedPref.longitude,
                            displayName = savedPref.displayName,
                            radiusKm = savedPref.radiusKm,
                            isInitializing = false,
                            cameraMoveId = it.cameraMoveId + 1
                        )
                    }
                }

                // Priority 2: Profile has location → use it as starting point
                user != null && user.latitude != null && user.longitude != null -> {
                    _state.update {
                        it.copy(
                            latitude = user.latitude!!,
                            longitude = user.longitude!!,
                            displayName = user.location.ifBlank { user.address },
                            isInitializing = false,
                            cameraMoveId = it.cameraMoveId + 1
                        )
                    }
                }

                // Priority 3: Nothing saved → need GPS auto-detect
                else -> {
                    _state.update {
                        it.copy(needsGpsAutoDetect = true)
                    }
                }
            }
        }
    }

    /**
     * Called from Screen once GPS permission is available and needsGpsAutoDetect is true.
     * Auto-detects current location for first-time users.
     */
    @SuppressLint("MissingPermission")
    fun autoDetectLocation(context: android.content.Context, geocoder: Geocoder) {
        if (!_state.value.needsGpsAutoDetect) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingGps = true, needsGpsAutoDetect = false) }
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedClient.lastLocation.await()
                if (location != null) {
                    val name = reverseGeocodeSync(location.latitude, location.longitude, geocoder)
                    _state.update {
                        it.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            displayName = name,
                            isLoadingGps = false,
                            isInitializing = false,
                            cameraMoveId = it.cameraMoveId + 1
                        )
                    }
                } else {
                    // GPS failed → fallback to 0,0 with message
                    _state.update {
                        it.copy(
                            isLoadingGps = false,
                            isInitializing = false,
                            error = "Could not detect location. Use search or buttons below."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingGps = false,
                        isInitializing = false,
                        error = "Location error. Use search or buttons below."
                    )
                }
            }
        }
    }

    fun onMapCenterChanged(latLng: LatLng, geocoder: Geocoder) {
        if (_state.value.isInitializing) return  // Ignore until initialized
        _state.update { it.copy(latitude = latLng.latitude, longitude = latLng.longitude) }
        viewModelScope.launch {
            _state.update { it.copy(isReverseGeocoding = true) }
            val name = reverseGeocodeSync(latLng.latitude, latLng.longitude, geocoder)
            _state.update { it.copy(displayName = name, isReverseGeocoding = false) }
        }
    }

    fun onRadiusChanged(radius: Int) { _state.update { it.copy(radiusKm = radius) } }

    fun onSearchQueryChanged(query: String, geocoder: Geocoder) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(350)
                _state.update { it.copy(isSearching = true) }
                try {
                    val results = withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION") geocoder.getFromLocationName(query, 8)
                    }
                    val suggestions = results?.mapNotNull { addr ->
                        val line = addr.getAddressLine(0) ?: return@mapNotNull null
                        PlaceSuggestion(
                            addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: line.split(",").first(),
                            line, addr.latitude, addr.longitude
                        )
                    }?.distinctBy { "%.4f,%.4f".format(it.latitude, it.longitude) } ?: emptyList()
                    _state.update { it.copy(searchSuggestions = suggestions, showSuggestions = suggestions.isNotEmpty(), isSearching = false) }
                } catch (_: Exception) { _state.update { it.copy(isSearching = false, showSuggestions = false) } }
            }
        } else _state.update { it.copy(searchSuggestions = emptyList(), showSuggestions = false) }
    }

    fun onSuggestionSelected(s: PlaceSuggestion) {
        _state.update {
            it.copy(latitude = s.latitude, longitude = s.longitude, displayName = s.name,
                searchQuery = "", searchSuggestions = emptyList(), showSuggestions = false,
                cameraMoveId = it.cameraMoveId + 1)
        }
    }

    fun useProfileLocation() {
        val s = _state.value
        if (s.profileLat != null && s.profileLng != null) {
            _state.update {
                it.copy(latitude = s.profileLat, longitude = s.profileLng,
                    displayName = s.profileCity ?: "Profile Location",
                    cameraMoveId = it.cameraMoveId + 1)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun useCurrentLocation(ctx: android.content.Context, geocoder: Geocoder) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingGps = true, error = null) }
            try {
                val loc = LocationServices.getFusedLocationProviderClient(ctx).lastLocation.await()
                if (loc != null) {
                    val name = reverseGeocodeSync(loc.latitude, loc.longitude, geocoder)
                    _state.update {
                        it.copy(latitude = loc.latitude, longitude = loc.longitude,
                            displayName = name, isLoadingGps = false,
                            cameraMoveId = it.cameraMoveId + 1)
                    }
                } else _state.update { it.copy(isLoadingGps = false, error = "Could not get location. Enable GPS.") }
            } catch (e: Exception) { _state.update { it.copy(isLoadingGps = false, error = "Error: ${e.message}") } }
        }
    }

    fun clearSearch() { _state.update { it.copy(searchQuery = "", searchSuggestions = emptyList(), showSuggestions = false) } }

    fun saveAndFinish(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            locationPreferenceStore.save(LocationPreference(s.latitude, s.longitude, s.radiusKm, s.displayName.ifBlank { "Selected Location" }))
            onDone()
        }
    }

    private suspend fun reverseGeocodeSync(lat: Double, lng: Double, geocoder: Geocoder): String = try {
        withContext(Dispatchers.IO) { @Suppress("DEPRECATION") geocoder.getFromLocation(lat, lng, 1) }
            ?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea ?: "Unknown" } ?: "Unknown"
    } catch (_: Exception) { "Unknown" }
}

// ═══════════════════════════════════════════════════════════════
// SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LocationPickerScreen(
    onLocationSelected: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    viewModel: LocationPickerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Auto-detect GPS for first-time users
    LaunchedEffect(state.needsGpsAutoDetect, locationPermission.status.isGranted) {
        if (state.needsGpsAutoDetect) {
            if (locationPermission.status.isGranted) {
                viewModel.autoDetectLocation(context, geocoder)
            } else {
                locationPermission.launchPermissionRequest()
            }
        }
    }

    // After permission granted → retry auto-detect
    var pendingGps by remember { mutableStateOf(false) }
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted && pendingGps) {
            pendingGps = false
            viewModel.useCurrentLocation(context, geocoder)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a location", fontWeight = FontWeight.Bold) },
                actions = { onDismiss?.let { IconButton(onClick = it) { Icon(Icons.Default.Close, "Close") } } }
            )
        }
    ) { padding ->
        // Show loading while initializing
        if (state.isInitializing) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Detecting your location...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(state.latitude, state.longitude), getZoomForRadius(state.radiusKm)
            )
        }

        // Camera animation trigger
        LaunchedEffect(state.cameraMoveId) {
            if (state.cameraMoveId > 0) cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(state.latitude, state.longitude), getZoomForRadius(state.radiusKm)), 600
            )
        }

        // Map stopped → get center
        LaunchedEffect(cameraPositionState.isMoving) {
            if (!cameraPositionState.isMoving) viewModel.onMapCenterChanged(cameraPositionState.position.target, geocoder)
        }

        // Radius zoom
        LaunchedEffect(state.radiusKm) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(cameraPositionState.position.target, getZoomForRadius(state.radiusKm)))
        }

        Column(Modifier.fillMaxSize().padding(padding)) {

            // ═══ MAP ═══
            Box(Modifier.fillMaxWidth().weight(0.45f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
                ) {
                    Circle(
                        center = cameraPositionState.position.target,
                        radius = state.radiusKm * 1000.0,
                        strokeColor = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                        strokeWidth = 2.5f,
                        fillColor = androidx.compose.ui.graphics.Color(0x182E7D32)
                    )
                }
                // Center pin
                Icon(Icons.Default.Place, "Pin", Modifier.size(44.dp).align(Alignment.Center).offset(y = (-22).dp), tint = MaterialTheme.colorScheme.primary)
                // Location badge
                Surface(Modifier.align(Alignment.TopCenter).padding(top = 8.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shadowElevation = 3.dp) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.isReverseGeocoding) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                        else Icon(Icons.Default.Place, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(state.displayName.ifBlank { "Move map to select" }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ═══ CONTROLS ═══
            Column(
                Modifier.fillMaxWidth().weight(0.55f).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Radius
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Search Radius", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary) {
                                Text("${state.radiusKm} km", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Slider(value = state.radiusKm.toFloat(), onValueChange = { viewModel.onRadiusChanged(it.toInt()) }, valueRange = 1f..50f, steps = 48)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1 km", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("50 km", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Quick buttons
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ElevatedButton(
                        onClick = {
                            if (locationPermission.status.isGranted) viewModel.useCurrentLocation(context, geocoder)
                            else { pendingGps = true; locationPermission.launchPermissionRequest() }
                        },
                        modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp), enabled = !state.isLoadingGps
                    ) {
                        if (state.isLoadingGps) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.MyLocation, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Current", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    if (state.hasProfileLocation) {
                        ElevatedButton(
                            onClick = { viewModel.useProfileLocation() },
                            modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Person, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Profile", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }

                // Search
                Text("Search for a place", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it, geocoder) },
                    placeholder = { Text("City, address, or landmark...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
                    trailingIcon = {
                        when {
                            state.isSearching -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            state.searchQuery.isNotBlank() -> IconButton(onClick = { viewModel.clearSearch() }) { Icon(Icons.Default.Clear, "Clear", Modifier.size(18.dp)) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                // Suggestions
                if (state.showSuggestions) {
                    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                        Column {
                            state.searchSuggestions.forEachIndexed { index, suggestion ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { viewModel.onSuggestionSelected(suggestion) }.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Place, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(suggestion.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(suggestion.fullAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.NorthEast, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                                if (index < state.searchSuggestions.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                // Error
                state.error?.let {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ErrorOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Choose
                Button(
                    onClick = { viewModel.saveAndFinish(onLocationSelected) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Choose this location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun getZoomForRadius(radiusKm: Int): Float = when {
    radiusKm <= 1 -> 15f; radiusKm <= 3 -> 13.5f; radiusKm <= 5 -> 12.5f
    radiusKm <= 10 -> 11.5f; radiusKm <= 20 -> 10.5f; radiusKm <= 30 -> 9.5f
    radiusKm <= 50 -> 9f; else -> 8f
}