package com.gorilla.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gorilla.music.data.model.Track
import com.gorilla.music.data.repo.albumArtUri
import com.gorilla.music.data.settings.ArtBackgroundStyle
import com.gorilla.music.ui.theme.CapsuleShape
import com.gorilla.music.ui.theme.GlassDepth
import com.gorilla.music.ui.theme.LiquidGlassSurface
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.music.ui.theme.LocalTrueLiquidGlassEnabled
import com.gorilla.music.ui.theme.SpringSpecs
import com.gorilla.music.ui.theme.glassContentColor
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private data class PolygonCookieShape(
    val sides: Int,
    val indent: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val maxRadius = size.minDimension / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val steps = 120

        for (step in 0..steps) {
            val angle = step * Math.PI * 2 / steps
            val radius = maxRadius * (1f - indent + indent * cos(sides * angle))
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            if (step == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Echo Music's current mini-player design adapted to Gorilla's local playback model.
 * The expanded accessory is 64dp tall; the inline form is used when the floating
 * navigation bar collapses while scrolling.
 */
@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    backgroundStyle: ArtBackgroundStyle,
    dynamicThemeEnabled: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val liquidGlassEnabled = LocalTrueLiquidGlassEnabled.current
    val contentBackdrop = LocalLiquidGlassContentBackdrop.current
    val glassTextColor = glassContentColor()
    val usesArtworkBackground = dynamicThemeEnabled && !liquidGlassEnabled
    val contentColor = when {
        liquidGlassEnabled -> glassTextColor
        usesArtworkBackground -> Color.White
        appColors.isDark -> Color.White
        else -> appColors.textPrimary
    }
    val progressColor = if (usesArtworkBackground) Color.White else accent

    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var dragStartedAt by remember { mutableLongStateOf(0L) }
    var draggedDistance by remember { mutableFloatStateOf(0f) }
    val swipeSpring = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        )
    }
    val height = if (compact) 52.dp else 64.dp
    val artworkSize = if (compact) 36.dp else 48.dp
    val artworkInnerSize = if (compact) 30.dp else 40.dp
    val controlSize = if (compact) 34.dp else 40.dp

    LiquidGlassSurface(
        depth = GlassDepth.HIGH,
        shape = CapsuleShape,
        surfaceColor = if (liquidGlassEnabled) {
            appColors.bgSurface.copy(alpha = 0.40f)
        } else if (usesArtworkBackground) {
            Color.Transparent
        } else {
            appColors.bgSurface
        },
        border = true,
        backdrop = contentBackdrop,
        shadow = !usesArtworkBackground && appColors.isDark,
        modifier = modifier
            .height(height)
            .pressScale(interaction)
            .pointerInput(track.id, layoutDirection) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragStartedAt = System.currentTimeMillis()
                        draggedDistance = 0f
                    },
                    onDragCancel = {
                        coroutineScope.launch { offsetX.animateTo(0f, swipeSpring) }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val adjusted = if (layoutDirection == LayoutDirection.Rtl) {
                            -dragAmount
                        } else {
                            dragAmount
                        }
                        draggedDistance += abs(adjusted)
                        coroutineScope.launch {
                            offsetX.snapTo((offsetX.value + adjusted).coerceIn(-220f, 220f))
                        }
                    },
                    onDragEnd = {
                        val duration = (System.currentTimeMillis() - dragStartedAt).coerceAtLeast(1L)
                        val velocity = draggedDistance / duration
                        val shouldSkip = abs(offsetX.value) > 72f || velocity > 1.15f
                        if (shouldSkip) {
                            haptic()
                            if (offsetX.value > 0f) onPrevious() else onNext()
                        }
                        coroutineScope.launch { offsetX.animateTo(0f, swipeSpring) }
                    },
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) {
                haptic()
                onExpand()
            },
    ) {
        if (usesArtworkBackground) {
            MiniPlayerBackground(
                track = track,
                style = backgroundStyle,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CapsuleShape)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(horizontal = if (compact) 8.dp else 10.dp),
        ) {
            CircularArtwork(
                track = track,
                progress = progress,
                accent = progressColor,
                contentColor = contentColor,
                size = artworkSize,
                innerSize = artworkInnerSize,
            )

            if (compact) {
                Text(
                    text = track.title,
                    color = contentColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = track.title,
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(
                            iterations = 1,
                            initialDelayMillis = 3000,
                            velocity = 30.dp,
                        ),
                    )
                    Text(
                        text = track.displayArtist,
                        color = contentColor.copy(alpha = 0.70f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (usesArtworkBackground) {
                ExpressivePlayPauseControl(
                    isPlaying = isPlaying,
                    size = if (compact) controlSize else 48.dp,
                    onClick = {
                        haptic()
                        onPlayPause()
                    },
                )
            } else {
                MiniControl(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    tint = contentColor,
                    size = controlSize,
                    onClick = {
                        haptic()
                        onPlayPause()
                    },
                )
            }
            if (!compact) {
                MiniControl(
                    icon = Icons.Rounded.SkipNext,
                    tint = contentColor,
                    size = controlSize,
                    onClick = {
                        haptic()
                        onNext()
                    },
                )
            }
        }

        if (abs(offsetX.value) > 48f) {
            Icon(
                imageVector = if (offsetX.value > 0f) {
                    Icons.Rounded.SkipPrevious
                } else {
                    Icons.Rounded.SkipNext
                },
                contentDescription = null,
                tint = contentColor.copy(alpha = (abs(offsetX.value) / 120f).coerceIn(0f, 1f)),
                modifier = Modifier
                    .align(if (offsetX.value > 0f) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 18.dp),
            )
        }
    }
}

@Composable
private fun MiniPlayerBackground(
    track: Track,
    style: ArtBackgroundStyle,
) {
    val context = LocalContext.current
    val colors = LocalDynamicColors.current
    val artwork = track.artworkUri ?: albumArtUri(track.albumId)

    when (style) {
        ArtBackgroundStyle.SOLID_COLOR -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.artBackground)
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        }

        ArtBackgroundStyle.GRADIENT -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                colors.artPrimary,
                                colors.artSecondary,
                                colors.artBackground,
                            )
                        )
                    )
                    .background(Color.Black.copy(alpha = 0.34f))
            )
        }

        ArtBackgroundStyle.BLURRED_ART -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.artBackground)
            )
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artwork)
                    .size(160, 160)
                    .allowHardware(false)
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.35f
                        scaleY = 1.35f
                    }
                    .blur(24.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f))
            )
        }

        ArtBackgroundStyle.LIVE_MESH -> {
            val transition = rememberInfiniteTransition(label = "miniLiveMesh")
            val rotation by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(60_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "miniLiveMeshRotation",
            )
            val colorFilter = remember {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.7f) })
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                colors.artPrimary,
                                colors.artSecondary,
                                colors.artBackground,
                            )
                        )
                    )
            )
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artwork)
                    .size(160, 160)
                    .allowHardware(false)
                    .crossfade(400)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.65f
                        scaleY = 1.65f
                        rotationZ = rotation
                    }
                    .blur(32.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
            )
        }
    }
}

