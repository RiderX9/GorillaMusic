package com.gorilla.music.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.music.data.model.Track
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.rememberHaptic

/**
 * Centred-hero detail page for an album or an artist: ambient wash, large artwork,
 * title/artist/meta stack, shuffle–play–queue action bar, then the tracklist as a single
 * grouped card. Rows stay lazy (corners are rounded per-row) so large artists scroll cheaply.
 *
 * Genres, years, folders and playlists keep the flat BrowseDetailPage header.
 */
@Composable
fun CollectionDetailPage(
    title: String,
    subtitle: String?,
    metaLine: String,
    albumId: Long,
    artworkUri: Uri?,
    tracks: List<Track>,
    activeId: Long?,
    isFavorite: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPlay: (Track) -> Unit,
    onMenu: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    isPlaying: Boolean = false,
) {
    val colors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()
    val cardShape = RoundedCornerShape(24.dp)

    Box(Modifier.fillMaxSize().background(colors.bgBase)) {
        // Ambient wash behind the hero, fading into the page background.
        Box(
            Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.verticalGradient(
                        0f to accent.copy(alpha = 0.42f),
                        0.65f to accent.copy(alpha = 0.12f),
                        1f to colors.bgBase,
                    )
                )
        )

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 78.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AlbumArt(
                        albumId = albumId,
                        artworkUri = artworkUri,
                        shape = RoundedCornerShape(26.dp),
                        fallbackTitle = title,
                        fallbackSubtitle = subtitle,
                        modifier = Modifier
                            .size(190.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(26.dp)),
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = title,
                            color = colors.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                            tint = if (isFavorite) accent else colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .instantClickable(pressedScale = 0.88f) {
                                    haptic()
                                    onToggleFavorite()
                                },
                        )
                    }

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Text(
                        text = metaLine.uppercase(),
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.7.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Row(
                        modifier = Modifier.padding(top = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CircleAction(Icons.Rounded.Shuffle, "Shuffle") { haptic(); onShuffle() }

                        Row(
                            modifier = Modifier
                                .width(140.dp)
                                .height(46.dp)
                                .instantClickable(pressedScale = 0.95f) { haptic(); onPlayAll() }
                                .clip(CircleShape)
                                .background(accent),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isPlaying) "Pause" else "Play",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }

                        CircleAction(Icons.AutoMirrored.Rounded.QueueMusic, "Add to queue") {
                            haptic()
                            onAddToQueue()
                        }
                    }
                }
            }

            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                val shape = when {
                    tracks.size == 1 -> cardShape
                    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    index == tracks.lastIndex -> RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                CollectionTrackRow(
                    track = track,
                    position = index + 1,
                    isActive = track.id == activeId,
                    showDivider = index != tracks.lastIndex,
                    shape = shape,
                    onPlay = { onPlay(track) },
                    onMenu = { onMenu(track) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleAction(
                icon = Icons.AutoMirrored.Rounded.ArrowBack, 
                label = "Back"
            ) { haptic(); onBack() }
            Spacer(Modifier.weight(1f))
            CircleAction(
                icon = Icons.Rounded.Share, 
                label = "Share"
            ) { haptic(); onShare() }
        }
    }
}

/** "Album • 2024 • FLAC Lossless" — year and format come from the tracks themselves. */
fun albumMetaLine(tracks: List<Track>): String {
    val parts = mutableListOf("Album")
    if (tracks.isNotEmpty()) {
        parts += "${tracks.size} ${if (tracks.size == 1) "song" else "songs"}"
    }
    tracks.mapNotNull { it.overrideYear ?: it.year.takeIf { y -> y > 0 } }
        .maxOrNull()
        ?.let { parts += it.toString() }
    val format = tracks.mapNotNull { it.audioFormat.takeIf { f -> f.isNotBlank() } }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    if (format != null) {
        parts += if (tracks.any { it.isLossless }) "$format Lossless" else format
    }
    return parts.joinToString(" • ")
}

/** "Artist • 4 albums • 37 songs". */
fun artistMetaLine(albumCount: Int, trackCount: Int): String = buildString {
    append("Artist")
    if (albumCount > 0) append(" • $albumCount ${if (albumCount == 1) "album" else "albums"}")
    append(" • $trackCount ${if (trackCount == 1) "song" else "songs"}")
}

/** Shares the collection as a plain-text tracklist through the system share sheet. */
fun shareCollection(context: android.content.Context, title: String, tracks: List<Track>) {
    if (tracks.isEmpty()) return
    val body = buildString {
        appendLine(title)
        tracks.take(50).forEachIndexed { index, track ->
            appendLine("${index + 1}. ${track.title} — ${track.displayArtist}")
        }
        if (tracks.size > 50) appendLine("…and ${tracks.size - 50} more")
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, title)
        putExtra(android.content.Intent.EXTRA_TEXT, body.trim())
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share $title"))
}

@Composable
private fun NavCircleButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .instantClickable(pressedScale = 0.9f, onClick = onClick)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CircleAction(
    icon: ImageVector, 
    label: String, 
    backgroundColor: Color? = null,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .instantClickable(pressedScale = 0.9f, onClick = onClick)
            .clip(CircleShape)
            .background(backgroundColor ?: colors.bgGlass)
            .border(1.dp, colors.borderGlass.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = iconTint ?: colors.textPrimary, modifier = Modifier.size(18.dp))
    }
}

/** One tracklist row. The number is replaced by a play glyph while the track is active. */
@Composable
private fun CollectionTrackRow(
    track: Track,
    position: Int,
    isActive: Boolean,
    showDivider: Boolean,
    shape: RoundedCornerShape,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
) {
    val colors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val haptic = rememberHaptic()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isActive) accent.copy(alpha = 0.12f) else colors.bgGlass)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .instantClickable(pressedScale = 0.98f) { haptic(); onPlay() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(18.dp), contentAlignment = Alignment.Center) {
                if (isActive) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(13.dp),
                    )
                } else {
                    Text(
                        text = "$position",
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isActive) accent else colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (track.durationMs > 0) {
                        "${track.displayArtist} • ${formatDuration(track.durationMs)}"
                    } else {
                        track.displayArtist
                    },
                    color = colors.textSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More",
                tint = colors.textSecondary,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .instantClickable(pressedScale = 0.85f) { haptic(); onMenu() }
                    .padding(7.dp),
            )
        }

        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.borderGlass.copy(alpha = 0.25f))
            )
        }
    }
}
