package com.gorilla.music.ui.screens.library

import android.widget.Toast
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.music.data.model.Album
import com.gorilla.music.data.model.Artist
import com.gorilla.music.data.model.Folder
import com.gorilla.music.data.model.Track
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.components.CollectionDetailPage
import com.gorilla.music.ui.components.EmptyState
import com.gorilla.music.ui.components.albumMetaLine
import com.gorilla.music.ui.components.artistMetaLine
import com.gorilla.music.ui.components.shareCollection
import com.gorilla.music.ui.components.SwipeToQueue
import com.gorilla.music.ui.components.formatDuration
import com.gorilla.music.ui.liquidglass.backdrop.Backdrop
import com.gorilla.music.ui.screens.playlists.PickPlaylistDialog
import com.gorilla.music.ui.screens.playlists.TextPromptDialog
import com.gorilla.music.ui.theme.GlassDepth
import com.gorilla.music.ui.theme.LiquidGlassSurface
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.LocalLiquidGlassBackdrop
import com.gorilla.music.ui.theme.LocalLiquidGlassContentBackdrop
import com.gorilla.music.ui.theme.LocalTrueLiquidGlassEnabled
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import com.gorilla.music.ui.theme.songCardColor

@Composable
fun LibraryScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    onOpenNowPlaying: () -> Unit,
    onOpenPlaylists: () -> Unit = {},
    onOpenRadio: () -> Unit = {},
    onBackToOrigin: () -> Unit = {},
    vm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val playback by app.playbackState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var sortMenuOpen by remember { mutableStateOf(false) }
    val playlists by app.playlists.collectAsStateWithLifecycle()
    var swipeTargetTrack by remember { mutableStateOf<Track?>(null) }
    var showPickPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    val requestedTab by app.requestedLibraryTab.collectAsStateWithLifecycle()
    var activeCategory by remember { mutableStateOf(requestedTab) }
    var showingGenres by remember { mutableStateOf(false) }
    var activeGenre by remember { mutableStateOf<String?>(null) }
    var activeFolder by remember { mutableStateOf<Folder?>(null) }
    var activeAlbum by remember { mutableStateOf<Album?>(null) }
    var activeArtist by remember { mutableStateOf<Artist?>(null) }
    val albumsListState = rememberLazyListState()
    var openedFromShortcut by remember { mutableStateOf(requestedTab != null) }

    val closeActiveCategory: () -> Unit = {
        if (openedFromShortcut) {
            openedFromShortcut = false
            onBackToOrigin()
        } else {
            activeCategory = null
        }
    }

    BackHandler(
        enabled = activeCategory != null || showingGenres || activeGenre != null ||
            activeFolder != null || activeAlbum != null || activeArtist != null
    ) {
        when {
            activeAlbum != null -> activeAlbum = null
            activeArtist != null -> activeArtist = null
            activeGenre != null -> activeGenre = null
            activeFolder != null -> activeFolder = null
            showingGenres -> showingGenres = false
            else -> closeActiveCategory()
        }
    }

    androidx.compose.runtime.LaunchedEffect(requestedTab) {
        requestedTab?.let { tab ->
            activeCategory = tab
            openedFromShortcut = true
            vm.selectTab(tab)
            app.requestLibraryTab(null)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(LocalAppColors.current.bgBase)
        ) {
            when (val state = uiState) {
                is LibraryScreenUiState.Loading -> Spacer(Modifier.fillMaxSize())
                is LibraryScreenUiState.Empty -> LibraryMenu(
                    songs = emptyList(),
                    contentPadding = contentPadding,
                    activeId = playback.current?.id,
                    onPlayRecent = {},
                    onOpenPlaylists = onOpenPlaylists,
                    onOpenRadio = onOpenRadio,
                    onOpenCategory = { tab ->
                        activeCategory = tab
                        openedFromShortcut = false
                        vm.selectTab(tab)
                    },
                )
                is LibraryScreenUiState.Success -> {
                    val genreGroups = remember(state.songs) { state.songs.groupByGenre() }
                    val selectedGenre = activeGenre
                    val currentSubpage = remember(
                        activeCategory, activeFolder, activeGenre, showingGenres, activeAlbum, activeArtist,
                    ) {
                        when {
                            activeAlbum != null -> "album:${activeAlbum?.id}"
                            activeArtist != null -> "artist:${activeArtist?.name}"
                            activeFolder != null -> "folder:${activeFolder?.path}"
                            activeGenre != null -> "genre:$activeGenre"
                            showingGenres -> "genres"
                            activeCategory != null -> "category:$activeCategory"
                            else -> null
                        }
                    }

                    AnimatedContent(
                        targetState = currentSubpage,
                        transitionSpec = {
                            if (librarySubpageDepth(targetState) > librarySubpageDepth(initialState)) {
                                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn())
                                    .togetherWith(slideOutHorizontally(tween(260)) { -it / 5 } + fadeOut())
                            } else {
                                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn())
                                    .togetherWith(slideOutHorizontally(tween(260)) { it / 5 } + fadeOut())
                            }
                        },
                        label = "librarySubpageTransition",
                        modifier = Modifier.fillMaxSize(),
                    ) { subpage ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(LocalAppColors.current.bgBase),
                        ) {
                            if (subpage == null) {
                                LibraryMenu(
                                    songs = state.songs,
                                    contentPadding = contentPadding,
                                    activeId = playback.current?.id,
                                    onPlayRecent = { track -> app.playTrack(track, state.songs) },
                                    onOpenPlaylists = onOpenPlaylists,
                                    onOpenRadio = onOpenRadio,
                                    onOpenCategory = { tab ->
                                        activeCategory = tab
                                        openedFromShortcut = false
                                        vm.selectTab(tab)
                                    },
                                )
                            } else if (subpage.startsWith("album:") || subpage.startsWith("artist:")) {
                                val album = if (subpage.startsWith("album:")) {
                                    val albumId = subpage.removePrefix("album:").toLongOrNull()
                                    state.albums.firstOrNull { it.id == albumId } ?: activeAlbum
                                } else {
                                    null
                                }
                                val artist = if (subpage.startsWith("artist:")) {
                                    val artistName = subpage.removePrefix("artist:")
                                    state.artists.firstOrNull { it.name == artistName } ?: activeArtist
                                } else {
                                    null
                                }
                                val collectionTracks = when {
                                    album != null -> state.songs.filter { it.displayAlbum == album.title && it.displayArtist == album.artist }
                                    artist != null -> state.songs.filter { it.displayArtist == artist.name }
                                    else -> emptyList()
                                }
                                val allFavorite =
                                    collectionTracks.isNotEmpty() && collectionTracks.all { it.isFavorite }
                                val collectionTitle = album?.title ?: artist?.name.orEmpty()
                                CollectionDetailPage(
                                    title = collectionTitle,
                                    subtitle = album?.artist,
                                    metaLine = if (album != null) {
                                        albumMetaLine(collectionTracks)
                                    } else {
                                        artistMetaLine(artist?.albumCount ?: 0, collectionTracks.size)
                                    },
                                    albumId = album?.id ?: artist?.representativeAlbumId ?: -1L,
                                    artworkUri = artist?.artworkUri
                                        ?: collectionTracks.firstOrNull { it.artworkUri != null }?.artworkUri,
                                    tracks = collectionTracks,
                                    activeId = playback.current?.id,
                                    isFavorite = allFavorite,
                                    contentPadding = contentPadding,
                                    onBack = {
                                        activeAlbum = null
                                        activeArtist = null
                                    },
                                    onPlay = { track -> app.playTrack(track, collectionTracks) },
                                    onMenu = { track -> app.openTrackMenu(track, collectionTracks) },
                                    isPlaying = collectionTracks.any { it.id == playback.current?.id } && playback.isPlaying,
                                    onPlayAll = {
                                        if (collectionTracks.any { it.id == playback.current?.id }) {
                                            app.togglePlayPause()
                                        } else {
                                            app.playTracks(collectionTracks, 0)
                                            onOpenNowPlaying()
                                        }
                                    },
                                    onShuffle = {
                                        app.playTracks(collectionTracks.shuffled(), 0)
                                        onOpenNowPlaying()
                                    },
                                    onAddToQueue = {
                                        collectionTracks.forEach { app.addToQueue(it) }
                                        Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                                    },
                                    onToggleFavorite = {
                                        app.setTracksFavorite(collectionTracks, !allFavorite)
                                    },
                                    onShare = { shareCollection(context, collectionTitle, collectionTracks) },
                                )
                            } else if (subpage.startsWith("folder:")) {
                                val folderPath = subpage.removePrefix("folder:")
                                val folder = state.folders.firstOrNull { it.path == folderPath } ?: activeFolder
                                if (folder != null) {
                                    val folderSongs = state.songs.filter { it.folder == folder.path }
                                    Column(Modifier.fillMaxSize()) {
                                        LibraryDetailHeader(
                                            title = folder.name,
                                            onBack = { activeFolder = null },
                                        )
                                        SongsList(
                                            songs = folderSongs,
                                            activeId = playback.current?.id,
                                            contentPadding = contentPadding,
                                            listState = listState,
                                            onPlay = { track -> app.playTrack(track, folderSongs) },
                                            onQueue = { track ->
                                                swipeTargetTrack = track
                                                if (playlists.isEmpty()) showCreatePlaylistDialog = true else showPickPlaylistDialog = true
                                            },
                                            onMenu = { track -> app.openTrackMenu(track, folderSongs) },
                                        )
                                    }
                                }
                            } else if (subpage.startsWith("genre:")) {
                                val genreName = subpage.removePrefix("genre:")
                                val genreSongs = genreGroups.firstOrNull { it.name == genreName }?.tracks.orEmpty()
                                Column(Modifier.fillMaxSize()) {
                                    LibraryDetailHeader(
                                        title = genreName,
                                        onBack = { activeGenre = null },
                                    )
                                    SongsList(
                                        songs = genreSongs,
                                        activeId = playback.current?.id,
                                        contentPadding = contentPadding,
                                        listState = listState,
                                        onPlay = { track -> app.playTrack(track, genreSongs) },
                                        onQueue = { track ->
                                            swipeTargetTrack = track
                                            if (playlists.isEmpty()) showCreatePlaylistDialog = true else showPickPlaylistDialog = true
                                        },
                                        onMenu = { track -> app.openTrackMenu(track, genreSongs) },
                                    )
                                }
                            } else if (subpage == "genres") {
                                Column(Modifier.fillMaxSize()) {
                                    LibraryDetailHeader(
                                        title = "Genres",
                                        onBack = { showingGenres = false },
                                    )
                                    GenresList(
                                        genres = genreGroups,
                                        contentPadding = contentPadding,
                                        onClick = { genre -> activeGenre = genre.name },
                                    )
                                }
                            } else if (subpage.startsWith("category:")) {
                                val categoryName = subpage.removePrefix("category:")
                                val selectedCategory = LibraryTab.entries.firstOrNull { it.name == categoryName } ?: activeCategory
                                if (selectedCategory != null) {
                                    if (selectedCategory == LibraryTab.SONGS) {
                                        SongsCollectionScreen(
                                            songs = state.songs,
                                            activeId = playback.current?.id,
                                            contentPadding = contentPadding,
                                            sort = state.sort,
                                            sortMenuOpen = sortMenuOpen,
                                            onBack = closeActiveCategory,
                                            onOpenSort = { sortMenuOpen = true },
                                            onDismissSort = { sortMenuOpen = false },
                                            onSort = {
                                                vm.setSort(it)
                                                sortMenuOpen = false
                                            },
                                            onPlay = { track -> app.playTrack(track, state.songs) },
                                            onQueue = { track ->
                                                swipeTargetTrack = track
                                                if (playlists.isEmpty()) showCreatePlaylistDialog = true else showPickPlaylistDialog = true
                                            },
                                            onMenu = { track -> app.openTrackMenu(track, state.songs) },
                                        )
                                    } else {
                                        Column(Modifier.fillMaxSize()) {
                                            LibraryDetailHeader(
                                                title = selectedCategory.title,
                                                onBack = closeActiveCategory,
                                            )
                                            when (selectedCategory) {
                                                LibraryTab.SONGS -> Unit
                                                LibraryTab.ARTISTS -> ArtistsList(state.artists, contentPadding) { artist ->
                                                    activeArtist = artist
                                                }
                                                LibraryTab.ALBUMS -> AlbumsList(state.albums, contentPadding, albumsListState) { album ->
                                                    activeAlbum = album
                                                }
                                                LibraryTab.FOLDERS -> FoldersList(state.folders, contentPadding, app) { folder ->
                                                    activeFolder = folder
                                                }
                                                LibraryTab.FAVORITES -> SongsList(
                                                    songs = state.songs.filter { it.isFavorite },
                                                    activeId = playback.current?.id,
                                                    contentPadding = contentPadding,
                                                    listState = listState,
                                                    onPlay = { track ->
                                                        val favorites = state.songs.filter { it.isFavorite }
                                                        app.playTrack(track, favorites)
                                                    },
                                                    onQueue = { track ->
                                                        swipeTargetTrack = track
                                                        if (playlists.isEmpty()) showCreatePlaylistDialog = true else showPickPlaylistDialog = true
                                                    },
                                                    onMenu = { track ->
                                                        app.openTrackMenu(track, state.songs.filter { it.isFavorite })
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            }
                        }
                    }
                }
            }

        val targetTrack = swipeTargetTrack
        if (targetTrack != null) {
            if (showPickPlaylistDialog) {
                PickPlaylistDialog(
                    playlists = playlists,
                    onPick = { playlist ->
                        app.addTrackToPlaylist(playlist.id, targetTrack.id)
                        Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                        showPickPlaylistDialog = false
                        swipeTargetTrack = null
                    },
                    onCreateNew = {
                        showPickPlaylistDialog = false
                        showCreatePlaylistDialog = true
                    },
                    onDismiss = {
                        showPickPlaylistDialog = false
                        swipeTargetTrack = null
                    },
                )
            }

            if (showCreatePlaylistDialog) {
                TextPromptDialog(
                    title = "Create playlist",
                    initial = "",
                    confirmLabel = "Create",
                    onConfirm = { name ->
                        app.createPlaylistWithTrack(name, targetTrack.id)
                        Toast.makeText(context, "Created $name and added song", Toast.LENGTH_SHORT).show()
                        showCreatePlaylistDialog = false
                        swipeTargetTrack = null
                    },
                    onDismiss = {
                        showCreatePlaylistDialog = false
                        swipeTargetTrack = null
                    },
                )
            }
        }
    }
}

private fun librarySubpageDepth(subpage: String?): Int = when {
    subpage == null -> 0
    subpage.startsWith("category:") || subpage == "genres" -> 1
    else -> 2
}

private val LibraryTab.title: String
    get() = when (this) {
        LibraryTab.FAVORITES -> "Favourites"
        else -> name.lowercase().replaceFirstChar { it.uppercase() }
    }

internal data class GenreGroup(
    val name: String,
    val tracks: List<Track>,
)

internal fun List<Track>.groupByGenre(): List<GenreGroup> =
    asSequence()
        .filter { !it.genre.isNullOrBlank() }
        .groupBy { it.genre!!.trim() }
        .map { (genre, tracks) -> GenreGroup(genre, tracks.sortedBy { it.title.lowercase() }) }
        .sortedWith(compareByDescending<GenreGroup> { it.tracks.size }.thenBy { it.name.lowercase() })

@Composable
private fun LibraryMenu(
    songs: List<Track>,
    contentPadding: PaddingValues,
    activeId: Long?,
    onPlayRecent: (Track) -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenRadio: () -> Unit,
    onOpenCategory: (LibraryTab) -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            LibraryHeader()
        }

        item {
            LibraryBentoGrid(
                songCount = songs.size,
                onOpenPlaylists = onOpenPlaylists,
                onOpenCategory = onOpenCategory,
                onOpenRadio = onOpenRadio,
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .height(1.dp)
                    .background(LocalAppColors.current.borderGlass),
            )
        }

        item {
            RecentlyAddedSection(
                songs = songs,
                activeId = activeId,
                onPlay = onPlayRecent,
                onSeeAll = { onOpenCategory(LibraryTab.SONGS) },
            )
        }
    }
}

@Composable
private fun LibraryHeader() {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "LOCAL COLLECTION",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = "Library",
                color = appColors.textPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 38.sp,
            )
        }
    }
}

