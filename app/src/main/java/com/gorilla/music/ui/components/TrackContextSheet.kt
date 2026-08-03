package com.gorilla.music.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gorilla.music.data.db.PlaylistEntity
import com.gorilla.music.data.model.Track
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.rememberHaptic
import com.gorilla.music.ui.theme.instantClickable

/**
 * Floating bottom context card shown for a track's ⋮ button. Hosts the playlist submenu
 * plus the create-playlist and delete confirmation views. All seven actions are wired.
 */
@Composable
fun TrackContextSheet(
    track: Track,
    playlists: List<PlaylistEntity>,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onNewPlaylist: (String) -> Unit,
    onEqualizer: () -> Unit,
    onTrackInfo: () -> Unit,
    onEditTags: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var view by remember { mutableStateOf(MenuView.ROOT) }
    var appeared by remember { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val entryProgress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(
            durationMillis = 220,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
        ),
        label = "contextMenuEntry",
    )

    LaunchedEffect(Unit) { appeared = true }

    BackHandler {
        if (view == MenuView.ROOT) onDismiss() else view = MenuView.ROOT
    }

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            ContextTrackHeader(track)

            when (view) {
                        MenuView.ROOT -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                QuickActionButton(
                                    icon = Icons.Rounded.PlayArrow,
                                    label = "Play now",
                                    primary = true,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    haptic()
                                    onPlayNow()
                                    onDismiss()
                                }
                                QuickActionButton(
                                    icon = Icons.Rounded.SkipNext,
                                    label = "Play next",
                                    primary = false,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    haptic()
                                    onPlayNext()
                                    onDismiss()
                                }
                            }

                            MenuGroup {
                                MenuItem(Icons.AutoMirrored.Rounded.QueueMusic, "Add to queue") {
                                    haptic()
                                    onAddToQueue()
                                    onDismiss()
                                }
                                MenuItem(
                                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    "Add to playlist",
                                    trailing = Icons.Rounded.ChevronRight,
                                ) {
                                    haptic()
                                    view = MenuView.PLAYLISTS
                                }
                                MenuItem(
                                    Icons.Rounded.Tune,
                                    "Equalizer",
                                    trailing = Icons.Rounded.ChevronRight,
                                ) {
                                    haptic()
                                    onEqualizer()
                                    onDismiss()
                                }
                                MenuItem(Icons.Rounded.Info, "Track info") {
                                    haptic()
                                    onTrackInfo()
                                    onDismiss()
                                }
                                MenuItem(Icons.Rounded.Edit, "Edit tags") {
                                    haptic()
                                    onEditTags()
                                    onDismiss()
                                }
                                MenuItem(
                                    icon = Icons.Rounded.Delete,
                                    label = "Delete",
                                    destructive = true,
                                    showDivider = false,
                                ) {
                                    haptic()
                                    view = MenuView.DELETE
                                }
                            }
                        }

                        MenuView.PLAYLISTS -> {
                            MenuGroup {
                                MenuItem(Icons.Rounded.Add, "New playlist…") {
                                    haptic()
                                    view = MenuView.NEW_PLAYLIST
                                }
                                if (playlists.isEmpty()) {
                                    Text(
                                        "No playlists yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    )
                                } else {
                                    playlists.forEach { pl ->
                                        MenuItem(Icons.AutoMirrored.Rounded.PlaylistAdd, pl.name) {
                                            haptic()
                                            onAddToPlaylist(pl.id)
                                            onDismiss()
                                        }
                                    }
                                }
                                MenuItem(
                                    Icons.Rounded.ChevronRight,
                                    "Back",
                                    mirror = true,
                                    showDivider = false,
                                ) {
                                    view = MenuView.ROOT
                                }
                            }
                        }

                        MenuView.NEW_PLAYLIST -> {
                            InlinePrompt(
                                title = "New playlist",
                                initial = "",
                                confirmLabel = "Create & add",
                                onConfirm = { name ->
                                    onNewPlaylist(name)
                                    onDismiss()
                                },
                                onCancel = { view = MenuView.PLAYLISTS },
                            )
                        }

                        MenuView.DELETE -> {
                            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                                Text(
                                    "Delete this track?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    "“${track.title}” will be permanently removed from this device.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                                    PillButton("Cancel", destructive = false) { view = MenuView.ROOT }
                                    Spacer(Modifier.width(8.dp))
                                    PillButton("Delete", destructive = true) { onDelete(); onDismiss() }
                                }
                            }
                        }
            }
        }
    }
}

