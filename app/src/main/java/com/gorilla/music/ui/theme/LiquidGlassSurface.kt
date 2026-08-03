package com.gorilla.music.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gorilla.music.ui.liquidglass.backdrop.Backdrop

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * True Liquid Glass material system replacing flat glassmorphism.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    depth: GlassDepth = GlassDepth.MID,
    shape: RoundedCornerShape = CardShape,
    tint: Color = LocalAppColors.current.glassTint,
    surfaceColor: Color? = null,
    blurContent: Painter? = null,
    border: Boolean = true,
    tintAlphaOverride: Float? = null,
    saturationOverride: Float? = null,
    shadow: Boolean = true,
    enableLens: Boolean = true,
    useBackdrop: Boolean = true,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val trueBackdrop = if (useBackdrop) {
        backdrop ?: LocalLiquidGlassContentBackdrop.current ?: LocalLiquidGlassBackdrop.current
    } else {
        null
    }
    val glassConfig = LocalGlassEffectConfig.current
    val useTrueLiquidGlass = glassConfig.globalEnabled && isGlassSupported() && trueBackdrop != null
    if (useTrueLiquidGlass) {
        val trueGlassColor = surfaceColor ?: LocalAppColors.current.bgSurface
        val trueGlassConfig = glassConfig.copy(
            surfaceTintColor = trueGlassColor.copy(alpha = 1f),
            surfaceOpacity = surfaceColor?.alpha ?: glassConfig.surfaceOpacity,
        )
        TrueLiquidGlassSurface(
            modifier = modifier,
            shape = shape,
            backdrop = trueBackdrop,
            config = trueGlassConfig,
            shadow = shadow,
            content = content,
        )
        return
    }

    val tokens = LocalGlassTokens.current
    val appColors = LocalAppColors.current
    val isDark = appColors.isDark
    val intensity = LocalGlassTokens.current.intensity
    val defaultSurfaceColor = appColors.bgSurface.copy(alpha = 0.72f)
    val resolvedSurfaceColor = surfaceColor ?: defaultSurfaceColor
    val usesDefaultSurfaceColor = resolvedSurfaceColor == defaultSurfaceColor
    val computedTintAlpha = blurAlphaForIntensity(intensity)
    val computedBaseAlpha = baseAlphaForIntensity(intensity)

    val isTransparent = resolvedSurfaceColor.alpha == 0f

    // Unified base fill color: base fill Color.White.copy(alpha = 0.70f) in light mode
    val baseFillColor = if (isTransparent) {
        Color.Transparent
    } else if (!isDark) {
        resolvedSurfaceColor
    } else {
        if (usesDefaultSurfaceColor) {
            appColors.bgSurface.copy(alpha = computedBaseAlpha)
        } else {
            resolvedSurfaceColor
        }
    }
    
    // Unified tint overlay
    val tintAlpha = if (isTransparent) 0f else (tintAlphaOverride ?: computedTintAlpha)
    
    // Unified two-tier shadow alpha
    val contactAlpha = if (isTransparent || !shadow) 0f else 0.25f
    val ambientAlpha = if (isTransparent || !shadow) 0f else 0.12f
    val isOpaque = resolvedSurfaceColor.alpha >= 0.99f

    Box(
        modifier = modifier
            .then(
                if (shadow && isOpaque) Modifier.shadow(
                    elevation = 16.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = ambientAlpha),
                    spotColor = Color.Black.copy(alpha = contactAlpha)
                ) else Modifier
            )
            .drawBehind {
                if (shadow && !isOpaque && !isTransparent) {
                    drawIntoCanvas { canvas ->
                        val paint = Paint()
                        paint.color = Color.Transparent
                        val frameworkPaint = paint.asFrameworkPaint()

                        // Ambient Shadow (16dp, 32dp blur) at 0.12f alpha
                        if (ambientAlpha > 0f) {
                            frameworkPaint.color = android.graphics.Color.TRANSPARENT
                            frameworkPaint.setShadowLayer(32.dp.toPx(), 0f, 16.dp.toPx(), Color.Black.copy(alpha = ambientAlpha).toArgb())
                            canvas.drawRoundRect(0f, 0f, size.width, size.height, shape.topStart.toPx(size, this), shape.topStart.toPx(size, this), paint)
                        }

                        // Contact Shadow (4dp, 8dp blur) at 0.25f alpha
                        if (contactAlpha > 0f) {
                            frameworkPaint.color = android.graphics.Color.TRANSPARENT
                            frameworkPaint.setShadowLayer(8.dp.toPx(), 0f, 4.dp.toPx(), Color.Black.copy(alpha = contactAlpha).toArgb())
                            canvas.drawRoundRect(0f, 0f, size.width, size.height, shape.topStart.toPx(size, this), shape.topStart.toPx(size, this), paint)
                        }
                    }
                }
            }
            .clip(shape)
    ) {
        val boxScope = this
        
        // 0. Base translucent body
        Box(
            Modifier
                .matchParentSize()
                .background(baseFillColor)
        )

        // Layer 1: Refraction base (stronger blur + saturation/hue shift) - Unified 20dp blur
        if (blurContent != null) {
            Image(
                painter = blurContent,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val radius = 20.dp
                        val overscanPx = radius.toPx() * 1.5f
                        scaleX = (size.width + overscanPx * 2f) / size.width.coerceAtLeast(1f)
                        scaleY = (size.height + overscanPx * 2f) / size.height.coerceAtLeast(1f)
                        
                        val px = radius.toPx()
                        if (px > 0.5f) {
                            val blurEffect = RenderEffect.createBlurEffect(px, px, Shader.TileMode.DECAL)
                            val matrix = ColorMatrix().apply {
                                setSaturation(saturationOverride ?: 1.80f)
                            }
                            val colorEffect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))
                            renderEffect = RenderEffect.createChainEffect(blurEffect, colorEffect).asComposeRenderEffect()
                        }
                        clip = false
                    }
            )
        }

        // Layer 2: Tint overlay
        val overlayColor = tint.copy(
            alpha = if (!isDark) tintAlphaOverride ?: 0.04f else tintAlpha
        )
        Box(
            Modifier
                .matchParentSize()
                .background(overlayColor)
        )

        // Layer 3: Specular highlight (top-left linear gradient to 35% of height)
        if (!isTransparent) {
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (!isDark) 0.6f else 0.18f),
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height * 0.35f)
                        )
                        drawRoundRect(
                            brush = brush,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.topStart.toPx(size, this))
                        )
                    }
            )
        }

        // Layer 4: Uniform edge definition border (dual-tone: top/left 35% white, bottom/right 20% black)
        if (border && !isTransparent) {
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val halfStroke = strokeWidth / 2f
                        val startColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.8f)
                        val endColor = if (isDark) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
                        val brush = Brush.linearGradient(
                            colors = listOf(startColor, endColor),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(halfStroke, halfStroke),
                            size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.topStart.toPx(size, this)),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                        )
                    }
            )
        }

        // Content
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
        ) {
            boxScope.content()
        }
    }
}