@Composable
private fun LibraryBentoGrid(
    songCount: Int,
    onOpenPlaylists: () -> Unit,
    onOpenCategory: (LibraryTab) -> Unit,
    onOpenRadio: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoCard(
                title = "Playlists",
                subtitle = "Your saved mixes",
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                gradientColors = listOf(Color(0xFF5C2038), Color(0xFF2A0E19)),
                onClick = onOpenPlaylists,
                modifier = Modifier.weight(1f),
            )
            BentoCard(
                title = "Songs",
                subtitle = "$songCount local tracks",
                icon = Icons.Rounded.MusicNote,
                gradientColors = listOf(Color(0xFF175A47), Color(0xFF0A2A20)),
                onClick = { onOpenCategory(LibraryTab.SONGS) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoCard(
                title = "Favourites",
                subtitle = "Tracks you love",
                icon = Icons.Rounded.Favorite,
                gradientColors = listOf(Color(0xFF7A2646), Color(0xFF3A1022)),
                onClick = { onOpenCategory(LibraryTab.FAVORITES) },
                modifier = Modifier.weight(1f),
            )
            BentoCard(
                title = "Radio",
                subtitle = "Internet stations",
                icon = Icons.Rounded.Radio,
                gradientColors = listOf(Color(0xFF1F3F66), Color(0xFF0D1D33)),
                onClick = onOpenRadio,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BentoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val cardShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .height(130.dp)
            .instantClickable(pressedScale = 0.95f) {
                onClick()
            }
            .clip(cardShape)
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, Color.White.copy(alpha = 0.10f), cardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RecentlyAddedSection(
    songs: List<Track>,
    activeId: Long?,
    onPlay: (Track) -> Unit,
    onSeeAll: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val recentTracks = remember(songs) { songs.sortedByDescending { it.dateAddedSec }.take(10) }

    if (recentTracks.isEmpty()) return

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "ON DEVICE",
                    color = appColors.textSecondary.copy(alpha = 0.60f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                )
                Text(
                    text = "Recently Added",
                    color = appColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            val seeAllInteraction = remember { MutableInteractionSource() }
            val haptic = rememberHaptic()
            Text(
                text = "See All",
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .instantClickable(pressedScale = 0.92f) {
                        onSeeAll()
                    }
                    .padding(bottom = 2.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(recentTracks, key = { it.id }) { track ->
                RecentlyAddedSquareCard(
                    track = track,
                    isActive = activeId == track.id,
                    onClick = { onPlay(track) },
                )
            }
        }
    }
}

@Composable
private fun RecentlyAddedSquareCard(
    track: Track,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val artShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(128.dp)
            .instantClickable(pressedScale = 0.95f) {
                onClick()
            },
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(artShape)
                .border(
                    1.dp,
                    if (isActive) accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                    artShape,
                ),
        ) {
            AlbumArt(
                albumId = track.albumId,
                artworkUri = track.artworkUri,
                shape = artShape,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = track.title,
            color = if (isActive) accent else appColors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = track.displayArtist,
            color = appColors.textSecondary,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun LibraryDetailHeader(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val backInteraction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val backPillColor = appColors.bgSurface.copy(alpha = 0.94f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    text = title,
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = appColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) trailing()
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .instantClickable(pressedScale = 0.90f) {
                    onBack()
                }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(backPillColor, backPillColor)))
                .border(1.dp, appColors.borderGlass, CircleShape)
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
    }
}

@Composable
private fun HeaderIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val buttonColor = appColors.bgSurface.copy(alpha = 0.94f)

    Box(
        modifier = Modifier
            .instantClickable(pressedScale = 0.90f) { onClick() }
            .size(40.dp)
            .shadow(8.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(buttonColor, buttonColor)))
            .border(1.dp, appColors.borderGlass, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SortButton(onClick: () -> Unit) {
    val accent = LocalDynamicColors.current.accent
    HeaderIconButton(onClick = onClick) {
        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort", tint = accent)
    }
}

@Composable
private fun SortOption(label: String, order: SortOrder, current: SortOrder, onPick: (SortOrder) -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = if (order == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
        onClick = { onPick(order) },
    )
}

private enum class SongsLayout {
    LIST,
    GRID,
}

@Composable
private fun SongsCollectionScreen(
    songs: List<Track>,
    activeId: Long?,
    contentPadding: PaddingValues,
    sort: SortOrder,
    sortMenuOpen: Boolean,
    onBack: () -> Unit,
    onOpenSort: () -> Unit,
    onDismissSort: () -> Unit,
    onSort: (SortOrder) -> Unit,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onMenu: (Track) -> Unit,
) {
    var layout by remember { mutableStateOf(SongsLayout.LIST) }
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "Songs",
                        color = appColors.textPrimary,
                        fontSize = 34.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Box {
                    SortButton(onClick = onOpenSort)
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = onDismissSort) {
                        SortOption("Title A-Z", SortOrder.TITLE_ASC, sort, onSort)
                        SortOption("Title Z-A", SortOrder.TITLE_DESC, sort, onSort)
                        SortOption("Artist", SortOrder.ARTIST_ASC, sort, onSort)
                        SortOption("Recently added", SortOrder.DATE_ADDED_DESC, sort, onSort)
                        SortOption("Longest", SortOrder.DURATION_DESC, sort, onSort)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                val haptic = rememberHaptic()
                val backPillColor = appColors.bgSurface.copy(alpha = 0.94f)

                Box(
                    modifier = Modifier
                        .instantClickable(pressedScale = 0.90f) {
                            onBack()
                        }
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(backPillColor, backPillColor)))
                        .border(1.dp, appColors.borderGlass, CircleShape)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${songs.size} ${if (songs.size == 1) "SONG" else "SONGS"}",
                        color = appColors.textSecondary.copy(alpha = 0.70f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                    )
                    SongsLayoutToggle(
                        selected = layout,
                        onSelected = { layout = it },
                    )
                }
            }
        }

        if (songs.isEmpty()) {
            EmptyState("No songs", "Scan your library from Settings > About.")
        } else if (layout == SongsLayout.LIST) {
            SongsCardList(
                songs = songs,
                activeId = activeId,
                contentPadding = contentPadding,
                onPlay = onPlay,
                onQueue = onQueue,
                onMenu = onMenu,
            )
        } else {
            SongsCardGrid(
                songs = songs,
                activeId = activeId,
                contentPadding = contentPadding,
                onPlay = onPlay,
                onMenu = onMenu,
            )
        }
    }
}

@Composable
private fun SongsLayoutToggle(
    selected: SongsLayout,
    onSelected: (SongsLayout) -> Unit,
) {
    val appColors = LocalAppColors.current
    val outerShape = RoundedCornerShape(18.dp)
    val barColor = appColors.bgSurface.copy(alpha = 0.94f)

    Row(
        modifier = Modifier
            .clip(outerShape)
            .background(Brush.linearGradient(listOf(barColor, barColor)))
            .border(1.dp, appColors.borderGlass, outerShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SongsLayout.entries.forEach { layout ->
            val selectedLayout = layout == selected
            val interaction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .instantClickable(pressedScale = 0.90f) { onSelected(layout) }
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selectedLayout) accentSelectionColor() else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (selectedLayout) LocalDynamicColors.current.accent.copy(alpha = 0.50f) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (layout == SongsLayout.LIST) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                    contentDescription = null,
                    tint = if (selectedLayout) LocalDynamicColors.current.accent else appColors.textSecondary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = if (layout == SongsLayout.LIST) "List" else "Grid",
                    color = if (selectedLayout) LocalDynamicColors.current.accent else appColors.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun accentSelectionColor(): Color =
    LocalDynamicColors.current.accent.copy(alpha = 0.18f)

@Composable
private fun SongsCardList(
    songs: List<Track>,
    activeId: Long?,
    contentPadding: PaddingValues,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onMenu: (Track) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = 2.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(songs, key = { it.id }) { track ->
            SongListCard(
                track = track,
                isActive = activeId == track.id,
                onClick = { onPlay(track) },
                onMenu = { onMenu(track) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SongListCard(
    track: Track,
    isActive: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val shape = RoundedCornerShape(18.dp)
    val rowColor = appColors.songCardColor()
    val background = if (isActive) Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), rowColor)) else Brush.linearGradient(listOf(rowColor, rowColor))

    Row(
        modifier = modifier
            .instantClickable(pressedScale = 0.96f) {
                onClick()
            }
            .clip(shape)
            .background(background)
            .border(
                1.dp,
                if (isActive) accent.copy(alpha = 0.42f) else appColors.borderGlass,
                shape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = track.albumId,
            artworkUri = track.artworkUri,
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
        ) {
            Text(
                text = track.title,
                color = if (isActive) accent else appColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (track.durationMs > 0) "${track.displayArtist} • ${formatDuration(track.durationMs)}" else track.displayArtist,
                color = appColors.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .instantClickable(pressedScale = 0.90f) {
                    onMenu()
                }
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (appColors.isDark) Color.White.copy(alpha = 0.06f)
                    else Color.Black.copy(alpha = 0.04f),
                )
                .border(1.dp, appColors.borderGlass, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MoreHoriz,
                contentDescription = "Track options",
                tint = appColors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SongsCardGrid(
    songs: List<Track>,
    activeId: Long?,
    contentPadding: PaddingValues,
    onPlay: (Track) -> Unit,
    onMenu: (Track) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 2.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItems(songs, key = { it.id }) { track ->
            SongGridCard(
                track = track,
                isActive = activeId == track.id,
                onClick = { onPlay(track) },
                onMenu = { onMenu(track) },
            )
        }
    }
}

@Composable
private fun SongGridCard(
    track: Track,
    isActive: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val interaction = remember { MutableInteractionSource() }
    val menuInteraction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val shape = RoundedCornerShape(22.dp)
    val cardColor = appColors.songCardColor()
    val background = if (isActive) {
        Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), cardColor))
    } else {
        Brush.linearGradient(listOf(cardColor, cardColor))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .instantClickable(pressedScale = 0.96f) {
                onClick()
            }
            .clip(shape)
            .background(background)
            .border(
                1.dp,
                if (isActive) accent.copy(alpha = 0.42f) else appColors.borderGlass,
                shape,
            )
            .padding(12.dp),
    ) {
        Box {
            AlbumArt(
                albumId = track.albumId,
                artworkUri = track.artworkUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(menuInteraction, indication = null) {
                        haptic()
                        onMenu()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    contentDescription = "Track options",
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = track.title,
            color = if (isActive) accent else appColors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = track.displayArtist,
            color = appColors.textSecondary,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
internal fun SongsList(
    songs: List<Track>,
    activeId: Long?,
    contentPadding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState,
    enableSwipeToQueue: Boolean = true,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onMenu: (Track) -> Unit,
) {
    if (songs.isEmpty()) {
        EmptyState("No songs", "Scan your library from Settings > About.")
        return
    }
    LazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { track ->
            if (enableSwipeToQueue) {
                SwipeToQueue(
                    onQueue = { onQueue(track) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    LibrarySongRow(
                        track = track,
                        isActive = activeId == track.id,
                        onClick = { onPlay(track) },
                        onMenu = { onMenu(track) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    LibrarySongRow(
                        track = track,
                        isActive = activeId == track.id,
                        onClick = { onPlay(track) },
                        onMenu = { onMenu(track) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun GenresList(
    genres: List<GenreGroup>,
    contentPadding: PaddingValues,
    onClick: (GenreGroup) -> Unit,
) {
    if (genres.isEmpty()) {
        EmptyState("No genres", "Genre metadata was not found in your current songs.")
        return
    }
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        items(genres, key = { it.name }) { genre ->
            GroupRow(
                title = genre.name,
                subtitle = "${genre.tracks.size} songs",
                icon = Icons.Rounded.GraphicEq,
                onClick = { onClick(genre) },
            )
        }
    }
}

@Composable
private fun LibrarySongRow(
    track: Track,
    isActive: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val interaction = remember { MutableInteractionSource() }
    val menuInteraction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val shape = RoundedCornerShape(16.dp)
    val rowColor = appColors.songCardColor()
    val background = if (isActive) Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), rowColor)) else Brush.linearGradient(listOf(rowColor, rowColor))

    Row(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.96f)
            .clip(shape)
            .background(background)
            .border(
                1.dp,
                if (isActive) accent.copy(alpha = 0.42f) else appColors.borderGlass,
                shape,
            )
            .clickable(interaction, indication = null) { haptic(); onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(track.albumId, Modifier.size(52.dp), shape = RoundedCornerShape(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 8.dp),
        ) {
            Text(
                text = track.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) accent else appColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (track.durationMs > 0) "${track.displayArtist} • ${formatDuration(track.durationMs)}" else track.displayArtist,
                fontSize = 12.5.sp,
                color = appColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .pressScale(menuInteraction, pressedScale = 0.92f)
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (appColors.isDark) Color.White.copy(alpha = 0.06f)
                    else Color.Black.copy(alpha = 0.04f),
                )
                .border(1.dp, appColors.borderGlass, CircleShape)
                .clickable(menuInteraction, indication = null) { haptic(); onMenu() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = "Track options", tint = appColors.textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun ArtistsList(artists: List<Artist>, contentPadding: PaddingValues, onClick: (Artist) -> Unit) {
    if (artists.isEmpty()) { EmptyState("No artists", "Scan your library to populate artists."); return }
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        items(artists, key = { it.name }) { artist ->
            GroupRow(
                title = artist.name,
                subtitle = "${artist.trackCount} songs • ${artist.albumCount} albums",
                albumId = artist.representativeAlbumId,
                artworkUri = artist.artworkUri,
                artworkShape = CircleShape,
                icon = Icons.Rounded.Person,
                onClick = { onClick(artist) },
            )
        }
    }
}

@Composable
internal fun AlbumsList(albums: List<Album>, contentPadding: PaddingValues, listState: androidx.compose.foundation.lazy.LazyListState, onClick: (Album) -> Unit) {
    if (albums.isEmpty()) { EmptyState("No albums", "Scan your library to populate albums."); return }
    LazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        items(albums, key = { it.id }) { album ->
            GroupRow(
                title = album.title,
                subtitle = "${album.artist} • ${album.trackCount} songs",
                albumId = album.id,
                onClick = { onClick(album) },
            )
        }
    }
}

@Composable
internal fun FoldersList(
    folders: List<Folder>, 
    contentPadding: PaddingValues, 
    app: AppViewModel,
    onClick: (Folder) -> Unit
) {
    if (folders.isEmpty()) { EmptyState("No folders", "Scan your library to populate folders."); return }
    var folderOptions by remember { mutableStateOf<Folder?>(null) }
    val context = LocalContext.current
    
    val settings by app.settings.collectAsState()
    val customOrder = settings?.customFolderOrder ?: emptyList()
    val hiddenFolders = settings?.hiddenFolders ?: emptyList()
    
    val displayFolders = remember(folders, customOrder, hiddenFolders) {
        val visible = folders.filter { it.path !in hiddenFolders }
        if (customOrder.isNotEmpty()) {
            visible.sortedBy { customOrder.indexOf(it.path).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
        } else {
            visible
        }
    }

    if (displayFolders.isEmpty()) { EmptyState("No folders", "All folders are hidden."); return }

    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        items(displayFolders, key = { it.path }) { folder ->
            GroupRow(
                title = folder.name,
                subtitle = "${folder.trackCount} songs",
                icon = Icons.Rounded.Folder,
                onClick = { onClick(folder) },
                onLongClick = { folderOptions = folder }
            )
        }
    }

    folderOptions?.let { folder ->
        CategoryOptionsDialog(
            title = folder.name,
            onDismiss = { folderOptions = null }
        ) {
            CategoryOptionRow(
                icon = Icons.Rounded.ArrowUpward,
                iconColor = LocalDynamicColors.current.accent,
                label = "Move Up",
                onClick = { app.moveFolder(folder.path, up = true, displayFolders); folderOptions = null },
            )
            CategoryOptionRow(
                icon = Icons.Rounded.ArrowDownward,
                iconColor = LocalDynamicColors.current.accent,
                label = "Move Down",
                onClick = { app.moveFolder(folder.path, up = false, displayFolders); folderOptions = null },
            )
            CategoryOptionRow(
                icon = Icons.Rounded.Delete,
                iconColor = Color(0xFFFF3B30),
                label = "Delete",
                onClick = { app.removeFolder(folder.path); folderOptions = null },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun GroupRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    albumId: Long? = null,
    artworkUri: android.net.Uri? = null,
    artworkShape: Shape = RoundedCornerShape(12.dp),
    icon: ImageVector = Icons.Rounded.Album,
) {
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pressScale(interaction, pressedScale = 0.97f)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { haptic(); onClick() },
                onLongClick = { haptic(); onLongClick?.invoke() }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (albumId != null) {
            AlbumArt(
                albumId = albumId,
                artworkUri = artworkUri,
                modifier = Modifier.size(54.dp),
                shape = artworkShape,
            )
        } else {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(artworkShape)
                    .background(appColors.bgSurface.copy(alpha = 0.90f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = appColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = appColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = appColors.textSecondary.copy(alpha = 0.55f))
    }
    DividerLine(start = 82.dp)
}

@Composable
private fun DividerLine(start: androidx.compose.ui.unit.Dp) {
    val appColors = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = start, end = 16.dp)
            .height(1.dp)
            .background(appColors.borderGlass)
    )
}

@Composable
private fun CategoryOptionsDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backdrop = LocalLiquidGlassContentBackdrop.current ?: LocalLiquidGlassBackdrop.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            CategoryOptionsPanel(
                title = title,
                backdrop = backdrop,
                content = content,
            )
        }
    }
}

@Composable
private fun CategoryOptionsPanel(
    title: String,
    backdrop: Backdrop?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appColors = LocalAppColors.current
    val panelShape = RoundedCornerShape(24.dp)
    val liquidGlassEnabled = LocalTrueLiquidGlassEnabled.current

    val modifier = Modifier
        .width(280.dp)
        .clickable(enabled = false) {}

    if (liquidGlassEnabled) {
        LiquidGlassSurface(
            depth = GlassDepth.HIGH,
            shape = panelShape,
            border = true,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = appColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                content()
            }
        }
    } else {
        Box(
            modifier = modifier
                .clip(panelShape)
                .background(appColors.bgSurface)
                .border(1.dp, appColors.borderGlass, panelShape)
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = appColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                content()
            }
        }
    }
}

@Composable
private fun CategoryOptionRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    onClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.95f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interaction, indication = null) {
                haptic()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = appColors.textPrimary)
    }
}
