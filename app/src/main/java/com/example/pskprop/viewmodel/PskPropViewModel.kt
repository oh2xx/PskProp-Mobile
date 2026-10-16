package com.example.pskprop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pskprop.data.PskReporterClient
import com.example.pskprop.data.SampleSpotsProvider
import com.example.pskprop.model.HamBand
import com.example.pskprop.model.MapProjectionType
import com.example.pskprop.model.PropConfig
import com.example.pskprop.model.QueryDirection
import com.example.pskprop.model.Spot
import com.example.pskprop.util.MaidenheadUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PskPropViewModel(
    private val client: PskReporterClient = PskReporterClient()
) : ViewModel() {

    private val _config = MutableStateFlow(PropConfig())
    val config: StateFlow<PropConfig> = _config.asStateFlow()

    private val _spots = MutableStateFlow<List<Spot>>(emptyList())
    val spots: StateFlow<List<Spot>> = _spots.asStateFlow()

    private val _selectedSpot = MutableStateFlow<Spot?>(null)
    val selectedSpot: StateFlow<Spot?> = _selectedSpot.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        // Start with realistic sample spots immediately so UI is vibrant on launch
        val initialConfig = _config.value
        _spots.value = SampleSpotsProvider.generateSampleSpots(initialConfig.homeLat, initialConfig.homeLon)
        // Then query live PSK Reporter (merging spots)
        fetchSpots(clearExisting = false)
        startAutoRefresh()
    }

    fun updateHomeLocator(grid: String) {
        val clean = grid.trim().uppercase()
        val coords = MaidenheadUtil.maidenheadToLatLon(clean)
        if (coords != null) {
            _config.update {
                it.copy(
                    homeLocator = clean,
                    homeLat = coords.first,
                    homeLon = coords.second
                )
            }
            fetchSpots(clearExisting = true)
        }
    }

    fun updateCallsignFilter(call: String) {
        _config.update { it.copy(callsignFilter = call.trim().uppercase()) }
    }

    fun updateQueryDirection(dir: QueryDirection) {
        _config.update { it.copy(queryDirection = dir) }
        fetchSpots(clearExisting = true)
    }

    fun updateRadiusKm(radius: Double) {
        _config.update { it.copy(radiusKm = radius) }
        fetchSpots(clearExisting = false)
    }

    fun updateAgeMinutes(age: Int) {
        _config.update { it.copy(ageMinutes = age) }
        pruneAgedSpots(age)
        fetchSpots(clearExisting = false)
    }

    fun toggleBand(band: String) {
        var updatedBands: Set<String> = emptySet()
        _config.update { current ->
            val bands = current.selectedBands.toMutableSet()
            if (bands.contains(band)) {
                if (bands.size > 1) bands.remove(band) // Keep at least one
            } else {
                bands.add(band)
            }
            updatedBands = bands
            current.copy(selectedBands = bands)
        }
        // Deselect spot if its band was removed
        if (_selectedSpot.value?.let { !updatedBands.contains(it.band) } == true) {
            _selectedSpot.value = null
        }
        // Immediately prune spots for deselected bands
        pruneAgedSpots(allowedBands = updatedBands)
    }

    fun selectAllBands() {
        _config.update { it.copy(selectedBands = HamBand.DEFAULT_BANDS) }
        fetchSpots(clearExisting = false)
    }

    fun updateMapProjection(proj: MapProjectionType) {
        _config.update { it.copy(mapProjection = proj) }
    }

    fun toggleGrayline() {
        _config.update { it.copy(showGrayline = !it.showGrayline) }
    }

    fun toggleRings() {
        _config.update { it.copy(showRings = !it.showRings) }
    }

    fun toggleMapTheme() {
        _config.update { it.copy(darkMapTheme = !it.darkMapTheme) }
    }

    fun updateAutoRefresh(seconds: Int) {
        _config.update { it.copy(autoRefreshSeconds = seconds) }
        startAutoRefresh()
    }

    fun updateDotScale(scale: Float) {
        _config.update { it.copy(dotScale = scale.coerceIn(0.25f, 1.5f)) }
    }

    fun selectSpot(spot: Spot?) {
        _selectedSpot.value = spot
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Prunes any spots in the collection that are older than the specified age window
     * or that belong to deselected bands.
     */
    fun pruneAgedSpots(
        ageMinutes: Int = _config.value.ageMinutes,
        allowedBands: Set<String> = _config.value.selectedBands
    ) {
        val nowSec = System.currentTimeMillis() / 1000L
        val cutoffSec = nowSec - (ageMinutes * 60L)
        _spots.update { list ->
            list.filter { spot ->
                spot.timestampSeconds >= cutoffSec &&
                (allowedBands.isEmpty() || allowedBands.contains(spot.band))
            }
        }
        if (_selectedSpot.value?.let { !allowedBands.contains(it.band) } == true) {
            _selectedSpot.value = null
        }
    }

    /**
     * Fetches spots from PSK Reporter and merges them with existing spots, preserving
     * all reports within the configured ageMinutes parameter and active selected bands.
     */
    fun fetchSpots(clearExisting: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val cfg = _config.value
            val result = client.querySpots(cfg)

            val nowSec = System.currentTimeMillis() / 1000L
            val cutoffSec = nowSec - (cfg.ageMinutes * 60L)

            result.fold(
                onSuccess = { fetched ->
                    val current = if (clearExisting) emptyList() else _spots.value
                    val spotMap = LinkedHashMap<String, Spot>()

                    // 1. Retain all existing spots that are still within the age limit AND whose band is selected
                    for (s in current) {
                        if (s.timestampSeconds >= cutoffSec && (cfg.selectedBands.isEmpty() || cfg.selectedBands.contains(s.band))) {
                            spotMap[s.id] = s
                        }
                    }

                    // 2. Add or update with newly retrieved spots that are within age limit AND whose band is selected
                    for (s in fetched) {
                        if (s.timestampSeconds >= cutoffSec && (cfg.selectedBands.isEmpty() || cfg.selectedBands.contains(s.band))) {
                            spotMap[s.id] = s
                        }
                    }

                    if (spotMap.isNotEmpty()) {
                        _spots.value = spotMap.values.sortedByDescending { it.timestampSeconds }
                    } else if (_spots.value.isEmpty()) {
                        // Fallback to sample if completely empty on initial launch
                        _spots.value = SampleSpotsProvider.generateSampleSpots(cfg.homeLat, cfg.homeLon)
                            .filter { cfg.selectedBands.isEmpty() || cfg.selectedBands.contains(it.band) }
                    } else {
                        _spots.value = emptyList()
                    }
                    _lastUpdated.value = System.currentTimeMillis()
                },
                onFailure = { err ->
                    _errorMessage.value = "PSK Reporter: ${err.message ?: "Failed to retrieve"}"
                    // Even on network error, keep existing valid spots and prune expired/deselected ones
                    pruneAgedSpots(cfg.ageMinutes, cfg.selectedBands)
                    if (_spots.value.isEmpty()) {
                        _spots.value = SampleSpotsProvider.generateSampleSpots(cfg.homeLat, cfg.homeLon)
                            .filter { cfg.selectedBands.isEmpty() || cfg.selectedBands.contains(it.band) }
                    }
                }
            )
            _isLoading.value = false
        }
    }

    fun loadSampleDataset() {
        val cfg = _config.value
        _spots.value = SampleSpotsProvider.generateSampleSpots(cfg.homeLat, cfg.homeLon)
        _lastUpdated.value = System.currentTimeMillis()
        _errorMessage.value = null
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        val intervalSec = _config.value.autoRefreshSeconds
        if (intervalSec <= 0) return

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalSec * 1000L)
                pruneAgedSpots()
                fetchSpots(clearExisting = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