@Composable
private fun TrueLiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CardShape,
    backdrop: Backdrop? = null,
    config: GlassEffectConfig = LocalGlassEffectConfig.current,
    shadow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .liquidGlass(
                config = config,
                backdrop = backdrop,
                shape = shape,
                applyShadow = shadow,
            )
    ) {
        val boxScope = this

        CompositionLocalProvider(
            LocalContentColor provides glassContentColor(config),
        ) {
            boxScope.content()
        }
    }
}

/**
 * Applies a real Gaussian [RenderEffect] blur to this layer (Android 12+). Radius near
 * zero falls back to no effect.
 *
 * IMPORTANT: this does NOT clip. Clipping the blurred layer to the exact visible bounds
 * is what produces the visible "ghost ring" halo — the [Shader.TileMode] kernel samples
 * past the edge and the hard clip slices it into a ring. Callers must instead let the
 * blurred content overflow a padded/oversized layer and clip to the final shape with a
 * separate `Modifier.clip(shape)` applied AFTER this blur (see [LiquidGlassSurface], which
 * oversizes the backdrop and clips on its outer Box). [TileMode.DECAL] also lets the
 * kernel fade to transparent past the source edge instead of smearing a clamped band.
 */
fun Modifier.renderEffectBlur(radius: Dp): Modifier = this.graphicsLayer {
    val px = radius.toPx()
    renderEffect = if (px <= 0.5f) {
        null
    } else {
        RenderEffect
            .createBlurEffect(px, px, Shader.TileMode.DECAL)
            .asComposeRenderEffect()
    }
    clip = false
}
