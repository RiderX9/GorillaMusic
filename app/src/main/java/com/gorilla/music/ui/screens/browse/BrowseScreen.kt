package com.gorilla.music.ui.screens.browse

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gorilla.music.data.db.PlaylistEntity
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
import com.gorilla.music.ui.components.ScrollableLiquidGlassTabBar
import com.gorilla.music.ui.components.TopBarIconButton
import com.gorilla.music.ui.components.formatDuration
import com.gorilla.music.ui.components.formatFileSize
import com.gorilla.music.ui.screens.library.GenreGroup
import com.gorilla.music.ui.screens.library.LibraryScreenUiState
import com.gorilla.music.ui.screens.library.LibraryViewModel
import com.gorilla.music.ui.screens.library.SortOrder
import com.gorilla.music.ui.screens.library.groupByGenre
import com.gorilla.music.ui.screens.playlists.PlaylistsViewModel
import com.gorilla.music.ui.screens.playlists.TextPromptDialog
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.accentBloom
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.pressScale
import com.gorilla.music.ui.theme.rememberHaptic
import com.gorilla.music.ui.theme.songCardColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private enum class BrowseTab(val label: String) {
    SONGS("Songs"),
    DATES("Dates"),
    FILES("Files"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
}

private sealed interface BrowseDetail {
    val title: String
    val tracks: List<Track>

    data class AlbumPage(val album: Album, override val tracks: List<Track>) : BrowseDetail {
        override val title: String = album.title
    }

    data class ArtistPage(val artist: Artist, override val tracks: List<Track>) : BrowseDetail {
        override val title: String = artist.name
    }

    data class GenrePage(val genre: GenreGroup) : BrowseDetail {
        override val title: String = genre.name
        override val tracks: List<Track> = genre.tracks
    }

    data class YearPage(val year: GenreGroup) : BrowseDetail {
        override val title: String = year.name
        override val tracks: List<Track> = year.tracks
    }

    data class FolderPage(val folder: Folder, override val tracks: List<Track>) : BrowseDetail {
        override val title: String = folder.name
    }

    data class PlaylistPage(
        val id: Long,
        override val title: String,
    ) : BrowseDetail {
        override val tracks: List<Track> = emptyList()
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun BrowseScreen(
    app: AppViewModel,
    contentPadding: PaddingValues,
    onOpenNowPlaying: () -> Unit,
    vm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
    playlistsVm: PlaylistsViewModel = viewModel(factory = PlaylistsViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val playback by app.playbackState.collectAsStateWithLifecycle()
    val playlists by playlistsVm.allPlaylists.collectAsStateWithLifecycle()
    val selectedPlaylistTracks by playlistsVm.selectedTracks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tabs = BrowseTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val albumsGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    var detail by remember { mutableStateOf<BrowseDetail?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = detail != null) { detail = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.bgBase)
    ) {
        AnimatedContent(
            targetState = detail,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn())
                        .togetherWith(slideOutHorizontally(tween(260)) { -it / 5 } + fadeOut())
                } else {
                    (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn())
                        .togetherWith(slideOutHorizontally(tween(260)) { it / 5 } + fadeOut())
                }
            },
            label = "browseDetail",
            modifier = Modifier.fillMaxSize(),
        ) { selectedDetail ->
            if (selectedDetail != null) {
                val trackMap = if (state is com.gorilla.music.ui.screens.library.LibraryScreenUiState.Success) {
                    (state as com.gorilla.music.ui.screens.library.LibraryScreenUiState.Success).songs.associateBy { it.id }
                } else emptyMap()
                val detailTracks = if (selectedDetail is BrowseDetail.PlaylistPage) {
                    selectedPlaylistTracks ?: emptyList()
                } else {
                    selectedDetail.tracks.map { trackMap[it.id] ?: it }
                }
                if (selectedDetail is BrowseDetail.AlbumPage || selectedDetail is BrowseDetail.ArtistPage) {
                    val album = (selectedDetail as? BrowseDetail.AlbumPage)?.album
                    val artist = (selectedDetail as? BrowseDetail.ArtistPage)?.artist
                    CollectionDetailPage(
                        title = selectedDetail.title,
                        subtitle = album?.artist,
                        metaLine = if (album != null) {
                            albumMetaLine(detailTracks)
                        } else {
                            artistMetaLine(artist?.albumCount ?: 0, detailTracks.size)
                        },
                        albumId = album?.id ?: artist?.representativeAlbumId ?: -1L,
                        artworkUri = artist?.artworkUri
                            ?: detailTracks.firstOrNull { it.artworkUri != null }?.artworkUri,
                        tracks = detailTracks,
                        activeId = playback.current?.id,
                        isFavorite = detailTracks.isNotEmpty() && detailTracks.all { it.isFavorite },
                        contentPadding = contentPadding,
                        onBack = { detail = null },
                        onPlay = { track -> app.playTrack(track, detailTracks) },
                        onMenu = { track -> app.openTrackMenu(track, detailTracks) },
                        isPlaying = detailTracks.any { it.id == playback.current?.id } && playback.isPlaying,
                        onPlayAll = {
                            if (detailTracks.any { it.id == playback.current?.id }) {
                                app.togglePlayPause()
                            } else {
                                app.playTracks(detailTracks, 0)
                                onOpenNowPlaying()
                            }
                        },
                        onShuffle = {
                            app.playTracks(detailTracks.shuffled(), 0)
                            onOpenNowPlaying()
                        },
                        onAddToQueue = {
                            detailTracks.forEach { app.addToQueue(it) }
                            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                        },
                        onToggleFavorite = {
                            val allFavorite = detailTracks.isNotEmpty() && detailTracks.all { it.isFavorite }
                            app.setTracksFavorite(detailTracks, !allFavorite)
                        },
                        onShare = { shareCollection(context, selectedDetail.title, detailTracks) },
                    )
                } else BrowseDetailPage(
                    title = selectedDetail.title,
                    tracks = detailTracks,
                    activeId = playback.current?.id,
                    contentPadding = contentPadding,
                    onBack = { detail = null },
                    onPlay = { track -> app.playTrack(track, detailTracks) },
                    onQueue = { track ->
                        app.addToQueue(track)
                        Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                    },
                    onMenu = { track -> app.openTrackMenu(track, detailTracks) },
                    onPlayAll = {
                        app.playTracks(detailTracks, 0)
                        onOpenNowPlaying()
                    },
                    onShuffle = {
                        app.playTracks(detailTracks.shuffled(), 0)
                        onOpenNowPlaying()
                    },
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    BrowseHeader()
                    BrowseTabs(
                        tabs = tabs,
                        pagerState = pagerState,
                        onSelect = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                    )
                    when (val ui = state) {
                        LibraryScreenUiState.Loading -> Spacer(Modifier.fillMaxSize())
                        LibraryScreenUiState.Empty -> EmptyState(
                            title = "Your library is empty",
                            subtitle = "Add music to your device, then scan from Settings > About.",
                        )
                        is LibraryScreenUiState.Success -> {
                            val genres = remember(ui.songs) { ui.songs.groupByGenre() }
                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 1,
                                flingBehavior = PagerDefaults.flingBehavior(
                                    state = pagerState,
                                    pagerSnapDistance = PagerSnapDistance.atMost(1),
                                ),
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                when (tabs[page]) {
                                    BrowseTab.SONGS -> BrowseSongsPage(
                                        songs = ui.songs,
                                        sort = ui.sort,
                                        activeId = playback.current?.id,
                                        contentPadding = contentPadding,
                                        onSort = vm::setSort,
                                        onPlay = { track -> app.playTrack(track, ui.songs) },
                                        onQueue = { track ->
                                            app.addToQueue(track)
                                            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                                        },
                                        onMenu = { track -> app.openTrackMenu(track, ui.songs) },
                                        onPlayAll = {
                                            app.playTracks(ui.songs, 0)
                                            onOpenNowPlaying()
                                        },
                                        onShuffle = {
                                            app.playTracks(ui.songs.shuffled(), 0)
                                            onOpenNowPlaying()
                                        },
                                    )
                                    BrowseTab.DATES -> BrowseDatesPage(
                                        songs = ui.songs,
                                        activeId = playback.current?.id,
                                        contentPadding = contentPadding,
                                        onPlay = { track -> app.playTrack(track, ui.songs) },
                                        onQueue = { track ->
                                            app.addToQueue(track)
                                            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                                        },
                                        onMenu = { track -> app.openTrackMenu(track, ui.songs) },
                                    )
                                    BrowseTab.FILES -> BrowseFilesPage(
                                        songs = ui.songs,
                                        contentPadding = contentPadding,
                                        onClick = { label, tracks ->
                                            detail = BrowseDetail.GenrePage(GenreGroup(label, tracks))
                                        },
                                    )
                                    BrowseTab.FOLDERS -> BrowseFolderPathsPage(
                                        folders = ui.folders,
                                        contentPadding = contentPadding,
                                        onClick = { folder ->
                                            detail = BrowseDetail.FolderPage(
                                                folder,
                                                ui.songs.filter { it.folder == folder.path },
                                            )
                                        },
                                    )
                                    BrowseTab.PLAYLISTS -> BrowsePlaylistsPage(
                                        playlists = playlists,
                                        contentPadding = contentPadding,
                                        onCreateNew = { showCreatePlaylistDialog = true },
                                        onClick = { playlist ->
                                            playlistsVm.open(playlist.id)
                                            detail = BrowseDetail.PlaylistPage(
                                                id = playlist.id,
                                                title = playlist.name,
                                            )
                                        },
                                    )
                                    BrowseTab.ALBUMS -> BrowseAlbumsPage(
                                        albums = ui.albums,
                                        contentPadding = contentPadding,
                                        gridState = albumsGridState,
                                        onClick = { album ->
                                            detail = BrowseDetail.AlbumPage(
                                                album,
                                                ui.songs.filter { it.displayAlbum == album.title && it.displayArtist == album.artist },
                                            )
                                        },
                                    )
                                    BrowseTab.ARTISTS -> BrowseArtistsPage(ui.artists, contentPadding) { artist ->
                                        detail = BrowseDetail.ArtistPage(
                                            artist,
                                            ui.songs.filter { it.displayArtist == artist.name },
                                        )
                                    }
                                    BrowseTab.GENRES -> BrowseGenresPage(genres, contentPadding) { genre ->
                                        detail = BrowseDetail.GenrePage(genre)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreatePlaylistDialog) {
            TextPromptDialog(
                title = "New Playlist",
                initial = "",
                confirmLabel = "Create",
                onDismiss = { showCreatePlaylistDialog = false },
                onConfirm = { name ->
                    showCreatePlaylistDialog = false
                    playlistsVm.create(name)
                },
            )
        }
    }
}

@Composable
private fun BrowseHeader() {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "LOCAL EXPLORER",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Text(
                text = "Browse",
                color = appColors.textPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 38.sp,
            )
        }
    }
}

@Composable
private fun BrowseTabs(
    tabs: List<BrowseTab>,
    pagerState: PagerState,
    onSelect: (Int) -> Unit,
) {
    ScrollableLiquidGlassTabBar(
        labels = tabs.map { it.label },
        selectedIndex = pagerState.currentPage,
        selectionPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction,
        onSelect = onSelect,
        compact = true,
    )
}

@Composable
private fun BrowseSongsPage(
    songs: List<Track>,
    sort: SortOrder,
    activeId: Long?,
    contentPadding: PaddingValues,
    onSort: (SortOrder) -> Unit,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onMenu: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        BrowseActionRow(
            countLabel = "${songs.size} songs",
            onPlayAll = onPlayAll,
            onShuffle = onShuffle,
            onSort = { sortMenuOpen = true },
        ) {
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                BrowseSortOption("Title A-Z", SortOrder.TITLE_ASC, sort, onSort) { sortMenuOpen = false }
                BrowseSortOption("Title Z-A", SortOrder.TITLE_DESC, sort, onSort) { sortMenuOpen = false }
                BrowseSortOption("Artist", SortOrder.ARTIST_ASC, sort, onSort) { sortMenuOpen = false }
                BrowseSortOption("Recently added", SortOrder.DATE_ADDED_DESC, sort, onSort) { sortMenuOpen = false }
                BrowseSortOption("Longest", SortOrder.DURATION_DESC, sort, onSort) { sortMenuOpen = false }
            }
        }
        BrowseTrackList(
            songs = songs,
            activeId = activeId,
            contentPadding = contentPadding,
            listState = listState,
            onPlay = onPlay,
            onQueue = onQueue,
            onMenu = onMenu,
        )
    }
}

@Composable
private fun BrowseDatesPage(
    songs: List<Track>,
    activeId: Long?,
    contentPadding: PaddingValues,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onMenu: (Track) -> Unit,
) {
    if (songs.isEmpty()) {
        EmptyState("Nothing here", "No music files were found in your library.")
        return
    }

    val sorted = remember(songs) { songs.sortedByDescending { it.dateAddedSec } }
    val nowSec = System.currentTimeMillis() / 1000
    val todaySec = 86400L
    val weekSec = 7 * 86400L
    val monthSec = 30 * 86400L

    val todaySongs = remember(sorted) { sorted.filter { (nowSec - it.dateAddedSec) <= todaySec } }
    val weekSongs = remember(sorted) { sorted.filter { (nowSec - it.dateAddedSec) in (todaySec + 1)..weekSec } }
    val monthSongs = remember(sorted) { sorted.filter { (nowSec - it.dateAddedSec) in (weekSec + 1)..monthSec } }
    val earlierSongs = remember(sorted) { sorted.filter { (nowSec - it.dateAddedSec) > monthSec } }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 4.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            BrowseActionRow(
                countLabel = "Timeline View",
                onPlayAll = { if (sorted.isNotEmpty()) onPlay(sorted.first()) },
                onShuffle = { if (sorted.isNotEmpty()) onPlay(sorted.shuffled().first()) },
            )
        }

        if (todaySongs.isNotEmpty()) {
            item {
                DateSectionHeader("Added Today")
            }
            items(todaySongs, key = { "today_${it.id}" }) { track ->
                BrowseTrackRow(
                    track = track,
                    isActive = activeId == track.id,
                    subText = formatRelativeTime(track.dateAddedSec),
                    onClick = { onPlay(track) },
                    onQueue = { onQueue(track) },
                    onMenu = { onMenu(track) },
                )
            }
        }

        if (weekSongs.isNotEmpty()) {
            item {
                DateSectionHeader("Added This Week")
            }
            items(weekSongs, key = { "week_${it.id}" }) { track ->
                BrowseTrackRow(
                    track = track,
                    isActive = activeId == track.id,
                    subText = formatRelativeTime(track.dateAddedSec),
                    onClick = { onPlay(track) },
                    onQueue = { onQueue(track) },
                    onMenu = { onMenu(track) },
                )
            }
        }

        if (monthSongs.isNotEmpty()) {
            item {
                DateSectionHeader("Added This Month")
            }
            items(monthSongs, key = { "month_${it.id}" }) { track ->
                BrowseTrackRow(
                    track = track,
                    isActive = activeId == track.id,
                    subText = formatRelativeTime(track.dateAddedSec),
                    onClick = { onPlay(track) },
                    onQueue = { onQueue(track) },
                    onMenu = { onMenu(track) },
                )
            }
        }

        if (earlierSongs.isNotEmpty() || (todaySongs.isEmpty() && weekSongs.isEmpty() && monthSongs.isEmpty())) {
            val list = if (todaySongs.isEmpty() && weekSongs.isEmpty() && monthSongs.isEmpty()) sorted else earlierSongs
            item {
                DateSectionHeader("Earlier")
            }
            items(list, key = { "earlier_${it.id}" }) { track ->
                BrowseTrackRow(
                    track = track,
                    isActive = activeId == track.id,
                    subText = formatRelativeTime(track.dateAddedSec),
                    onClick = { onPlay(track) },
                    onQueue = { onQueue(track) },
                    onMenu = { onMenu(track) },
                )
            }
        }
    }
}