private enum class MenuView { ROOT, PLAYLISTS, NEW_PLAYLIST, DELETE }

private val ContextMenuDelete = Color(0xFFEF4444)

@Composable
private fun ContextTrackHeader(track: Track) {
    val colors = LocalAppColors.current
    val artShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AlbumArt(
                albumId = track.albumId,
                artworkUri = track.artworkUri,
                shape = artShape,
                modifier = Modifier
                    .size(50.dp)
                    .border(1.dp, colors.borderGlass.copy(alpha = 0.75f), artShape),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = colors.textPrimary,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${track.displayArtist} • ${track.displayAlbum}",
                    color = colors.textSecondary.copy(alpha = 0.9f),
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.borderGlass.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val shape = RoundedCornerShape(16.dp)
    val background = if (primary) {
        accent
    } else {
        colors.bgGlass
    }
    val borderColor = if (primary) {
        accent
    } else {
        colors.borderGlass.copy(alpha = 0.55f)
    }
    val contentColor = if (primary) Color.White else colors.textPrimary

    Row(
        modifier = modifier
            .height(42.dp)
            .instantClickable(pressedScale = 0.94f) {
                onClick()
            }
            .background(background, shape)
            .border(1.dp, borderColor, shape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(if (primary) 16.dp else 17.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MenuGroup(content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.bgGlass)
            .border(1.dp, colors.borderGlass.copy(alpha = 0.45f), shape),
    ) {
        content()
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    trailing: ImageVector? = null,
    destructive: Boolean = false,
    mirror: Boolean = false,
    showDivider: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val bgAlpha by animateFloatAsState(if (isPressed) 0.08f else 0f, label = "menuItemAlpha")
    val colors = LocalAppColors.current
    val normalTint = colors.textPrimary
    val tint = if (destructive) ContextMenuDelete else normalTint
    val iconBackground = if (destructive) {
        ContextMenuDelete.copy(alpha = 0.20f)
    } else {
        colors.textPrimary.copy(alpha = 0.08f)
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .clickable(interaction, indication = null, onClick = onClick)
                .background(normalTint.copy(alpha = bgAlpha))
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(iconBackground, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint.copy(alpha = if (destructive) 1f else 0.85f),
                        modifier = Modifier
                            .size(16.dp)
                            .then(if (mirror) Modifier.graphicsLayerMirror() else Modifier),
                    )
                }
                Text(
                    text = label,
                    color = tint,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (trailing != null) {
                    Icon(
                        trailing,
                        contentDescription = null,
                        tint = normalTint.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.borderGlass.copy(alpha = 0.25f))
            )
        }
    }
}

private fun Modifier.graphicsLayerMirror(): Modifier =
    this.graphicsLayer { scaleX = -1f }

@Composable
private fun InlinePrompt(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val colors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val inputShape = RoundedCornerShape(18.dp)
    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .background(colors.bgGlass, inputShape)
                .border(1.dp, colors.borderGlass.copy(alpha = 0.55f), inputShape),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("Name", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PillButton("Cancel", destructive = false) { onCancel() }
            Spacer(Modifier.width(8.dp))
            PillButton(confirmLabel, accentButton = true, enabled = text.isNotBlank()) { onConfirm(text.trim()) }
        }
    }
}

@Composable
private fun PillButton(
    label: String,
    destructive: Boolean = false,
    accentButton: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val shape = RoundedCornerShape(16.dp)
    val background = when {
        accentButton -> accent
        destructive -> ContextMenuDelete.copy(alpha = 0.16f)
        else -> colors.bgGlass
    }
    val border = when {
        accentButton -> accent
        destructive -> ContextMenuDelete.copy(alpha = 0.35f)
        else -> colors.borderGlass.copy(alpha = 0.55f)
    }
    val contentColor = when {
        !enabled -> colors.textDisabled
        destructive -> ContextMenuDelete
        accentButton -> Color.White
        else -> colors.textPrimary
    }

    Box(
        modifier = Modifier
            .then(
                if (enabled) {
                    Modifier.instantClickable(
                        pressedScale = 0.94f,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .background(background, shape)
            .border(1.dp, border, shape),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
