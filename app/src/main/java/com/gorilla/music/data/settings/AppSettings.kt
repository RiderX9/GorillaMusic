package com.gorilla.music.data.settings

import androidx.compose.ui.graphics.Color
import com.gorilla.music.ui.theme.BlurIntensity
import com.gorilla.music.ui.theme.ThemeMode

/** How the Now Playing background is rendered. */
enum class ArtBackgroundStyle { BLURRED_ART, SOLID_COLOR, GRADIENT, LIVE_MESH }

/** Selects the full-screen Now Playing composition. */
enum class PlayerStyle { GORILLA, APPLE_MUSIC }

/** Repeat mode mirrored to ExoPlayer. */
enum class RepeatMode { OFF, ALL, ONE }

/** Built-in equalizer curves. Custom keeps the user's manual band levels. */
enum class EqualizerPreset(val label: String, val gains: List<Int>) {
    CUSTOM("Custom", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
    FLAT("Flat", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
    BASS_BOOST("Bass Boost", listOf(6, 5, 4, 2, 0, -1, -2, -2, -1, 0)),
    TREBLE_BOOST("Treble Boost", listOf(-2, -2, -1, 0, 1, 2, 3, 4, 5, 6)),
    VOCAL("Vocal", listOf(-2, -1, 0, 2, 4, 4, 3, 1, 0, -1)),
    ROCK("Rock", listOf(5, 4, 2, -1, -2, 1, 3, 4, 4, 3)),
    POP("Pop", listOf(-1, 2, 4, 4, 2, 0, -1, -1, 1, 2)),
    JAZZ("Jazz", listOf(3, 2, 1, 2, -1, -1, 0, 1, 3, 4)),
    CLASSICAL("Classical", listOf(4, 3, 2, 1, -1, -1, 0, 2, 4, 5)),
    ELECTRONIC("Electronic", listOf(5, 4, 1, 0, -1, 1, 2, 3, 5, 6));
}

val EqualizerUiPresets = listOf(
    EqualizerPreset.FLAT,
    EqualizerPreset.BASS_BOOST,
    EqualizerPreset.TREBLE_BOOST,
    EqualizerPreset.VOCAL,
    EqualizerPreset.ROCK,
    EqualizerPreset.POP,
    EqualizerPreset.JAZZ,
    EqualizerPreset.CLASSICAL,
    EqualizerPreset.ELECTRONIC,
)

val EqualizerPreset.shortLabel: String
    get() = when (this) {
        EqualizerPreset.BASS_BOOST -> "Bass"
        EqualizerPreset.TREBLE_BOOST -> "Treble"
        EqualizerPreset.CLASSICAL -> "Classic"
        EqualizerPreset.ELECTRONIC -> "EDM"
        else -> label
    }

val EqualizerBandLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

/** Selectable accent presets. Each seeds the full Material color scheme. */
enum class AccentChoice(val label: String, val color: Color) {
    RED("Red", Color(0xFFC62828)),
    CRIMSON("Crimson", Color(0xFFEC5464)),
    ROSE("Rose", Color(0xFFD81B60)),
    PURPLE("Purple", Color(0xFF8E24AA)),
    DEEP_PURPLE("Deep Purple", Color(0xFF5E35B1)),
    INDIGO("Indigo", Color(0xFF3949AB)),
    BLUE("Blue", Color(0xFF1E88E5)),
    SKY_BLUE("Sky Blue", Color(0xFF039BE5)),
    CYAN("Cyan", Color(0xFF00ACC1)),
    ELECTRIC_CYAN("Electric Cyan", Color(0xFF00E5FF)),
    TEAL("Teal", Color(0xFF00897B)),
    GREEN("Green", Color(0xFF43A047)),
    LIGHT_GREEN("Light Green", Color(0xFF7CB342)),
    LIME("Lime", Color(0xFFC0CA33)),
    YELLOW("Yellow", Color(0xFFFDD835)),
    AMBER("Amber", Color(0xFFFFB300)),
    ORANGE("Orange", Color(0xFFFB8C00)),
    DEEP_ORANGE("Deep Orange", Color(0xFFF4511E)),
    BROWN("Brown", Color(0xFF6D4C41)),
    BLUE_GREY("Blue Grey", Color(0xFF546E7A));
}

/** All persisted settings as a single immutable snapshot. */
data class AppSettings(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val accent: AccentChoice = AccentChoice.SKY_BLUE,
    val dynamicTheme: Boolean = false,
    val blurIntensity: BlurIntensity = BlurIntensity.MEDIUM,
    val liquidGlassEnabled: Boolean = true,
    val liquidGlassIntensity: BlurIntensity = BlurIntensity.MEDIUM,
    // Default to the live mesh to match Echo's Now Playing background out of
    // the box (Echo ships with the animated mesh, not the static blurred art).
    val artBackground: ArtBackgroundStyle = ArtBackgroundStyle.LIVE_MESH,
    val playerStyle: PlayerStyle = PlayerStyle.GORILLA,

    // Playback
    val defaultShuffle: Boolean = false,
    val defaultRepeat: RepeatMode = RepeatMode.OFF,
    val crossfadeSeconds: Int = 0,
    val resumeOnOpen: Boolean = true,
    val lockScreenArtwork: Boolean = true,
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val equalizerBandGains: List<Int> = EqualizerPreset.FLAT.gains,
    val equalizerPreampDb: Int = 0,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,

    // Audio
    val loudnessNormalization: Boolean = false,
    val gaplessPlayback: Boolean = true,
    val resamplingHighQuality: Boolean = false,

    // Resume state
    val lastTrackId: Long = -1L,
    val lastPositionMs: Long = 0L,

    // Library
    val hiddenFolders: List<String> = emptyList(),
    val customFolderOrder: List<String> = emptyList(),
)
