package com.gorilla.music.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.gorilla.music.data.settings.AccentChoice
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/** App theme mode (Settings → Appearance). AMOLED uses a black canvas with elevated controls. */
enum class ThemeMode { AUTO, LIGHT, DARK, AMOLED }

val LocalTrueLiquidGlassEnabled = staticCompositionLocalOf { false }
val LocalAccentChoice = staticCompositionLocalOf { AccentChoice.SKY_BLUE }

/**
 * Root theme. The full Material color scheme is generated from [seedColor] via
 * material-kolor — either the user's accent choice or, when Dynamic Theme is on,
 * the dominant color of the current album artwork (resolved in MainActivity).
 * Wires blur intensity into [LocalGlassTokens] and the dynamic album-art palette
 * into [LocalDynamicColors] (animated on track change). All update at runtime as
 * Settings change.
 */
@Composable
fun GorillaTheme(
    themeMode: ThemeMode,
    seedColor: Color,
    accentChoice: AccentChoice = AccentChoice.SKY_BLUE,
    blurIntensity: BlurIntensity,
    liquidGlassEnabled: Boolean = false,
    liquidGlassIntensity: BlurIntensity = BlurIntensity.MEDIUM,
    dynamicColors: DynamicColors,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val amoled = themeMode == ThemeMode.AMOLED

    // Smoothly animate the palette colors when the track changes.
    val animSeed by animateColorAsState(seedColor, tween(800), label = "seed")
    val animAccent by animateColorAsState(dynamicColors.accent, tween(800), label = "accent")
    val animPrimary by animateColorAsState(dynamicColors.artPrimary, SpringSpecs.ColorSpring, label = "artPrimary")
    val animSecondary by animateColorAsState(dynamicColors.artSecondary, SpringSpecs.ColorSpring, label = "artSecondary")
    val animBackground by animateColorAsState(dynamicColors.artBackground, SpringSpecs.ColorSpring, label = "artBg")

    // material-kolor supplies the AMOLED scheme; app surface tokens preserve visible elevation.
    val scheme = rememberDynamicColorScheme(
        seedColor = animSeed,
        isDark = isDark,
        isAmoled = amoled,
        style = PaletteStyle.TonalSpot,
    )

    val tokens = remember(blurIntensity, isDark) {
        GlassTokens(intensity = blurIntensity, isDark = isDark)
    }
    val glassEffectConfig = remember(liquidGlassEnabled, liquidGlassIntensity) {
        GlassEffectConfig(
            globalEnabled = liquidGlassEnabled,
            blurRadius = when (liquidGlassIntensity) {
                BlurIntensity.LOW -> 4f
                BlurIntensity.MEDIUM -> 8f
                BlurIntensity.HIGH -> 12f
            },
        )
    }

    val appColors = remember(scheme, isDark, amoled) {
        appColorsFrom(scheme, isDark, amoled)
    }
    val themedGlassEffectConfig = remember(glassEffectConfig, scheme) {
        glassEffectConfig.copy(
            surfaceTintColor = scheme.surfaceContainer,
            textColor = scheme.onSurface,
        )
    }

    val animatedDynamic = DynamicColors(
        accent = animAccent,
        artPrimary = animPrimary,
        artSecondary = animSecondary,
        artBackground = animBackground,
        adaptiveAccent = dynamicColors.adaptiveAccent,
        themeSeed = dynamicColors.themeSeed,
    )

    CompositionLocalProvider(
        LocalGlassTokens provides tokens,
        LocalTrueLiquidGlassEnabled provides liquidGlassEnabled,
        LocalGlassEffectConfig provides themedGlassEffectConfig,
        LocalAccentChoice provides accentChoice,
        LocalDynamicColors provides animatedDynamic,
        LocalAppColors provides appColors,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = GorillaTypography,
            shapes = GorillaShapes,
            content = content,
        )
    }
}
