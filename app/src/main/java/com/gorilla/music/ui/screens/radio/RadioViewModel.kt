package com.gorilla.music.ui.screens.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.music.data.model.RadioStation
import com.gorilla.music.data.repo.RadioRepository
import com.gorilla.music.data.settings.SettingsRepository
import com.gorilla.music.ui.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RadioUiState {
    data object Loading : RadioUiState
    data class Error(val message: String) : RadioUiState
    data class Success(val regionStations: Map<String, List<RadioStation>>) : RadioUiState {
        val stations: List<RadioStation>
            get() = regionStations.values.flatten().distinctBy { it.streamUrl }
    }
}

class RadioViewModel(
    private val radio: RadioRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<RadioUiState>(RadioUiState.Loading)
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadAllRegions(forceRefresh = false)
    }

    fun refresh() {
        loadAllRegions(forceRefresh = true)
    }

    private fun loadAllRegions(forceRefresh: Boolean) {
        viewModelScope.launch {
            if (_uiState.value !is RadioUiState.Success) {
                _uiState.value = RadioUiState.Loading
            }
            _isRefreshing.value = true

            val balkanDeferred = async { radio.fetchRegionStations("balkan", forceRefresh) }
            val europeDeferred = async { radio.fetchRegionStations("europe", forceRefresh) }
            val usaDeferred = async { radio.fetchRegionStations("usa", forceRefresh) }
            val worldwideDeferred = async { radio.fetchRegionStations("worldwide", forceRefresh) }

            val balkan = balkanDeferred.await().getOrDefault(emptyList())
            val europe = europeDeferred.await().getOrDefault(emptyList())
            val usa = usaDeferred.await().getOrDefault(emptyList())
            val worldwide = worldwideDeferred.await().getOrDefault(emptyList())

            _isRefreshing.value = false

            val regionMap = mapOf(
                "balkan" to balkan,
                "europe" to europe,
                "usa" to usa,
                "worldwide" to worldwide,
            )

            if (balkan.isEmpty() && europe.isEmpty() && usa.isEmpty() && worldwide.isEmpty()) {
                _uiState.value = RadioUiState.Error("Unable to connect to radio server. Please check internet connection.")
            } else {
                _uiState.value = RadioUiState.Success(regionMap)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory { c -> RadioViewModel(c.radioRepository, c.settingsRepository) }
    }
}
