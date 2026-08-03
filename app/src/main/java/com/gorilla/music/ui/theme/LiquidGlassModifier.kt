/**
 * Liquid Glass effect ported from Echo Music.
 *
 * Echo Music is licensed under GPL-3.0. Its rendering backend is a modified,
 * source-vendored version of Kyant0/backdrop 2.0.0 (Apache-2.0).
 */
package com.gorilla.music.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gorilla.music.ui.liquidglass.backdrop.Backdrop
import com.gorilla.music.ui.liquidglass.backdrop.drawBackdrop
import com.gorilla.music.ui.liquidglass.backdrop.effects.blur
import com.gorilla.music.ui.liquidglass.backdrop.effects.colorControls
import com.gorilla.music.ui.liquidglass.backdrop.effects.lens
import com.gorilla.music.ui.liquidglass.backdrop.highlight.Highlight
import com.gorilla.music.ui.liquidglass.backdrop.shadow.Shadow

@Stable
data class GlassEffectConfig(
    val globalEnabled: Boolean = false,
    val vibrancy: Float = 1f,
    val blurRadius: Float = 8f,
    val lensHeight: Float = 0.5f,
    val lensAmount: Float = 0.5f,
    val chromaticAberration: Boolean = true,
    val depthEffect: Boolean = true,
    val surfaceTintColor: Color = Color.Unspecified,
    val surfaceOpacity: Float = 0.4f,
    val textColor: Color = Color.Unspecified,
)

private const val LensMaxDp = 48f
private const val MinGlassResolutionScale = 0.33f
private const val FullQualityBlurDp = 8f

val LocalGlassEffectConfig = staticCompositionLocalOf { GlassEffectConfig() }

fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= Build.VERSION_CODES.S

fun glassSaturation(vibrancy: Float): Float =
    1f + 0.5f * vibrancy.coerceIn(0f, 2f)

fun glassResolutionScale(blurRadiusDp: Float): Float {
    val progress = (blurRadiusDp / FullQualityBlurDp).coerceIn(0f, 1f)
    return 1f - progress * (1f - MinGlassResolutionScale)
}

@Composable
fun glassContentColor(config: GlassEffectConfig = LocalGlassEffectConfig.current): Color {
    if (config.textColor.isSpecified) return config.textColor
    return if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}

/**
 * Echo Music's Liquid Glass recipe. The backdrop is processed at a blur-dependent
 * resolution, then receives color controls, blur, optional Android 13+ lens
 * refraction, a highlight rim, a shadow, and an adaptive surface tint.
 */
@Composable
fun Modifier.liquidGlass(
    config: GlassEffectConfig = LocalGlassEffectConfig.current,
    backdrop: Backdrop?,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    applyEdgeEffects: Boolean = true,
    applyShadow: Boolean = applyEdgeEffects,
    blurRadiusDp: Float = config.blurRadius,
): Modifier {
    if (!config.globalEnabled || !isGlassSupported() || backdrop == null) return this

    val density = LocalDensity.current
    val resolutionScale = glassResolutionScale(blurRadiusDp)
    val blurPx = with(density) { blurRadiusDp.dp.toPx() } * resolutionScale
    val saturation = glassSaturation(config.vibrancy)
    val lensHeightPx =
        with(density) { (config.lensHeight * LensMaxDp).dp.toPx() } * resolutionScale
    val lensAmountPx =
        with(density) { (config.lensAmount * LensMaxDp).dp.toPx() } * resolutionScale
    val surfaceTintColor = if (config.surfaceTintColor.isSpecified) {
        config.surfaceTintColor
    } else if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF121212)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (saturation != 1f) {
                colorControls(saturation = saturation)
            }
            if (blurPx > 0f) {
                blur(blurPx)
            }
            if (
                applyEdgeEffects &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (lensHeightPx > 0f || lensAmountPx > 0f)
            ) {
                lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = config.depthEffect,
                    chromaticAberration = config.chromaticAberration,
                )
            }
        },
        highlight = if (applyEdgeEffects) ({ Highlight.Default }) else null,
        shadow = if (applyShadow) ({ Shadow.Default }) else null,
        onDrawSurface = {
            if (config.surfaceOpacity > 0f) {
                drawRect(
                    color = surfaceTintColor.copy(alpha = config.surfaceOpacity),
                    size = size,
                )
            }
        },
        backdropScale = resolutionScale,
    )
}
