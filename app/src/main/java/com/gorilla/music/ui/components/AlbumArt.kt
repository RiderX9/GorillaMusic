package com.gorilla.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.gorilla.music.data.repo.albumArtUri
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.ThumbShape
import com.gorilla.music.ui.theme.darken

/**
 * Album art for a track, loaded from the MediaStore albumart provider via Coil. Falls
 * back to a palette-tinted music-note placeholder when no art exists.
 */
@Composable
fun AlbumArt(
    albumId: Long,
    modifier: Modifier = Modifier,
    shape: Shape = ThumbShape,
    artworkUri: android.net.Uri? = null,
    fallbackTitle: String? = null,
    fallbackSubtitle: String? = null,
    showLoadingPlaceholder: Boolean = true,
    allowHardware: Boolean = true,
    crossfade: Boolean = true,
    painterOverride: AsyncImagePainter? = null,
) {
    val dynamicColors = LocalDynamicColors.current
    val appColors = LocalAppColors.current
    val context = LocalContext.current

    val model = remember(albumId, artworkUri, allowHardware, crossfade) {
        ImageRequest.Builder(context)
            .data(artworkUri ?: albumArtUri(albumId))
            .crossfade(crossfade)
            .allowHardware(allowHardware)
            .build()
    }
    var loadedState by remember {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    var lastSuccessPainter by remember {
        mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null)
    }
    val state = painterOverride?.state ?: loadedState

    if (state is AsyncImagePainter.State.Success) {
        lastSuccessPainter = (state as AsyncImagePainter.State.Success).painter
    }

    BoxWithConstraints(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center
    ) {
        val iconSize = when {
            maxWidth >= 200.dp -> 48.dp
            maxWidth >= 100.dp -> 32.dp
            else -> 20.dp
        }

        val isRadioArt = albumId < 0L
        val imageFailed = state is AsyncImagePainter.State.Error
        val imageLoading =
            state is AsyncImagePainter.State.Empty || state is AsyncImagePainter.State.Loading
        // Do not place generated radio artwork below a real favicon. Some station
        // thumbnails contain transparent pixels, which would otherwise reveal the
        // generated initials behind the image.
        if (isRadioArt && (artworkUri == null || imageFailed)) {
            GeneratedRadioArtwork(
                title = fallbackTitle.orEmpty(),
                subtitle = fallbackSubtitle.orEmpty(),
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (!isRadioArt || (artworkUri != null && !imageFailed)) {
            Box(Modifier.fillMaxSize()) {
                // Keep displaying last successful artwork while new artwork loads (Accord style)
                lastSuccessPainter?.let { painter ->
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (painterOverride != null) {
                    Image(
                        painter = painterOverride,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onState = { loadedState = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (!isRadioArt && imageFailed && lastSuccessPainter == null) {
            // Render fallback: solid BgSurface (#1E1E2A) colored rectangle with a centered music note icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xFF1E1E2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = appColors.textSecondary,
                )
            }
        } else if (
            showLoadingPlaceholder &&
            imageLoading &&
            lastSuccessPainter == null
        ) {
            // Show loading state with gradient background only when no previous artwork is available
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(dynamicColors.artPrimary.darken(0.3f), dynamicColors.artSecondary.darken(0.5f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = appColors.textSecondary.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun GeneratedRadioArtwork(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val accent = LocalDynamicColors.current.accent
    val initials = remember(title) {
        title
            .split(' ', '-', '_', '.', '/', '|')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "GM" }
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    accent.copy(alpha = 0.92f),
                    Color(0xFFFF2D55).copy(alpha = 0.82f),
                    Color(0xFF171720),
                )
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.24f), Color.Transparent)))
        )
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = initials.take(3),
                color = Color.White,
                fontSize = 36.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = subtitle.ifBlank { "Live Radio" },
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
