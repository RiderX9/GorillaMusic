package com.gorilla.music.ui.components

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

@Composable
fun BlurredArtworkBackground(
    artwork: Any?,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 64.dp,
    overlayAlpha: Float = 0.44f,
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artwork)
                .size(512, 512)
                .allowHardware(false)
                .crossfade(400)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.25f
                    scaleY = 1.25f
                }
                .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.30f),
                        )
                    )
                )
        )
    }
}

/**
 * 1:1 Port of Accord's exact BlendView background
 * (org.akanework.gramophone.ui.components.BlendView):
 * - Enhances saturation 2.5x (SATURATION_FACTOR = 2.5f)
 * - Crops top-left quarter (type1) and bottom-right quarter (type3)
 * - Rotates type1 (+1.2 deg/frame), type3 (+0.67 deg/frame), and container (-0.6 deg/frame)
 * - Applies 80dp RenderEffect blur (FULL_BLUR_RADIUS = 80f)
 * - Scaled 2.2x so rotating corners never expose screen edges
 * - 400ms crossfade animation on track change
 */
@Composable
fun LiveMeshBackground(artwork: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var baseBlend by remember { mutableStateOf<AccordBlendBitmaps?>(null) }
    var overlayBlend by remember { mutableStateOf<AccordBlendBitmaps?>(null) }
    val overlayAlpha = remember { Animatable(0f) }
    val loadedBlends = remember { Channel<AccordBlendBitmaps>(Channel.CONFLATED) }

    LaunchedEffect(artwork) {
        if (artwork == null) return@LaunchedEffect
        if (baseBlend?.source == artwork || overlayBlend?.source == artwork) {
            return@LaunchedEffect
        }

        val request = ImageRequest.Builder(context)
            .data(artwork)
            .size(256, 256)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        if (result is SuccessResult) {
            val originalBitmap = result.drawable.toBitmap()
            withContext(Dispatchers.Default) {
                val enhanced = enhanceBitmap(originalBitmap)
                val topLeft = cropTopLeftQuarter(enhanced)
                val bottomRight = cropBottomRightQuarter(enhanced)

                loadedBlends.trySend(
                    AccordBlendBitmaps(
                        source = artwork,
                        bgBitmap = enhanced.asImageBitmap(),
                        topLeftBitmap = topLeft.asImageBitmap(),
                        bottomRightBitmap = bottomRight.asImageBitmap(),
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        for (loadedBlend in loadedBlends) {
            if (baseBlend?.source == loadedBlend.source) continue

            if (baseBlend == null) {
                baseBlend = loadedBlend
            } else {
                overlayBlend = loadedBlend
                overlayAlpha.snapTo(0f)
                overlayAlpha.animateTo(1f, tween(400)) // VIEW_TRANSIT_DURATION = 400ms
                baseBlend = loadedBlend
                overlayBlend = null
                overlayAlpha.snapTo(0f)
            }
        }
    }

    // Accord slow ambient liquid rotation speeds
    val infiniteTransition = rememberInfiniteTransition(label = "accordBlendRotation")

    val tsRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(65000, easing = LinearEasing), // Slow ambient drift (~65s)
            repeatMode = RepeatMode.Restart,
        ),
        label = "tsRotation",
    )
    val beRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(95000, easing = LinearEasing), // Slow ambient drift (~95s)
            repeatMode = RepeatMode.Restart,
        ),
        label = "beRotation",
    )
    val frameRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing), // Slow ambient drift (~120s)
            repeatMode = RepeatMode.Restart,
        ),
        label = "frameRotation",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 2.2f
                scaleY = 2.2f
            }
            .blur(80.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    ) {
        baseBlend?.let { blend ->
            AccordBlendLayer(
                blend = blend,
                tsRotation = tsRotation,
                beRotation = beRotation,
                frameRotation = frameRotation,
            )
        }
        overlayBlend?.let { blend ->
            AccordBlendLayer(
                blend = blend,
                tsRotation = tsRotation,
                beRotation = beRotation,
                frameRotation = frameRotation,
                modifier = Modifier.graphicsLayer { alpha = overlayAlpha.value },
            )
        }

        // Accord contrast_blendOverlayColor (#66000000 = 40% dark overlay) + vignette
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.20f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.50f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun AccordBlendLayer(
    blend: AccordBlendBitmaps,
    tsRotation: Float,
    beRotation: Float,
    frameRotation: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Base full enhanced bitmap (imageViewBG)
        Image(
            painter = BitmapPainter(blend.bgBitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // rotateFrame container with type1 (top-left) and type3 (bottom-right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = frameRotation }
        ) {
            // Type1 (top-left quarter)
            Image(
                painter = BitmapPainter(blend.topLeftBitmap),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopStart,
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .align(Alignment.TopStart)
                    .graphicsLayer { rotationZ = tsRotation }
            )

            // Type3 (bottom-right quarter)
            Image(
                painter = BitmapPainter(blend.bottomRightBitmap),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomEnd,
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .align(Alignment.BottomEnd)
                    .graphicsLayer { rotationZ = beRotation }
            )
        }
    }
}

// Accord 2.5x Saturation enhancement (SATURATION_FACTOR = 2.5F)
private fun enhanceBitmap(bitmap: Bitmap): Bitmap {
    val enhanced = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(enhanced)
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(2.5f) })
    }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return enhanced
}

// Accord cropTopLeftQuarter
private fun cropTopLeftQuarter(bitmap: Bitmap): Bitmap {
    val quarterWidth = (bitmap.width / 2).coerceAtLeast(1)
    val quarterHeight = (bitmap.height / 2).coerceAtLeast(1)
    return Bitmap.createBitmap(bitmap, 0, 0, quarterWidth, quarterHeight)
}

// Accord cropBottomRightQuarter
private fun cropBottomRightQuarter(bitmap: Bitmap): Bitmap {
    val quarterWidth = (bitmap.width / 2).coerceAtLeast(1)
    val quarterHeight = (bitmap.height / 2).coerceAtLeast(1)
    return Bitmap.createBitmap(bitmap, quarterWidth, quarterHeight, quarterWidth, quarterHeight)
}

private data class AccordBlendBitmaps(
    val source: Any,
    val bgBitmap: ImageBitmap,
    val topLeftBitmap: ImageBitmap,
    val bottomRightBitmap: ImageBitmap,
)