@Composable
private fun DateSectionHeader(title: String) {
    val accent = LocalDynamicColors.current.accent
    Text(
        text = title,
        color = accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun BrowseFilesPage(
    songs: List<Track>,
    contentPadding: PaddingValues,
    onClick: (String, List<Track>) -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val groups = remember(songs) {
        songs
            .groupBy { track ->
                track.data.substringAfterLast('.', "")
                    .ifBlank { track.mimeType.substringAfterLast('/') }
                    .uppercase()
                    .ifBlank { "AUDIO" }
            }
            .map { (format, tracks) -> Triple(format, tracks, tracks.sumOf { it.size }) }
            .sortedWith(
                compareByDescending<Triple<String, List<Track>, Long>> { it.second.size }
                    .thenBy { it.first },
            )
    }
    if (groups.isEmpty()) {
        EmptyState("No audio files", "Scan your library to populate local audio files.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 4.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = "LOCAL AUDIO FILES",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 6.dp),
            )
        }
        items(groups, key = { it.first }) { (format, tracks, bytes) ->
            val iconColor = when (format) {
                "FLAC", "WAV", "ALAC" -> accent
                "MP3", "AAC", "M4A" -> Color(0xFF38BDF8)
                else -> Color(0xFF10B981)
            }
            val qualityBadge = when (format) {
                "FLAC", "WAV", "ALAC" -> "Lossless"
                "MP3", "AAC", "M4A" -> "High Quality"
                else -> "Audio File"
            }
            BrowseGroupCard(
                title = "$format Audio (.$format)",
                subtitle = "$qualityBadge • ${tracks.size} ${if (tracks.size == 1) "file" else "files"}",
                trailing = formatFileSize(bytes),
                icon = Icons.Rounded.MusicNote,
                iconTint = iconColor,
                onClick = { onClick("$format Audio", tracks) },
            )
        }
    }
}

@Composable
private fun BrowseFolderPathsPage(
    folders: List<Folder>,
    contentPadding: PaddingValues,
    onClick: (Folder) -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    if (folders.isEmpty()) {
        EmptyState("No folders", "Scan your library to populate folders.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 4.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = "STORAGE DIRECTORIES",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 6.dp),
            )
        }
        items(folders.sortedBy { it.path.lowercase() }, key = { it.path }) { folder ->
            BrowseGroupCard(
                title = folder.name,
                subtitle = "${folder.trackCount} ${if (folder.trackCount == 1) "audio file" else "audio files"}",
                icon = Icons.Rounded.Folder,
                iconTint = Color(0xFFF59E0B),
                actionLabel = "Open",
                onClick = { onClick(folder) },
            )
        }
    }
}

