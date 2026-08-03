package com.gorilla.music.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.preferencesDataStore
import com.gorilla.music.ui.theme.BlurIntensity
import com.gorilla.music.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gorilla_settings")

internal data class EqualizerResetState(
    val preset: EqualizerPreset,
    val bands: List<Int>,
    val preampDb: Int,
    val bassBoostStrength: Int,
    val virtualizerStrength: Int,
    val loudnessNormalization: Boolean,
)

internal fun flatEqualizerResetState() = EqualizerResetState(
    preset = EqualizerPreset.FLAT,
    bands = EqualizerPreset.FLAT.gains,
    preampDb = 0,
    bassBoostStrength = 0,
    virtualizerStrength = 0,
    loudnessNormalization = false,
)

/**
 * Single source of truth for all user settings, persisted with DataStore. Every
 * Settings screen control reads from [settings] and writes through one of the
 * update methods; changes propagate immediately to the UI and the playback engine.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val dynamicTheme = booleanPreferencesKey("dynamic_theme")
        val blur = stringPreferencesKey("blur_intensity")
        val liquidGlass = booleanPreferencesKey("liquid_glass")
        val liquidGlassIntensity = stringPreferencesKey("liquid_glass_intensity")
        val artBg = stringPreferencesKey("art_background")
        val playerStyle = stringPreferencesKey("player_style")
        val defaultShuffle = booleanPreferencesKey("default_shuffle")
        val defaultRepeat = stringPreferencesKey("default_repeat")
        val crossfade = intPreferencesKey("crossfade_seconds")
        val resumeOnOpen = booleanPreferencesKey("resume_on_open")
        val lockArtwork = booleanPreferencesKey("lock_artwork")
        val eqEnabled = booleanPreferencesKey("eq_enabled")
        val eqPreset = stringPreferencesKey("eq_preset")
        val eqBands = stringPreferencesKey("eq_bands")
        val eqPreamp = intPreferencesKey("eq_preamp")
        val bassBoost = intPreferencesKey("bass_boost")
        val virtualizer = intPreferencesKey("virtualizer")
        val loudness = booleanPreferencesKey("loudness_norm")
        val gapless = booleanPreferencesKey("gapless")
        val resampleHq = booleanPreferencesKey("resample_hq")
        val lastTrackId = longPreferencesKey("last_track_id")
        val lastPositionMs = longPreferencesKey("last_position_ms")
        val hiddenFolders = stringPreferencesKey("hidden_folders")
        val customFolderOrder = stringPreferencesKey("custom_folder_order")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val storedAccent = p[Keys.accent]
        AppSettings(
            themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.AUTO,
            accent = migrateAccent(storedAccent),
            // Users who had the old art-adaptive accent selected get the new
            // full-scheme dynamic theme instead.
            dynamicTheme = p[Keys.dynamicTheme] ?: (storedAccent == "ADAPTIVE"),
            blurIntensity = p[Keys.blur]?.let { runCatching { BlurIntensity.valueOf(it) }.getOrNull() } ?: BlurIntensity.MEDIUM,
            liquidGlassEnabled = p[Keys.liquidGlass] ?: true,
            liquidGlassIntensity = p[Keys.liquidGlassIntensity]?.let { runCatching { BlurIntensity.valueOf(it) }.getOrNull() } ?: BlurIntensity.MEDIUM,
            artBackground = p[Keys.artBg]?.let { runCatching { ArtBackgroundStyle.valueOf(it) }.getOrNull() } ?: ArtBackgroundStyle.LIVE_MESH,
            playerStyle = p[Keys.playerStyle]?.let { runCatching { PlayerStyle.valueOf(it) }.getOrNull() } ?: PlayerStyle.GORILLA,
            defaultShuffle = p[Keys.defaultShuffle] ?: false,
            defaultRepeat = p[Keys.defaultRepeat]?.let { runCatching { RepeatMode.valueOf(it) }.getOrNull() } ?: RepeatMode.OFF,
            crossfadeSeconds = p[Keys.crossfade] ?: 0,
            resumeOnOpen = p[Keys.resumeOnOpen] ?: true,
            lockScreenArtwork = p[Keys.lockArtwork] ?: true,
            equalizerEnabled = p[Keys.eqEnabled] ?: false,
            equalizerPreset = p[Keys.eqPreset]?.let { runCatching { EqualizerPreset.valueOf(it) }.getOrNull() } ?: EqualizerPreset.FLAT,
            equalizerBandGains = decodeEqBands(p[Keys.eqBands]),
            equalizerPreampDb = (p[Keys.eqPreamp] ?: 0).coerceIn(-12, 12),
            bassBoostStrength = (p[Keys.bassBoost] ?: 0).coerceIn(0, 100),
            virtualizerStrength = (p[Keys.virtualizer] ?: 0).coerceIn(0, 100),
            loudnessNormalization = p[Keys.loudness] ?: false,
            gaplessPlayback = p[Keys.gapless] ?: true,
            resamplingHighQuality = p[Keys.resampleHq] ?: false,
            lastTrackId = p[Keys.lastTrackId] ?: -1L,
            lastPositionMs = p[Keys.lastPositionMs] ?: 0L,
            hiddenFolders = p[Keys.hiddenFolders]?.split("|||")?.filter { it.isNotBlank() } ?: emptyList(),
            customFolderOrder = p[Keys.customFolderOrder]?.split("|||")?.filter { it.isNotBlank() } ?: emptyList(),
        )
    }

    suspend fun setThemeMode(v: ThemeMode) = edit { it[Keys.themeMode] = v.name }
    suspend fun setAccent(v: AccentChoice) = edit { it[Keys.accent] = v.name }
    suspend fun setDynamicTheme(v: Boolean) = edit { it[Keys.dynamicTheme] = v }
    suspend fun setBlurIntensity(v: BlurIntensity) = edit { it[Keys.blur] = v.name }
    suspend fun setLiquidGlassEnabled(v: Boolean) = edit { it[Keys.liquidGlass] = v }
    suspend fun setLiquidGlassIntensity(v: BlurIntensity) = edit { it[Keys.liquidGlassIntensity] = v.name }
    suspend fun setArtBackground(v: ArtBackgroundStyle) = edit { it[Keys.artBg] = v.name }
    suspend fun setPlayerStyle(v: PlayerStyle) = edit { it[Keys.playerStyle] = v.name }

    suspend fun setDefaultShuffle(v: Boolean) = edit { it[Keys.defaultShuffle] = v }
    suspend fun setDefaultRepeat(v: RepeatMode) = edit { it[Keys.defaultRepeat] = v.name }
    suspend fun setCrossfadeSeconds(v: Int) = edit { it[Keys.crossfade] = v.coerceIn(0, 10) }
    suspend fun setResumeOnOpen(v: Boolean) = edit { it[Keys.resumeOnOpen] = v }
    suspend fun setLockScreenArtwork(v: Boolean) = edit { it[Keys.lockArtwork] = v }
    suspend fun setEqualizerEnabled(v: Boolean) = edit { it[Keys.eqEnabled] = v }
    suspend fun setEqualizerPreset(v: EqualizerPreset) = edit {
        it[Keys.eqPreset] = v.name
        if (v != EqualizerPreset.CUSTOM) it[Keys.eqBands] = encodeEqBands(v.gains)
    }
    suspend fun setEqualizerBandGain(index: Int, gainDb: Int) = edit { prefs ->
        val bands = decodeEqBands(prefs[Keys.eqBands]).toMutableList()
        if (index in bands.indices) {
            bands[index] = gainDb.coerceIn(-12, 12)
            prefs[Keys.eqBands] = encodeEqBands(bands)
            prefs[Keys.eqPreset] = EqualizerPreset.CUSTOM.name
        }
    }
    suspend fun resetEqualizerBands() = edit {
        val reset = flatEqualizerResetState()
        it[Keys.eqPreset] = reset.preset.name
        it[Keys.eqBands] = encodeEqBands(reset.bands)
        it[Keys.eqPreamp] = reset.preampDb
        it[Keys.bassBoost] = reset.bassBoostStrength
        it[Keys.virtualizer] = reset.virtualizerStrength
        it[Keys.loudness] = reset.loudnessNormalization
    }
    suspend fun setEqualizerPreampDb(v: Int) = edit { it[Keys.eqPreamp] = v.coerceIn(-12, 12) }
    suspend fun setBassBoostStrength(v: Int) = edit { it[Keys.bassBoost] = v.coerceIn(0, 100) }
    suspend fun setVirtualizerStrength(v: Int) = edit { it[Keys.virtualizer] = v.coerceIn(0, 100) }

    suspend fun setLoudnessNormalization(v: Boolean) = edit { it[Keys.loudness] = v }
    suspend fun setGaplessPlayback(v: Boolean) = edit { it[Keys.gapless] = v }
    suspend fun setResamplingHighQuality(v: Boolean) = edit { it[Keys.resampleHq] = v }
    suspend fun setResumeState(trackId: Long, positionMs: Long) = edit {
        it[Keys.lastTrackId] = trackId
        it[Keys.lastPositionMs] = positionMs
    }

    suspend fun setHiddenFolders(v: List<String>) = edit {
        it[Keys.hiddenFolders] = v.joinToString("|||")
    }

    suspend fun setCustomFolderOrder(v: List<String>) = edit {
        it[Keys.customFolderOrder] = v.joinToString("|||")
    }

    private suspend fun edit(block: suspend (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    /** Maps legacy accent names (pre color-palette rework) to their closest new entry. */
    private fun migrateAccent(stored: String?): AccentChoice = when (stored) {
        null -> AccentChoice.SKY_BLUE
        "ADAPTIVE" -> AccentChoice.SKY_BLUE
        "VIOLET" -> AccentChoice.DEEP_PURPLE
        "MINT" -> AccentChoice.TEAL
        "GREY" -> AccentChoice.BLUE_GREY
        else -> runCatching { AccentChoice.valueOf(stored) }.getOrNull() ?: AccentChoice.SKY_BLUE
    }

    private fun decodeEqBands(raw: String?): List<Int> {
        val parsed = raw
            ?.split(',')
            ?.mapNotNull { it.toIntOrNull()?.coerceIn(-12, 12) }
            .orEmpty()
        return if (parsed.size == EqualizerPreset.FLAT.gains.size) parsed else EqualizerPreset.FLAT.gains
    }

    private fun encodeEqBands(bands: List<Int>): String =
        bands.take(EqualizerPreset.FLAT.gains.size).joinToString(",") { it.coerceIn(-12, 12).toString() }

}
