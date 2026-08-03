package com.gorilla.music.ui.screens.playlists

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.gorilla.music.ui.theme.LocalAppColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.music.data.db.PlaylistEntity
import com.gorilla.music.data.model.Track
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.components.EmptyState
import com.gorilla.music.ui.components.TopBarIconButton
import com.gorilla.music.ui.components.formatDuration
import com.gorilla.music.ui.theme.CapsuleShape
import com.gorilla.music.ui.theme.CardShape
import com.gorilla.music.ui.theme.GlassDepth
import com.gorilla.music.ui.theme.LiquidGlassSurface
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.pressScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gorilla.music.ui.theme.rememberHaptic
import com.gorilla.music.ui.theme.songCardColor

private enum class PlaylistLayout {
    LIST,
    GRID,
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun PlaylistsScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    onOpenNowPlaying: () -> Unit,
    onBackToOrigin: () -> Unit = {},
    vm: PlaylistsViewModel = viewModel(factory = PlaylistsViewModel.Factory),
) {
    val playlists by vm.allPlaylists.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val selectedTracks by vm.selectedTracks.collectAsStateWithLifecycle()
    val allTracks by vm.allTracks.collectAsStateWithLifecycle()
    val playback by app.playbackState.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistEntity?>(null) }
    var addPickerFor by remember { mutableStateOf<Long?>(null) }
    var layout by rememberSaveable { mutableStateOf(PlaylistLayout.LIST) }

    var directOpen by rememberSaveable { mutableStateOf(false) }

    val requestedPlaylistId by app.requestedPlaylistId.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(requestedPlaylistId) {
        requestedPlaylistId?.let {
            directOpen = true
            vm.open(it)
            app.requestPlaylist(null)
        }
    }

    val openPlaylist = playlists.firstOrNull { it.id == selectedId }

    BackHandler(enabled = openPlaylist != null) {
        if (directOpen) {
            onBackToOrigin()
        } else {
            vm.closeDetail()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = openPlaylist,
            transitionSpec = {
                if (directOpen) {
                    androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                } else if (targetState != null) {
                    (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn())
                        .togetherWith(slideOutHorizontally(tween(260)) { -it / 5 } + fadeOut())
                } else {
                    (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn())
                        .togetherWith(slideOutHorizontally(tween(260)) { it / 5 } + fadeOut())
                }
            },
            label = "playlistDetailTransition",
            modifier = Modifier.fillMaxSize(),
        ) { currentPlaylist ->
            if (currentPlaylist == null) {
                if (directOpen || requestedPlaylistId != null) {
                    Box(Modifier.fillMaxSize())
                } else {
                    Column(Modifier.fillMaxSize()) {
                    PlaylistsHeader(onAdd = { showCreate = true })
                    PlaylistLayoutBar(
                        count = playlists.size,
                        layout = layout,
                        onLayoutChange = { layout = it },
                    )
                    if (playlists.isEmpty()) {
                        EmptyState(
                            title = "No playlists yet",
                            subtitle = "Tap + to create your first playlist.",
                            icon = Icons.Rounded.PlaylistPlay,
                        )
                    } else if (layout == PlaylistLayout.LIST) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(playlists, key = { it.id }) { playlist ->
                                PlaylistListCard(
                                    playlist = playlist,
                                    vm = vm,
                                    onOpen = { vm.open(playlist.id) },
                                    onRename = { renameTarget = playlist },
                                    onDelete = { vm.delete(playlist.id) },
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            gridItems(playlists, key = { it.id }) { playlist ->
                                PlaylistGridCard(
                                    playlist = playlist,
                                    vm = vm,
                                    onOpen = { vm.open(playlist.id) },
                                    onRename = { renameTarget = playlist },
                                    onDelete = { vm.delete(playlist.id) },
                                )
                            }
                        }
                    }
                }
            }
        } else {
                // ---- Detail: one playlist ----
                PlaylistDetail(
                    playlist = currentPlaylist,
                    tracks = selectedTracks,
                    activeId = playback.current?.id,
                    contentPadding = contentPadding,
                    onBack = {
                        if (directOpen) onBackToOrigin() else vm.closeDetail()
                    },
                    onPlay = { idx -> app.playTracks(selectedTracks ?: emptyList(), idx) },
                    onAdd = { addPickerFor = currentPlaylist.id },
                    onRemove = { track -> vm.removeTrack(currentPlaylist.id, track.id) },
                    onMove = { from, to ->
                        val ids = (selectedTracks ?: emptyList()).map { it.id }.toMutableList()
                        if (from in ids.indices && to in ids.indices) {
                            ids.add(to, ids.removeAt(from))
                            vm.reorder(currentPlaylist.id, ids)
                        }
                    },
                )
            }
        }
    }

    if (showCreate) {
        TextPromptDialog(
            title = "New playlist",
            initial = "",
            confirmLabel = "Create",
            onConfirm = { name -> vm.create(name); showCreate = false },
            onDismiss = { showCreate = false },
        )
    }
    renameTarget?.let { target ->
        TextPromptDialog(
            title = "Rename playlist",
            initial = target.name,
            confirmLabel = "Save",
            onConfirm = { name -> vm.rename(target.id, name); renameTarget = null },
            onDismiss = { renameTarget = null },
        )
    }
    addPickerFor?.let { playlistId ->
        AddTracksDialog(
            tracks = allTracks,
            onPick = { track -> vm.addTrack(playlistId, track.id) },
            onDismiss = { addPickerFor = null },
        )
    }
}