@Composable
private fun BrowsePlaylistsPage(
    playlists: List<PlaylistEntity>,
    contentPadding: PaddingValues,
    onCreateNew: () -> Unit,
    onClick: (PlaylistEntity) -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Your Playlists",
                color = appColors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            val interaction = remember { MutableInteractionSource() }
            val haptic = rememberHaptic()
            Box(
                modifier = Modifier
                    .instantClickable(pressedScale = 0.92f) {
                        onCreateNew()
                    }
                    .clip(CircleShape)
                    .background(appColors.bgSurface.copy(alpha = 0.90f))
                    .border(1.dp, appColors.borderGlass, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+ New",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (playlists.isEmpty()) {
            EmptyState(
                title = "No playlists",
                subtitle = "Create playlists using the '+ New' button above.",
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 4.dp,
                    end = 20.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                gridItems(playlists, key = { it.id }) { playlist ->
                    val interaction = remember { MutableInteractionSource() }
                    val haptic = rememberHaptic()
                    val cardShape = RoundedCornerShape(20.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .instantClickable(pressedScale = 0.95f) {
                                onClick(playlist)
                            }
                            .clip(cardShape)
                            .background(appColors.bgSurface.copy(alpha = 0.90f))
                            .border(1.dp, appColors.borderGlass, cardShape)
                            .padding(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(playlistPalette(playlist.id))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.PlaylistPlay,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(42.dp),
                            )
                        }
                        Text(
                            text = playlist.name,
                            color = appColors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 9.dp),
                        )
                        Text(
                            text = "Local playlist",
                            color = appColors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseAlbumsPage(
    albums: List<Album>,
    contentPadding: PaddingValues,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onClick: (Album) -> Unit,
) {
    val appColors = LocalAppColors.current
    var descending by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val sortedAlbums = remember(albums, descending) {
        if (descending) {
            albums.sortedByDescending { it.title.lowercase() }
        } else {
            albums.sortedBy { it.title.lowercase() }
        }
    }

    Column(Modifier.fillMaxSize()) {
        BrowseCollectionHeader(
            countLabel = "${albums.size} albums",
            menuOpen = sortMenuOpen,
            onOpenMenu = { sortMenuOpen = true },
            onDismissMenu = { sortMenuOpen = false },
            descending = descending,
            onDescendingChange = { descending = it },
        )
        if (albums.isEmpty()) {
            EmptyState("No albums", "Scan your library to populate albums.")
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 8.dp,
                    end = 20.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                gridItems(sortedAlbums, key = { it.id }) { album ->
                    val interaction = remember { MutableInteractionSource() }
                    val haptic = rememberHaptic()
                    val cardShape = RoundedCornerShape(20.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .instantClickable(pressedScale = 0.95f) {
                                onClick(album)
                            }
                            .clip(cardShape)
                            .background(appColors.bgSurface.copy(alpha = 0.90f))
                            .border(1.dp, appColors.borderGlass, cardShape)
                            .padding(12.dp),
                    ) {
                        AlbumArt(
                            albumId = album.id,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        Text(
                            text = album.title,
                            color = appColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            text = album.artist,
                            color = appColors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseArtistsPage(
    artists: List<Artist>,
    contentPadding: PaddingValues,
    onClick: (Artist) -> Unit,
) {
    val appColors = LocalAppColors.current
    if (artists.isEmpty()) {
        EmptyState("No artists", "Scan your library to populate artists.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 4.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(artists, key = { it.name }) { artist ->
            val interaction = remember { MutableInteractionSource() }
            val haptic = rememberHaptic()
            val rowShape = RoundedCornerShape(18.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .instantClickable(pressedScale = 0.96f) {
                        onClick(artist)
                    }
                    .clip(rowShape)
                    .background(appColors.bgSurface.copy(alpha = 0.90f))
                    .border(1.dp, appColors.borderGlass, rowShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(artistAvatarPalette(artist.name))),
                    contentAlignment = Alignment.Center,
                ) {
                    AlbumArt(
                        albumId = artist.representativeAlbumId,
                        artworkUri = artist.artworkUri,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = artist.name,
                        color = appColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${artist.trackCount} ${if (artist.trackCount == 1) "Song" else "Songs"} • Local",
                        color = appColors.textSecondary,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = appColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun BrowseGenresPage(
    genres: List<GenreGroup>,
    contentPadding: PaddingValues,
    onClick: (GenreGroup) -> Unit,
) {
    if (genres.isEmpty()) {
        EmptyState("No genres", "Genre metadata was not found in your current songs.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 8.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItems(genres, key = { it.name }) { genre ->
            val appColors = LocalAppColors.current
            val cardShape = RoundedCornerShape(20.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .instantClickable(pressedScale = 0.95f) {
                        onClick(genre)
                    }
                    .clip(cardShape)
                    .background(Brush.linearGradient(genrePalette(genre.name)))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), cardShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = genre.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${genre.tracks.size} ${if (genre.tracks.size == 1) "track" else "tracks"}",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun BrowseCollectionHeader(
    countLabel: String,
    menuOpen: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    descending: Boolean,
    onDescendingChange: (Boolean) -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = countLabel,
            color = appColors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
        )
        Box {
            BrowseActionButton(Icons.AutoMirrored.Rounded.Sort, "Sort") { onOpenMenu() }
            DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text("Title A-Z") },
                    onClick = {
                        onDescendingChange(false)
                        onDismissMenu()
                    },
                    trailingIcon = {
                        if (!descending) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = accent,
                            )
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text("Title Z-A") },
                    onClick = {
                        onDescendingChange(true)
                        onDismissMenu()
                    },
                    trailingIcon = {
                        if (descending) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = accent,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BrowseDetailPage(
    title: String,
    tracks: List<Track>,
    activeId: Long?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onMenu: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val accent = com.gorilla.music.ui.theme.LocalDynamicColors.current.accent
    val backPillColor = appColors.bgSurface.copy(alpha = 0.94f)

    Column(Modifier.fillMaxSize()) {
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
                        text = "BROWSE",
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
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .instantClickable(pressedScale = 0.90f) { onBack() }
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
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        BrowseActionRow(
            countLabel = "${tracks.size} songs",
            onPlayAll = onPlayAll,
            onShuffle = onShuffle,
        )
        BrowseTrackList(
            songs = tracks,
            activeId = activeId,
            contentPadding = contentPadding,
            listState = rememberLazyListState(),
            onPlay = onPlay,
            onQueue = onQueue,
            onMenu = onMenu,
        )
    }
}

@Composable
private fun BrowseTrackList(
    songs: List<Track>,
    activeId: Long?,
    contentPadding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPlay: (Track) -> Unit,
    onQueue: (Track) -> Unit,
    onMenu: (Track) -> Unit,
) {
    if (songs.isEmpty()) {
        EmptyState("No songs", "Scan your library from Settings > About.")
        return
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 4.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(songs, key = { it.id }) { track ->
            BrowseTrackRow(
                track = track,
                isActive = activeId == track.id,
                onClick = { onPlay(track) },
                onQueue = { onQueue(track) },
                onMenu = { onMenu(track) },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BrowseTrackRow(
    track: Track,
    isActive: Boolean,
    subText: String? = null,
    onClick: () -> Unit,
    onQueue: () -> Unit,
    onMenu: (Track) -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val rowShape = RoundedCornerShape(16.dp)
    val rowColor = appColors.songCardColor()
    val background = if (isActive) {
        Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), rowColor))
    } else {
        Brush.linearGradient(listOf(rowColor, rowColor))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .instantClickable(pressedScale = 0.96f) {
                onClick()
            }
            .clip(rowShape)
            .background(background)
            .border(
                1.dp,
                if (isActive) accent.copy(alpha = 0.42f) else appColors.borderGlass,
                rowShape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            albumId = track.albumId,
            artworkUri = track.artworkUri,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(52.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 4.dp),
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
                text = subText ?: if (track.durationMs > 0) "${track.displayArtist} • ${formatDuration(track.durationMs)}" else track.displayArtist,
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
                    onMenu(track)
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
private fun BrowseGroupCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color? = null,
    trailing: String? = null,
    actionLabel: String? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val accent = LocalDynamicColors.current.accent
    val appColors = LocalAppColors.current
    val cardShape = RoundedCornerShape(18.dp)
    val tint = iconTint ?: accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .instantClickable(pressedScale = 0.96f) {
                onClick()
            }
            .clip(cardShape)
            .background(appColors.bgSurface.copy(alpha = 0.90f))
            .border(1.dp, appColors.borderGlass, cardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(iconShape)
                .background(appColors.bgGlass)
                .border(1.dp, appColors.borderGlass, iconShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = title,
                color = appColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = appColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        } else if (trailing != null) {
            Text(
                text = trailing,
                color = appColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
            )
        } else {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = appColors.textSecondary,
            )
        }
    }
}

private fun genrePalette(name: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFFFF007A), Color(0xFF7928CA)),
        listOf(Color(0xFF0070F3), Color(0xFF00DFD8)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
        listOf(Color(0xFF10B981), Color(0xFF059669)),
        listOf(Color(0xFF7C3AED), Color(0xFFDB2777)),
    )
    val index = (name.hashCode().toLong() and Long.MAX_VALUE) % palettes.size
    return palettes[index.toInt()]
}

private fun playlistPalette(id: Long): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFFFF007A), Color(0xFF7928CA)),
        listOf(Color(0xFF0070F3), Color(0xFF00DFD8)),
        listOf(Color(0xFF10B981), Color(0xFF059669)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    )
    return palettes[((id and Long.MAX_VALUE) % palettes.size).toInt()]
}

private fun artistAvatarPalette(name: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)),
        listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
        listOf(Color(0xFF10B981), Color(0xFF059669)),
        listOf(Color(0xFFFF007A), Color(0xFF7928CA)),
    )
    val index = (name.hashCode().toLong() and Long.MAX_VALUE) % palettes.size
    return palettes[index.toInt()]
}

@Composable
private fun BrowseActionRow(
    countLabel: String,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSort: (() -> Unit)? = null,
    menu: @Composable () -> Unit = {},
) {
    val appColors = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = countLabel,
                color = appColors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            BrowseActionButton(Icons.Rounded.PlayArrow, "Play all", onPlayAll)
            BrowseActionButton(Icons.Rounded.Shuffle, "Shuffle", onShuffle)
            if (onSort != null) {
                Box {
                    BrowseActionButton(Icons.AutoMirrored.Rounded.Sort, "Sort", onSort)
                    menu()
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(1.dp)
                .background(appColors.borderGlass),
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun BrowseActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(36.dp)
            .instantClickable(pressedScale = 0.90f) {
                onClick()
            }
            .clip(CircleShape)
            .background(appColors.bgSurface.copy(alpha = 0.94f))
            .border(1.dp, appColors.borderGlass, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = appColors.textPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun BrowseSortOption(
    label: String,
    order: SortOrder,
    current: SortOrder,
    onSort: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = LocalDynamicColors.current.accent
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = if (order == current) accent else MaterialTheme.colorScheme.onSurface,
            )
        },
        onClick = {
            onSort(order)
            onDismiss()
        },
    )
}

private fun formatRelativeTime(dateSec: Long): String {
    if (dateSec <= 0) return "Recently added"
    val millis = dateSec * 1000
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < TimeUnit.HOURS.toMillis(24) -> "Today " + SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(Date(millis))
        diff < TimeUnit.DAYS.toMillis(7) -> "${diff / TimeUnit.DAYS.toMillis(1)} days ago"
        diff < TimeUnit.DAYS.toMillis(30) -> "${diff / TimeUnit.DAYS.toMillis(7)} weeks ago"
        else -> SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(Date(millis))
    }
}
