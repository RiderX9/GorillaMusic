package com.gorilla.music.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.music.data.model.Track
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.components.ModalSheetScaffold
import com.gorilla.music.ui.components.formatBitrate
import com.gorilla.music.ui.components.formatChannels
import com.gorilla.music.ui.components.formatDuration
import com.gorilla.music.ui.components.formatFileSize
import com.gorilla.music.ui.components.formatSampleRate
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.instantClickable

private val BitrateAccent = Color(0xFF38BDF8)

/**
 * Compact track details sheet based on the grouped layout used by the supplied design.
 * Technical metadata is loaded on demand and the two footer actions remain owned by the
 * root app state.
 */
@Composable
fun TrackInfoSheet(
    track: Track,
    onDismiss: () -> Unit,
    onEditTags: () -> Unit,
    onPlayNext: () -> Unit,
    vm: TrackInfoViewModel = viewModel(factory = TrackInfoViewModel.Factory),
) {
    val info by vm.info.collectAsStateWithLifecycle()
    LaunchedEffect(track.id) { vm.load(track) }

    val appColors = LocalAppColors.current
    ModalSheetScaffold(
        onDismiss = onDismiss,
        heightFraction = null,
        surfaceColor = appColors.bgSurface,
        plainSurface = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
                TrackHeader(track)

                val format = info?.format ?: formatFromTrack(track)
                val codecAndEncoding = info?.let { combineCodecAndEncoding(it.codec, it.encoding) } ?: "..."
                val rows = listOf(
                    TrackSpec("Format", format, badge = if (isLossless(format)) "Lossless" else null),
                    TrackSpec("Codec & Encoding", codecAndEncoding),
                    TrackSpec("Bitrate", info?.let { formatBitrate(it.bitrateKbps) } ?: "...", BitrateAccent),
                    TrackSpec("Sample Rate", info?.let { formatSampleRate(it.sampleRateHz) } ?: "..."),
                    TrackSpec("Channels", info?.let { formatChannelsDetailed(it.channels) } ?: "..."),
                    TrackSpec(
                        "File Size & Duration",
                        "${info?.let { formatFileSize(it.sizeBytes) } ?: formatFileSize(track.size)} • " +
                            formatDuration(info?.durationMs?.takeIf { it > 0 } ?: track.durationMs),
                    ),
                )

                SpecGroup(rows)
                Spacer(Modifier.height(14.dp))
                FileLocation(track.data)
                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TrackActionButton(
                        label = "Edit Tags",
                        icon = { tint ->
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(17.dp),
                            )
                        },
                        primary = false,
                        onClick = onEditTags,
                        modifier = Modifier.weight(1f),
                    )
                    TrackActionButton(
                        label = "Play Next",
                        icon = { tint ->
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(19.dp),
                            )
                        },
                        primary = true,
                        onClick = onPlayNext,
                        modifier = Modifier.weight(1f),
                    )
                }
        }
    }
}

@Composable
private fun TrackHeader(track: Track) {
    val colors = LocalAppColors.current
    val artShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AlbumArt(
            albumId = track.albumId,
            artworkUri = track.artworkUri,
            shape = artShape,
            modifier = Modifier
                .size(58.dp)
                .border(1.dp, colors.borderGlass.copy(alpha = 0.6f), artShape),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.displayArtist} • ${track.displayAlbum}",
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private data class TrackSpec(
    val label: String,
    val value: String,
    val valueColor: Color? = null,
    val badge: String? = null,
)

@Composable
private fun SpecGroup(rows: List<TrackSpec>) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgGlass, shape)
            .border(1.dp, colors.borderGlass.copy(alpha = 0.45f), shape)
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        rows.forEachIndexed { index, row ->
            SpecRow(row)
            if (index < rows.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.borderGlass.copy(alpha = 0.26f))
                )
            }
        }
    }
}

@Composable
private fun SpecRow(row: TrackSpec) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.label,
            color = colors.textSecondary.copy(alpha = 0.82f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.weight(1.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            Text(
                text = row.value,
                color = row.valueColor ?: colors.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            row.badge?.let { LosslessBadge(it) }
        }
    }
}

@Composable
private fun LosslessBadge(label: String) {
    val accent = LocalDynamicColors.current.accent
    Text(
        text = label,
        color = accent,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun FileLocation(path: String) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgGlass, shape)
            .border(1.dp, colors.borderGlass.copy(alpha = 0.45f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "FILE LOCATION",
            color = colors.textSecondary.copy(alpha = 0.72f),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
        )
        Text(
            text = path.ifBlank { "Location unavailable" },
            color = colors.textSecondary.copy(alpha = 0.95f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TrackActionButton(
    label: String,
    icon: @Composable (Color) -> Unit,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val shape = RoundedCornerShape(18.dp)
    val background = if (primary) {
        accent
    } else {
        colors.bgGlass
    }
    val border = if (primary) accent else colors.borderGlass.copy(alpha = 0.55f)
    val contentColor = if (primary) Color.White else colors.textPrimary

    Row(
        modifier = modifier
            .then(
                if (primary) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = shape,
                        ambientColor = accent.copy(alpha = 0.35f),
                        spotColor = accent.copy(alpha = 0.35f),
                    )
                } else {
                    Modifier
                }
            )
            .height(48.dp)
            .instantClickable(pressedScale = 0.94f) {
                onClick()
            }
            .background(background, shape)
            .border(1.dp, border, shape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(contentColor)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

private fun formatFromTrack(track: Track): String =
    track.data.substringAfterLast('.', "").uppercase().ifBlank {
        track.mimeType.substringAfterLast('/', "Unknown").uppercase()
    }

private fun combineCodecAndEncoding(codec: String, encoding: String): String {
    val values = listOf(codec, encoding)
        .filter { it.isNotBlank() && it != "—" }
        .distinct()
    return values.joinToString(" • ").ifBlank { "—" }
}

private fun isLossless(format: String): Boolean =
    format.uppercase() in setOf("ALAC", "FLAC", "WAV", "AIFF", "APE", "DSD")

private fun formatChannelsDetailed(channels: Int): String = when (channels) {
    1 -> "Mono (1.0)"
    2 -> "Stereo (2.0)"
    else -> formatChannels(channels)
}
