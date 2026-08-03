package com.gorilla.music.ui.screens.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gorilla.music.data.db.PlaylistEntity
import com.gorilla.music.data.model.Track
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.theme.CapsuleShape
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import com.gorilla.music.ui.theme.songCardColor

/** Glass dialog with a single text field (create / rename playlist). */
@Composable
fun TextPromptDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clickable(enabled = false) {}
                    .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(28.dp))
                    .background(appColors.bgSurface)
                    .border(1.dp, appColors.borderGlass.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                    .padding(22.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = title.uppercase(),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                    Text(
                        text = title,
                        color = appColors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(appColors.songCardColor())
                            .border(1.dp, appColors.borderGlass.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                color = appColors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            cursorBrush = SolidColor(accent),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (text.isEmpty()) {
                                    Text(
                                        text = "Playlist name",
                                        color = appColors.textSecondary.copy(alpha = 0.5f),
                                        fontSize = 16.sp,
                                    )
                                }
                                inner()
                            },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DialogButton("Cancel", accent = false, onClick = onDismiss)
                        Spacer(Modifier.width(10.dp))
                        DialogButton(confirmLabel, accent = true, enabled = text.isNotBlank()) {
                            onConfirm(text.trim())
                        }
                    }
                }
            }
        }
    }
}

/** Full-height glass picker to add tracks to a playlist. */
@Composable
fun AddTracksDialog(
    tracks: List<Track>,
    onPick: (Track) -> Unit,
    onDismiss: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.78f)
                    .clickable(enabled = false) {}
                    .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (appColors.isDark) Color(0xFF14151E) else appColors.bgSurface)
                    .border(1.dp, appColors.borderGlass.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                    .padding(20.dp),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "ADD TRACKS",
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                            )
                            Text(
                                text = "Select Songs",
                                color = appColors.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        DialogButton(
                            label = "Done",
                            accent = true,
                            onClick = onDismiss,
                        )
                    }

                    if (tracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No songs available to add",
                                color = appColors.textSecondary,
                                fontSize = 14.sp,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(tracks, key = { it.id }) { track ->
                                AddTrackItemCard(
                                    track = track,
                                    onPick = { onPick(track) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTrackItemCard(
    track: Track,
    onPick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val shape = RoundedCornerShape(16.dp)
    val rowColor = appColors.songCardColor()
    val background = Brush.linearGradient(listOf(rowColor, rowColor))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.96f)
            .clip(shape)
            .background(background)
            .border(1.dp, appColors.borderGlass.copy(alpha = 0.55f), shape)
            .clickable(interaction, indication = null) {
                haptic()
                onPick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = track.albumId,
            artworkUri = track.artworkUri,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(12.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 8.dp),
        ) {
            Text(
                text = track.title,
                color = appColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.displayArtist,
                color = appColors.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add track",
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    accent: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val accentColor = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .shadow(if (accent && enabled) 6.dp else 0.dp, CapsuleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CapsuleShape)
            .background(
                when {
                    !enabled -> appColors.bgGlass.copy(alpha = 0.45f)
                    accent -> accentColor
                    else -> appColors.bgGlass
                }
            )
            .border(
                1.dp,
                when {
                    !enabled -> Color.Transparent
                    accent -> accentColor.copy(alpha = 0.8f)
                    else -> appColors.borderGlass.copy(alpha = 0.65f)
                },
                CapsuleShape,
            )
            .pressScale(interaction, pressedScale = 0.94f)
            .clickable(interaction, indication = null, enabled = enabled) {
                haptic()
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> appColors.textSecondary.copy(alpha = 0.4f)
                accent -> MaterialTheme.colorScheme.onPrimary
                else -> appColors.textPrimary
            },
        )
    }
}

@Composable
fun PickPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onPick: (PlaylistEntity) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.65f)
                    .clickable(enabled = false) {}
                    .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (appColors.isDark) Color(0xFF14151E) else appColors.bgSurface)
                    .border(1.dp, appColors.borderGlass.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                    .padding(20.dp),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "PLAYLIST",
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                            )
                            Text(
                                text = "Add to Playlist",
                                color = appColors.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        DialogButton(
                            label = "Cancel",
                            accent = false,
                            onClick = onDismiss,
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            val haptic = rememberHaptic()
                            val interaction = remember { MutableInteractionSource() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(interaction, pressedScale = 0.97f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(accent.copy(alpha = 0.12f))
                                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                                    .clickable(interaction, indication = null) {
                                        haptic()
                                        onCreateNew()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                        .shadow(4.dp, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Text(
                                    text = "Create New Playlist",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                )
                            }
                        }

                        items(playlists, key = { it.id }) { playlist ->
                            val haptic = rememberHaptic()
                            val interaction = remember { MutableInteractionSource() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(interaction, pressedScale = 0.97f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (appColors.isDark) Color(0xFF1A1C28) else appColors.bgGlass)
                                    .border(1.dp, appColors.borderGlass.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                                    .clickable(interaction, indication = null) {
                                        haptic()
                                        onPick(playlist)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(appColors.bgGlass)
                                        .border(1.dp, appColors.borderGlass, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Text(
                                    text = playlist.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = appColors.textPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
