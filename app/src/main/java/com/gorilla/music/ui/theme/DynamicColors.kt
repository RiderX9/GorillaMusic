package com.gorilla.music.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors derived dynamically from the current track's album art (Palette API),
 * plus the user-chosen accent. Propagated app-wide via [LocalDynamicColors] and
 * animated whenever the track changes (see GorillaTheme).
 */
@Immutable
data class DynamicColors(
    val accent: Color = DefaultAccent,
    /** Dominant vibrant color from album art, falls back to accent. */
    val artPrimary: Color = DefaultAccent,
    /** Secondary muted color from album art. */
    val artSecondary: Color = AccentCyan,
    /** Deep background-tinted color for Now Playing wash. */
    val artBackground: Color = GorillaNight,
    /** Adaptive accent color derived from album art. */
    val adaptiveAccent: Color = Color(0xFFA78BFA),
    /** Seed color for full-scheme dynamic theming (null until art is extracted). */
    val themeSeed: Color? = null,
)

val LocalDynamicColors = compositionLocalOf { DynamicColors() }
