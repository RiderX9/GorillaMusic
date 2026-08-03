package com.gorilla.music.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorilla.music.data.settings.AccentChoice
import com.gorilla.music.data.settings.AppSettings
import com.gorilla.music.data.settings.ArtBackgroundStyle
import com.gorilla.music.data.settings.EqualizerPreset
import com.gorilla.music.data.settings.RepeatMode
import com.gorilla.music.data.settings.PlayerStyle
import com.gorilla.music.data.settings.SettingsRepository
import com.gorilla.music.playback.PlaybackTuning
import com.gorilla.music.ui.viewModelFactory
import com.gorilla.music.ui.theme.BlurIntensity
import com.gorilla.music.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** All Settings controls write through here; reads come from [settings]. */
class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val audioEffectStatus = PlaybackTuning.audioEffectStatus

    fun setThemeMode(v: ThemeMode) = launch { repo.setThemeMode(v) }
    fun setAccent(v: AccentChoice) = launch { repo.setAccent(v) }
    fun setDynamicTheme(v: Boolean) = launch { repo.setDynamicTheme(v) }
    fun setBlurIntensity(v: BlurIntensity) = launch { repo.setBlurIntensity(v) }
    fun setLiquidGlassEnabled(v: Boolean) = launch { repo.setLiquidGlassEnabled(v) }
    fun setLiquidGlassIntensity(v: BlurIntensity) = launch { repo.setLiquidGlassIntensity(v) }
    fun setArtBackground(v: ArtBackgroundStyle) = launch { repo.setArtBackground(v) }
    fun setPlayerStyle(v: PlayerStyle) = launch { repo.setPlayerStyle(v) }

    fun setDefaultShuffle(v: Boolean) = launch { repo.setDefaultShuffle(v) }
    fun setDefaultRepeat(v: RepeatMode) = launch { repo.setDefaultRepeat(v) }
    fun setCrossfadeSeconds(v: Int) = launch { repo.setCrossfadeSeconds(v) }
    fun setResumeOnOpen(v: Boolean) = launch { repo.setResumeOnOpen(v) }
    fun setLockScreenArtwork(v: Boolean) = launch { repo.setLockScreenArtwork(v) }
    fun setEqualizerEnabled(v: Boolean) = launch { repo.setEqualizerEnabled(v) }
    fun setEqualizerPreset(v: EqualizerPreset) = launch { repo.setEqualizerPreset(v) }
    fun setEqualizerBandGain(index: Int, gainDb: Int) = launch { repo.setEqualizerBandGain(index, gainDb) }
    fun resetEqualizerBands() = launch { repo.resetEqualizerBands() }
    fun setEqualizerPreampDb(v: Int) = launch { repo.setEqualizerPreampDb(v) }
    fun setBassBoostStrength(v: Int) = launch { repo.setBassBoostStrength(v) }
    fun setVirtualizerStrength(v: Int) = launch { repo.setVirtualizerStrength(v) }

    fun setLoudnessNormalization(v: Boolean) = launch { repo.setLoudnessNormalization(v) }
    fun setGaplessPlayback(v: Boolean) = launch { repo.setGaplessPlayback(v) }
    fun setResamplingHighQuality(v: Boolean) = launch { repo.setResamplingHighQuality(v) }
    private inline fun launch(crossinline block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = viewModelFactory { c -> SettingsViewModel(c.settingsRepository) }
    }
}
