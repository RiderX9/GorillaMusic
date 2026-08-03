package com.gorilla.music.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for the premium dark look. Every screen pulls colours,
 * radii, blur tiers and motion from here instead of hardcoding values, so the whole
 * UI stays consistent and can be retuned in one place.
 *
 * The neutral [Color] constants in `Color.kt` are kept in lockstep with these (they
 * feed the Material colour scheme); reference whichever is more convenient at the call
 * site — they resolve to the same hexes.
 */
object DesignTokens {
    val BgBase: Color @Composable get() = LocalAppColors.current.bgBase
    val BgSurface: Color @Composable get() = LocalAppColors.current.bgSurface
    val TextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
    val TextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
    val TextDisabled: Color @Composable get() = LocalAppColors.current.textDisabled

    val BgGlass = Color.White.copy(alpha = 0.10f)
    val BgGlassStrong = Color.White.copy(alpha = 0.18f)
    val BorderGlass = Color.White.copy(alpha = 0.18f)
    val GlassBorder = BorderStroke(1.dp, BorderGlass)

    // Radii
    val RadiusCard = 20.dp
    val RadiusSheet = 28.dp
    val RadiusPanel = 24.dp
    val RadiusChip = 100.dp               // pill

    // Blur tiers (dp radius for the RenderEffect Gaussian blur)
    val BlurLow = 8.dp
    val BlurMid = 16.dp
    val BlurHigh = 28.dp

    // Motion — rewired to tween to prevent physics jank.
    val SpringSnappy = androidx.compose.animation.core.tween<Float>(150, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
    val SpringBouncy = androidx.compose.animation.core.tween<Float>(200, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
}

fun blurAlphaForIntensity(intensity: BlurIntensity): Float = when (intensity) {
    BlurIntensity.LOW -> 0.04f
    BlurIntensity.MEDIUM -> 0.07f
    BlurIntensity.HIGH -> 0.13f
}

fun baseAlphaForIntensity(intensity: BlurIntensity): Float = when (intensity) {
    BlurIntensity.LOW -> 0.60f
    BlurIntensity.MEDIUM -> 0.75f
    BlurIntensity.HIGH -> 0.88f
}