@Composable
private fun PlaylistsHeader(
    onAdd: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "LIBRARY",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = "Playlists",
                color = appColors.textPrimary,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Box(
            modifier = Modifier
                .pressScale(interaction, pressedScale = 0.94f)
                .size(40.dp)
                .shadow(8.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape)
                .background(appColors.bgGlass)
                .border(1.dp, appColors.borderGlass.copy(alpha = 0.75f), CircleShape)
                .clickable(interaction, indication = null) {
                    haptic()
                    onAdd()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New playlist",
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PlaylistLayoutBar(
    count: Int,
    layout: PlaylistLayout,
    onLayoutChange: (PlaylistLayout) -> Unit,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$count ${if (count == 1) "PLAYLIST" else "PLAYLISTS"}",
            color = appColors.textSecondary.copy(alpha = 0.72f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.bgGlass)
                .border(1.dp, appColors.borderGlass.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PlaylistLayoutOption("List", layout == PlaylistLayout.LIST, accent) {
                onLayoutChange(PlaylistLayout.LIST)
            }
            PlaylistLayoutOption("Grid", layout == PlaylistLayout.GRID, accent) {
                onLayoutChange(PlaylistLayout.GRID)
            }
        }
    }
}

@Composable
private fun PlaylistLayoutOption(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        color = if (selected) accent else appColors.textSecondary.copy(alpha = 0.7f),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .instantClickable(pressedScale = 0.90f) {
                onClick()
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun PlaylistListCard(
    playlist: PlaylistEntity,
    vm: PlaylistsViewModel,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val tracksFlow = remember(playlist.id) { vm.observeTracks(playlist.id) }
    val tracks by tracksFlow.collectAsStateWithLifecycle(emptyList())
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val appColors = LocalAppColors.current
    val cardShape = RoundedCornerShape(18.dp)
    val cardColor = appColors.bgSurface.copy(alpha = 0.94f)
    val background = Brush.linearGradient(listOf(cardColor, cardColor))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .instantClickable(pressedScale = 0.96f) {
                onOpen()
            }
            .clip(cardShape)
            .background(background)
            .border(1.dp, appColors.borderGlass, cardShape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PlaylistArtwork(
            tracks = tracks,
            modifier = Modifier
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                .size(54.dp),
            cornerRadius = 14.dp,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = appColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = playlistMeta(tracks),
                color = appColors.textSecondary.copy(alpha = 0.75f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PlaylistOverflowButton(
            onRename = onRename,
            onDelete = onDelete,
            modifier = Modifier.padding(end = 14.dp),
        )
    }
}

@Composable
private fun PlaylistGridCard(
    playlist: PlaylistEntity,
    vm: PlaylistsViewModel,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val tracksFlow = remember(playlist.id) { vm.observeTracks(playlist.id) }
    val tracks by tracksFlow.collectAsStateWithLifecycle(emptyList())
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(20.dp)
    val cardColor = appColors.bgSurface.copy(alpha = 0.94f)
    val background = Brush.linearGradient(listOf(cardColor, cardColor))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .instantClickable(pressedScale = 0.95f) {
                onOpen()
            }
            .clip(cardShape)
            .background(background)
            .border(1.dp, appColors.borderGlass, cardShape)
            .padding(12.dp),
    ) {
        Box {
            PlaylistArtwork(
                tracks = tracks,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                cornerRadius = 16.dp,
            )
            PlaylistOverflowButton(
                onRename = onRename,
                onDelete = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
        }
        Text(
            text = playlist.name,
            color = appColors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = "${tracks.size} ${if (tracks.size == 1) "song" else "songs"}",
            color = appColors.textSecondary.copy(alpha = 0.75f),
            fontSize = 11.5.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun PlaylistArtwork(
    tracks: List<Track>,
    modifier: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp,
) {
    val accent = LocalDynamicColors.current.accent
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(6.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.8f), Color(0xFF171720)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            tracks.isEmpty() -> Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(28.dp),
            )
            tracks.size == 1 -> {
                val track = tracks.first()
                AlbumArt(
                    albumId = track.albumId,
                    artworkUri = track.artworkUri,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                )
            }
            else -> {
                val mosaicTracks = List(4) { tracks[it % tracks.size] }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    repeat(2) { row ->
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            repeat(2) { column ->
                                val track = mosaicTracks[row * 2 + column]
                                AlbumArt(
                                    albumId = track.albumId,
                                    artworkUri = track.artworkUri,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                    shape = RoundedCornerShape(0.dp),
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
private fun PlaylistOverflowButton(
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()

    Box(modifier) {
        Box(
            modifier = Modifier
                .instantClickable(pressedScale = 0.90f) {
                    expanded = true
                }
                .size(32.dp)
                .clip(CircleShape)
                .background(appColors.bgGlass)
                .border(1.dp, appColors.borderGlass.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "Playlist options",
                tint = appColors.textSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(appColors.bgSurface)
                .border(1.dp, appColors.borderGlass.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
        ) {
            DropdownMenuItem(
                text = { Text("Rename", color = appColors.textPrimary, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = accent) },
                onClick = {
                    expanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

private fun playlistMeta(tracks: List<Track>?): String {
    if (tracks == null) return "Loading..."
    val count = tracks.size
    val countLabel = "$count ${if (count == 1) "song" else "songs"}"
    val detail = when {
        count == 0 -> "No songs yet"
        count == 1 -> tracks.first().displayArtist
        else -> "Album Art Mosaic"
    }
    return "$countLabel • $detail"
}

@Composable
private fun PlaylistDetail(
    playlist: PlaylistEntity,
    tracks: List<Track>?,
    activeId: Long?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Track) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val addInteraction = remember { MutableInteractionSource() }
    val backInteraction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
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
                    text = playlist.name,
                    color = appColors.textPrimary,
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .instantClickable(pressedScale = 0.90f) {
                        onAdd()
                    }
                    .size(40.dp)
                    .shadow(8.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(CircleShape)
                    .background(appColors.bgGlass)
                    .border(1.dp, appColors.borderGlass.copy(alpha = 0.75f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add tracks",
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backPillColor = appColors.bgSurface.copy(alpha = 0.94f)
            Box(
                modifier = Modifier
                    .instantClickable(pressedScale = 0.90f) {
                        onBack()
                    }
                    .clip(CapsuleShape)
                    .background(Brush.linearGradient(listOf(backPillColor, backPillColor)))
                    .border(1.dp, appColors.borderGlass, CapsuleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = appColors.textPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "Back",
                        color = appColors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = if (tracks == null) "LOADING..." else "${tracks.size} ${if (tracks.size == 1) "SONG" else "SONGS"}",
                color = appColors.textSecondary.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
            )
        }

        if (tracks == null) {
            Box(Modifier.fillMaxSize())
        } else if (tracks.isEmpty()) {
            EmptyState("Empty playlist", "Add songs with the + button.", icon = Icons.Rounded.PlaylistPlay)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 2.dp,
                    bottom = contentPadding.calculateBottomPadding() + 20.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(tracks, key = { it.id }) { track ->
                    val index = tracks.indexOf(track)
                    PlaylistTrackCard(
                        track = track,
                        isActive = track.id == activeId,
                        onClick = { onPlay(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        onMoveUp = {
                            if (index > 0) onMove(index, index - 1)
                        },
                        onMoveDown = {
                            if (index < tracks.lastIndex) onMove(index, index + 1)
                        },
                        canMoveUp = index > 0,
                        canMoveDown = index < tracks.lastIndex,
                        onRemove = { onRemove(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackCard(
    track: Track,
    isActive: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val shape = RoundedCornerShape(16.dp)
    val rowColor = appColors.songCardColor()
    val background = if (isActive) {
        Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), rowColor))
    } else {
        Brush.linearGradient(listOf(rowColor, rowColor))
    }

    Row(
        modifier = modifier
            .instantClickable(pressedScale = 0.96f) {
                haptic()
                onClick()
            }
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (isActive) accent.copy(alpha = 0.42f) else appColors.borderGlass,
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                color = if (isActive) accent else appColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.displayArtist} • ${formatDuration(track.durationMs)}",
                color = appColors.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            ReorderButton(
                icon = Icons.Rounded.ArrowUpward,
                contentDescription = "Move up",
                enabled = canMoveUp,
                onClick = onMoveUp,
            )
            ReorderButton(
                icon = Icons.Rounded.ArrowDownward,
                contentDescription = "Move down",
                enabled = canMoveDown,
                onClick = onMoveDown,
            )
            ReorderButton(
                icon = Icons.Rounded.RemoveCircleOutline,
                contentDescription = "Remove from playlist",
                enabled = true,
                destructive = true,
                onClick = onRemove,
            )
        }
    }
}

@Composable
private fun ReorderButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val appColors = LocalAppColors.current
    val haptic = rememberHaptic()
    val iconColor = when {
        !enabled -> appColors.textSecondary.copy(alpha = 0.25f)
        destructive -> Color(0xFFFF5A66)
        else -> appColors.textSecondary
    }

    Box(
        Modifier
            .size(32.dp)
            .pressScale(interaction, pressedScale = 0.90f)
            .clip(CircleShape)
            .background(
                if (appColors.isDark) Color.White.copy(alpha = 0.06f)
                else Color.Black.copy(alpha = 0.04f),
            )
            .border(1.dp, appColors.borderGlass, CircleShape)
            .clickable(interaction, indication = null, enabled = enabled) {
                haptic()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(18.dp),
        )
    }
}