@Composable
private fun CircularArtwork(
    track: Track,
    progress: Float,
    accent: Color,
    contentColor: Color,
    size: androidx.compose.ui.unit.Dp,
    innerSize: androidx.compose.ui.unit.Dp,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = SpringSpecs.Smooth,
        label = "miniArtworkProgress",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .drawWithContent {
                drawContent()
                val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                val diameter = this.size.minDimension
                val topLeft = Offset(
                    x = (this.size.width - diameter) / 2f,
                    y = (this.size.height - diameter) / 2f,
                )
                drawArc(
                    color = contentColor.copy(alpha = 0.20f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = stroke,
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = stroke,
                )
            },
    ) {
        AlbumArt(
            albumId = track.albumId,
            artworkUri = track.artworkUri,
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape),
            shape = CircleShape,
        )
    }
}

@Composable
private fun MiniControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
    }
}

@Composable
private fun ExpressivePlayPauseControl(
    isPlaying: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val cookieIndent by animateFloatAsState(
        targetValue = if (isPlaying) 0.08f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "miniCookieIndent",
    )
    val transition = rememberInfiniteTransition(label = "miniCookieRotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "miniCookieRotationAngle",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                    clip = true
                    shape = PolygonCookieShape(sides = 9, indent = cookieIndent)
                }
                .background(Color.White),
        )
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black.copy(alpha = 0.88f),
            modifier = Modifier.size(if (size >= 48.dp) 24.dp else 20.dp),
        )
    }
}
